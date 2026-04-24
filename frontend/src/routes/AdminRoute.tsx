import type { ReactNode } from "react";
import { useAuthContext } from "../context/AuthContext";

export function AdminRoute({ children }: { children: ReactNode }) {
  const { user } = useAuthContext();
  if (user?.role !== "admin") return <p>Admin access required.</p>;
  return <>{children}</>;
}
