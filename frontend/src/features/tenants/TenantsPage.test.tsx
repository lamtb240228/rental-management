import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { TenantsPage } from "./TenantsPage";

vi.mock("./tenantApi", () => ({
  listTenants: async () => [],
  createTenant: vi.fn(),
  updateTenant: vi.fn(),
  listTenantContracts: async () => [],
}));

describe("TenantsPage", () => {
  it("renders tenant management UI", async () => {
    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <TenantsPage />
      </QueryClientProvider>,
    );

    expect(await screen.findByRole("heading", { name: "Người thuê" })).toBeInTheDocument();
    expect(screen.getByText("Thêm người thuê")).toBeInTheDocument();
  });
});
