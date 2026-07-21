const AUTH_SESSION_LOCK_NAME = "rental-management-auth-refresh";

/** Serializes every response that can rotate or clear the shared auth cookie. */
export async function runWithAuthSessionLock<T>(
  callback: () => Promise<T>,
  signal?: AbortSignal,
): Promise<T> {
  if (typeof navigator !== "undefined" && navigator.locks?.request) {
    return navigator.locks.request(
      AUTH_SESSION_LOCK_NAME,
      { mode: "exclusive", signal },
      callback,
    );
  }
  return callback();
}
