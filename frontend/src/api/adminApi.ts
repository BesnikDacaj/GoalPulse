import { apiClient } from "./axiosClient";

export function updateMatch(id: number, homeScore: number, awayScore: number, status: string) {
  return apiClient(`/admin/matches/${id}`, {
    method: "PUT",
    body: JSON.stringify({ homeScore, awayScore, status })
  });
}

export function getAuditLogs() {
  return apiClient("/admin/audit-logs");
}
