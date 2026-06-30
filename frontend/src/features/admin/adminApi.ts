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

export async function updateAdminUserStatus(id: number, status: AdminUser["status"]) {
  const response = await apiClient.patch<ApiResponse<AdminUser>>(`/admin/users/${id}/status`, { status });
  return response.data.data;
}
