import { AxiosError, type InternalAxiosRequestConfig } from "axios";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  apiClient,
  clearToken,
  getToken,
  setToken,
  subscribeToUnauthorized,
} from "./client";

function unauthorizedAdapter(beforeReject?: () => void) {
  return async (config: InternalAxiosRequestConfig) => {
    beforeReject?.();
    throw new AxiosError(
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
  };
}

describe("apiClient session handling", () => {
  afterEach(() => clearToken());

  it("clears the current session and notifies once after a 401", async () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToUnauthorized(listener);
    setToken("expired-session");

    await expect(apiClient.get("/test", { adapter: unauthorizedAdapter() })).rejects.toBeInstanceOf(AxiosError);

    expect(getToken()).toBeNull();
    expect(listener).toHaveBeenCalledOnce();
    unsubscribe();
  });

  it("does not clear a newer session because of a late 401", async () => {
    const listener = vi.fn();
    const unsubscribe = subscribeToUnauthorized(listener);
    setToken("old-session");

    await expect(apiClient.get("/test", {
      adapter: unauthorizedAdapter(() => setToken("new-session")),
    })).rejects.toBeInstanceOf(AxiosError);

    expect(getToken()).toBe("new-session");
    expect(listener).not.toHaveBeenCalled();
    unsubscribe();
  });
});
