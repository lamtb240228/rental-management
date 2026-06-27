import { apiClient } from "../../lib/api/client";
import type { ApiResponse, MaintenanceRequest } from "../../lib/api/types";

export type MaintenanceStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";

export type MaintenanceStatusUpdatePayload = {
  status: MaintenanceStatus;
  resolutionNotes?: string;
};

export async function listMaintenanceRequests() {
  const response = await apiClient.get<ApiResponse<MaintenanceRequest[]>>("/maintenance-requests");
  return response.data.data;
}

export async function updateMaintenanceStatus(id: number, payload: MaintenanceStatusUpdatePayload) {
  const response = await apiClient.patch<ApiResponse<MaintenanceRequest>>(
    `/maintenance-requests/${id}/status`,
    payload,
  );
  return response.data.data;
}
