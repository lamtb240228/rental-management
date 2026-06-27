import { apiClient } from "../../lib/api/client";
import type { ApiResponse, AuthResponse, UserProfile } from "../../lib/api/types";

export type LoginPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = LoginPayload & {
  fullName: string;
  phone?: string;
};

export async function login(payload: LoginPayload) {
  const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/login", payload);
  return response.data.data;
}

export async function register(payload: RegisterPayload) {
  const response = await apiClient.post<ApiResponse<AuthResponse>>("/auth/register", payload);
  return response.data.data;
}

export async function me() {
  const response = await apiClient.get<ApiResponse<UserProfile>>("/auth/me");
  return response.data.data;
}
