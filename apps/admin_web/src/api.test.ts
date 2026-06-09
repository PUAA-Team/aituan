import { beforeEach, describe, expect, test, vi } from 'vitest';

import {
  adminRedeemVoucher,
  clearToken,
  fetchCatalogItems,
  fetchGovernanceComplaints,
  fetchOrders,
  getToken,
  login,
  resolveAssetUrl,
  setToken,
  updateConfig,
  uploadStoreCover,
} from './api';

let fetchMock: ReturnType<typeof vi.fn>;

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
    expect(url).toBe('http://localhost:8080/api/open/auth/admin/login/password');
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

    expect(url).toBe('http://localhost:8080/api/admin/stores/7/cover');
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
      'http://localhost:8080/api/admin/catalog/items?page=1&pageSize=80&storeId=3&businessType=group_buy&status=on_sale&keyword=%E5%A5%97%E9%A4%90',
    );

    mockApiResponse({ list: [] });
    await fetchGovernanceComplaints({ status: 'pending', category: 'service', orderNo: 'AT 1', storeName: '塔斯汀', page: 2, pageSize: 5 });
    expect(lastRequest().url).toBe(
      'http://localhost:8080/api/admin/governance/complaints?page=2&pageSize=5&status=pending&category=service&orderNo=AT+1&storeName=%E5%A1%94%E6%96%AF%E6%B1%80',
    );

    mockApiResponse({});
    await adminRedeemVoucher('券 1/中文');
    expect(lastRequest().url).toContain('/api/admin/trade/vouchers/%E5%88%B8%201%2F%E4%B8%AD%E6%96%87/redeem');

    expect(resolveAssetUrl()).toBe('');
    expect(resolveAssetUrl('http://example.com/a.png')).toBe('http://example.com/a.png');
    expect(resolveAssetUrl('/uploads/a.png')).toBe('http://localhost:8080/uploads/a.png');
  });
});
