import { apiClient } from "../../lib/api/client";
import type { ApiResponse, Invoice } from "../../lib/api/types";

export type InvoiceItemPayload = {
  itemType: "RENT" | "ELECTRICITY" | "WATER" | "SERVICE" | "OTHER";
  description: string;
  quantity: number;
  unitPrice: number;
};

export type InvoicePayload = {
  contractId: number;
  invoiceNumber?: string;
  billingYear: number;
  billingMonth: number;
  dueDate: string;
  discountAmount?: number;
  notes?: string;
  items: InvoiceItemPayload[];
};

export async function listInvoices() {
  const response = await apiClient.get<ApiResponse<Invoice[]>>("/invoices");
  return response.data.data;
}

export async function createInvoice(payload: InvoicePayload) {
  const response = await apiClient.post<ApiResponse<Invoice>>("/invoices", payload);
  return response.data.data;
}
