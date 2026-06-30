import { apiClient } from "../../lib/api/client";
import type { ApiResponse, UtilityReading } from "../../lib/api/types";

export type UtilityReadingPayload = {
  billingYear: number;
  billingMonth: number;
  electricityOldReading: number;
  electricityNewReading: number;
  electricityUnitPrice: number;
  waterOldReading: number;
  waterNewReading: number;
  waterUnitPrice: number;
};

export async function listUtilityReadings(roomId: number) {
  const response = await apiClient.get<ApiResponse<UtilityReading[]>>(`/rooms/${roomId}/utility-readings`);
  return response.data.data;
}

export async function createUtilityReading(roomId: number, payload: UtilityReadingPayload) {
  const response = await apiClient.post<ApiResponse<UtilityReading>>(`/rooms/${roomId}/utility-readings`, payload);
  return response.data.data;
}

export async function updateUtilityReading(id: number, payload: UtilityReadingPayload) {
  const response = await apiClient.put<ApiResponse<UtilityReading>>(`/utility-readings/${id}`, payload);
  return response.data.data;
}
