export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  message: string;
  status?: number;
}

export async function apiClient<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem("goalpulse.token");
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(`/api/v1${path}`, { ...options, headers });
  const envelope = (await response.json()) as ApiEnvelope<T>;
  if (!response.ok || envelope.success === false) {
    throw new Error(envelope.message || "Request failed");
  }
  return envelope.data;
}
