import { useQuery } from "@tanstack/react-query";
import { createContext, type PropsWithChildren, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { clearToken, getToken, setToken, subscribeToUnauthorized, TOKEN_KEY } from "../../lib/api/client";
import type { AuthResponse, UserProfile } from "../../lib/api/types";
import { queryClient } from "../../lib/query-client/queryClient";
import { me } from "./authApi";

type AuthContextValue = {
  user: UserProfile | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  signIn: (response: AuthResponse) => void;
  signOut: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const [hasToken, setHasToken] = useState(Boolean(getToken()));
  const sessionRevisionRef = useRef(0);
  const [sessionRevision, setSessionRevision] = useState(0);
  const profileQuery = useQuery({
    queryKey: ["auth", "me", sessionRevision],
    queryFn: me,
    enabled: hasToken,
  });

  const advanceSession = useCallback(() => {
    sessionRevisionRef.current += 1;
    setSessionRevision(sessionRevisionRef.current);
    return sessionRevisionRef.current;
  }, []);

  const clearSession = useCallback(() => {
    clearToken();
    queryClient.clear();
    advanceSession();
    setHasToken(false);
  }, [advanceSession]);

  useEffect(() => subscribeToUnauthorized(clearSession), [clearSession]);

  useEffect(() => {
    function handleStorage(event: StorageEvent) {
      if (event.key !== TOKEN_KEY) return;

      queryClient.clear();
      advanceSession();
      setHasToken(Boolean(event.newValue));
    }

    window.addEventListener("storage", handleStorage);
    return () => window.removeEventListener("storage", handleStorage);
  }, [advanceSession]);

  const signIn = useCallback((response: AuthResponse) => {
    // Clear every account-scoped query before accepting another identity.
    queryClient.clear();
    setToken(response.accessToken);
    const nextRevision = advanceSession();
    queryClient.setQueryData(["auth", "me", nextRevision], response.user);
    setHasToken(true);
  }, [advanceSession]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user: profileQuery.data ?? null,
      isAuthenticated: Boolean(profileQuery.data),
      isLoading: hasToken && profileQuery.isLoading,
      signIn,
      signOut: clearSession,
    }),
    [clearSession, hasToken, profileQuery.data, profileQuery.isLoading, signIn],
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
