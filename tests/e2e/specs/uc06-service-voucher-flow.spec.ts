import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';
import { clickMerchantNav, loginMerchantWeb } from '../support/vue-web.js';

test.describe('UC06 用户购买非外卖服务并获得券码凭证', () => {
  test('用户购买团购套餐获得券码，商家端查询并核销', async ({ browser }) => {
    const api = newApi();
    await api.login('demo_user', '123456');
    const order = await api.createOrder(3, 'group_buy', [{ itemId: 2002, quantity: 1 }]);
    const paid = await api.pay(order.id);
    expect(paid.paymentStatus).toBe('paid');
    expect(paid.voucher?.voucherCode).toBeTruthy();
    const voucherCode = paid.voucher!.voucherCode;

    const merchantPage = await browser.newPage();
    await loginMerchantWeb(merchantPage, 'demo_groupbuy_merchant', '123456');
    await clickMerchantNav(merchantPage, '券码核销');
    await merchantPage.getByPlaceholder('输入用户出示的券码（先查后销）').fill(voucherCode);
    await merchantPage.getByRole('button', { name: '查询券码' }).click();
    await expect(merchantPage.getByText(order.orderNo, { exact: false }).first()).toBeVisible({
      timeout: 30_000,
    });
    await merchantPage.getByRole('button', { name: '确认核销' }).click();
    await expect(merchantPage.getByText(`券码 ${voucherCode} 已核销`, { exact: false }).first()).toBeVisible({
      timeout: 30_000,
    });
    await merchantPage.close();

    const detail = await api.orderDetail(order.id);
    expect(detail.voucher?.status).toBe('used');
    expect(detail.displayStatus).toBe('used');
  });
});
