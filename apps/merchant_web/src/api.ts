import type {
  AuthSession,
  CatalogCategory,
  CatalogItem,
  CatalogItemForm,
  CertificationMaterial,
  DeliveryRule,
  MerchantApplicationForm,
  MerchantApplicationView,
  MerchantCertification,
  MerchantProfile,
  MerchantStore,
  OpsOrder,
  OrderDetail,
  OrderStatusCount,
  PageResponse,
  TakeawaySetting,
} from './types';

const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const tokenKey = 'aituan_merchant_token';

interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean;
}

export function getToken() {
  return localStorage.getItem(tokenKey) || '';
}

export function setToken(token: string) {
  localStorage.setItem(tokenKey, token);
}

export function clearToken() {
  localStorage.removeItem(tokenKey);
}

export function resolveAssetUrl(path?: string) {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) return path;
  return `${baseUrl}${path.startsWith('/') ? path : `/${path}`}`;
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

export function submitMerchantApplication(payload: MerchantApplicationForm) {
  return request<MerchantApplicationView>('/api/open/merchant/applications', {
    method: 'POST',
    body: payload,
    auth: false,
  });
}

export function fetchCertification() {
  return request<MerchantCertification>('/api/merchant/certification');
}

export function uploadCertificationMaterial(materialType: string, materialName: string, file: File) {
  const body = new FormData();
  body.append('materialType', materialType);
  body.append('materialName', materialName);
  body.append('file', file);
  return request<CertificationMaterial>('/api/merchant/certification/materials', {
    method: 'POST',
    body,
  });
}

export function fetchMerchantProfile() {
  return request<MerchantProfile>('/api/merchant/profile/me');
}

export function updateMerchantProfile(payload: Pick<MerchantProfile, 'merchantName' | 'contactName' | 'contactPhone'>) {
  return request<MerchantProfile>('/api/merchant/profile/me', {
    method: 'PUT',
    body: payload,
  });
}

export function fetchCurrentStore() {
  return request<MerchantStore>('/api/merchant/stores/current');
}

export function updateCurrentStore(payload: Partial<MerchantStore>) {
  return request<MerchantStore>('/api/merchant/stores/current', {
    method: 'PUT',
    body: {
      storeName: payload.storeName,
      summary: payload.summary,
      address: payload.address,
      businessHoursText: payload.businessHoursText,
      tagText: payload.tagText,
      contactPhone: payload.contactPhone,
      announcement: payload.announcement,
      status: payload.status,
      longitude: payload.longitude,
      latitude: payload.latitude,
    },
  });
}

export function uploadStoreCover(file: File) {
  const body = new FormData();
  body.append('file', file);
  return request<MerchantStore>('/api/merchant/stores/current/cover', {
    method: 'POST',
    body,
  });
}

export function fetchOrders(params: { displayStatus?: string; fulfillmentStatus?: string; page?: number; pageSize?: number } = {}) {
  const query = new URLSearchParams({
    page: String(params.page || 1),
    pageSize: String(params.pageSize || 30),
  });
  if (params.displayStatus) query.set('displayStatus', params.displayStatus);
  if (params.fulfillmentStatus) query.set('fulfillmentStatus', params.fulfillmentStatus);
  return request<PageResponse<OpsOrder>>(`/api/merchant/trade/orders?${query}`);
}

export function fetchOrderDetail(orderId: number) {
  return request<OrderDetail>(`/api/merchant/trade/orders/${orderId}`);
}

export function fetchStats() {
  return request<OrderStatusCount[]>('/api/merchant/trade/orders/stats');
}

export function runOrderAction(orderId: number, action: string, remark = '') {
  return request<OrderDetail>(`/api/merchant/trade/orders/${orderId}/${action}`, {
    method: 'POST',
    body: remark ? { remark } : {},
  });
}

export function redeemVoucher(voucherCode: string) {
  return request<OrderDetail>(`/api/merchant/trade/vouchers/${encodeURIComponent(voucherCode)}/redeem`, {
    method: 'POST',
    body: {},
  });
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
      maxDeliveryDistanceKm: rule.maxDeliveryDistanceKm,
      deliveryText: rule.deliveryText,
    },
  });
}

export function fetchCatalogItems(filters: { businessType?: string; status?: string; keyword?: string } = {}) {
  const query = new URLSearchParams();
  if (filters.businessType) query.set('businessType', filters.businessType);
  if (filters.status) query.set('status', filters.status);
  if (filters.keyword) query.set('keyword', filters.keyword);
  const suffix = query.toString() ? `?${query}` : '';
  return request<CatalogItem[]>(`/api/merchant/catalog/items${suffix}`);
}

export function createCatalogItem(item: CatalogItemForm) {
  return request<CatalogItem>('/api/merchant/catalog/items', {
    method: 'POST',
    body: item,
  });
}

export function updateCatalogItem(itemId: number, item: CatalogItemForm) {
  return request<CatalogItem>(`/api/merchant/catalog/items/${itemId}`, {
    method: 'PUT',
    body: item,
  });
}

export function updateCatalogItemStatus(itemId: number, status: 'on_sale' | 'off_sale') {
  return request<CatalogItem>(`/api/merchant/catalog/items/${itemId}/status`, {
    method: 'POST',
    body: { status },
  });
}

export function uploadItemCover(itemId: number, file: File) {
  const body = new FormData();
  body.append('file', file);
  return request<CatalogItem>(`/api/merchant/catalog/items/${itemId}/cover`, {
    method: 'POST',
    body,
  });
}

export function fetchCategories(businessType = '') {
  const query = new URLSearchParams();
  if (businessType) query.set('businessType', businessType);
  const suffix = query.toString() ? `?${query}` : '';
  return request<CatalogCategory[]>(`/api/merchant/catalog/categories${suffix}`);
}

export function createCategory(payload: { storeId?: number; businessType: string; categoryName: string; sortOrder?: number; status?: string }) {
  return request<CatalogCategory>('/api/merchant/catalog/categories', {
    method: 'POST',
    body: payload,
  });
}

async function request<T>(path: string, options: RequestOptions = {}) {
  const headers: Record<string, string> = { Accept: 'application/json' };
  const isFormData = options.body instanceof FormData;
  if (options.body !== undefined && !isFormData) headers['Content-Type'] = 'application/json';
  if (options.auth !== false && getToken()) headers.Authorization = `Bearer ${getToken()}`;
  const response = await fetch(`${baseUrl}${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body === undefined ? undefined : isFormData ? (options.body as FormData) : JSON.stringify(options.body),
  });
  const text = await response.text();
  const json = text ? JSON.parse(text) : {};
  if (!response.ok || json.code !== 0) {
    const error = new Error(json.message || `请求失败：${response.status}`);
    (error as Error & { status?: number }).status = response.status;
    throw error;
  }
  return json.data as T;
}
