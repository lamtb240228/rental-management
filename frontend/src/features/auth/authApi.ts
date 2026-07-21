import { apiClient, authSessionClient, getSessionEpoch } from "../../lib/api/client";
import { toApplicationError } from "../../lib/api/ApplicationError";
import { runWithAuthSessionLock } from "../../lib/api/authRequestCoordinator";
import type { ApiResponse, AuthResponse, UserProfile } from "../../lib/api/types";
import { clearLogoutPending, markLogoutPending } from "./logoutPending";
import {
  clearSessionGeneration,
  createSessionGeneration,
  getSessionGeneration,
  setSessionGeneration,
} from "./sessionGeneration";

const LOGOUT_TIMEOUT_MS = 10_000;

type ActiveLogoutRequest = {
  promise: Promise<void>;
};

let activeLogoutRequest: ActiveLogoutRequest | null = null;

export type LoginPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = LoginPayload & {
  fullName: string;
  phone?: string;
};

export async function login(payload: LoginPayload) {
  await settleActiveLogoutBeforeExplicitAuth();
  const expectedEpoch = getSessionEpoch();
  try {
    const authenticated = await runWithAuthSessionLock(async () => {
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/login", payload);
      return response.data.data;
    });
    if (getSessionEpoch() !== expectedEpoch) {
      await quarantineSupersededAuthentication();
      throw new Error("Authentication response was superseded by a session transition");
    }
    return authenticated;
  } catch (error) {
    throw toApplicationError(error);
  }
}

export async function register(payload: RegisterPayload) {
  await settleActiveLogoutBeforeExplicitAuth();
  const expectedEpoch = getSessionEpoch();
  try {
    const authenticated = await runWithAuthSessionLock(async () => {
      const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/register", payload);
      return response.data.data;
    });
    if (getSessionEpoch() !== expectedEpoch) {
      await quarantineSupersededAuthentication();
      throw new Error("Authentication response was superseded by a session transition");
    }
    return authenticated;
  } catch (error) {
    throw toApplicationError(error);
  }
}

export async function me() {
  const response = await apiClient.get<ApiResponse<UserProfile>>("/auth/me");
  return response.data.data;
}

export async function logout(signal?: AbortSignal) {
  if (activeLogoutRequest) {
    return activeLogoutRequest.promise;
  }

  const controller = new AbortController();
  const forwardAbort = () => controller.abort();
  if (signal?.aborted) {
    controller.abort();
  } else {
    signal?.addEventListener("abort", forwardAbort, { once: true });
  }
  const timeoutId = globalThis.setTimeout(() => controller.abort(), LOGOUT_TIMEOUT_MS);
  const promise = runWithAuthSessionLock(async () => {
    await authSessionClient.post("/auth/logout", undefined, { signal: controller.signal });
  }, controller.signal)
    .catch((error: unknown) => Promise.reject(toApplicationError(error)))
    .finally(() => {
      globalThis.clearTimeout(timeoutId);
      signal?.removeEventListener("abort", forwardAbort);
      if (activeLogoutRequest?.promise === promise) {
        activeLogoutRequest = null;
      }
    });

  activeLogoutRequest = { promise };
  return promise;
}

export async function logoutAll() {
  await settleActiveLogoutBeforeExplicitAuth();
  try {
    await runWithAuthSessionLock(async () => {
      await authSessionClient.post("/auth/logout-all");
    });
  } catch (error) {
    throw toApplicationError(error);
  }
}

async function settleActiveLogoutBeforeExplicitAuth() {
  const active = activeLogoutRequest;
  if (!active) {
    return;
  }

  // Wait for the prior response (bounded by logout's timeout) before sending a
  // new authentication request so an older Set-Cookie cannot arrive last.
  try {
    await active.promise;
  } catch {
    // A failed or timed-out logout leaves the pending tombstone in place until
    // the successful explicit authentication callback clears it.
  }
}

async function quarantineSupersededAuthentication() {
  const generation = getSessionGeneration() ?? createSessionGeneration();
  setSessionGeneration(generation);
  markLogoutPending(generation);
  try {
    await logout();
    clearLogoutPending(generation);
    clearSessionGeneration(generation);
  } catch {
    // The non-credential tombstone prevents a late Set-Cookie response from
    // restoring the session. Startup/online retry will finish server cleanup.
  }
}
