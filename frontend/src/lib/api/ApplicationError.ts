import axios from "axios";

type ApplicationErrorDetails = {
  status: number | null;
  code: string;
  message: string;
  requestId: string | null;
};

type ErrorPayload = {
  code?: unknown;
  error?: unknown;
  message?: unknown;
  requestId?: unknown;
  correlationId?: unknown;
};

/**
 * Safe error contract exposed to UI code. It deliberately retains neither the
 * Axios error nor its request config, headers, body, or original cause.
 */
export class ApplicationError extends Error {
  readonly status: number | null;
  readonly code: string;
  readonly requestId: string | null;

  constructor({ status, code, message, requestId }: ApplicationErrorDetails) {
    super(message);
    this.name = "ApplicationError";
    this.status = status;
    this.code = code;
    this.requestId = requestId;
  }

  toJSON() {
    return {
      status: this.status,
      code: this.code,
      message: this.message,
      requestId: this.requestId,
    };
  }
}

export function toApplicationError(error: unknown): ApplicationError {
  if (error instanceof ApplicationError) {
    return error;
  }

  if (!axios.isAxiosError(error)) {
    return new ApplicationError({
      status: null,
      code: "REQUEST_FAILED",
      message: "The request could not be completed",
      requestId: null,
    });
  }

  const status = error.response?.status ?? null;
  const payload = readPayload(error.response?.data);
  const requestId = normalizeRequestId(
    readHeader(error.response?.headers, "x-request-id") ??
    readHeader(error.response?.headers, "x-correlation-id") ??
    payload?.requestId ??
    payload?.correlationId,
  );

  return new ApplicationError({
    status,
    code:
      normalizeCode(payload?.code) ??
      normalizeCode(payload?.error) ??
      normalizeCode(error.code) ??
      statusCode(status),
    message: safeMessage(payload?.message, error.code, status),
    requestId,
  });
}

function readPayload(value: unknown): ErrorPayload | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return null;
  }
  return value as ErrorPayload;
}

function readHeader(headers: unknown, name: string): unknown {
  if (!headers || typeof headers !== "object") {
    return undefined;
  }

  const withGetter = headers as { get?: (headerName: string) => unknown };
  if (typeof withGetter.get === "function") {
    return withGetter.get(name);
  }

  const record = headers as Record<string, unknown>;
  const key = Object.keys(record).find((candidate) => candidate.toLowerCase() === name);
  return key ? record[key] : undefined;
}

function normalizeCode(value: unknown): string | null {
  if (typeof value !== "string") {
    return null;
  }

  const normalized = value.trim().toUpperCase().replace(/[^A-Z0-9_.-]+/g, "_").slice(0, 64);
  return normalized || null;
}

function statusCode(status: number | null) {
  return status === null ? "NETWORK_ERROR" : `HTTP_${status}`;
}

function safeMessage(value: unknown, axiosCode: string | undefined, status: number | null) {
  if (typeof value === "string") {
    const normalized = value.trim().slice(0, 500);
    if (normalized) {
      return normalized;
    }
  }

  if (axiosCode === "ERR_CANCELED") {
    return "The request was cancelled";
  }
  if (status === null) {
    return "Unable to connect to the server";
  }
  return `Request failed with status ${status}`;
}

function normalizeRequestId(value: unknown): string | null {
  if (typeof value !== "string") {
    return null;
  }

  const normalized = value.trim();
  return /^[A-Za-z0-9._:-]{1,128}$/.test(normalized) ? normalized : null;
}
