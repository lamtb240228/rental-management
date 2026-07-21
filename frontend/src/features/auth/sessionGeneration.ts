export const SESSION_GENERATION_STORAGE_KEY = "rental_auth_session_generation";
export const SESSION_GENERATION_COOKIE_NAME = "rental_session_generation";

const COOKIE_MAX_AGE_SECONDS = 366 * 24 * 60 * 60;
const GENERATION_PATTERN = /^[A-Za-z0-9_-]{8,128}$/;
let memoryGeneration: string | null = null;
let persistenceFailed = false;

/** Random correlation marker only; it is never accepted by the backend as a credential. */
export function createSessionGeneration() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
}

export function setSessionGeneration(generation: string) {
  if (!isValidSessionGeneration(generation)) {
    throw new Error("Invalid session generation");
  }

  memoryGeneration = generation;
  if (typeof window === "undefined") {
    return;
  }

  let persisted = false;
  try {
    window.localStorage.setItem(SESSION_GENERATION_STORAGE_KEY, generation);
    persisted = true;
  } catch {
    // The first-party marker cookie is the durable fallback below.
  }
  persisted = writeGenerationCookie(generation) || persisted;
  persistenceFailed = !persisted;
}

export function getSessionGeneration() {
  if (typeof window === "undefined") {
    return memoryGeneration;
  }

  const storageGeneration = readStorageGeneration();
  const cookieGeneration = readGenerationCookie();
  const persistedGeneration = storageGeneration ?? cookieGeneration;
  if (persistedGeneration) {
    memoryGeneration = persistedGeneration;
  } else if (!persistenceFailed) {
    memoryGeneration = null;
  }
  return memoryGeneration;
}

export function clearSessionGeneration(expectedGeneration?: string) {
  const currentGeneration = getSessionGeneration();
  if (expectedGeneration && currentGeneration && currentGeneration !== expectedGeneration) {
    return false;
  }

  memoryGeneration = null;
  if (typeof window !== "undefined") {
    try {
      window.localStorage.removeItem(SESSION_GENERATION_STORAGE_KEY);
    } catch {
      // The cookie is cleared independently below.
    }
    clearGenerationCookie();
  }
  persistenceFailed = false;
  return true;
}

export function isValidSessionGeneration(value: unknown): value is string {
  return typeof value === "string" && GENERATION_PATTERN.test(value);
}

function readStorageGeneration() {
  try {
    const value = window.localStorage.getItem(SESSION_GENERATION_STORAGE_KEY);
    return isValidSessionGeneration(value) ? value : null;
  } catch {
    return null;
  }
}

function writeGenerationCookie(generation: string) {
  try {
    document.cookie = [
      `${SESSION_GENERATION_COOKIE_NAME}=${generation}`,
      "Path=/",
      `Max-Age=${COOKIE_MAX_AGE_SECONDS}`,
      "SameSite=Strict",
      secureCookieAttribute(),
    ].filter(Boolean).join("; ");
    return readGenerationCookie() === generation;
  } catch {
    return false;
  }
}

function readGenerationCookie() {
  try {
    const prefix = `${SESSION_GENERATION_COOKIE_NAME}=`;
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

function clearGenerationCookie() {
  try {
    document.cookie = [
      `${SESSION_GENERATION_COOKIE_NAME}=`,
      "Path=/",
      "Max-Age=0",
      "Expires=Thu, 01 Jan 1970 00:00:00 GMT",
      "SameSite=Strict",
      secureCookieAttribute(),
    ].filter(Boolean).join("; ");
  } catch {
    // There was no durable marker when browser cookie access is unavailable.
  }
}

function secureCookieAttribute() {
  return window.location.protocol === "https:" ? "Secure" : "";
}
