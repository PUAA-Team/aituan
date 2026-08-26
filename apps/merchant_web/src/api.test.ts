import { beforeEach, describe, expect, test, vi } from 'vitest';

import {
  clearToken,
  fetchMerchantReviews,
  fetchVouchers,
  getToken,
  login,
  lookupVoucher,
  redeemVoucher,
  replyMerchantReview,
  resolveAssetUrl,
  setToken,
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

describe('merchant api', () => {
  beforeEach(() => {
    localStorage.clear();
    fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
  });

  test('login 使用商家登录接口且保存 token 和账号', async () => {
    mockApiResponse({ token: 'merchant-token', profile: { nickname: '商家' } });

    const session = await login('demo_merchant', '123456');
    const { url, options } = lastRequest();

    expect(session.token).toBe('merchant-token');
    expect(getToken()).toBe('merchant-token');
    expect(localStorage.getItem('aituan_merchant_account')).toBe('demo_merchant');
    expect(url).toBe(expectedUrl('/api/open/auth/merchant/login/password'));
    expect(options.method).toBe('POST');
    expect(options.headers.Authorization).toBeUndefined();
    expect(options.headers['Content-Type']).toBe('application/json');
    expect(options.body).toBe(JSON.stringify({ account: 'demo_merchant', password: '123456' }));
  });

  test('普通 JSON 请求带 Authorization，auth:false 不带 Authorization', async () => {
    setToken('token-1');
    mockApiResponse({ id: 9 });

    await replyMerchantReview(9, '感谢反馈');
    const jsonRequest = lastRequest();

    expect(jsonRequest.url).toBe(expectedUrl('/api/merchant/ops/reviews/9/reply'));
    expect(jsonRequest.options.headers.Authorization).toBe('Bearer token-1');
    expect(jsonRequest.options.headers['Content-Type']).toBe('application/json');
    expect(jsonRequest.options.body).toBe(JSON.stringify({ content: '感谢反馈' }));

    clearToken();
    mockApiResponse({ token: 'merchant-token', profile: { nickname: '商家' } });
    await login('demo_merchant', '123456');
    const loginRequest = lastRequest();
    expect(loginRequest.options.headers.Authorization).toBeUndefined();
  });

  test('FormData 上传不手动设置 JSON Content-Type', async () => {
    setToken('token-2');
    mockApiResponse({ id: 1 });
    const file = new File(['cover'], 'cover.png', { type: 'image/png' });

    await uploadStoreCover(file);
    const { url, options } = lastRequest();

    expect(url).toBe(expectedUrl('/api/merchant/stores/current/cover'));
    expect(options.headers.Authorization).toBe('Bearer token-2');
    expect(options.headers['Content-Type']).toBeUndefined();
    expect(options.body).toBeInstanceOf(FormData);
  });

  test('失败响应抛出后端 message 且保留 HTTP status', async () => {
    setToken('expired');
    mockApiResponse(null, { status: 401, code: 2001, message: '未登录或登录已过期' });

    await expect(fetchMerchantReviews()).rejects.toMatchObject({
      message: '未登录或登录已过期',
      status: 401,
    });
  });

  test('查询参数、券码编码与资源地址拼接稳定', async () => {
    mockApiResponse({ list: [] });
    await fetchMerchantReviews({ status: 'published', replied: false, page: 2, pageSize: 5 });
    expect(lastRequest().url).toBe(
      expectedUrl('/api/merchant/ops/reviews?page=2&pageSize=5&status=published&replied=false'),
    );

    mockApiResponse({ list: [] });
    await fetchVouchers({ status: 'unused', keyword: '券 A', page: 3, pageSize: 10 });
    expect(lastRequest().url).toBe(
      expectedUrl('/api/merchant/trade/vouchers?page=3&pageSize=10&status=unused&keyword=%E5%88%B8+A'),
    );

    mockApiResponse({});
    await redeemVoucher('券 1/中文');
    expect(lastRequest().url).toContain('/api/merchant/trade/vouchers/%E5%88%B8%201%2F%E4%B8%AD%E6%96%87/redeem');

    mockApiResponse({});
    await lookupVoucher('券 1/中文');
    expect(lastRequest().url).toContain('/api/merchant/trade/vouchers/%E5%88%B8%201%2F%E4%B8%AD%E6%96%87');

    expect(resolveAssetUrl()).toBe('');
    expect(resolveAssetUrl('https://example.com/a.png')).toBe('https://example.com/a.png');
    expect(resolveAssetUrl('/uploads/a.png')).toBe(expectedUrl('/uploads/a.png'));
    expect(resolveAssetUrl('uploads/a.png')).toBe(expectedUrl('/uploads/a.png'));
  });
});
