import { expect, test } from '@playwright/test';
import type { BookingView, OrderDetail } from '../support/api-client';
import { E2eApi } from '../support/api-client';
import {
  createBooking,
  createServiceOrder,
  demo,
  loginMerchantPage,
  payOrder,
  uniqueText,
} from '../support/scenarios';

interface CatalogItem {
  id: number;
  title: string;
  stock: number;
  status: string;
}

interface UserCoupon {
  id: number;
  status: string;
  usedOrderId?: number;
}

test.describe('UC08 取消与退款闭环', () => {
  test('用户退款释放券码、预约、优惠券和库存', async ({ request }) => {
    const api = new E2eApi(request);
    const user = await api.loginUser();
    const merchant = await api.loginMerchant(demo.groupBuyMerchant);
    const marker = uniqueText('UC08-USER');

    const beforeItems = await api.get<CatalogItem[]>('/api/merchant/catalog/items', {
      token: merchant.token,
      params: { businessType: 'group_buy', keyword: '江南小馆 双人餐' },
    });
    const beforeItem = beforeItems.find((item) => item.id === 2002);
    expect(beforeItem).toBeDefined();

    const created = await createServiceOrder(api, user.token, marker, { couponId: 9002 });
    const paid = await payOrder(api, user.token, created.id);
    const booking = await createBooking(api, user.token, created.id);

    expect(paid.paymentStatus).toBe('paid');
    expect(paid.voucher?.status).toBe('unused');
    expect(booking.storeConfirmStatus).toBe('pending');

    const refunded = await api.post<OrderDetail>(`/api/app/trade/orders/${created.id}/refund`, {
      token: user.token,
      data: { reason: `${marker}-用户申请退款` },
    });

    expect(refunded.displayStatus).toBe('refunded');
    expect(refunded.paymentStatus).toBe('refunded');
    expect(refunded.fulfillmentStatus).toBe('refunded');
    expect(refunded.refundStatus).toBe('succeeded');
    expect(refunded.voucher?.status).toBe('refunded');
    expect(refunded.booking?.storeConfirmStatus).toBe('cancelled');

    const refreshedBooking = await api.get<BookingView>(`/api/app/trade/orders/${created.id}/booking`, {
      token: user.token,
    });
    expect(refreshedBooking.storeConfirmStatus).toBe('cancelled');

    const usableCoupons = await api.get<UserCoupon[]>('/api/app/account/coupons', {
      token: user.token,
      params: { status: 'usable' },
    });
    expect(usableCoupons).toContainEqual(expect.objectContaining({ id: 9002, status: 'unused' }));
    expect(usableCoupons.find((coupon) => coupon.id === 9002)?.usedOrderId).toBeFalsy();

    const afterItems = await api.get<CatalogItem[]>('/api/merchant/catalog/items', {
      token: merchant.token,
      params: { businessType: 'group_buy', keyword: '江南小馆 双人餐' },
    });
    expect(afterItems.find((item) => item.id === 2002)?.stock).toBe(beforeItem?.stock);
  });

  test('商家通过订单中心处理人工退款', async ({ page, request }) => {
    const api = new E2eApi(request);
    const user = await api.loginUser();
    const marker = uniqueText('UC08-MERCHANT');
    const created = await createServiceOrder(api, user.token, marker);
    const paid = await payOrder(api, user.token, created.id);
    expect(paid.paymentStatus).toBe('paid');

    await loginMerchantPage(page, demo.groupBuyMerchant);
    await page.getByRole('button', { name: /订单中心/ }).click();

    const row = page.getByTestId(`merchant-order-${created.id}`);
    await expect(row).toContainText(created.orderNo);
    page.once('dialog', (dialog) => dialog.accept(`${marker}-商家人工退款`));
    await row.getByRole('button', { name: '退款', exact: true }).click();

    await expect(page.locator('.notice-bar')).toContainText(`${created.orderNo} 已退款`);
    await expect(page.getByTestId(`merchant-order-${created.id}`)).toContainText('已退款');

    const detail = await api.get<OrderDetail>(`/api/app/trade/orders/${created.id}`, { token: user.token });
    expect(detail.refundStatus).toBe('succeeded');
    expect(detail.refundReason).toBe(`${marker}-商家人工退款`);
    expect(detail.voucher?.status).toBe('refunded');
  });
});
