import { createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import {
  establishAccessSession,
  getSessionEpoch,
  invalidateAccessSession,
  refreshAccessToken,
  subscribeToUnauthorized,
} from "../../lib/api/client";
import type { AuthResponse, UserProfile } from "../../lib/api/types";
import { queryClient } from "../../lib/query-client/queryClient";
import { logout } from "./authApi";
import {
  clearLogoutPending,
  getLogoutPendingGeneration,
  isLogoutPending,
  markLogoutPending,
} from "./logoutPending";
import { createSessionChannel, type SessionChannel } from "./sessionChannel";
import {
  clearSessionGeneration,
  createSessionGeneration,
  getSessionGeneration,
  setSessionGeneration,
} from "./sessionGeneration";

type AuthContextValue = {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  signIn: (response: AuthResponse) => void;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

type ActiveLogoutAttempt = {
  controller: AbortController;
  generation: string;
  promise: Promise<void>;
};

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const channelRef = useRef<SessionChannel | null>(null);
  const uiRevisionRef = useRef(0);
  const mountedRef = useRef(false);
  const logoutAttemptRef = useRef<ActiveLogoutAttempt | null>(null);
  const logoutAttemptRevisionRef = useRef(0);
  const sessionGenerationRef = useRef<string | null>(getSessionGeneration());
  const retiredSessionGenerationsRef = useRef(new Set<string>());

  const clearSessionUi = useCallback(() => {
    uiRevisionRef.current += 1;
    queryClient.clear();
    if (mountedRef.current) {
      setUser(null);
      setIsLoading(false);
    }
  }, []);

  const cancelLogoutAttempt = useCallback((expectedGeneration?: string) => {
    if (
      expectedGeneration &&
      logoutAttemptRef.current?.generation !== expectedGeneration
    ) {
      return;
    }
    logoutAttemptRevisionRef.current += 1;
    logoutAttemptRef.current?.controller.abort();
    logoutAttemptRef.current = null;
  }, []);

  const retryPendingLogout = useCallback(() => {
    const generation = getLogoutPendingGeneration();
    if (!generation) {
      return Promise.resolve();
    }
    retiredSessionGenerationsRef.current.add(generation);
    const currentGeneration = getSessionGeneration();
    if (currentGeneration && currentGeneration !== generation) {
      clearLogoutPending(generation);
      return Promise.resolve();
    }

    if (logoutAttemptRef.current) {
      if (logoutAttemptRef.current.generation === generation) {
        return logoutAttemptRef.current.promise;
      }
      logoutAttemptRevisionRef.current += 1;
      logoutAttemptRef.current.controller.abort();
      logoutAttemptRef.current = null;
    }

    const controller = new AbortController();
    const revision = logoutAttemptRevisionRef.current;
    const promise = logout(controller.signal)
      .then(() => {
        if (
          controller.signal.aborted ||
          revision !== logoutAttemptRevisionRef.current ||
          getLogoutPendingGeneration() !== generation
        ) {
          return;
        }
        clearLogoutPending(generation);
        clearSessionGeneration(generation);
        if (sessionGenerationRef.current === generation) {
          sessionGenerationRef.current = null;
        }
        channelRef.current?.publish("logout-complete", generation);
      })
      .catch(() => {
        // Keep the non-sensitive tombstone. Startup and the next online event
        // retry server revocation without ever restoring from the cookie.
      })
      .finally(() => {
        if (logoutAttemptRef.current?.promise === promise) {
          logoutAttemptRef.current = null;
        }
      });

    logoutAttemptRef.current = { controller, generation, promise };
    return promise;
  }, []);

  const restoreSession = useCallback(async () => {
    if (isLogoutPending()) {
      invalidateAccessSession();
      clearSessionUi();
      return;
    }

    const expectedEpoch = getSessionEpoch();
    const expectedGeneration = getSessionGeneration() ?? sessionGenerationRef.current;
    const uiRevision = uiRevisionRef.current + 1;
    uiRevisionRef.current = uiRevision;

    // Defer React state changes out of the mounting effect body. This also
    // gives a synchronous login/logout transition priority over bootstrap.
    await Promise.resolve();
    if (isLogoutPending()) {
      invalidateAccessSession();
      clearSessionUi();
      return;
    }
    if (
      !mountedRef.current ||
      uiRevisionRef.current !== uiRevision ||
      getSessionEpoch() !== expectedEpoch
    ) {
      return;
    }

    queryClient.clear();
    setUser(null);
    setIsLoading(true);

    try {
      const response = await refreshAccessToken(expectedEpoch);
      if (isLogoutPending()) {
        invalidateAccessSession();
        clearSessionUi();
        return;
      }
      const persistedGeneration = getSessionGeneration();
      if (
        expectedGeneration &&
        persistedGeneration &&
        expectedGeneration !== persistedGeneration
      ) {
        invalidateAccessSession();
        clearSessionUi();
        return;
      }
      const restoredGeneration = expectedGeneration ?? persistedGeneration ?? createSessionGeneration();
      sessionGenerationRef.current = restoredGeneration;
      setSessionGeneration(restoredGeneration);
      if (
        mountedRef.current &&
        uiRevisionRef.current === uiRevision &&
        getSessionEpoch() === expectedEpoch
      ) {
        setUser(response.user);
      }
    } catch {
      if (
        mountedRef.current &&
        uiRevisionRef.current === uiRevision &&
        getSessionEpoch() === expectedEpoch
      ) {
        setUser(null);
      }
    } finally {
      if (
        mountedRef.current &&
        uiRevisionRef.current === uiRevision &&
        getSessionEpoch() === expectedEpoch
      ) {
        setIsLoading(false);
      }
    }
  }, [clearSessionUi]);

  const signIn = useCallback((response: AuthResponse) => {
    cancelLogoutAttempt();
    clearLogoutPending();
    const generation = createSessionGeneration();
    retiredSessionGenerationsRef.current.delete(generation);
    sessionGenerationRef.current = generation;
    setSessionGeneration(generation);
    uiRevisionRef.current += 1;
    queryClient.clear();
    establishAccessSession(response.accessToken);
    setUser(response.user);
    setIsLoading(false);
    channelRef.current?.publish("login", generation);
  }, [cancelLogoutAttempt]);

  const signOut = useCallback(async () => {
    // Invalidate memory and the transport epoch before any network wait. A
    // refresh response that arrives later can no longer restore this session.
    cancelLogoutAttempt();
    const generation =
      getSessionGeneration() ?? sessionGenerationRef.current ?? createSessionGeneration();
    retiredSessionGenerationsRef.current.add(generation);
    sessionGenerationRef.current = generation;
    setSessionGeneration(generation);
    markLogoutPending(generation);
    invalidateAccessSession();
    clearSessionUi();
    channelRef.current?.publish("logout", generation);
    await retryPendingLogout();
  }, [cancelLogoutAttempt, clearSessionUi, retryPendingLogout]);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => subscribeToUnauthorized(clearSessionUi), [clearSessionUi]);

  useEffect(() => {
    const channel = createSessionChannel((signal) => {
      if (signal.kind === "logout") {
        const currentGeneration = getSessionGeneration() ?? sessionGenerationRef.current;
        if (currentGeneration !== signal.generation) {
          return;
        }
        retiredSessionGenerationsRef.current.add(signal.generation);
        sessionGenerationRef.current = signal.generation;
        markLogoutPending(signal.generation);
        invalidateAccessSession();
        clearSessionUi();
        return;
      }

      if (signal.kind === "logout-complete") {
        retiredSessionGenerationsRef.current.add(signal.generation);
        const currentGeneration = getSessionGeneration();
        const targetsCurrentSession =
          currentGeneration === signal.generation ||
          sessionGenerationRef.current === signal.generation;
        const targetsPendingLogout = getLogoutPendingGeneration() === signal.generation;
        if (!targetsCurrentSession && !targetsPendingLogout) {
          return;
        }

        cancelLogoutAttempt(signal.generation);
        if (targetsPendingLogout) {
          clearLogoutPending(signal.generation);
        }
        if (targetsCurrentSession) {
          clearSessionGeneration(signal.generation);
          sessionGenerationRef.current = null;
          invalidateAccessSession();
          clearSessionUi();
        }
        return;
      }

      if (
        retiredSessionGenerationsRef.current.has(signal.generation) ||
        getLogoutPendingGeneration() === signal.generation
      ) {
        return;
      }
      const persistedGeneration = getSessionGeneration();
      if (persistedGeneration && persistedGeneration !== signal.generation) {
        return;
      }
      cancelLogoutAttempt();
      clearLogoutPending();
      retiredSessionGenerationsRef.current.delete(signal.generation);
      sessionGenerationRef.current = signal.generation;
      setSessionGeneration(signal.generation);

      // A login signal carries no credential. The receiving tab invalidates
      // its old identity, then obtains its own access token from the shared
      // HttpOnly refresh cookie. It does not rebroadcast the received event.
      invalidateAccessSession();
      void restoreSession();
    });

    channelRef.current = channel;
    return () => {
      channelRef.current = null;
      channel.close();
    };
  }, [cancelLogoutAttempt, clearSessionUi, restoreSession]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      if (isLogoutPending()) {
        invalidateAccessSession();
        clearSessionUi();
        void retryPendingLogout();
        return;
      }
      void restoreSession();
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [clearSessionUi, restoreSession, retryPendingLogout]);

  useEffect(() => {
    const retryWhenOnline = () => {
      if (!isLogoutPending()) {
        return;
      }
      invalidateAccessSession();
      clearSessionUi();
      void retryPendingLogout();
    };

    window.addEventListener("online", retryWhenOnline);
    return () => window.removeEventListener("online", retryWhenOnline);
  }, [clearSessionUi, retryPendingLogout]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isLoading,
      signIn,
      signOut,
    }),
    [isLoading, signIn, signOut, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
