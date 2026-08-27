import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';
import { clickAdminNav, loginAdminWeb } from '../support/vue-web.js';

test.describe('UC10 用户提交投诉，平台受理、处理并关闭', () => {
  test('用户投诉订单，后台端完成受理、处理、关闭', async ({ browser }) => {
    const api = newApi();
    await api.login('demo_user', '123456');
    const order = await api.createOrder(3, 'group_buy', [{ itemId: 2002, quantity: 1 }]);
    await api.pay(order.id);

    const complaint = await api.createComplaint({
      orderId: order.id,
      category: 'quality',
      title: 'E2E 投诉工单',
      detail: 'E2E 投诉详情：商品与描述不符',
      evidenceUrls: [],
    });
    expect(complaint.id).toBeGreaterThan(0);

    const page = await browser.newPage();
    await loginAdminWeb(page, 'demo_admin', '123456');
    await clickAdminNav(page, '投诉工单');
    await page.getByPlaceholder('订单号').fill(order.orderNo);
    await page.keyboard.press('Enter');
    const row = page.locator('tbody tr', { hasText: complaint.ticketNo }).first();
    await row.click();

    await page.getByRole('button', { name: '受理', exact: true }).click();
    await expect(page.getByText('已受理', { exact: false }).first()).toBeVisible({ timeout: 30_000 });
    await page.getByRole('button', { name: '处理完成', exact: true }).click();
    await expect(page.getByText('已处理完成', { exact: false }).first()).toBeVisible({ timeout: 30_000 });
    await page.getByRole('button', { name: '关闭', exact: true }).click();
    await expect(page.getByText('已关闭', { exact: false }).first()).toBeVisible({ timeout: 30_000 });
    await page.close();

    const detail = await api.complaintDetail(complaint.id);
    expect(detail.complaint.status).toBe('closed');
  });
});
