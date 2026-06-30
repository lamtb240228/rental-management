import { expect, test, type Page } from "@playwright/test";

async function login(page: Page, email: string) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(email);
  await page.getByLabel("Mật khẩu").fill("Password123!");
  await page.getByRole("button", { name: "Đăng nhập" }).click();
  await expect(page).toHaveURL(/\/$/);
}

async function navigate(page: Page, name: string) {
  const visibleLink = () => page.locator("a:visible").filter({ hasText: name }).first();
  if (!(await visibleLink().isVisible())) {
    await page.getByRole("button", { name: "Menu" }).click();
  }
  await visibleLink().click();
}

test("landlord can use the monthly rental workflow", async ({ page }) => {
  await login(page, "demo@rental.local");
  await expect(page.getByRole("heading", { name: "Dashboard vận hành" })).toBeVisible();

  await navigate(page, "Khu trọ");
  await expect(page.getByRole("heading", { name: "Khu trọ và phòng" })).toBeVisible();
  await expect(page.getByText("Demo Property", { exact: true }).first()).toBeVisible();

  await navigate(page, "Điện nước");
  await expect(page.getByRole("heading", { name: "Chỉ số điện nước" })).toBeVisible();
  await expect(page.getByText("Lịch sử phòng 101")).toBeVisible();

  await navigate(page, "Hóa đơn");
  await expect(page.getByRole("heading", { name: "Hóa đơn", exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Thu tiền" }).first().click();
  await expect(page.getByText(/Thu tiền INV-DEMO-202606/)).toBeVisible();
});

test("tenant sees only the tenant portal", async ({ page }) => {
  await login(page, "tenant@rental.local");
  await expect(page.getByRole("heading", { name: "Xin chào, Demo Tenant" })).toBeVisible();
  await expect(page.getByText("101", { exact: true }).first()).toBeVisible();
  await expect(page.getByRole("link", { name: "Khu trọ" })).toHaveCount(0);
});

test("admin sees system users and account actions", async ({ page }) => {
  await login(page, "admin@rental.local");
  await expect(page.getByRole("heading", { name: "Tổng quan dữ liệu" })).toBeVisible();
  await expect(page.locator("p:visible", { hasText: "tenant@rental.local" }).first()).toBeVisible();
  await expect(page.locator("button:visible").filter({ hasText: "Khóa" }).first()).toBeVisible();
});
