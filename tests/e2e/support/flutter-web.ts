import { expect, type Page } from '@playwright/test';

export async function openUserWeb(page: Page, path = '/web/') {
  await page.goto(path, { waitUntil: 'load' });
  await enableFlutterSemantics(page);
}

export async function enableFlutterSemantics(page: Page) {
  const placeholder = page.locator('flt-semantics-placeholder');
  await placeholder.waitFor({ state: 'attached', timeout: 90_000 });
  await placeholder.dispatchEvent('click');
  await page.waitForSelector('flt-semantics', { timeout: 30_000 });
}

export async function loginViaFlutterWeb(page: Page, account: string, password: string) {
  await page.getByText('登录 / 注册', { exact: false }).first().click({ timeout: 60_000 });
  const accountBox = page.getByRole('textbox', { name: '手机号或邮箱' });
  await enterFlutterText(accountBox, account);
  const passwordBox = page.getByRole('textbox', { name: '密码' });
  await enterFlutterText(passwordBox, password);
  const loginButton = page.getByRole('button', { name: '登录' });
  const homeHint = page.getByText('搜索外卖、团购、景点、洗脚', { exact: false }).first();
  const loginResponse = page.waitForResponse(
    (response) => response.url().includes('/api/open/auth/user/login/password')
      && response.request().method() === 'POST',
    { timeout: 30_000 },
  );
  await loginButton.click({ timeout: 30_000 });
  expect((await loginResponse).ok()).toBeTruthy();
  await homeHint.waitFor({ state: 'visible', timeout: 60_000 });
}

async function enterFlutterText(
  textbox: ReturnType<Page['getByRole']>,
  value: string,
) {
  await textbox.click({ timeout: 30_000 });
  await textbox.press('ControlOrMeta+A');
  await textbox.press('Backspace');
  await textbox.pressSequentially(value, { delay: 20 });
}

export async function openUserOrdersPage(page: Page) {
  await page.getByRole('tab').nth(2).click();
  await expect(page.getByText('我的订单', { exact: false }).first()).toBeVisible({ timeout: 30_000 });
}

export async function openUserProfilePage(page: Page) {
  await page.getByRole('tab').nth(3).click();
  await expect(page.getByRole('button', { name: /退出/ }).first()).toBeVisible({ timeout: 30_000 });
}

export async function expectFlutterText(page: Page, text: string, timeout = 30_000) {
  await expect(page.getByText(text, { exact: false }).first()).toBeVisible({ timeout });
}

export async function expectFlutterRole(page: Page, role: 'button' | 'group' | 'tab', name: string | RegExp, timeout = 30_000) {
  await expect(page.getByRole(role, { name }).first()).toBeVisible({ timeout });
}
