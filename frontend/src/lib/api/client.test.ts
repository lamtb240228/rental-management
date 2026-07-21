import {
  AxiosError,
  type AxiosAdapter,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApplicationError } from "./ApplicationError";
import type { ApiResponse, AuthResponse } from "./types";
import {
  apiClient,
  authSessionClient,
  clearLegacyBrowserTokens,
  establishAccessSession,
  getAccessToken,
  invalidateAccessSession,
  LEGACY_TOKEN_KEY,
  refreshAccessToken,
  subscribeToUnauthorized,
} from "./client";

const user = {
  id: 2,
  email: "account@example.test",
  fullName: "Account",
  roles: ["LANDLORD"],
};

const originalAuthAdapter = authSessionClient.defaults.adapter;
const originalLocks = navigator.locks;

function authResponse(accessToken = "refreshed-access"): AuthResponse {
  return { accessToken, tokenType: "Bearer", user };
}

function response<T>(config: InternalAxiosRequestConfig, data: T, status = 200): AxiosResponse<T> {
  return {
    config,
    data,
    headers: {},
    status,
    statusText: status === 200 ? "OK" : "Unauthorized",
  };
}

function unauthorized(config: InternalAxiosRequestConfig) {
  return new AxiosError(
    "Unauthorized",
    AxiosError.ERR_BAD_REQUEST,
    config,
    undefined,
    response(config, null, 401),
  );
}

function setRefreshAdapter(
  handler: (config: InternalAxiosRequestConfig) => Promise<AuthResponse> | AuthResponse,
) {
  authSessionClient.defaults.adapter = async (config) => {
    const result = await handler(config);
    return response<ApiResponse<AuthResponse>>(config, { data: result });
  };
}

describe("apiClient hardened session handling", () => {
  beforeEach(() => {
    invalidateAccessSession();
    window.localStorage.clear();
    window.sessionStorage.clear();
  });

  afterEach(() => {
    invalidateAccessSession();
    authSessionClient.defaults.adapter = originalAuthAdapter;
    Object.defineProperty(navigator, "locks", {
      configurable: true,
      value: originalLocks,
    });
    vi.restoreAllMocks();
  });

  it("keeps access tokens in memory and removes legacy browser-storage tokens", () => {
    window.localStorage.setItem(LEGACY_TOKEN_KEY, "legacy-local-token");
    window.sessionStorage.setItem(LEGACY_TOKEN_KEY, "legacy-session-token");

    clearLegacyBrowserTokens();
    establishAccessSession("memory-only-token");

    expect(getAccessToken()).toBe("memory-only-token");
    expect(window.localStorage.getItem(LEGACY_TOKEN_KEY)).toBeNull();
    expect(window.sessionStorage.getItem(LEGACY_TOKEN_KEY)).toBeNull();
    expect(Object.values(window.localStorage)).not.toContain("memory-only-token");
    expect(Object.values(window.sessionStorage)).not.toContain("memory-only-token");
  });

  it("uses credentialed clients and does not attach bearer tokens to credential endpoints", async () => {
    establishAccessSession("current-access");
    expect(apiClient.defaults.withCredentials).toBe(true);
    expect(authSessionClient.defaults.withCredentials).toBe(true);

    const adapter: AxiosAdapter = async (config) => {
      expect(config.headers.Authorization).toBeUndefined();
      throw unauthorized(config);
    };

    await expect(apiClient.post("/auth/login", {}, { adapter })).rejects.toBeInstanceOf(ApplicationError);
    expect(getAccessToken()).toBe("current-access");
  });

  it("refreshes after a 401 and retries the request once with the new access token", async () => {
    establishAccessSession("expired-access");
    let refreshCalls = 0;
    const requestTokens: unknown[] = [];
    setRefreshAdapter(() => {
      refreshCalls += 1;
      return authResponse();
    });

    const adapter: AxiosAdapter = async (config) => {
      requestTokens.push(config.headers.Authorization);
      if (config.headers.Authorization === "Bearer expired-access") {
        throw unauthorized(config);
      }
      return response(config, { ok: true });
    };

    await expect(apiClient.get("/protected", { adapter })).resolves.toMatchObject({ status: 200 });
    expect(refreshCalls).toBe(1);
    expect(requestTokens).toEqual(["Bearer expired-access", "Bearer refreshed-access"]);
    expect(getAccessToken()).toBe("refreshed-access");
  });

  it("shares one refresh promise among concurrent 401 responses", async () => {
    establishAccessSession("expired-access");
    let refreshCalls = 0;
    let releaseRefresh!: () => void;
    const refreshGate = new Promise<void>((resolve) => {
      releaseRefresh = resolve;
    });
    setRefreshAdapter(async () => {
      refreshCalls += 1;
      await refreshGate;
      return authResponse();
    });

    const adapter: AxiosAdapter = async (config) => {
      if (config.headers.Authorization === "Bearer expired-access") {
        throw unauthorized(config);
      }
      return response(config, { ok: true });
    };

    const requests = [1, 2, 3].map((id) => apiClient.get(`/protected/${id}`, { adapter }));
    await vi.waitFor(() => expect(refreshCalls).toBe(1));
    releaseRefresh();

    await expect(Promise.all(requests)).resolves.toHaveLength(3);
    expect(refreshCalls).toBe(1);
  });

  it("does not enter a refresh loop when the retried request is also unauthorized", async () => {
    establishAccessSession("expired-access");
    const listener = vi.fn();
    const unsubscribe = subscribeToUnauthorized(listener);
    let refreshCalls = 0;
    let protectedCalls = 0;
    setRefreshAdapter(() => {
      refreshCalls += 1;
      return authResponse();
    });

    const adapter: AxiosAdapter = async (config) => {
      protectedCalls += 1;
      throw unauthorized(config);
    };

    await expect(apiClient.get("/protected", { adapter })).rejects.toBeInstanceOf(ApplicationError);

    expect(refreshCalls).toBe(1);
    expect(protectedCalls).toBe(2);
    expect(getAccessToken()).toBeNull();
    expect(listener).toHaveBeenCalledOnce();
    unsubscribe();
  });

  it("logs out once when refresh fails", async () => {
    establishAccessSession("expired-access");
    const listener = vi.fn();
    const unsubscribe = subscribeToUnauthorized(listener);
    let refreshCalls = 0;
    authSessionClient.defaults.adapter = async (config) => {
      refreshCalls += 1;
      throw unauthorized(config);
    };

    const adapter: AxiosAdapter = async (config) => {
      throw unauthorized(config);
    };

    await expect(apiClient.get("/protected", { adapter })).rejects.toBeInstanceOf(ApplicationError);
    expect(refreshCalls).toBe(1);
    expect(getAccessToken()).toBeNull();
    expect(listener).toHaveBeenCalledOnce();
    unsubscribe();
  });

  it("does not let a late refresh restore a session invalidated by logout", async () => {
    establishAccessSession("expired-access");
    let refreshCalls = 0;
    let releaseRefresh!: () => void;
    const refreshGate = new Promise<void>((resolve) => {
      releaseRefresh = resolve;
    });
    setRefreshAdapter(async () => {
      refreshCalls += 1;
      await refreshGate;
      return authResponse("late-access");
    });

    const adapter: AxiosAdapter = async (config) => {
      throw unauthorized(config);
    };

    const request = apiClient.get("/protected", { adapter });
    await vi.waitFor(() => expect(refreshCalls).toBe(1));
    invalidateAccessSession();
    releaseRefresh();

    await expect(request).rejects.toBeInstanceOf(ApplicationError);
    expect(getAccessToken()).toBeNull();
  });

  it("does not clear a newer identity because of a late 401", async () => {
    establishAccessSession("old-access");
    const listener = vi.fn();
    const unsubscribe = subscribeToUnauthorized(listener);

    const adapter: AxiosAdapter = async (config) => {
      establishAccessSession("new-access");
      throw unauthorized(config);
    };

    await expect(apiClient.get("/protected", { adapter })).rejects.toBeInstanceOf(ApplicationError);
    expect(getAccessToken()).toBe("new-access");
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });

  it("uses the Web Locks refresh mutex when the browser provides it", async () => {
    const requestLock = vi.fn(async (
      _name: string,
      _options: LockOptions,
      callback: () => Promise<AuthResponse>,
    ) => callback());
    Object.defineProperty(navigator, "locks", {
      configurable: true,
      value: { request: requestLock },
    });
    setRefreshAdapter(() => authResponse());

    await refreshAccessToken();

    expect(requestLock).toHaveBeenCalledOnce();
    expect(requestLock.mock.calls[0][0]).toBe("rental-management-auth-refresh");
    expect(requestLock.mock.calls[0][1]).toMatchObject({ mode: "exclusive" });
  });

  it("never exposes or serializes Axios config, bearer tokens, or login credentials", async () => {
    const accessSecret = "access-secret-that-must-not-escape";
    const passwordSecret = "password-secret-that-must-not-escape";
    establishAccessSession(accessSecret);

    const adapter: AxiosAdapter = async (config) => {
      const isLogin = config.url === "/auth/login";
      const status = isLogin ? 401 : 500;
      throw new AxiosError(
        "Raw Axios failure",
        AxiosError.ERR_BAD_REQUEST,
        config,
        undefined,
        response(
          config,
          { message: isLogin ? "Email or password is incorrect" : "Unexpected server error" },
          status,
        ),
      );
    };

    const loginError = await apiClient
      .post("/auth/login", { email: "person@example.test", password: passwordSecret }, { adapter })
      .catch((error: unknown) => error);
    const protectedError = await apiClient
      .get("/protected", { adapter })
      .catch((error: unknown) => error);

    expect(loginError).toBeInstanceOf(ApplicationError);
    expect(protectedError).toBeInstanceOf(ApplicationError);
    expect(loginError).toMatchObject({
      status: 401,
      code: "ERR_BAD_REQUEST",
      message: "Email or password is incorrect",
      requestId: null,
    });

    const serialized = JSON.stringify({ loginError, protectedError });
    expect(serialized).not.toContain(accessSecret);
    expect(serialized).not.toContain(passwordSecret);
    expect(serialized).not.toContain("person@example.test");
    expect(serialized).not.toContain("Authorization");
    expect(serialized).not.toContain("_sessionToken");
    expect(serialized).not.toContain("config");
    expect((loginError as { config?: unknown }).config).toBeUndefined();
    expect((protectedError as { cause?: unknown }).cause).toBeUndefined();
    expect(JSON.parse(JSON.stringify(loginError))).toEqual({
      status: 401,
      code: "ERR_BAD_REQUEST",
      message: "Email or password is incorrect",
      requestId: null,
    });
  });
});
