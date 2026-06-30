import { apiClient } from "../../lib/api/client";
import type { ApiResponse, Invoice, Payment } from "../../lib/api/types";

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

export type PaymentPayload = {
  amount: number;
  paidAt?: string;
  paymentMethod: "CASH" | "BANK_TRANSFER" | "CARD" | "OTHER";
  paymentStatus?: "PENDING" | "COMPLETED";
  transactionReference?: string;
  note?: string;
};

export async function listPayments(invoiceId: number) {
  const response = await apiClient.get<ApiResponse<Payment[]>>(`/invoices/${invoiceId}/payments`);
  return response.data.data;
}

export async function createPayment(invoiceId: number, payload: PaymentPayload) {
  const response = await apiClient.post<ApiResponse<Invoice>>(`/invoices/${invoiceId}/payments`, payload);
  return response.data.data;
}

export async function cancelInvoice(id: number) {
  const response = await apiClient.patch<ApiResponse<Invoice>>(`/invoices/${id}/cancel`);
  return response.data.data;
}
