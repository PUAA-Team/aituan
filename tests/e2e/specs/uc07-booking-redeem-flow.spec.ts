import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';
import { clickMerchantNav, loginMerchantWeb } from '../support/vue-web.js';

test.describe('UC07 用户预约到店/团购服务并由商家核销确认', () => {
  test('用户购买预约服务并填写预约信息，商家端确认预约', async ({ browser }) => {
    const api = newApi();
    await api.login('demo_user', '123456');
    const order = await api.createOrder(9, 'massage', [{ itemId: 8001, quantity: 1 }]);
    await api.pay(order.id);
    const booking = await api.upsertBooking(order.id, {
      contactName: 'E2E 预约人',
      contactPhone: '18800009999',
      bookingDate: '2026-09-30',
      bookingTimeSlot: '14:00-15:00',
      guestCount: 1,
    });
    expect(booking.storeConfirmStatus).toBe('pending');

    const detail = await api.orderDetail(order.id);
    expect(detail.booking?.storeConfirmStatus).toBe('pending');

    const merchantPage = await browser.newPage();
    await loginMerchantWeb(merchantPage, 'demo_massage_merchant', '123456');
    await clickMerchantNav(merchantPage, '预约确认');
    const row = merchantPage.locator('tbody tr', { hasText: order.orderNo }).first();
    await expect(row).toBeVisible({ timeout: 30_000 });
    await row.getByRole('button', { name: '确认预约' }).click();
    await expect(row.getByText('已确认', { exact: true }).first()).toBeVisible({ timeout: 30_000 });
    await merchantPage.close();

    const confirmed = await api.orderDetail(order.id);
    expect(confirmed.booking?.storeConfirmStatus).toBe('confirmed');
    expect(confirmed.booking?.orderId).toBe(order.id);
  });
});
