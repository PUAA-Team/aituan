import type { Page } from '@playwright/test';
import { expect } from '@playwright/test';
import { adminBaseUrl, merchantBaseUrl } from '../playwright.config';
import type { BookingView, OrderDetail } from './api-client';
import { E2eApi } from './api-client';

export const demo = {
  password: '123456',
  user: 'demo_user',
  admin: 'demo_admin',
  takeawayMerchant: 'demo_merchant',
  groupBuyMerchant: 'demo_groupbuy_merchant',
} as const;

export function uniqueText(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export async function createServiceOrder(
  api: E2eApi,
  userToken: string,
  marker: string,
  options: { couponId?: number; itemId?: number; quantity?: number } = {},
) {
  return api.post<OrderDetail>('/api/app/trade/orders', {
    token: userToken,
    data: {
      storeId: 3,
      businessType: 'group_buy',
      items: [{ itemId: options.itemId ?? 2002, quantity: options.quantity ?? 1 }],
      remark: marker,
      couponId: options.couponId,
      idempotencyKey: marker,
    },
  });
}

export async function createTakeawayOrder(api: E2eApi, userToken: string, marker: string) {
  const address = await api.post<{ id: number }>('/api/app/account/addresses', {
    token: userToken,
    data: {
      contactName: '端到端测试用户',
      contactPhone: '18800001111',
      province: '北京市',
      city: '北京市',
      district: '海淀区',
      detailAddress: `城市广场 E2E ${marker}`,
      longitude: 116.3137,
      latitude: 39.9824,
      tagName: 'E2E',
      isDefault: false,
      deliveryNote: marker,
    },
  });
  return api.post<OrderDetail>('/api/app/trade/orders', {
    token: userToken,
    data: {
      storeId: 1,
      businessType: 'takeaway',
      addressId: address.id,
      items: [{ itemId: 1002, quantity: 2 }],
      remark: marker,
      tablewareOption: 'none',
      idempotencyKey: marker,
    },
  });
}

export async function payOrder(api: E2eApi, userToken: string, orderId: number) {
  return api.post<OrderDetail>(`/api/app/trade/orders/${orderId}/pay`, {
    token: userToken,
    data: { paymentMode: 'mock' },
  });
}

export async function createBooking(api: E2eApi, userToken: string, orderId: number) {
  const date = new Date();
  date.setDate(date.getDate() + 7);
  const bookingDate = date.toISOString().slice(0, 10);
  return api.post<BookingView>(`/api/app/trade/orders/${orderId}/booking`, {
    token: userToken,
    data: {
      contactName: '端到端测试用户',
      contactPhone: '18800001111',
      bookingDate,
      bookingTimeSlot: '18:00-19:00',
      guestCount: 2,
      remark: 'UC08 自动化预约',
    },
  });
}

export async function completeTakeawayOrder(
  api: E2eApi,
  merchantToken: string,
  orderId: number,
) {
  const prefix = `/api/merchant/trade/orders/${orderId}`;
  await api.post<OrderDetail>(`${prefix}/accept`, { token: merchantToken, data: { remark: 'E2E 接单' } });
  await api.post<OrderDetail>(`${prefix}/prepare`, { token: merchantToken, data: { remark: 'E2E 备餐' } });
  await api.post<OrderDetail>(`${prefix}/ready`, { token: merchantToken, data: { remark: 'E2E 出餐' } });
  await api.post<OrderDetail>(`${prefix}/delivery/advance`, { token: merchantToken });
  await api.post<OrderDetail>(`${prefix}/delivery/advance`, { token: merchantToken });
  return api.post<OrderDetail>(`${prefix}/complete`, {
    token: merchantToken,
    data: { remark: 'E2E 完成' },
  });
}

export async function loginMerchantPage(page: Page, account: string) {
  await page.goto(merchantBaseUrl);
  await page.getByLabel('账号').fill(account);
  await page.getByLabel('密码').fill(demo.password);
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await expect(page.getByRole('heading', { name: '经营概览' })).toBeVisible();
}

export async function loginAdminPage(page: Page) {
  await page.goto(adminBaseUrl);
  await page.getByLabel('账号').fill(demo.admin);
  await page.getByLabel('密码').fill(demo.password);
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await expect(page.getByRole('heading', { name: '平台总览' })).toBeVisible();
}
