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
import { createSessionChannel, type SessionChannel } from "./sessionChannel";

type AuthContextValue = {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  signIn: (response: AuthResponse) => void;
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const channelRef = useRef<SessionChannel | null>(null);
  const uiRevisionRef = useRef(0);
  const mountedRef = useRef(false);

  const clearSessionUi = useCallback(() => {
    uiRevisionRef.current += 1;
    queryClient.clear();
    if (mountedRef.current) {
      setUser(null);
      setIsLoading(false);
    }
  }, []);

  const restoreSession = useCallback(async () => {
    const expectedEpoch = getSessionEpoch();
    const uiRevision = uiRevisionRef.current + 1;
    uiRevisionRef.current = uiRevision;

    // Defer React state changes out of the mounting effect body. This also
    // gives a synchronous login/logout transition priority over bootstrap.
    await Promise.resolve();
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
  }, []);

  const signIn = useCallback((response: AuthResponse) => {
    uiRevisionRef.current += 1;
    queryClient.clear();
    establishAccessSession(response.accessToken);
    setUser(response.user);
    setIsLoading(false);
    channelRef.current?.publish("login");
  }, []);

  const signOut = useCallback(async () => {
    // Invalidate memory and the transport epoch before any network wait. A
    // refresh response that arrives later can no longer restore this session.
    invalidateAccessSession();
    clearSessionUi();
    channelRef.current?.publish("logout");

    try {
      await logout();
    } catch {
      // Local logout is intentionally final even if the network is offline.
      // The short access-token lifetime limits the abandoned server session.
    }
  }, [clearSessionUi]);

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
        invalidateAccessSession();
        clearSessionUi();
        return;
      }

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
  }, [clearSessionUi, restoreSession]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void restoreSession();
    }, 0);
    return () => window.clearTimeout(timeoutId);
  }, [restoreSession]);

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
