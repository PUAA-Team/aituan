import { expect, type APIRequestContext, type APIResponse } from '@playwright/test';

export interface Envelope<T> {
  code: number;
  message?: string;
  data: T;
}

interface RequestOptions {
  token?: string;
  data?: unknown;
  params?: Record<string, string | number | boolean | undefined>;
}

export interface AuthSession {
  token: string;
  expiresIn?: number;
  profile?: {
    id?: number;
    nickname?: string;
    phone?: string;
    email?: string;
  };
}

export interface UserProfile {
  nickname?: string;
  avatarUrl?: string;
  phone?: string;
  email?: string;
  memberLevelName?: string;
  growthValue?: number;
}

export interface AddressView {
  id: number;
  contactName: string;
  contactPhone: string;
  detailAddress?: string;
  isDefault?: boolean;
}

export interface PageData<T> {
  list: T[];
  total?: number;
  page?: number;
  pageSize?: number;
}

export interface PageResponse<T> {
  list: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface StoreCard {
  id: number;
  name?: string;
  businessType?: string;
  status?: string;
}

export interface StoreDetail {
  store?: {
    id: number;
    name?: string;
    businessType?: string;
    status?: string;
    rating?: number;
  };
  itemGroups?: Array<{
    categoryId: number;
    categoryName: string;
    items?: Array<{
      id: number;
      title?: string;
      price?: number;
    }>;
  }>;
}

export interface ItemDetail {
  item?: {
    id: number;
    itemName?: string;
    name?: string;
    price?: number;
    status?: string;
  };
  store?: { id: number; storeName?: string };
}

export interface CartView {
  storeId: number;
  storeName: string;
  amount: number;
  items: Array<{
    itemId: number;
    itemName: string;
    unitPrice: number;
    quantity: number;
    totalPrice: number;
  }>;
}

export interface CheckoutPreview {
  amount?: number;
  deliveryFee?: number;
  discountAmount?: number;
  payableAmount?: number;
  coupon?: {
    id: number;
    name?: string;
    faceValue?: number;
  } | null;
}

export interface BookingView {
  orderId: number;
  orderNo: string;
  bookingDate: string;
  bookingTimeSlot: string;
  storeConfirmStatus: string;
  storeConfirmRemark?: string;
  storeName?: string;
  contactName?: string;
  contactPhone?: string;
  guestCount?: number;
}

export interface OrderDetail {
  id: number;
  orderNo: string;
  orderKind?: string;
  orderType?: string;
  displayStatus: string;
  paymentStatus: string;
  fulfillmentStatus: string;
  storeId?: number;
  storeName?: string;
  title?: string;
  amount?: number;
  deliveryFee?: number;
  discountAmount?: number;
  payableAmount?: number;
  refundStatus?: string;
  refundAmount?: number;
  refundReason?: string;
  addressSnapshot?: string;
  items?: Array<{
    itemId: number;
    itemName: string;
    quantity: number;
    unitPrice?: number;
    totalPrice?: number;
  }>;
  voucher?: {
    voucherCode: string;
    status: string;
  } | null;
  booking?: BookingView | null;
  deliveryTimeline?: {
    nodes?: Array<{
      code: string;
      text: string;
      reachedAt?: string | null;
    }>;
  };
}

export type OrderView = OrderDetail;

export interface StationMessage {
  id: number;
  type: string;
  title: string;
  content: string;
  unread: boolean;
  relatedOrderId?: number;
}

export class E2eApi {
  constructor(private readonly requestContext: APIRequestContext) {}

  get<T>(path: string, options: Omit<RequestOptions, 'data'> = {}) {
    return this.call<T>('GET', path, options);
  }

  post<T>(path: string, options: RequestOptions = {}) {
    return this.call<T>('POST', path, options);
  }

  put<T>(path: string, options: RequestOptions = {}) {
    return this.call<T>('PUT', path, options);
  }

  delete<T>(path: string, options: Omit<RequestOptions, 'data'> = {}) {
    return this.call<T>('DELETE', path, options);
  }

  async loginUser(account = 'demo_user', password = '123456') {
    return this.login('/api/open/auth/user/login/password', account, password);
  }

  async loginMerchant(account: string, password = '123456') {
    return this.login('/api/open/auth/merchant/login/password', account, password);
  }

  async loginAdmin(account = 'demo_admin', password = '123456') {
    return this.login('/api/open/auth/admin/login/password', account, password);
  }

  private async login(path: string, account: string, password: string) {
    return this.post<AuthSession>(path, { data: { account, password } });
  }

  private async call<T>(method: 'GET' | 'POST' | 'PUT' | 'DELETE', path: string, options: RequestOptions) {
    const response = await this.requestContext.fetch(path, {
      method,
      headers: options.token ? { Authorization: `Bearer ${options.token}` } : undefined,
      data: options.data,
      params: cleanParams(options.params),
    });
    return readSuccessfulEnvelope<T>(response, method, path);
  }
}

export class E2EApi {
  constructor(
    public readonly origin: string,
    public token = '',
  ) {}

  async request<T>(method: string, path: string, body?: unknown): Promise<Envelope<T>> {
    const headers: Record<string, string> = { accept: 'application/json' };
    if (body !== undefined) headers['content-type'] = 'application/json';
    if (this.token) headers.authorization = `Bearer ${this.token}`;

    const response = await fetch(`${this.origin}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await response.text();
    if (!response.ok) {
      throw new Error(`API ${method} ${path} 返回 HTTP ${response.status}: ${text}`);
    }

    let json: Envelope<T>;
    try {
      json = JSON.parse(text) as Envelope<T>;
    } catch {
      throw new Error(`API ${method} ${path} 未返回 JSON: ${text}`);
    }
    if (json.code !== 0) {
      throw new Error(`API ${method} ${path} 失败: code=${json.code} message=${json.message}`);
    }
    return json;
  }

  async login(account: string, password: string, role: 'user' | 'merchant' | 'admin' = 'user') {
    const suffix =
      role === 'merchant' ? 'merchant/login/password' : role === 'admin' ? 'admin/login/password' : 'user/login/password';
    const json = await this.request<AuthSession>('POST', `/api/open/auth/${suffix}`, {
      account,
      password,
    });
    this.token = json.data.token;
    return json.data;
  }

  async emailCode(email: string, scene: string): Promise<string> {
    const json = await this.request<{ code?: string }>('POST', '/api/open/auth/email-code', {
      email,
      scene,
    });
    const code = json.data.code;
    expect(code, 'e2e profile 应回显调试验证码').toBeTruthy();
    return code!;
  }

  async register(phone: string, email: string, password: string): Promise<AuthSession> {
    const code = await this.emailCode(email, 'register');
    const json = await this.request<AuthSession>('POST', '/api/open/auth/user/register', {
      phone,
      email,
      emailCode: code,
      password,
    });
    this.token = json.data.token;
    return json.data;
  }

  async createUser(prefix = 'e2e') {
    const stamp = `${Date.now()}${Math.floor(Math.random() * 900 + 100)}`;
    const phone = `13${String(stamp).slice(-9)}`;
    const email = `${prefix}_${stamp}@test.local`;
    const password = '123456';
    const session = await this.register(phone, email, password);
    return { phone, email, password, session };
  }

  profile() {
    return this.request<UserProfile>('GET', '/api/app/account/profile').then((r) => r.data);
  }

  updateProfile(nickname: string, avatarUrl = '') {
    return this.request<UserProfile>('PUT', '/api/app/account/profile', {
      nickname,
      avatarUrl,
    }).then((r) => r.data);
  }

  addresses() {
    return this.request<AddressView[]>('GET', '/api/app/account/addresses').then((r) => r.data);
  }

  createAddress(input: Partial<Record<string, unknown>>) {
    const body = {
      contactName: '端到端测试',
      contactPhone: '18800009999',
      province: '北京市',
      city: '北京市',
      district: '海淀区',
      detailAddress: 'E2E 测试路 1 号',
      longitude: 116.3161,
      latitude: 39.9811,
      tagName: '家',
      isDefault: true,
      deliveryNote: '',
      ...input,
    };
    return this.request<AddressView>('POST', '/api/app/account/addresses', body).then((r) => r.data);
  }

  deleteAddress(id: number) {
    return this.request<void>('DELETE', `/api/app/account/addresses/${id}`);
  }

  favorites() {
    return this.request<PageData<{ id: number; favoriteType: string; targetId: number; targetName: string }>>(
      'GET',
      '/api/app/account/favorites',
    ).then((r) => r.data);
  }

  saveFavorite(favoriteType: string, targetId: number, targetName: string, subtitle = '') {
    return this.request('POST', '/api/app/account/favorites', {
      favoriteType,
      targetId,
      targetName,
      coverUrl: '',
      subtitle,
    });
  }

  deleteFavorite(favoriteType: string, targetId: number) {
    return this.request('DELETE', `/api/app/account/favorites/${favoriteType}/${targetId}`);
  }

  home() {
    return this.request<{
      modules?: Array<{ code: string; name: string }>;
      recommendations?: {
        list?: Array<{ id: number; title?: string; storeName?: string }>;
      };
    }>('GET', '/api/app/discovery/home').then((r) => r.data);
  }

  search(keyword: string, extra = '') {
    return this.request<PageData<StoreCard>>(
      'GET',
      `/api/app/discovery/stores/search?keyword=${encodeURIComponent(keyword)}${extra}`,
    ).then((r) => r.data);
  }

  storeDetail(storeId: number) {
    return this.request<StoreDetail>('GET', `/api/app/discovery/stores/${storeId}`).then((r) => r.data);
  }

  itemDetail(itemId: number) {
    return this.request<ItemDetail>('GET', `/api/app/discovery/items/${itemId}`).then((r) => r.data);
  }

  addCart(storeId: number, itemId: number, quantity = 1) {
    return this.request<CartView>('POST', '/api/app/trade/cart/items', {
      storeId,
      itemId,
      quantity,
    }).then((r) => r.data);
  }

  preview(storeId: number, businessType: string, items: Array<{ itemId: number; quantity: number }>, addressId?: number) {
    return this.request<CheckoutPreview>('POST', '/api/app/trade/checkout/preview', {
      storeId,
      businessType,
      addressId,
      items,
      remark: '',
    }).then((r) => r.data);
  }

  createOrder(
    storeId: number,
    businessType: string,
    items: Array<{ itemId: number; quantity: number }>,
    addressId?: number,
  ) {
    return this.request<OrderView>('POST', '/api/app/trade/orders', {
      storeId,
      businessType,
      addressId,
      items,
      remark: '',
      idempotencyKey: `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    }).then((r) => r.data);
  }

  pay(orderId: number, paymentMode = 'mock') {
    return this.request<OrderView>('POST', `/api/app/trade/orders/${orderId}/pay`, {
      paymentMode,
    }).then((r) => r.data);
  }

  orders(displayStatus?: string) {
    const query = displayStatus ? `?displayStatus=${encodeURIComponent(displayStatus)}` : '';
    return this.request<PageData<OrderView>>('GET', `/api/app/trade/orders${query}`).then((r) => r.data);
  }

  orderDetail(orderId: number) {
    return this.request<OrderView>('GET', `/api/app/trade/orders/${orderId}`).then((r) => r.data);
  }

  deliveryTimeline(orderId: number) {
    return this.request<{ nodes: Array<{ code: string; text: string; reachedAt?: string | null }> }>(
      'GET',
      `/api/app/trade/orders/${orderId}/delivery/timeline`,
    ).then((r) => r.data);
  }

  upsertBooking(
    orderId: number,
    input: Record<string, unknown>,
  ): Promise<{
    orderId: number;
    storeConfirmStatus: string;
    contactName?: string;
    bookingDate?: string;
    bookingTimeSlot?: string;
  }> {
    return this.request<{
      orderId: number;
      storeConfirmStatus: string;
      contactName?: string;
      bookingDate?: string;
      bookingTimeSlot?: string;
    }>('POST', `/api/app/trade/orders/${orderId}/booking`, {
      contactName: '端到端测试',
      contactPhone: '18800009999',
      bookingDate: '2026-09-30',
      bookingTimeSlot: '14:00-15:00',
      guestCount: 1,
      remark: '',
      ...input,
    }).then((r) => r.data);
  }

  messages() {
    return this.request<PageData<StationMessage>>('GET', '/api/app/message/station').then((r) => r.data);
  }

  merchantOrders(fulfillmentStatus?: string) {
    const query = fulfillmentStatus ? `?fulfillmentStatus=${encodeURIComponent(fulfillmentStatus)}` : '';
    return this.request<PageData<OrderView>>('GET', `/api/merchant/trade/orders${query}`).then((r) => r.data);
  }

  merchantAction(orderId: number, action: string) {
    return this.request<OrderView>('POST', `/api/merchant/trade/orders/${orderId}/${action}`, {}).then((r) => r.data);
  }

  lookupVoucher(code: string) {
    return this.request<{
      voucherCode: string;
      orderId: number;
      orderNo: string;
      orderTitle?: string;
      status: string;
      payableAmount?: number;
      businessType?: string;
      storeName?: string;
    }>('GET', `/api/merchant/trade/vouchers/${code}`).then((r) => r.data);
  }

  redeemVoucher(code: string) {
    return this.request<OrderView>('POST', `/api/merchant/trade/vouchers/${code}/redeem`, {}).then((r) => r.data);
  }

  confirmBooking(orderId: number, remark = '端到端确认') {
    return this.request('POST', `/api/merchant/trade/orders/${orderId}/booking/confirm`, { remark }).then((r) => r.data);
  }
}

async function readSuccessfulEnvelope<T>(response: APIResponse, method: string, path: string) {
  const body = await response.text();
  if (!response.ok()) {
    throw new Error(`${method} ${path} 返回 HTTP ${response.status()}：${body}`);
  }
  let envelope: Envelope<T>;
  try {
    envelope = JSON.parse(body) as Envelope<T>;
  } catch {
    throw new Error(`${method} ${path} 未返回 JSON：${body}`);
  }
  if (envelope.code !== 0) {
    throw new Error(`${method} ${path} 业务失败 code=${envelope.code}：${envelope.message}`);
  }
  return envelope.data;
}

function cleanParams(params: RequestOptions['params']) {
  if (!params) return undefined;
  return Object.fromEntries(
    Object.entries(params).filter((entry): entry is [string, string | number | boolean] => entry[1] !== undefined),
  );
}

export function newApi(token = '') {
  const origin = process.env.E2E_API_ORIGIN ?? 'http://127.0.0.1:8080';
  return new E2EApi(origin, token);
}
