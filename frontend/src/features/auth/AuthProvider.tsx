import { useQuery } from "@tanstack/react-query";
import { createContext, type PropsWithChildren, useContext, useMemo, useState } from "react";
import { clearToken, getToken, setToken } from "../../lib/api/client";
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
  const profileQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: me,
    enabled: hasToken,
  });

  const value = useMemo<AuthContextValue>(
    () => ({
      user: profileQuery.data ?? null,
      isAuthenticated: Boolean(profileQuery.data),
      isLoading: hasToken && profileQuery.isLoading,
      signIn: (response) => {
        setToken(response.accessToken);
        setHasToken(true);
        queryClient.setQueryData(["auth", "me"], response.user);
      },
      signOut: () => {
        clearToken();
        setHasToken(false);
        queryClient.clear();
      },
    }),
    [hasToken, profileQuery.data, profileQuery.isLoading],
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
