import { isValidSessionGeneration } from "./sessionGeneration";

export const LOGOUT_PENDING_STORAGE_KEY = "rental_auth_logout_pending";
export const LOGOUT_PENDING_COOKIE_NAME = "rental_logout_pending";

const COOKIE_MAX_AGE_SECONDS = 366 * 24 * 60 * 60;
let memoryPendingGeneration: string | null = null;
let persistenceFailed = false;

/** A non-credential tombstone that prevents a failed logout being undone by refresh. */
export function markLogoutPending(generation: string) {
  if (!isValidSessionGeneration(generation)) {
    throw new Error("Invalid pending logout generation");
  }

  memoryPendingGeneration = generation;
  if (typeof window === "undefined") {
    return;
  }

  let persisted = false;
  try {
    window.localStorage.setItem(LOGOUT_PENDING_STORAGE_KEY, generation);
    persisted = true;
  } catch {
    // A first-party marker cookie is the durable fallback below.
  }
  persisted = writePendingCookie(generation) || persisted;
  persistenceFailed = !persisted;
}

export function clearLogoutPending(expectedGeneration?: string) {
  if (expectedGeneration && getLogoutPendingGeneration() !== expectedGeneration) {
    return false;
  }

  memoryPendingGeneration = null;
  if (typeof window === "undefined") {
    return true;
  }

  try {
    window.localStorage.removeItem(LOGOUT_PENDING_STORAGE_KEY);
  } catch {
    // The marker cookie is cleared independently below.
  }
  clearPendingCookie();
  persistenceFailed = false;
  return true;
}

export function isLogoutPending() {
  return getLogoutPendingGeneration() !== null;
}

export function getLogoutPendingGeneration() {
  if (typeof window === "undefined") {
    return memoryPendingGeneration;
  }

  try {
    const storageGeneration = readStorageGeneration();
    const cookieGeneration = readPendingCookie();
    const persistedGeneration = storageGeneration ?? cookieGeneration;
    if (persistedGeneration) {
      memoryPendingGeneration = persistedGeneration;
    } else if (!persistenceFailed) {
      memoryPendingGeneration = null;
    }
  } catch {
    const cookieGeneration = readPendingCookie();
    if (cookieGeneration) {
      memoryPendingGeneration = cookieGeneration;
    } else if (!persistenceFailed) {
      memoryPendingGeneration = null;
    }
  }
  return memoryPendingGeneration;
}

function readStorageGeneration() {
  const value = window.localStorage.getItem(LOGOUT_PENDING_STORAGE_KEY);
  return isValidSessionGeneration(value) ? value : null;
}

function writePendingCookie(generation: string) {
  try {
    document.cookie = [
      `${LOGOUT_PENDING_COOKIE_NAME}=${generation}`,
      "Path=/",
      `Max-Age=${COOKIE_MAX_AGE_SECONDS}`,
      "SameSite=Strict",
      secureCookieAttribute(),
    ].filter(Boolean).join("; ");
    return readPendingCookie() === generation;
  } catch {
    // If cookies and storage are both unavailable, the HttpOnly refresh cookie
    // cannot persist either; memory still keeps this document logged out.
    return false;
  }
}

function clearPendingCookie() {
  try {
    document.cookie = [
      `${LOGOUT_PENDING_COOKIE_NAME}=`,
      "Path=/",
      "Max-Age=0",
      "Expires=Thu, 01 Jan 1970 00:00:00 GMT",
      "SameSite=Strict",
      secureCookieAttribute(),
    ].filter(Boolean).join("; ");
  } catch {
    // There was no durable cookie when browser cookie access is unavailable.
  }
}

function readPendingCookie() {
  try {
    const prefix = `${LOGOUT_PENDING_COOKIE_NAME}=`;
    const value = document.cookie
      .split(";")
      .map((cookie) => cookie.trim())
      .find((cookie) => cookie.startsWith(prefix))
      ?.slice(prefix.length);
    return isValidSessionGeneration(value) ? value : null;
  } catch {
    return null;
  }
}

function secureCookieAttribute() {
  return window.location.protocol === "https:" ? "Secure" : "";
}
