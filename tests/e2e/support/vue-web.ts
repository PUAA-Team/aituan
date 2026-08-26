import { expect, type Page } from '@playwright/test';

export async function loginMerchantWeb(
  page: Page,
  account: string,
  password: string,
  path = '/merchant/',
) {
  await page.goto(path, { waitUntil: 'load' });
  await page.getByLabel('账号').fill(account);
  await page.getByLabel('密码').fill(password);
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page.getByText('经营概览', { exact: true }).first()).toBeVisible({ timeout: 30_000 });
}

export async function loginAdminWeb(page: Page, account: string, password: string, path = '/admin/') {
  await page.goto(path, { waitUntil: 'load' });
  await page.getByLabel('账号').fill(account);
  await page.getByLabel('密码').fill(password);
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page.getByText('平台总览', { exact: true }).first()).toBeVisible({ timeout: 30_000 });
}

export async function clickMerchantNav(page: Page, label: string) {
  const button = page
    .locator('button.nav-item')
    .filter({ has: page.locator('strong', { hasText: label }) })
    .first();
  await button.click();
}

export async function merchantOrderRow(page: Page, orderNo: string) {
  const row = page.locator('tbody tr', { hasText: orderNo }).first();
  await expect(row).toBeVisible({ timeout: 30_000 });
  return row;
}

export async function clickMerchantOrderAction(page: Page, orderNo: string, action: string) {
  const row = await merchantOrderRow(page, orderNo);
  await row.getByRole('button', { name: action }).first().click();
  await expect(page.getByText(`${orderNo} 已${action}`, { exact: false }).first()).toBeVisible({
    timeout: 30_000,
  });
}
