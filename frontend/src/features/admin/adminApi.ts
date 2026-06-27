import { apiClient } from "../../lib/api/client";
import type { AdminSummary, AdminUser, ApiResponse } from "../../lib/api/types";

export async function getAdminSummary() {
  const response = await apiClient.get<ApiResponse<AdminSummary>>("/admin/summary");
  return response.data.data;
}

export async function listAdminUsers() {
  const response = await apiClient.get<ApiResponse<AdminUser[]>>("/admin/users");
  return response.data.data;
}
