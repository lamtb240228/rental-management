import { apiClient } from "../../lib/api/client";
import type { ApiResponse, ContractItem } from "../../lib/api/types";

export type ContractPayload = {
  roomId: number;
  contractCode?: string;
  startDate: string;
  endDate?: string;
  monthlyRent: number;
  depositAmount: number;
  status?: "DRAFT" | "ACTIVE" | "ENDED" | "CANCELLED";
  tenantIds: number[];
  primaryTenantId: number;
  notes?: string;
};

export async function listContracts() {
  const response = await apiClient.get<ApiResponse<ContractItem[]>>("/contracts");
  return response.data.data;
}

export async function createContract(payload: ContractPayload) {
  const response = await apiClient.post<ApiResponse<ContractItem>>("/contracts", payload);
  return response.data.data;
}
