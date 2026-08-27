import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';

test.describe('UC12 用户查看会员成长值、领取并下单使用优惠券', () => {
  test('新用户领取优惠券并在下单时抵扣，支付后成长值增加', async () => {
    const api = newApi();
    await api.createUser('uc12');

    const initial = await api.memberInfo();
    expect(initial.growthValue).toBe(0);

    const available = await api.availableCoupons();
    const template = available.find((c) => c.templateId === 3);
    expect(template, '新人9折券应可领取').toBeTruthy();
    await api.claimCoupon(3);

    const coupons = await api.myCoupons();
    const claimed = coupons.find((c) => c.templateId === 3 && c.status === 'unused');
    expect(claimed).toBeTruthy();

    const address = await api.createAddress({});
    const items = [{ itemId: 1004, quantity: 2 }];
    const preview = await api.preview(1, 'takeaway', items, address.id, claimed!.id);
    expect(preview.discountAmount).toBeGreaterThan(0);
    const undiscounted = (preview.amount ?? 0) + (preview.deliveryFee ?? 0);
    expect(preview.payableAmount).toBeLessThan(undiscounted);

    const order = await api.createOrder(1, 'takeaway', items, address.id, claimed!.id);
    expect(order.discountAmount).toBeGreaterThan(0);
    await api.pay(order.id);

    const merchantApi = newApi();
    await merchantApi.login('demo_merchant', '123456');
    await merchantApi.completeTakeawayOrder(order.id);

    const after = await api.memberInfo();
    expect(after.growthValue).toBeGreaterThan(0);
  });
});
