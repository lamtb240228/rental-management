import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PropertyForm } from "./PropertyForm";

async function fillRequiredFields() {
  await userEvent.type(screen.getByLabelText("Tên khu trọ"), "Khu trọ A");
  await userEvent.type(screen.getByLabelText("Địa chỉ"), "12 Đường A");
  await userEvent.type(screen.getByLabelText("Tỉnh thành"), "TP HCM");
}

describe("PropertyForm", () => {
  it("keeps entered values when the mutation fails", async () => {
    const onSubmit = vi.fn().mockRejectedValue(new Error("request failed"));
    render(<PropertyForm isSubmitting={false} onSubmit={onSubmit} />);
    await fillRequiredFields();

    await userEvent.click(screen.getByRole("button", { name: "Thêm khu trọ" }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalledOnce());

    expect(screen.getByLabelText("Tên khu trọ")).toHaveValue("Khu trọ A");
  });

  it("resets create values only after the mutation succeeds", async () => {
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<PropertyForm isSubmitting={false} onSubmit={onSubmit} />);
    await fillRequiredFields();

    await userEvent.click(screen.getByRole("button", { name: "Thêm khu trọ" }));

    await waitFor(() => expect(screen.getByLabelText("Tên khu trọ")).toHaveValue(""));
  });
});
