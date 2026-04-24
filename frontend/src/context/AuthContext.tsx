import { createContext, useContext } from "react";
import type { User } from "../types/User";

export interface AuthContextValue {
  user: User | null;
  token: string | null;
  setSession: (token: string, refreshToken: string, user: User) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuthContext() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("AuthContext is missing");
  return value;
}
