import { QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  authSessionClient,
  getAccessToken,
  invalidateAccessSession,
  LEGACY_TOKEN_KEY,
} from "../../lib/api/client";
import type { ApiResponse, AuthResponse } from "../../lib/api/types";
import { queryClient } from "../../lib/query-client/queryClient";
import { AuthProvider, useAuth } from "./AuthProvider";
import {
  clearLogoutPending,
  isLogoutPending,
  LOGOUT_PENDING_COOKIE_NAME,
  LOGOUT_PENDING_STORAGE_KEY,
  markLogoutPending,
} from "./logoutPending";
import { SESSION_SIGNAL_STORAGE_KEY, type SessionSignal } from "./sessionChannel";
import {
  clearSessionGeneration,
  getSessionGeneration,
  setSessionGeneration,
} from "./sessionGeneration";

const GENERATION_A = "generation-a-123456";

const accountA: AuthResponse = {
  accessToken: "account-a-access",
  tokenType: "Bearer",
  user: {
    id: 1,
    email: "account-a@example.test",
    fullName: "Account A",
    roles: ["LANDLORD"],
  },
};

const accountB: AuthResponse = {
  accessToken: "account-b-access",
  tokenType: "Bearer",
  user: {
    id: 2,
    email: "account-b@example.test",
    fullName: "Account B",
    roles: ["LANDLORD"],
  },
};

const originalAuthAdapter = authSessionClient.defaults.adapter;
const originalBroadcastChannel = globalThis.BroadcastChannel;

function apiResponse(config: InternalAxiosRequestConfig, auth: AuthResponse) {
  return {
    config,
    data: { data: auth } satisfies ApiResponse<AuthResponse>,
    headers: {},
    status: 200,
    statusText: "OK",
  };
}

function noContent(config: InternalAxiosRequestConfig) {
  return {
    config,
    data: undefined,
    headers: {},
    status: 204,
    statusText: "No Content",
  };
}

function unauthorized(config: InternalAxiosRequestConfig) {
  return new AxiosError(
    "Unauthorized",
    AxiosError.ERR_BAD_REQUEST,
    config,
    undefined,
    {
      config,
      data: null,
      headers: {},
      status: 401,
      statusText: "Unauthorized",
    },
  );
}

function SessionProbe() {
  const { isLoading, signIn, signOut, user } = useAuth();

  return (
    <div>
      <span>{isLoading ? "restoring" : user?.email ?? "anonymous"}</span>
      <button type="button" onClick={() => signIn(accountB)}>Đổi tài khoản</button>
      <button type="button" onClick={() => void signOut()}>Đăng xuất</button>
    </div>
  );
}

function renderProvider() {
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <SessionProbe />
      </AuthProvider>
    </QueryClientProvider>,
  );
}

function dispatchSignal(
  kind: SessionSignal["kind"],
  generation = getSessionGeneration() ?? "generation-remote-123456",
) {
  const signal: SessionSignal = {
    id: `signal-${kind}`,
    generation,
    kind,
    source: "another-tab",
    issuedAt: Date.now(),
  };
  window.dispatchEvent(new StorageEvent("storage", {
    key: SESSION_SIGNAL_STORAGE_KEY,
    newValue: JSON.stringify(signal),
  }));
}

describe("AuthProvider refresh sessions", () => {
  beforeEach(() => {
    invalidateAccessSession();
    queryClient.clear();
    window.localStorage.clear();
    window.sessionStorage.clear();
    clearLogoutPending();
    clearSessionGeneration();
    // Exercise the production storage-event fallback deterministically. Its
    // payload contains signals only; access credentials remain in memory.
    Object.defineProperty(globalThis, "BroadcastChannel", {
      configurable: true,
      value: undefined,
    });
  });

  afterEach(() => {
    cleanup();
    invalidateAccessSession();
    clearLogoutPending();
    clearSessionGeneration();
    queryClient.clear();
    authSessionClient.defaults.adapter = originalAuthAdapter;
    Object.defineProperty(globalThis, "BroadcastChannel", {
      configurable: true,
      value: originalBroadcastChannel,
    });
    vi.restoreAllMocks();
  });

  it("keeps the guard loading until startup refresh restores the user", async () => {
    let releaseRefresh!: () => void;
    const gate = new Promise<void>((resolve) => {
      releaseRefresh = resolve;
    });
    authSessionClient.defaults.adapter = async (config) => {
      await gate;
      return apiResponse(config, accountA);
    };

    renderProvider();
    expect(screen.getByText("restoring")).toBeInTheDocument();

    releaseRefresh();
    expect(await screen.findByText("account-a@example.test")).toBeInTheDocument();
    expect(getAccessToken()).toBe("account-a-access");
    expect(window.localStorage.getItem(LEGACY_TOKEN_KEY)).toBeNull();
    expect(window.sessionStorage.getItem(LEGACY_TOKEN_KEY)).toBeNull();
  });

  it("becomes anonymous when startup refresh fails", async () => {
    authSessionClient.defaults.adapter = async (config) => {
      throw unauthorized(config);
    };

    renderProvider();

    expect(await screen.findByText("anonymous")).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
  });

  it("clears account-scoped queries and never stores JWTs when signing in", async () => {
    authSessionClient.defaults.adapter = async (config) => {
      throw unauthorized(config);
    };
    queryClient.setQueryData(["tenants"], [{ id: 1, fullName: "Account A data" }]);
    renderProvider();
    await screen.findByText("anonymous");

    await userEvent.click(screen.getByRole("button", { name: "Đổi tài khoản" }));

    expect(queryClient.getQueryData(["tenants"])).toBeUndefined();
    expect(await screen.findByText("account-b@example.test")).toBeInTheDocument();
    expect(getAccessToken()).toBe("account-b-access");
    expect(window.localStorage.getItem(LEGACY_TOKEN_KEY)).toBeNull();
    expect(window.sessionStorage.getItem(LEGACY_TOKEN_KEY)).toBeNull();
  });

  it("calls backend logout but clears memory and UI immediately", async () => {
    let logoutCalls = 0;
    let releaseLogout!: () => void;
    const logoutGate = new Promise<void>((resolve) => {
      releaseLogout = resolve;
    });
    authSessionClient.defaults.adapter = async (config) => {
      if (config.url === "/auth/refresh") {
        return apiResponse(config, accountA);
      }
      logoutCalls += 1;
      await logoutGate;
      return noContent(config);
    };
    renderProvider();
    await screen.findByText("account-a@example.test");

    await userEvent.click(screen.getByRole("button", { name: "Đăng xuất" }));

    expect(screen.getByText("anonymous")).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
    expect(logoutCalls).toBe(1);
    expect(isLogoutPending()).toBe(true);
    releaseLogout();
    await waitFor(() => expect(isLogoutPending()).toBe(false));
  });

  it("applies a cross-tab logout signal without calling backend logout again", async () => {
    let logoutCalls = 0;
    authSessionClient.defaults.adapter = async (config) => {
      if (config.url === "/auth/refresh") {
        return apiResponse(config, accountA);
      }
      logoutCalls += 1;
      return noContent(config);
    };
    renderProvider();
    await screen.findByText("account-a@example.test");

    dispatchSignal("logout");

    expect(await screen.findByText("anonymous")).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
    expect(logoutCalls).toBe(0);
    expect(isLogoutPending()).toBe(true);

    dispatchSignal("logout-complete");
    await waitFor(() => expect(isLogoutPending()).toBe(false));
    expect(screen.getByText("anonymous")).toBeInTheDocument();
  });

  it("restores from the HttpOnly cookie after a cross-tab login signal without rebroadcasting", async () => {
    let refreshCalls = 0;
    authSessionClient.defaults.adapter = async (config) => {
      refreshCalls += 1;
      if (refreshCalls === 1) {
        throw unauthorized(config);
      }
      return apiResponse(config, accountB);
    };
    const setItem = vi.spyOn(Storage.prototype, "setItem");
    renderProvider();
    await screen.findByText("anonymous");
    setItem.mockClear();

    dispatchSignal("login");

    expect(await screen.findByText("account-b@example.test")).toBeInTheDocument();
    expect(refreshCalls).toBe(2);
    expect(setItem).not.toHaveBeenCalledWith(
      SESSION_SIGNAL_STORAGE_KEY,
      expect.anything(),
    );
    expect(JSON.stringify(setItem.mock.calls)).not.toMatch(/token|password|email|account-b-access/i);
  });

  it("does not restore a late startup refresh after logout", async () => {
    let releaseRefresh!: () => void;
    const refreshGate = new Promise<void>((resolve) => {
      releaseRefresh = resolve;
    });
    authSessionClient.defaults.adapter = async (config) => {
      if (config.url === "/auth/logout") {
        return noContent(config);
      }
      await refreshGate;
      return apiResponse(config, accountA);
    };
    renderProvider();
    expect(screen.getByText("restoring")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Đăng xuất" }));
    releaseRefresh();

    expect(await screen.findByText("anonymous")).toBeInTheDocument();
    await Promise.resolve();
    expect(screen.queryByText("account-a@example.test")).not.toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
  });

  it("keeps a failed logout pending across reload and retries it when the browser comes online", async () => {
    let refreshCalls = 0;
    let logoutCalls = 0;
    let online = false;
    authSessionClient.defaults.adapter = async (config) => {
      if (config.url === "/auth/refresh") {
        refreshCalls += 1;
        return apiResponse(config, accountA);
      }
      logoutCalls += 1;
      if (!online) {
        throw new AxiosError("Network Error", AxiosError.ERR_NETWORK, config);
      }
      return noContent(config);
    };

    const firstRender = renderProvider();
    await screen.findByText("account-a@example.test");
    await userEvent.click(screen.getByRole("button", { name: "Đăng xuất" }));

    await waitFor(() => expect(logoutCalls).toBe(1));
    expect(screen.getByText("anonymous")).toBeInTheDocument();
    const pendingGeneration = getSessionGeneration();
    expect(pendingGeneration).not.toBeNull();
    expect(window.localStorage.getItem(LOGOUT_PENDING_STORAGE_KEY)).toBe(pendingGeneration);
    expect(document.cookie).toContain(`${LOGOUT_PENDING_COOKIE_NAME}=${pendingGeneration}`);
    expect(JSON.stringify(Object.fromEntries(Object.entries(window.localStorage))))
      .not.toMatch(/token|password|email|account-a-access/i);

    firstRender.unmount();
    renderProvider();

    expect(await screen.findByText("anonymous")).toBeInTheDocument();
    await waitFor(() => expect(logoutCalls).toBe(2));
    expect(refreshCalls).toBe(1);
    expect(isLogoutPending()).toBe(true);

    online = true;
    window.dispatchEvent(new Event("online"));

    await waitFor(() => expect(logoutCalls).toBe(3));
    await waitFor(() => expect(isLogoutPending()).toBe(false));
    expect(document.cookie).not.toContain(`${LOGOUT_PENDING_COOKIE_NAME}=`);
    expect(refreshCalls).toBe(1);
    expect(screen.getByText("anonymous")).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
  });

  it("clears pending logout only after an explicit successful login and ignores the late logout response", async () => {
    let logoutCalls = 0;
    let releaseLogout!: () => void;
    const logoutGate = new Promise<void>((resolve) => {
      releaseLogout = resolve;
    });
    setSessionGeneration(GENERATION_A);
    markLogoutPending(GENERATION_A);
    authSessionClient.defaults.adapter = async (config) => {
      if (config.url === "/auth/refresh") {
        throw unauthorized(config);
      }
      logoutCalls += 1;
      await logoutGate;
      return noContent(config);
    };

    renderProvider();
    expect(await screen.findByText("anonymous")).toBeInTheDocument();
    await waitFor(() => expect(logoutCalls).toBe(1));
    expect(isLogoutPending()).toBe(true);

    await userEvent.click(screen.getByRole("button", { name: "Đổi tài khoản" }));

    expect(await screen.findByText("account-b@example.test")).toBeInTheDocument();
    expect(isLogoutPending()).toBe(false);
    expect(getAccessToken()).toBe("account-b-access");

    releaseLogout();
    await Promise.resolve();
    expect(screen.getByText("account-b@example.test")).toBeInTheDocument();
    expect(isLogoutPending()).toBe(false);
  });

  it("ignores a delayed logout signal that targets the session before a newer login", async () => {
    setSessionGeneration(GENERATION_A);
    authSessionClient.defaults.adapter = async (config) => apiResponse(config, accountA);
    renderProvider();
    await screen.findByText("account-a@example.test");

    await userEvent.click(screen.getByRole("button", { name: "Đổi tài khoản" }));
    const newerGeneration = getSessionGeneration();
    expect(newerGeneration).not.toBe(GENERATION_A);

    dispatchSignal("logout", GENERATION_A);
    await Promise.resolve();

    expect(screen.getByText("account-b@example.test")).toBeInTheDocument();
    expect(getAccessToken()).toBe("account-b-access");
    expect(getSessionGeneration()).toBe(newerGeneration);
    expect(isLogoutPending()).toBe(false);
  });

  it("ignores a delayed logout-complete signal that targets the session before a newer login", async () => {
    setSessionGeneration(GENERATION_A);
    authSessionClient.defaults.adapter = async (config) => apiResponse(config, accountA);
    renderProvider();
    await screen.findByText("account-a@example.test");

    await userEvent.click(screen.getByRole("button", { name: "Đổi tài khoản" }));
    const newerGeneration = getSessionGeneration();
    expect(newerGeneration).not.toBe(GENERATION_A);

    dispatchSignal("logout-complete", GENERATION_A);
    await Promise.resolve();

    expect(screen.getByText("account-b@example.test")).toBeInTheDocument();
    expect(getAccessToken()).toBe("account-b-access");
    expect(getSessionGeneration()).toBe(newerGeneration);
    expect(isLogoutPending()).toBe(false);
  });

  it("never lets an old login signal revive a generation that started or completed logout", async () => {
    setSessionGeneration(GENERATION_A);
    authSessionClient.defaults.adapter = async (config) => apiResponse(config, accountA);
    renderProvider();
    await screen.findByText("account-a@example.test");

    dispatchSignal("logout", GENERATION_A);
    expect(await screen.findByText("anonymous")).toBeInTheDocument();
    expect(isLogoutPending()).toBe(true);

    dispatchSignal("login", GENERATION_A);
    await Promise.resolve();
    expect(screen.getByText("anonymous")).toBeInTheDocument();
    expect(isLogoutPending()).toBe(true);

    dispatchSignal("logout-complete", GENERATION_A);
    await waitFor(() => expect(isLogoutPending()).toBe(false));
    dispatchSignal("login", GENERATION_A);
    await Promise.resolve();

    expect(screen.getByText("anonymous")).toBeInTheDocument();
    expect(getAccessToken()).toBeNull();
    expect(getSessionGeneration()).toBeNull();
  });
});
