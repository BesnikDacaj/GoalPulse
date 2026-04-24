import { apiClient } from "./axiosClient";
import type { Match } from "../types/Match";

export const getMatches = () => apiClient<Match[]>("/matches");
export const getLiveMatches = () => apiClient<Match[]>("/matches/live");
export const getMatch = (id: number) => apiClient<Match>(`/matches/${id}`);
