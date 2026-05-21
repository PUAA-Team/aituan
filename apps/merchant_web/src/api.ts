import type {
  AuthSession,
  DeliveryRule,
  MerchantItem,
  OpsOrder,
  OrderDetail,
  OrderStatusCount,
  PageResponse,
  TakeawaySetting,
} from './types';

const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const tokenKey = 'aituan_merchant_token';

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
  const response = await request<AuthSession>('/api/open/auth/merchant/login/password', {
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
  return request<PageResponse<OpsOrder>>(`/api/merchant/trade/orders?${query}`);
}

export function fetchOrderDetail(orderId: number) {
  return request<OrderDetail>(`/api/merchant/trade/orders/${orderId}`);
}

export function fetchStats() {
  return request<OrderStatusCount[]>('/api/merchant/trade/orders/stats');
}

export function fetchTakeawaySetting(storeId: number) {
  return request<TakeawaySetting>(`/api/merchant/trade/stores/${storeId}/takeaway-setting`);
}

export function updateTakeawaySetting(storeId: number, acceptMode: 'manual' | 'auto') {
  return request<TakeawaySetting>(`/api/merchant/trade/stores/${storeId}/takeaway-setting`, {
    method: 'POST',
    body: { acceptMode },
  });
}

export function fetchItems(storeId: number, status = '') {
  const query = new URLSearchParams();
  if (status) query.set('status', status);
  const suffix = query.toString() ? `?${query}` : '';
  return request<MerchantItem[]>(`/api/merchant/trade/stores/${storeId}/items${suffix}`);
}

export function updateItem(storeId: number, item: MerchantItem) {
  return request<MerchantItem>(`/api/merchant/trade/stores/${storeId}/items/${item.id}`, {
    method: 'POST',
    body: {
      title: item.title,
      subtitle: item.subtitle,
      price: item.price,
      stock: item.stock,
      status: item.status,
    },
  });
}

export function updateItemStatus(storeId: number, itemId: number, status: 'on_sale' | 'off_sale') {
  return request<MerchantItem>(`/api/merchant/trade/stores/${storeId}/items/${itemId}/status`, {
    method: 'POST',
    body: { status },
  });
}

export function fetchDeliveryRule(storeId: number) {
  return request<DeliveryRule>(`/api/merchant/trade/stores/${storeId}/delivery-rule`);
}

export function updateDeliveryRule(storeId: number, rule: DeliveryRule) {
  return request<DeliveryRule>(`/api/merchant/trade/stores/${storeId}/delivery-rule`, {
    method: 'POST',
    body: {
      deliveryFee: rule.deliveryFee,
      startPrice: rule.startPrice,
      estimatedMinutes: rule.estimatedMinutes,
      deliveryText: rule.deliveryText,
    },
  });
}

export function runOrderAction(orderId: number, action: string) {
  return request(`/api/merchant/trade/orders/${orderId}/${action}`, {
    method: 'POST',
    body: {},
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
