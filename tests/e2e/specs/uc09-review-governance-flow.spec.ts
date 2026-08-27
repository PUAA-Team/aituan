import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';
import { clickAdminNav, clickMerchantNav, loginAdminWeb, loginMerchantWeb } from '../support/vue-web.js';

test.describe('UC09 用户发布评价，商家回复，平台审核治理', () => {
  test('完成订单后发布评价，商家端回复，后台端审核屏蔽', async ({ browser }) => {
    const api = newApi();
    await api.login('demo_user', '123456');
    const address = await api.createAddress({});
    const order = await api.createOrder(1, 'takeaway', [{ itemId: 1003, quantity: 1 }], address.id);
    await api.pay(order.id);
    const merchantApi = newApi();
    await merchantApi.login('demo_merchant', '123456');
    await merchantApi.completeTakeawayOrder(order.id);
    expect((await api.orderDetail(order.id)).displayStatus).toBe('used');

    const reviewContent = `E2E 评价-${order.orderNo}`;
    const review = await api.createReview(order.id, {
      rating: 5,
      content: reviewContent,
      labels: ['好吃'],
      imageUrls: [],
    });
    expect(review.id).toBeGreaterThan(0);

    const merchantPage = await browser.newPage();
    await loginMerchantWeb(merchantPage, 'demo_merchant', '123456');
    await clickMerchantNav(merchantPage, '评价管理');
    const reviewRow = merchantPage.locator('tbody tr', { hasText: reviewContent }).first();
    await reviewRow.click();
    await merchantPage.getByPlaceholder('感谢您的反馈…').fill('感谢惠顾，欢迎再来');
    await merchantPage.getByRole('button', { name: '发送回复' }).click();
    await expect(merchantPage.getByText('回复已发送', { exact: false }).first()).toBeVisible({
      timeout: 30_000,
    });
    await merchantPage.close();

    const replied = await api.reviewDetail(review.id);
    expect(replied.replied).toBe(true);
    expect(replied.replyContent).toContain('感谢惠顾');

    const adminPage = await browser.newPage();
    await loginAdminWeb(adminPage, 'demo_admin', '123456');
    await clickAdminNav(adminPage, '评价审核');
    const adminRow = adminPage.locator('tbody tr', { hasText: reviewContent }).first();
    await adminRow.click();
    await adminPage.getByRole('button', { name: '屏蔽', exact: true }).click();
    await expect(adminPage.getByText(/已屏蔽/).first()).toBeVisible({ timeout: 30_000 });
    await adminPage.close();

    const audited = await api.reviewDetail(review.id);
    expect(audited.status).toBe('hidden');
  });
});
