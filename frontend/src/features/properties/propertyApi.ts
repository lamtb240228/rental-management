import { apiClient } from "../../lib/api/client";
import type { ApiResponse, PropertyItem, RoomItem } from "../../lib/api/types";

export type PropertyPayload = {
  name: string;
  addressLine: string;
  ward?: string;
  district?: string;
  provinceCity: string;
  description?: string;
  status?: "ACTIVE" | "INACTIVE";
};

export type RoomPayload = {
  roomNumber: string;
  floorNumber?: number;
  area: number;
  monthlyRent: number;
  defaultDeposit: number;
  maxOccupants: number;
  status?: "AVAILABLE" | "OCCUPIED" | "MAINTENANCE" | "INACTIVE";
  description?: string;
};

export async function listProperties() {
  const response = await apiClient.get<ApiResponse<PropertyItem[]>>("/properties");
  return response.data.data;
}

export async function createProperty(payload: PropertyPayload) {
  const response = await apiClient.post<ApiResponse<PropertyItem>>("/properties", payload);
  return response.data.data;
}

export async function listRooms(propertyId: number) {
  const response = await apiClient.get<ApiResponse<RoomItem[]>>(`/properties/${propertyId}/rooms`);
  return response.data.data;
}

export async function createRoom(propertyId: number, payload: RoomPayload) {
  const response = await apiClient.post<ApiResponse<RoomItem>>(`/properties/${propertyId}/rooms`, payload);
  return response.data.data;
}
