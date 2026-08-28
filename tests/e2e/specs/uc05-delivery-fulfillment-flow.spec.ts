import { expect, test } from '@playwright/test';
import { newApi, type StationMessage } from '../support/api-client.js';
import {
  clickMerchantNav,
  clickMerchantOrderAction,
  loginMerchantWeb,
  merchantOrderRow,
} from '../support/vue-web.js';

test.describe('UC05 商家处理外卖订单并推进配送履约，用户接收状态消息', () => {
  test('用户支付后商家在商家端接单、备餐、配送，用户端收到订单消息', async ({ browser }) => {
    const api = newApi();
    await api.login('demo_user', '123456');
    const address = await api.createAddress({});
    const order = await api.createOrder(
      1,
      'takeaway',
      [{ itemId: 1003, quantity: 1 }],
      address.id,
    );
    await api.pay(order.id);
    expect((await api.orderDetail(order.id)).fulfillmentStatus).toMatch(/merchant_pending|accepted/);

    const merchantPage = await browser.newPage();
    await loginMerchantWeb(merchantPage, 'demo_merchant', '123456');
    await clickMerchantNav(merchantPage, '订单中心');

    const row = await merchantOrderRow(merchantPage, order.orderNo);
    await row.getByRole('button', { name: '详情' }).click();
    await expect(row.getByText(order.orderNo, { exact: false }).first()).toBeVisible();

    await clickMerchantOrderAction(merchantPage, order.orderNo, '接单');
    await clickMerchantOrderAction(merchantPage, order.orderNo, '开始备餐');
    await clickMerchantOrderAction(merchantPage, order.orderNo, '出餐');
    await clickMerchantOrderAction(merchantPage, order.orderNo, '推进配送');
    await clickMerchantOrderAction(merchantPage, order.orderNo, '推进配送');
    await clickMerchantOrderAction(merchantPage, order.orderNo, '完成订单');
    await merchantPage.close();

    const userDetail = await api.orderDetail(order.id);
    expect(userDetail.fulfillmentStatus).toBe('completed');
    expect(userDetail.displayStatus).toBe('used');

    const timeline = await api.deliveryTimeline(order.id);
    const codes = timeline.nodes.map((n: { code: string }) => n.code);
    expect(codes).toContain('accepted');
    expect(codes).toContain('delivering');
    expect(codes).toContain('delivered');
    expect(codes).toContain('completed');

    const messages = await api.messages();
    const related = messages.list.find((m: StationMessage) => m.relatedOrderId === order.id);
    expect(related, '支付和履约消息应写入用户站内消息').toBeTruthy();
    expect(related!.title).toBeTruthy();
  });
});
