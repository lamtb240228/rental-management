import { apiClient } from "../../lib/api/client";
import type { ApiResponse, ContractItem, TenantItem } from "../../lib/api/types";

export type TenantPayload = {
  fullName: string;
  dateOfBirth?: string;
  phone?: string;
  email?: string;
  identityNumber?: string;
  permanentAddress?: string;
  status?: "ACTIVE" | "INACTIVE";
};

export async function listTenants() {
  const response = await apiClient.get<ApiResponse<TenantItem[]>>("/tenants");
  return response.data.data;
}

export async function createTenant(payload: TenantPayload) {
  const response = await apiClient.post<ApiResponse<TenantItem>>("/tenants", payload);
  return response.data.data;
}

export async function updateTenant(id: number, payload: TenantPayload) {
  const response = await apiClient.put<ApiResponse<TenantItem>>(`/tenants/${id}`, payload);
  return response.data.data;
}

export async function listTenantContracts(id: number) {
  const response = await apiClient.get<ApiResponse<ContractItem[]>>(`/tenants/${id}/contracts`);
  return response.data.data;
}
