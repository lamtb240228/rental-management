import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  clearSessionGeneration,
  getSessionGeneration,
  SESSION_GENERATION_COOKIE_NAME,
  SESSION_GENERATION_STORAGE_KEY,
  setSessionGeneration,
} from "./sessionGeneration";

const GENERATION_A = "generation-a-123456";
const GENERATION_B = "generation-b-123456";

describe("session generation marker", () => {
  beforeEach(() => {
    window.localStorage.clear();
    clearSessionGeneration();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    clearSessionGeneration();
  });

  it("persists a non-credential generation and only clears the expected session", () => {
    setSessionGeneration(GENERATION_A);

    expect(getSessionGeneration()).toBe(GENERATION_A);
    expect(window.localStorage.getItem(SESSION_GENERATION_STORAGE_KEY)).toBe(GENERATION_A);
    expect(document.cookie).toContain(`${SESSION_GENERATION_COOKIE_NAME}=${GENERATION_A}`);
    expect(clearSessionGeneration(GENERATION_B)).toBe(false);
    expect(getSessionGeneration()).toBe(GENERATION_A);

    expect(clearSessionGeneration(GENERATION_A)).toBe(true);
    expect(getSessionGeneration()).toBeNull();
  });

  it("uses the first-party marker cookie after storage failure and module reload", async () => {
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new DOMException("Storage disabled", "SecurityError");
    });

    setSessionGeneration(GENERATION_A);

    expect(window.localStorage.getItem(SESSION_GENERATION_STORAGE_KEY)).toBeNull();
    expect(document.cookie).toContain(`${SESSION_GENERATION_COOKIE_NAME}=${GENERATION_A}`);

    vi.restoreAllMocks();
    vi.resetModules();
    const reloadedState = await import("./sessionGeneration");

    expect(reloadedState.getSessionGeneration()).toBe(GENERATION_A);
    reloadedState.clearSessionGeneration(GENERATION_A);
  });

  it("prefers a valid storage marker when the fallback cookie is stale", () => {
    setSessionGeneration(GENERATION_A);
    window.localStorage.setItem(SESSION_GENERATION_STORAGE_KEY, GENERATION_B);

    expect(getSessionGeneration()).toBe(GENERATION_B);
  });
});
