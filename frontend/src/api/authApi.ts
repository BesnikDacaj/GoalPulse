import { apiClient } from "./axiosClient";
import type { User } from "../types/User";

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: User;
}

export function login(email: string, password: string) {
  return apiClient<LoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password })
  });
}

export function register(username: string, email: string, password: string) {
  return apiClient<LoginResponse>("/auth/register", {
    method: "POST",
    body: JSON.stringify({ username, email, password })
  });
}
