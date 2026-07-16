import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { clearToken } from "../../lib/api/client";
import { queryClient } from "../../lib/query-client/queryClient";
import { AuthProvider, useAuth } from "./AuthProvider";

vi.mock("./authApi", () => ({ me: vi.fn() }));

function SessionProbe() {
  const { signIn, user } = useAuth();

  return (
    <div>
      <span>{user?.email ?? "anonymous"}</span>
      <button
        type="button"
        onClick={() => signIn({
          accessToken: "account-b-session",
          tokenType: "Bearer",
          user: {
            id: 2,
            email: "account-b@example.test",
            fullName: "Account B",
            roles: ["LANDLORD"],
          },
        })}
      >
        Đổi tài khoản
      </button>
    </div>
  );
}

describe("AuthProvider", () => {
  beforeEach(() => {
    clearToken();
    queryClient.clear();
  });

  afterEach(() => {
    clearToken();
    queryClient.clear();
  });

  it("clears account-scoped query data before accepting another identity", async () => {
    queryClient.setQueryData(["tenants"], [{ id: 1, fullName: "Account A data" }]);

    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <SessionProbe />
        </AuthProvider>
      </QueryClientProvider>,
    );

    await userEvent.click(screen.getByRole("button", { name: "Đổi tài khoản" }));

    expect(queryClient.getQueryData(["tenants"])).toBeUndefined();
    expect(await screen.findByText("account-b@example.test")).toBeInTheDocument();
  });
});
