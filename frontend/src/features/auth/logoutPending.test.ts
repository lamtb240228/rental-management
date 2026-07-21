import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  clearLogoutPending,
  getLogoutPendingGeneration,
  isLogoutPending,
  LOGOUT_PENDING_COOKIE_NAME,
  LOGOUT_PENDING_STORAGE_KEY,
  markLogoutPending,
} from "./logoutPending";

const GENERATION_A = "generation-a-123456";
const GENERATION_B = "generation-b-123456";

describe("logout pending tombstone", () => {
  beforeEach(() => {
    window.localStorage.clear();
    clearLogoutPending();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    clearLogoutPending();
  });

  it("persists only a non-sensitive generation and removes the matching marker", () => {
    markLogoutPending(GENERATION_A);

    expect(isLogoutPending()).toBe(true);
    expect(window.localStorage.getItem(LOGOUT_PENDING_STORAGE_KEY)).toBe(GENERATION_A);
    expect(document.cookie).toContain(`${LOGOUT_PENDING_COOKIE_NAME}=${GENERATION_A}`);
    expect(JSON.stringify({
      key: LOGOUT_PENDING_STORAGE_KEY,
      value: window.localStorage.getItem(LOGOUT_PENDING_STORAGE_KEY),
    })).not.toMatch(/access|refresh|bearer|password|email/i);

    expect(clearLogoutPending(GENERATION_B)).toBe(false);
    expect(isLogoutPending()).toBe(true);
    expect(clearLogoutPending(GENERATION_A)).toBe(true);

    expect(isLogoutPending()).toBe(false);
    expect(window.localStorage.getItem(LOGOUT_PENDING_STORAGE_KEY)).toBeNull();
    expect(document.cookie).not.toContain(`${LOGOUT_PENDING_COOKIE_NAME}=${GENERATION_A}`);
  });

  it("observes another tab clearing the shared tombstone", () => {
    markLogoutPending(GENERATION_A);
    window.localStorage.removeItem(LOGOUT_PENDING_STORAGE_KEY);
    document.cookie = `${LOGOUT_PENDING_COOKIE_NAME}=; Path=/; Max-Age=0; SameSite=Strict`;

    expect(isLogoutPending()).toBe(false);
  });

  it("persists through the marker cookie and a module reload when storage rejects the write", async () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new DOMException("Storage disabled", "SecurityError");
    });

    markLogoutPending(GENERATION_A);

    expect(isLogoutPending()).toBe(true);
    expect(window.localStorage.getItem(LOGOUT_PENDING_STORAGE_KEY)).toBeNull();
    expect(document.cookie).toContain(`${LOGOUT_PENDING_COOKIE_NAME}=${GENERATION_A}`);

    vi.restoreAllMocks();
    vi.resetModules();
    const reloadedState = await import("./logoutPending");

    expect(reloadedState.isLogoutPending()).toBe(true);
    reloadedState.clearLogoutPending();
  });

  it("prefers a valid storage tombstone when the fallback cookie is stale", () => {
    markLogoutPending(GENERATION_A);
    window.localStorage.setItem(LOGOUT_PENDING_STORAGE_KEY, GENERATION_B);

    expect(getLogoutPendingGeneration()).toBe(GENERATION_B);
  });
});
