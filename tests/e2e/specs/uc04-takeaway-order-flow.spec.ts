import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';

test.describe('UC04 用户完成外卖点单、优惠结算和模拟支付', () => {
  test('登录用户下单、结算并模拟支付，订单状态完整', async () => {
    const api = newApi();
    await api.login('demo_user', '123456');

    const address = await api.createAddress({});
    const preview = await api.preview(1, 'takeaway', [{ itemId: 1002, quantity: 2 }], address.id);
    expect(preview.payableAmount).toBeGreaterThan(0);

    const order = await api.createOrder(
      1,
      'takeaway',
      [{ itemId: 1002, quantity: 2 }],
      address.id,
    );
    expect(order.id).toBeGreaterThan(0);
    expect(order.paymentStatus).toBe('unpaid');

    const paid = await api.pay(order.id);
    expect(paid.paymentStatus).toBe('paid');
    expect(paid.fulfillmentStatus).toMatch(/merchant_pending|accepted/);

    const detail = await api.orderDetail(order.id);
    expect(detail.orderNo).toBe(order.orderNo);
    expect(detail.items?.some((i: { itemId: number }) => i.itemId === 1002)).toBe(true);
  });
});
