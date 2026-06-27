import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { PropertiesPage } from "./PropertiesPage";

vi.mock("./propertyApi", () => ({
  listProperties: async () => [],
  listRooms: async () => [],
  createProperty: vi.fn(),
  createRoom: vi.fn(),
}));

describe("PropertiesPage", () => {
  it("renders property workflow", async () => {
    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <PropertiesPage />
      </QueryClientProvider>,
    );

    expect(await screen.findByText("Khu trọ và phòng")).toBeInTheDocument();
    expect(screen.getAllByText("Thêm khu trọ").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Thêm phòng").length).toBeGreaterThan(0);
  });
});
