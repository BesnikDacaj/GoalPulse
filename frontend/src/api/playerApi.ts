import { apiClient } from "./axiosClient";
import type { Player } from "../types/Player";

export const getPlayers = () => apiClient<Player[]>("/players");
export const getPlayer = (id: number) => apiClient<Player>(`/players/${id}`);
