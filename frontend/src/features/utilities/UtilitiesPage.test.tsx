import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { UtilitiesPage } from "./UtilitiesPage";

vi.mock("../properties/propertyApi", () => ({
  listProperties: async () => [],
  listRooms: async () => [],
}));

vi.mock("./utilityApi", () => ({
  listUtilityReadings: async () => [],
  createUtilityReading: vi.fn(),
  updateUtilityReading: vi.fn(),
}));

describe("UtilitiesPage", () => {
  it("renders the utility reading workflow", async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <UtilitiesPage />
      </QueryClientProvider>,
    );

    expect(await screen.findByRole("heading", { name: "Chỉ số điện nước" })).toBeInTheDocument();
    expect(screen.getByText("Ghi chỉ số mới")).toBeInTheDocument();
  });
});
