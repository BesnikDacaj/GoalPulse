import type { ReactNode } from "react";
import { useAuthContext } from "../context/AuthContext";

export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user } = useAuthContext();
  if (!user) return <p>Please log in to view this page.</p>;
  return <>{children}</>;
}
