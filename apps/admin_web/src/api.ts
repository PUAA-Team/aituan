import type { AuthSession, OpsOrder, OrderDetail, OrderStatusCount, PageResponse } from './types';

const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const tokenKey = 'aituan_admin_token';

export function getToken() {
  return localStorage.getItem(tokenKey) || '';
}

export function setToken(token: string) {
  localStorage.setItem(tokenKey, token);
}

export function clearToken() {
  localStorage.removeItem(tokenKey);
}

export async function login(account: string, password: string) {
  const response = await request<AuthSession>('/api/open/auth/admin/login/password', {
    method: 'POST',
    body: { account, password },
    auth: false,
  });
  setToken(response.token);
  return response;
}

export function fetchOrders(fulfillmentStatus = '') {
  const query = new URLSearchParams({ page: '1', pageSize: '20' });
  if (fulfillmentStatus) query.set('fulfillmentStatus', fulfillmentStatus);
  return request<PageResponse<OpsOrder>>(`/api/admin/trade/orders?${query}`);
}

export function fetchStats() {
  return request<OrderStatusCount[]>('/api/admin/trade/orders/stats');
}

export function fetchOrderDetail(orderId: number) {
  return request<OrderDetail>(`/api/admin/trade/orders/${orderId}`);
}

export function runOrderAction(orderId: number, action: string, remark = '') {
  return request(`/api/admin/trade/orders/${orderId}/${action}`, {
    method: 'POST',
    body: remark ? { remark } : {},
  });
}

async function request<T>(path: string, options: { method?: string; body?: unknown; auth?: boolean } = {}) {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  if (options.auth !== false && getToken()) headers.Authorization = `Bearer ${getToken()}`;
  const response = await fetch(`${baseUrl}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const json = await response.json().catch(() => ({}));
  if (!response.ok || json.code !== 0) {
    throw new Error(json.message || `请求失败：${response.status}`);
  }
  return json.data as T;
}
