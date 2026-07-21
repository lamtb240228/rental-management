import {
  AxiosError,
  AxiosHeaders,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from "axios";
import { describe, expect, it } from "vitest";
import { ApplicationError, toApplicationError } from "./ApplicationError";

describe("ApplicationError", () => {
  it("keeps only the safe error contract when converting an Axios failure", () => {
    const accessSecret = "Bearer access-secret";
    const passwordSecret = "password-secret";
    const config = {
      url: "/auth/login",
      method: "post",
      headers: new AxiosHeaders({ Authorization: accessSecret }),
      data: JSON.stringify({ email: "person@example.test", password: passwordSecret }),
    } as InternalAxiosRequestConfig;
    const response: AxiosResponse = {
      config,
      data: {
        status: 401,
        error: "Unauthorized",
        message: "Email or password is incorrect",
      },
      headers: new AxiosHeaders({ "x-request-id": "request-123" }),
      status: 401,
      statusText: "Unauthorized",
    };
    const raw = new AxiosError(
      "Request failed",
      AxiosError.ERR_BAD_REQUEST,
      config,
      undefined,
      response,
    );

    const safe = toApplicationError(raw);

    expect(safe).toBeInstanceOf(ApplicationError);
    expect(JSON.parse(JSON.stringify(safe))).toEqual({
      status: 401,
      code: "UNAUTHORIZED",
      message: "Email or password is incorrect",
      requestId: "request-123",
    });
    const serialized = JSON.stringify(safe);
    expect(serialized).not.toContain(accessSecret);
    expect(serialized).not.toContain(passwordSecret);
    expect(serialized).not.toContain("person@example.test");
    expect(serialized).not.toContain("config");
    expect((safe as { config?: unknown }).config).toBeUndefined();
    expect((safe as { cause?: unknown }).cause).toBeUndefined();
  });

  it("does not reuse a potentially sensitive message from an unknown exception", () => {
    const safe = toApplicationError(new Error("secret-from-an-untrusted-exception"));

    expect(safe).toMatchObject({
      status: null,
      code: "REQUEST_FAILED",
      message: "The request could not be completed",
      requestId: null,
    });
    expect(JSON.stringify(safe)).not.toContain("secret-from-an-untrusted-exception");
  });
});
