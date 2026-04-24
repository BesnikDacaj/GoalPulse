export type UserRole = "guest" | "registered" | "admin";

export interface User {
  id: number;
  username: string;
  email: string;
  role: UserRole;
}
