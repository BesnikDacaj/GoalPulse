import { apiClient } from "./axiosClient";
import type { Team } from "../types/Team";

export const getTeams = () => apiClient<Team[]>("/teams");
export const getTeam = (id: number) => apiClient<Team>(`/teams/${id}`);
