import type { PropsWithChildren } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthProvider";

export function RoleRoute({ roles, children }: PropsWithChildren<{ roles: string[] }>) {
  const { user } = useAuth();

  if (!user || !roles.some((role) => user.roles.includes(role))) {
    return <Navigate to="/" replace />;
  }

  return children;
}
