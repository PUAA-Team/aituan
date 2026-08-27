import { expect, type Page } from '@playwright/test';

export async function openUserWeb(page: Page, path = '/web/') {
  await page.goto(path, { waitUntil: 'load' });
  await enableFlutterSemantics(page);
}

export async function enableFlutterSemantics(page: Page) {
  await page.waitForSelector('flt-semantics-placeholder', { timeout: 90_000 });
  const placeholder = page.locator('flt-semantics-placeholder');
  if (await placeholder.count()) {
    await placeholder.focus();
    await page.keyboard.press('Enter');
  }
  await page.waitForSelector('flt-semantics', { timeout: 30_000 });
}

export async function loginViaFlutterWeb(page: Page, account: string, password: string) {
  await page.getByText('登录 / 注册', { exact: false }).first().click({ timeout: 60_000 });
  const accountBox = page.getByRole('textbox', { name: '手机号或邮箱' });
  await accountBox.click({ timeout: 30_000 });
  await page.keyboard.type(account, { delay: 20 });
  const passwordBox = page.getByRole('textbox', { name: '密码' });
  await passwordBox.click({ timeout: 30_000 });
  await page.keyboard.type(password, { delay: 30 });
  await page.waitForTimeout(1500);
  const loginButton = page.getByRole('button', { name: '登录' });
  const homeHint = page.getByText('搜索外卖、团购、景点、洗脚', { exact: false }).first();
  for (let attempt = 0; attempt < 3; attempt += 1) {
    const box = await loginButton.boundingBox();
    if (box) {
      await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
      await page.waitForTimeout(800);
    }
    if (await homeHint.isVisible().catch(() => false)) return;
    if (await page.getByRole('textbox', { name: '密码' }).isVisible().catch(() => false)) {
      const passwordBox = page.getByRole('textbox', { name: '密码' });
      await passwordBox.press('Tab');
      await page.waitForTimeout(500);
    }
  }
  await homeHint.waitFor({ state: 'visible', timeout: 60_000 });
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

export async function expectFlutterRole(
  page: Page,
  role: 'button' | 'group' | 'tab',
  name: string | RegExp,
  timeout = 30_000,
) {
  await expect(page.getByRole(role, { name }).first()).toBeVisible({ timeout });
}
