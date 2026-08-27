import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';
import { clickMerchantNav, loginMerchantWeb, merchantOrderRow } from '../support/vue-web.js';

test.describe('UC08 用户申请取消与退款，商家处理退款', () => {
  test('用户取消未支付外卖订单，商家端处理已支付订单退款', async ({ browser }) => {
    const api = newApi();
    await api.login('demo_user', '123456');
    const address = await api.createAddress({});

    const unpaid = await api.createOrder(1, 'takeaway', [{ itemId: 1003, quantity: 1 }], address.id);
    await api.cancelOrder(unpaid.id);
    expect((await api.orderDetail(unpaid.id)).displayStatus).toBe('cancelled');

    const paid = await api.createOrder(1, 'takeaway', [{ itemId: 1003, quantity: 1 }], address.id);
    await api.pay(paid.id);
    expect((await api.orderDetail(paid.id)).paymentStatus).toBe('paid');

    const page = await browser.newPage();
    page.on('dialog', (dialog) => dialog.accept('E2E 商家退款'));
    await loginMerchantWeb(page, 'demo_merchant', '123456');
    await clickMerchantNav(page, '订单中心');
    const row = await merchantOrderRow(page, paid.orderNo);
    await row.getByRole('button', { name: '退款' }).click();
    await expect(page.getByText(`${paid.orderNo} 已退款`, { exact: false }).first()).toBeVisible({
      timeout: 30_000,
    });
    await page.close();

    const detail = await api.orderDetail(paid.id);
    expect(detail.displayStatus).toBe('refunded');
    expect(detail.refundStatus).toBe('succeeded');
    expect(detail.paymentStatus).toBe('refunded');
  });
});
