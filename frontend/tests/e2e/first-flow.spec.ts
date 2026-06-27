import { test, expect } from "@playwright/test";

test("login page is available", async ({ page }) => {
  await page.goto("/login");
  await expect(page.getByRole("heading", { name: "Đăng nhập" })).toBeVisible();
});
