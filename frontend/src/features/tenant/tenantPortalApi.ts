import { apiClient } from "../../lib/api/client";
import type { ApiResponse, MaintenanceRequest, TenantPortalSummary } from "../../lib/api/types";

export type TenantMaintenancePayload = {
  title: string;
  description: string;
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
};

export async function getTenantPortalSummary() {
  const response = await apiClient.get<ApiResponse<TenantPortalSummary>>("/tenant-portal/summary");
  return response.data.data;
}

export async function createTenantMaintenanceRequest(payload: TenantMaintenancePayload) {
  const response = await apiClient.post<ApiResponse<MaintenanceRequest>>("/tenant-portal/maintenance-requests", payload);
  return response.data.data;
}
