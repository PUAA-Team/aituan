import { beforeEach, describe, expect, test, vi } from 'vitest';

import {
  adminRedeemVoucher,
  auditGovernanceReview,
  clearToken,
  complaintAction,
  confirmBooking,
  createCatalogCategory,
  createCatalogItem,
  createCouponTemplate,
  createMemberLevel,
  fetchBookings,
  fetchCatalogItems,
  fetchCouponTemplates,
  fetchGovernanceComplaints,
  fetchGovernanceReviews,
  fetchMemberLevels,
  fetchMerchants,
  fetchOrders,
  fetchPlatformSupportSessions,
  fetchStores,
  getToken,
  login,
  refundOrder,
  resolveAssetUrl,
  sendPlatformSupportMessage,
  setToken,
  updateCatalogItemStatus,
  updateConfig,
  uploadStoreCover,
} from './api';

let fetchMock: ReturnType<typeof vi.fn>;

function expectedUrl(path: string) {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
  return `${baseUrl}${path}`;
}

function mockApiResponse(data: unknown, options: { status?: number; code?: number; message?: string } = {}) {
  const status = options.status ?? 200;
  const code = options.code ?? 0;
  fetchMock.mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    text: async () => JSON.stringify({ code, message: options.message ?? 'success', data }),
  });
}

function lastRequest() {
  const [url, options] = fetchMock.mock.calls[fetchMock.mock.calls.length - 1] ?? [];
  return { url: String(url), options: options as RequestInit & { headers: Record<string, string> } };
}

describe('admin api', () => {
  beforeEach(() => {
    localStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  test('login 使用管理员登录接口且保存 admin token 和账号', async () => {
    mockApiResponse({ token: 'admin-token', profile: { nickname: '管理员' } });

    const session = await login('demo_admin', '123456');
    const { url, options } = lastRequest();

    expect(session.token).toBe('admin-token');
    expect(getToken()).toBe('admin-token');
    expect(localStorage.getItem('aituan_admin_account')).toBe('demo_admin');
    expect(localStorage.getItem('aituan_merchant_token')).toBeNull();
    expect(url).toBe(expectedUrl('/api/open/auth/admin/login/password'));
    expect(options.method).toBe('POST');
    expect(options.headers.Authorization).toBeUndefined();
    expect(options.headers['Content-Type']).toBe('application/json');
  });

  test('普通 JSON 请求带 Authorization，clearToken 后不再携带', async () => {
    setToken('admin-token');
    mockApiResponse([]);

    await updateConfig('site.notice/title', '欢迎', '测试');
    const updateRequest = lastRequest();

    expect(updateRequest.url).toContain('/api/admin/configs/site.notice%2Ftitle');
    expect(updateRequest.options.method).toBe('PUT');
    expect(updateRequest.options.headers.Authorization).toBe('Bearer admin-token');
    expect(updateRequest.options.headers['Content-Type']).toBe('application/json');
    expect(updateRequest.options.body).toBe(JSON.stringify({ configValue: '欢迎', remark: '测试' }));

    clearToken();
    mockApiResponse({ list: [] });
    await fetchOrders();
    expect(lastRequest().options.headers.Authorization).toBeUndefined();
  });

  test('FormData 上传不设置 JSON Content-Type', async () => {
    setToken('admin-token');
    mockApiResponse({ id: 1 });
    const file = new File(['cover'], 'cover.png', { type: 'image/png' });

    await uploadStoreCover(7, file);
    const { url, options } = lastRequest();

    expect(url).toBe(expectedUrl('/api/admin/stores/7/cover'));
    expect(options.headers.Authorization).toBe('Bearer admin-token');
    expect(options.headers['Content-Type']).toBeUndefined();
    expect(options.body).toBeInstanceOf(FormData);
  });

  test('错误响应抛出后端 message', async () => {
    mockApiResponse(null, { status: 500, code: 9999, message: '服务暂时不可用' });

    await expect(fetchOrders()).rejects.toThrow('服务暂时不可用');
  });

  test('查询参数、编码和资源地址拼接稳定', async () => {
    mockApiResponse({ list: [] });
    await fetchCatalogItems({ storeId: 3, businessType: 'group_buy', status: 'on_sale', keyword: '套餐' });
    expect(lastRequest().url).toBe(
      expectedUrl('/api/admin/catalog/items?page=1&pageSize=80&storeId=3&businessType=group_buy&status=on_sale&keyword=%E5%A5%97%E9%A4%90'),
    );

    mockApiResponse({ list: [] });
    await fetchGovernanceComplaints({ status: 'pending', category: 'service', orderNo: 'AT 1', storeName: '塔斯汀', page: 2, pageSize: 5 });
    expect(lastRequest().url).toBe(
      expectedUrl('/api/admin/governance/complaints?page=2&pageSize=5&status=pending&category=service&orderNo=AT+1&storeName=%E5%A1%94%E6%96%AF%E6%B1%80'),
    );

    mockApiResponse({});
    await adminRedeemVoucher('券 1/中文');
    expect(lastRequest().url).toContain('/api/admin/trade/vouchers/%E5%88%B8%201%2F%E4%B8%AD%E6%96%87/redeem');

    expect(resolveAssetUrl()).toBe('');
    expect(resolveAssetUrl('http://example.com/a.png')).toBe('http://example.com/a.png');
    expect(resolveAssetUrl('/uploads/a.png')).toBe(expectedUrl('/uploads/a.png'));
  });

  test('后台交易退款、券码预约和确认接口稳定', async () => {
    mockApiResponse({ id: 8001 });
    await refundOrder(8001);
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/trade/orders/8001/refund'),
      options: { method: 'POST', body: JSON.stringify({ reason: '平台人工退款' }) },
    });

    mockApiResponse({ list: [] });
    await fetchBookings({ status: 'pending', businessType: 'group_buy', page: 2, pageSize: 12 });
    expect(lastRequest().url).toBe(
      expectedUrl('/api/admin/trade/bookings?page=2&pageSize=12&status=pending&businessType=group_buy'),
    );

    mockApiResponse({ id: 8002 });
    await confirmBooking(8002);
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/trade/orders/8002/booking/confirm'),
      options: { method: 'POST', body: JSON.stringify({}) },
    });
  });

  test('评价治理、投诉处理和平台客服接口稳定', async () => {
    mockApiResponse({ list: [] });
    await fetchGovernanceReviews({ status: 'reported', reported: true, page: 2, pageSize: 8 });
    expect(lastRequest().url).toBe(
      expectedUrl('/api/admin/governance/reviews?page=2&pageSize=8&status=reported&reported=true'),
    );

    mockApiResponse({ id: 3 });
    await auditGovernanceReview(3, 'hide', '虚假评价');
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/governance/reviews/3/audit'),
      options: { method: 'POST', body: JSON.stringify({ action: 'hide', remark: '虚假评价' }) },
    });

    mockApiResponse({ id: 5 });
    await complaintAction(5, 'accept', '已受理');
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/governance/complaints/5/accept'),
      options: { method: 'POST', body: JSON.stringify({ remark: '已受理' }) },
    });

    mockApiResponse({ list: [] });
    await fetchPlatformSupportSessions({ status: 'open', page: 3, pageSize: 6 });
    expect(lastRequest().url).toBe(
      expectedUrl('/api/admin/governance/support/sessions?page=3&pageSize=6&status=open'),
    );

    mockApiResponse({ id: 9 });
    await sendPlatformSupportMessage(9, '平台已介入');
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/governance/support/sessions/9/messages'),
      options: { method: 'POST', body: JSON.stringify({ content: '平台已介入' }) },
    });
  });

  test('会员等级和优惠券模板配置接口稳定', async () => {
    mockApiResponse([]);
    await fetchMemberLevels();
    expect(lastRequest().url).toBe(expectedUrl('/api/admin/operation/member-levels'));

    const level = {
      levelCode: 'gold',
      levelName: '黄金会员',
      minGrowthValue: 1000,
      benefits: [{ title: '专属优惠券', desc: '每月一张' }],
      color: '#d4a017',
      sortOrder: 2,
      status: 'enabled',
    };
    mockApiResponse({ id: 2 });
    await createMemberLevel(level);
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/operation/member-levels'),
      options: { method: 'POST', body: JSON.stringify(level) },
    });

    mockApiResponse([]);
    await fetchCouponTemplates();
    expect(lastRequest().url).toBe(expectedUrl('/api/admin/operation/coupon-templates'));

    const coupon = {
      name: '满50减10',
      type: 'amount_off',
      faceValue: 10,
      thresholdAmount: 50,
      businessScope: 'all',
      validKind: 'relative',
      validDays: 30,
      totalQty: 100,
      perUserLimit: 1,
      status: 'enabled',
    };
    mockApiResponse({ id: 4 });
    await createCouponTemplate(coupon);
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/operation/coupon-templates'),
      options: { method: 'POST', body: JSON.stringify(coupon) },
    });
  });

  test('商家、门店和目录维护接口稳定', async () => {
    mockApiResponse({ list: [] });
    await fetchMerchants('塔斯汀');
    expect(lastRequest().url).toBe(
      expectedUrl('/api/admin/merchants?page=1&pageSize=50&keyword=%E5%A1%94%E6%96%AF%E6%B1%80'),
    );

    mockApiResponse({ list: [] });
    await fetchStores({ merchantId: 2, businessType: 'takeaway', status: 'active' });
    expect(lastRequest().url).toBe(
      expectedUrl('/api/admin/stores?page=1&pageSize=80&merchantId=2&businessType=takeaway&status=active'),
    );

    const item = {
      storeId: 3,
      businessType: 'takeaway',
      categoryId: 5,
      title: '双人汉堡套餐',
      subtitle: '含饮料',
      price: 39.9,
      originalPrice: 49.9,
      stock: 30,
      status: 'on_sale' as const,
      coverUrl: '/uploads/item.png',
      tagText: '热销',
    };
    mockApiResponse({ id: 6 });
    await createCatalogItem(item);
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/catalog/items'),
      options: { method: 'POST', body: JSON.stringify(item) },
    });

    mockApiResponse({ id: 6 });
    await updateCatalogItemStatus(6, 'off_sale');
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/catalog/items/6/status'),
      options: { method: 'POST', body: JSON.stringify({ status: 'off_sale' }) },
    });

    const category = {
      storeId: 3,
      businessType: 'takeaway',
      categoryName: '套餐',
      sortOrder: 1,
      status: 'enabled',
    };
    mockApiResponse({ id: 7 });
    await createCatalogCategory(category);
    expect(lastRequest()).toMatchObject({
      url: expectedUrl('/api/admin/catalog/categories'),
      options: { method: 'POST', body: JSON.stringify(category) },
    });
  });
});
