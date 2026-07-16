import axios from "axios";

export const TOKEN_KEY = "rental_access_token";

const unauthorizedListeners = new Set<() => void>();

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      const currentToken = getToken();
      const requestAuthorization = error.config?.headers?.Authorization;

      // Ignore a late 401 from a request that used an older session. It must
      // never sign out a user who has already authenticated with a new token.
      if (currentToken && requestAuthorization === `Bearer ${currentToken}`) {
        clearToken();
        unauthorizedListeners.forEach((listener) => listener());
      }
    }

    return Promise.reject(error);
  },
);

export function subscribeToUnauthorized(listener: () => void) {
  unauthorizedListeners.add(listener);
  return () => {
    unauthorizedListeners.delete(listener);
  };
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}
