import type {
  AdminConfig,
  AdminMerchant,
  AdminStore,
  AdminUser,
  Announcement,
  AnnouncementForm,
  AuditLog,
  AuthSession,
  CatalogItem,
  DashboardView,
  DeliverySetting,
  DeliveryTask,
  OpsOrder,
  OrderDetail,
  OrderStatusCount,
  PageResponse,
} from './types';

const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const tokenKey = 'aituan_admin_token';

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
  const response = await request<AuthSession>('/api/open/auth/admin/login/password', {
    method: 'POST',
    body: { account, password },
    auth: false,
  });
  setToken(response.token);
  return response;
}

export function fetchDashboard() {
  return request<DashboardView>('/api/admin/dashboard');
}

export function fetchOrders(fulfillmentStatus = '') {
  const query = new URLSearchParams({ page: '1', pageSize: '30' });
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
  return request<OrderDetail>(`/api/admin/trade/orders/${orderId}/${action}`, {
    method: 'POST',
    body: remark ? { remark } : {},
  });
}

export function fetchMerchants(keyword = '') {
  const query = new URLSearchParams({ page: '1', pageSize: '50' });
  if (keyword) query.set('keyword', keyword);
  return request<PageResponse<AdminMerchant>>(`/api/admin/merchants?${query}`);
}

export function updateMerchantStatus(merchantId: number, status: string) {
  return request<AdminMerchant>(`/api/admin/merchants/${merchantId}/status`, {
    method: 'POST',
    body: { status },
  });
}

export function fetchStores(filters: { merchantId?: number; businessType?: string; status?: string } = {}) {
  const query = new URLSearchParams({ page: '1', pageSize: '80' });
  if (filters.merchantId) query.set('merchantId', String(filters.merchantId));
  if (filters.businessType) query.set('businessType', filters.businessType);
  if (filters.status) query.set('status', filters.status);
  return request<PageResponse<AdminStore>>(`/api/admin/stores?${query}`);
}

export function updateStoreStatus(storeId: number, status: string) {
  return request<AdminStore>(`/api/admin/stores/${storeId}/status`, {
    method: 'POST',
    body: { status },
  });
}

export function uploadStoreCover(storeId: number, file: File) {
  const body = new FormData();
  body.append('file', file);
  return request<AdminStore>(`/api/admin/stores/${storeId}/cover`, {
    method: 'POST',
    body,
  });
}

export function fetchUsers(keyword = '') {
  const query = new URLSearchParams({ page: '1', pageSize: '50' });
  if (keyword) query.set('keyword', keyword);
  return request<PageResponse<AdminUser>>(`/api/admin/users?${query}`);
}

export function updateUserStatus(accountId: number, status: string) {
  return request<AdminUser>(`/api/admin/users/${accountId}/status`, {
    method: 'POST',
    body: { status },
  });
}

export function fetchCatalogItems(filters: { storeId?: number; businessType?: string; status?: string; keyword?: string } = {}) {
  const query = new URLSearchParams({ page: '1', pageSize: '80' });
  if (filters.storeId) query.set('storeId', String(filters.storeId));
  if (filters.businessType) query.set('businessType', filters.businessType);
  if (filters.status) query.set('status', filters.status);
  if (filters.keyword) query.set('keyword', filters.keyword);
  return request<PageResponse<CatalogItem>>(`/api/admin/catalog/items?${query}`);
}

export function updateCatalogItemStatus(itemId: number, status: 'on_sale' | 'off_sale') {
  return request<CatalogItem>(`/api/admin/catalog/items/${itemId}/status`, {
    method: 'POST',
    body: { status },
  });
}

export function fetchDeliveryTasks(stage = '') {
  const query = new URLSearchParams({ page: '1', pageSize: '50' });
  if (stage) query.set('stage', stage);
  return request<PageResponse<DeliveryTask>>(`/api/admin/delivery/tasks?${query}`);
}

export function updateDeliveryTask(taskId: number, action: 'advance' | 'pause' | 'resume' | 'abnormal', remark = '') {
  return request<DeliveryTask>(`/api/admin/delivery/tasks/${taskId}/${action}`, {
    method: 'POST',
    body: remark ? { remark, reason: remark } : {},
  });
}

export function fetchDeliverySettings() {
  return request<DeliverySetting>('/api/admin/delivery/settings');
}

export function updateDeliverySettings(payload: DeliverySetting) {
  return request<DeliverySetting>('/api/admin/delivery/settings', {
    method: 'POST',
    body: payload,
  });
}

export function fetchAnnouncements(status = '') {
  const query = new URLSearchParams({ page: '1', pageSize: '50' });
  if (status) query.set('status', status);
  return request<PageResponse<Announcement>>(`/api/admin/announcements?${query}`);
}

export function createAnnouncement(payload: AnnouncementForm) {
  return request<Announcement>('/api/admin/announcements', {
    method: 'POST',
    body: payload,
  });
}

export function updateAnnouncement(id: number, payload: AnnouncementForm) {
  return request<Announcement>(`/api/admin/announcements/${id}`, {
    method: 'PUT',
    body: payload,
  });
}

export function updateAnnouncementStatus(id: number, status: string) {
  return request<Announcement>(`/api/admin/announcements/${id}/status`, {
    method: 'POST',
    body: { status },
  });
}

export function fetchConfigs() {
  return request<AdminConfig[]>('/api/admin/configs');
}

export function updateConfig(configKey: string, configValue: string, remark = '') {
  return request<AdminConfig[]>(`/api/admin/configs/${encodeURIComponent(configKey)}`, {
    method: 'PUT',
    body: { configValue, remark },
  });
}

export function fetchAuditLogs(actionType = '') {
  const query = new URLSearchParams({ page: '1', pageSize: '60' });
  if (actionType) query.set('actionType', actionType);
  return request<PageResponse<AuditLog>>(`/api/admin/audit-logs?${query}`);
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
    throw new Error(json.message || `请求失败：${response.status}`);
  }
  return json.data as T;
}
