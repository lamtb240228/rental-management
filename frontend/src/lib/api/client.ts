import axios, { type InternalAxiosRequestConfig } from "axios";
import type { ApiResponse, AuthResponse } from "./types";

export const LEGACY_TOKEN_KEY = "rental_access_token";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";
const REFRESH_LOCK_NAME = "rental-management-auth-refresh";
const unauthorizedListeners = new Set<() => void>();

type SessionRequestConfig = InternalAxiosRequestConfig & {
  _sessionEpoch?: number;
  _sessionRetry?: boolean;
  _sessionToken?: string;
};

type ActiveRefresh = {
  epoch: number;
  controller: AbortController;
  promise: Promise<AuthResponse>;
};

class SessionSupersededError extends Error {
  constructor() {
    super("Authentication session changed while refreshing");
    this.name = "SessionSupersededError";
  }
}

let accessToken: string | null = null;
let sessionEpoch = 0;
let activeRefresh: ActiveRefresh | null = null;

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

// Refresh/logout deliberately bypass the normal response interceptor so a
// rejected refresh can never recursively start another refresh.
export const authSessionClient = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

clearLegacyBrowserTokens();

apiClient.interceptors.request.use((config) => {
  const sessionConfig = config as SessionRequestConfig;
  const token = getAccessToken();

  sessionConfig._sessionEpoch ??= getSessionEpoch();
  if (token && !isCredentialEndpoint(config.url)) {
    sessionConfig._sessionToken = token;
    config.headers.Authorization = `Bearer ${token}`;
  } else {
    delete config.headers.Authorization;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401 || !error.config) {
      return Promise.reject(error);
    }

    const config = error.config as SessionRequestConfig;
    const requestEpoch = config._sessionEpoch;
    const requestToken = config._sessionToken;

    // Login/register/refresh/logout failures are terminal and must never enter
    // the access-token refresh interceptor.
    if (isCredentialEndpoint(config.url) || requestEpoch === undefined || !requestToken) {
      return Promise.reject(error);
    }

    // A response from an older identity must not modify or retry in the newer
    // identity's session.
    if (requestEpoch !== getSessionEpoch()) {
      return Promise.reject(error);
    }

    if (config._sessionRetry) {
      expireSession(requestEpoch);
      return Promise.reject(error);
    }

    try {
      // Another 401 may arrive just after the shared refresh completed. Reuse
      // that newer in-memory token instead of rotating the cookie again.
      let refreshedToken = getAccessToken();
      if (!refreshedToken || refreshedToken === requestToken) {
        const refreshedSession = await refreshAccessToken(requestEpoch);
        refreshedToken = refreshedSession.accessToken;
      }

      if (requestEpoch !== getSessionEpoch() || getAccessToken() !== refreshedToken) {
        throw new SessionSupersededError();
      }

      config._sessionRetry = true;
      config._sessionToken = refreshedToken;
      config.headers.Authorization = `Bearer ${refreshedToken}`;
      return apiClient.request(config);
    } catch {
      if (requestEpoch === getSessionEpoch()) {
        expireSession(requestEpoch);
      }

      // Keep Axios' original 401 contract for callers. Refresh failures remain
      // internal session-management details.
      return Promise.reject(error);
    }
  },
);

export function getAccessToken() {
  return accessToken;
}

export function getSessionEpoch() {
  return sessionEpoch;
}

/** Starts a new explicit login/register identity. */
export function establishAccessSession(token: string) {
  abortActiveRefresh();
  sessionEpoch += 1;
  accessToken = token;
  return sessionEpoch;
}

/** Invalidates local credentials before logout or an account switch. */
export function invalidateAccessSession() {
  abortActiveRefresh();
  sessionEpoch += 1;
  accessToken = null;
  return sessionEpoch;
}

export async function refreshAccessToken(expectedEpoch = getSessionEpoch()): Promise<AuthResponse> {
  if (expectedEpoch !== getSessionEpoch()) {
    throw new SessionSupersededError();
  }

  if (activeRefresh) {
    if (activeRefresh.epoch === expectedEpoch) {
      return activeRefresh.promise;
    }

    // Never run two refresh requests in the same tab, even while an old epoch
    // is being aborted during logout/account switching.
    try {
      await activeRefresh.promise;
    } catch {
      // The old session's outcome is intentionally irrelevant to the caller.
    }
    return refreshAccessToken(expectedEpoch);
  }

  const controller = new AbortController();
  const promise = runWithCrossTabRefreshLock(async () => {
    assertCurrentEpoch(expectedEpoch);
    const response = await authSessionClient.post<ApiResponse<AuthResponse>>(
      "/auth/refresh",
      undefined,
      { signal: controller.signal },
    );
    assertCurrentEpoch(expectedEpoch);
    accessToken = response.data.data.accessToken;
    return response.data.data;
  }).finally(() => {
    if (activeRefresh?.promise === promise) {
      activeRefresh = null;
    }
  });

  activeRefresh = { epoch: expectedEpoch, controller, promise };
  return promise;
}

export function subscribeToUnauthorized(listener: () => void) {
  unauthorizedListeners.add(listener);
  return () => {
    unauthorizedListeners.delete(listener);
  };
}

/** Removes the access-token format used before refresh sessions were added. */
export function clearLegacyBrowserTokens() {
  if (typeof window === "undefined") {
    return;
  }

  try {
    window.localStorage.removeItem(LEGACY_TOKEN_KEY);
    window.sessionStorage.removeItem(LEGACY_TOKEN_KEY);
  } catch {
    // Storage may be disabled by the browser. Authentication still works
    // because current credentials are held only in memory/cookies.
  }
}

function expireSession(expectedEpoch: number) {
  if (expectedEpoch !== getSessionEpoch()) {
    return;
  }

  invalidateAccessSession();
  unauthorizedListeners.forEach((listener) => listener());
}

function abortActiveRefresh() {
  activeRefresh?.controller.abort();
}

function assertCurrentEpoch(expectedEpoch: number) {
  if (expectedEpoch !== getSessionEpoch()) {
    throw new SessionSupersededError();
  }
}

function isCredentialEndpoint(url?: string) {
  if (!url) {
    return false;
  }

  const pathname = url.split("?", 1)[0].replace(/\/$/, "");
  return [
    "/auth/login",
    "/auth/register",
    "/auth/refresh",
    "/auth/logout",
    "/auth/logout-all",
  ].some((path) => pathname.endsWith(path));
}

async function runWithCrossTabRefreshLock<T>(callback: () => Promise<T>): Promise<T> {
  if (typeof navigator !== "undefined" && navigator.locks?.request) {
    return navigator.locks.request(REFRESH_LOCK_NAME, { mode: "exclusive" }, callback);
  }

  // Older browsers retain the per-tab single-flight guarantee. The backend's
  // transactional row lock remains the final concurrency guard across tabs.
  return callback();
}
