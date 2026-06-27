import { apiClient } from "../../lib/api/client";
import type { ApiResponse, DashboardSummary } from "../../lib/api/types";

export async function getDashboardSummary() {
  const response = await apiClient.get<ApiResponse<DashboardSummary>>("/dashboard/summary");
  return response.data.data;
}
