import {
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  apiClient,
  authSessionClient,
  invalidateAccessSession,
} from "../../lib/api/client";
import type { ApiResponse, AuthResponse } from "../../lib/api/types";
import { login, logout } from "./authApi";

const originalApiAdapter = apiClient.defaults.adapter;
const originalAuthAdapter = authSessionClient.defaults.adapter;
const originalLocks = navigator.locks;

const authResponse: AuthResponse = {
  accessToken: "new-login-access",
  tokenType: "Bearer",
  user: {
    id: 20,
    email: "new-login@example.test",
    fullName: "New Login",
    roles: ["LANDLORD"],
  },
};

function response<T>(config: InternalAxiosRequestConfig, data: T, status = 200): AxiosResponse<T> {
  return {
    config,
    data,
    headers: {},
    status,
    statusText: status === 200 ? "OK" : "No Content",
  };
}

describe("auth request ordering", () => {
  beforeEach(() => {
    Object.defineProperty(navigator, "locks", {
      configurable: true,
      value: undefined,
    });
  });

  afterEach(() => {
    apiClient.defaults.adapter = originalApiAdapter;
    authSessionClient.defaults.adapter = originalAuthAdapter;
    Object.defineProperty(navigator, "locks", {
      configurable: true,
      value: originalLocks,
    });
    vi.restoreAllMocks();
  });

  it("does not dispatch a new login until the obsolete logout request settles", async () => {
    let logoutCalls = 0;
    let loginCalls = 0;
    let logoutAborted = false;
    let releaseLogout!: () => void;
    const logoutGate = new Promise<void>((resolve) => {
      releaseLogout = resolve;
    });
    authSessionClient.defaults.adapter = async (config) => {
      logoutCalls += 1;
      config.signal?.addEventListener?.("abort", () => {
        logoutAborted = true;
      });
      await logoutGate;
      return response(config, undefined, 204);
    };
    apiClient.defaults.adapter = async (config) => {
      loginCalls += 1;
      return response<ApiResponse<AuthResponse>>(config, { data: authResponse });
    };

    const obsoleteLogout = logout().catch((error: unknown) => error);
    await vi.waitFor(() => expect(logoutCalls).toBe(1));

    const newLogin = login({
      email: "new-login@example.test",
      password: "correct-password",
    });
    await Promise.resolve();
    expect(loginCalls).toBe(0);
    expect(logoutAborted).toBe(false);

    releaseLogout();
    await obsoleteLogout;
    await expect(newLogin).resolves.toEqual(authResponse);
    expect(loginCalls).toBe(1);
  });

  it("rejects and clears a login response superseded by a later logout transition", async () => {
    let loginCalls = 0;
    let cleanupLogoutCalls = 0;
    let releaseLogin!: () => void;
    const loginGate = new Promise<void>((resolve) => {
      releaseLogin = resolve;
    });
    apiClient.defaults.adapter = async (config) => {
      loginCalls += 1;
      await loginGate;
      return response<ApiResponse<AuthResponse>>(config, { data: authResponse });
    };
    authSessionClient.defaults.adapter = async (config) => {
      cleanupLogoutCalls += 1;
      return response(config, undefined, 204);
    };

    const supersededLogin = login({
      email: "new-login@example.test",
      password: "correct-password",
    });
    await vi.waitFor(() => expect(loginCalls).toBe(1));

    invalidateAccessSession();
    releaseLogin();

    await expect(supersededLogin).rejects.toMatchObject({
      code: "REQUEST_FAILED",
      status: null,
    });
    expect(cleanupLogoutCalls).toBe(1);
  });
});
