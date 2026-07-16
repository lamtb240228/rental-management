import { expect, test, type Page, type Response } from "@playwright/test";

const DEMO_EMAIL = "demo@rental.local";
const DEMO_PASSWORD = "Password123!";
const LEGACY_TOKEN_KEY = "rental_access_token";

type AuthResponseBody = {
  data: {
    accessToken: string;
  };
};

async function login(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Email").fill(DEMO_EMAIL);
  await page.getByLabel("Mật khẩu").fill(DEMO_PASSWORD);

  const responsePromise = page.waitForResponse(isAuthResponse("/api/auth/login"));
  await page.getByRole("button", { name: "Đăng nhập" }).click();
  const response = await responsePromise;

  expect(response.ok()).toBe(true);
  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole("heading", { name: "Dashboard vận hành" })).toBeVisible();
  return (await response.json() as AuthResponseBody).data.accessToken;
}

async function expectNoJwtInBrowserStorage(page: Page, accessToken?: string) {
  const storage = await page.evaluate(() => ({
    local: Object.fromEntries(
      Array.from({ length: window.localStorage.length }, (_, index) => {
        const key = window.localStorage.key(index)!;
        return [key, window.localStorage.getItem(key)];
      }),
    ),
    session: Object.fromEntries(
      Array.from({ length: window.sessionStorage.length }, (_, index) => {
        const key = window.sessionStorage.key(index)!;
        return [key, window.sessionStorage.getItem(key)];
      }),
    ),
  }));

  expect(storage.local[LEGACY_TOKEN_KEY]).toBeUndefined();
  expect(storage.session[LEGACY_TOKEN_KEY]).toBeUndefined();

  const serialized = JSON.stringify(storage);
  expect(serialized).not.toMatch(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/);
  if (accessToken) {
    expect(serialized).not.toContain(accessToken);
  }
}

test("login keeps the access JWT out of localStorage and sessionStorage", async ({ page }) => {
  const accessToken = await login(page);

  await expectNoJwtInBrowserStorage(page, accessToken);
});

test("reload restores the authenticated session through refresh", async ({ page }) => {
  await login(page);
  const refreshResponse = page.waitForResponse(isAuthResponse("/api/auth/refresh"));

  await page.reload();

  expect((await refreshResponse).ok()).toBe(true);
  await expect(page.getByRole("heading", { name: "Dashboard vận hành" })).toBeVisible();
  await expectNoJwtInBrowserStorage(page);
});

test("logout revokes the current session and reload remains logged out", async ({ page }) => {
  await login(page);
  const logoutResponse = page.waitForResponse(isAuthResponse("/api/auth/logout"));

  await page.getByRole("button", { name: "Đăng xuất" }).click();

  expect((await logoutResponse).status()).toBe(204);
  await expect(page).toHaveURL(/\/login$/);

  const rejectedRefresh = page.waitForResponse(isAuthResponse("/api/auth/refresh"));
  await page.reload();
  expect((await rejectedRefresh).ok()).toBe(false);

  await page.goto("/");
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("heading", { name: "Đăng nhập" })).toBeVisible();
  await expectNoJwtInBrowserStorage(page);
});

test("logout is propagated to another page in the same browser context", async ({ context, page }) => {
  await login(page);

  const secondPage = await context.newPage();
  const secondPageRefresh = secondPage.waitForResponse(isAuthResponse("/api/auth/refresh"));
  await secondPage.goto("/");
  expect((await secondPageRefresh).ok()).toBe(true);
  await expect(secondPage.getByRole("heading", { name: "Dashboard vận hành" })).toBeVisible();

  const logoutResponse = page.waitForResponse(isAuthResponse("/api/auth/logout"));
  await page.getByRole("button", { name: "Đăng xuất" }).click();

  expect((await logoutResponse).status()).toBe(204);
  await expect(page).toHaveURL(/\/login$/);
  await expect(secondPage).toHaveURL(/\/login$/);
  await expect(secondPage.getByRole("heading", { name: "Đăng nhập" })).toBeVisible();
  await expectNoJwtInBrowserStorage(secondPage);
});

function isAuthResponse(pathname: string) {
  return (response: Response) =>
    response.request().method() === "POST" &&
    new URL(response.url()).pathname.endsWith(pathname);
}
