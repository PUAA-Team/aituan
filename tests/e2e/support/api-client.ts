import { expect } from '@playwright/test';

export interface Envelope<T> {
  code: number;
  message?: string;
  data: T;
}

export interface AuthSession {
  token: string;
  expiresIn?: number;
  profile?: {
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

export interface OrderView {
  id: number;
  orderNo: string;
  orderType?: string;
  title?: string;
  displayStatus?: string;
  fulfillmentStatus?: string;
  paymentStatus?: string;
  refundStatus?: string;
  amount?: number;
  deliveryFee?: number;
  discountAmount?: number;
  payableAmount?: number;
  addressSnapshot?: string;
  voucher?: {
    voucherCode: string;
    status: string;
  } | null;
  booking?: {
    orderId: number;
    orderNo?: string;
    storeName?: string;
    contactName?: string;
    contactPhone?: string;
    bookingDate?: string;
    bookingTimeSlot?: string;
    guestCount?: number;
    storeConfirmStatus?: string;
  } | null;
  deliveryTimeline?: {
    nodes?: Array<{
      code: string;
      text: string;
      reachedAt?: string | null;
    }>;
  };
  items?: Array<{
    itemId: number;
    itemName: string;
    quantity: number;
    unitPrice: number;
    totalPrice: number;
  }>;
}

export interface StationMessage {
  id: number;
  type: string;
  title: string;
  content: string;
  unread: boolean;
  relatedOrderId?: number;
}

export interface MemberInfo {
  currentLevelName?: string;
  growthValue: number;
  nextLevelName?: string;
}

export interface AvailableCoupon {
  templateId: number;
  name: string;
  thresholdAmount?: number;
  faceValue?: number;
  status?: string;
}

export interface UserCoupon {
  id: number;
  templateId: number;
  status: string;
  name?: string;
  faceValue?: number;
}

export interface ReviewView {
  id: number;
  orderId?: number;
  orderNo?: string;
  status?: string;
  replied: boolean;
  replyContent?: string;
  content?: string;
}

export interface ComplaintView {
  id: number;
  ticketNo: string;
  status?: string;
  orderNo?: string;
}

export interface SupportMessage {
  id: number;
  sessionId?: number;
  senderType: string;
  content: string;
}

export interface SupportSessionDetail {
  session: {
    id: number;
    sessionNo?: string;
    status?: string;
    assistantMode?: string;
    platformInterventionStatus?: string;
  };
  messages: SupportMessage[];
}

export class E2EApi {
  constructor(
    public readonly origin: string,
    public token = '',
  ) {}

  async request<T>(
    method: string,
    path: string,
    body?: unknown,
  ): Promise<Envelope<T>> {
    const headers: Record<string, string> = { accept: 'application/json' };
    if (body !== undefined) headers['content-type'] = 'application/json';
    if (this.token) headers.authorization = `Bearer ${this.token}`;
    const response = await fetch(`${this.origin}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const json = (await response.json()) as Envelope<T>;
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

  preview(
    storeId: number,
    businessType: string,
    items: Array<{ itemId: number; quantity: number }>,
    addressId?: number,
    couponId?: number,
  ) {
    return this.request<CheckoutPreview>('POST', '/api/app/trade/checkout/preview', {
      storeId,
      businessType,
      addressId,
      items,
      remark: '',
      couponId,
    }).then((r) => r.data);
  }

  createOrder(
    storeId: number,
    businessType: string,
    items: Array<{ itemId: number; quantity: number }>,
    addressId?: number,
    couponId?: number,
  ) {
    return this.request<OrderView>('POST', '/api/app/trade/orders', {
      storeId,
      businessType,
      addressId,
      items,
      remark: '',
      couponId,
      idempotencyKey: `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    }).then((r) => r.data);
  }

  pay(orderId: number, paymentMode = 'mock') {
    return this.request<OrderView>('POST', `/api/app/trade/orders/${orderId}/pay`, {
      paymentMode,
    }).then((r) => r.data);
  }

  cancelOrder(orderId: number) {
    return this.request<OrderView>('POST', `/api/app/trade/orders/${orderId}/cancel`, {}).then(
      (r) => r.data,
    );
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

  async completeTakeawayOrder(orderId: number) {
    for (const action of ['accept', 'prepare', 'ready', 'delivery/advance', 'delivery/advance', 'complete']) {
      await this.merchantAction(orderId, action);
    }
    return this.request<OrderView>('GET', `/api/merchant/trade/orders/${orderId}`).then((r) => r.data);
  }

  memberInfo() {
    return this.request<MemberInfo>('GET', '/api/app/account/member/info').then((r) => r.data);
  }

  availableCoupons() {
    return this.request<AvailableCoupon[]>('GET', '/api/app/account/coupons/available').then((r) => r.data);
  }

  claimCoupon(templateId: number) {
    return this.request<void>('POST', `/api/app/account/coupons/${templateId}/claim`);
  }

  myCoupons(status = 'usable') {
    return this.request<UserCoupon[]>('GET', `/api/app/account/coupons?status=${status}`).then((r) => r.data);
  }

  createReview(orderId: number, body: Record<string, unknown>) {
    return this.request<ReviewView>('POST', `/api/app/interaction/orders/${orderId}/review`, body).then(
      (r) => r.data,
    );
  }

  reviewDetail(reviewId: number) {
    return this.request<ReviewView>('GET', `/api/app/interaction/reviews/${reviewId}`).then((r) => r.data);
  }

  createComplaint(body: Record<string, unknown>) {
    return this.request<ComplaintView>('POST', '/api/app/complaints', body).then((r) => r.data);
  }

  complaintDetail(id: number) {
    return this.request<{ complaint: ComplaintView }>('GET', `/api/app/complaints/${id}`).then(
      (r) => r.data,
    );
  }

  createSupportSession(storeId: number, topic: string) {
    return this.request<{ id: number; sessionNo?: string; status?: string }>(
      'POST',
      '/api/app/support/sessions',
      { storeId, topic, relatedOrderId: null },
    ).then((r) => r.data);
  }

  sendSupportMessage(sessionId: number, content: string) {
    return this.request<SupportMessage>('POST', `/api/app/support/sessions/${sessionId}/messages`, {
      content,
    }).then((r) => r.data);
  }

  requestPlatformIntervention(sessionId: number) {
    return this.request('POST', `/api/app/support/sessions/${sessionId}/platform-intervention`);
  }

  supportSessionDetail(sessionId: number) {
    return this.request<SupportSessionDetail>('GET', `/api/app/support/sessions/${sessionId}`).then(
      (r) => r.data,
    );
  }

  merchantCurrentStore() {
    return this.request<{
      id: number;
      storeName?: string;
      announcement?: string;
      summary?: string;
      status?: string;
    }>('GET', '/api/merchant/stores/current').then((r) => r.data);
  }

  merchantStoreUpdate(body: Record<string, unknown>) {
    return this.request('PUT', '/api/merchant/stores/current', body).then((r) => r.data);
  }

  updateTakeawaySetting(storeId: number, acceptMode: string) {
    return this.request('POST', `/api/merchant/trade/stores/${storeId}/takeaway-setting`, {
      acceptMode,
    }).then((r) => r.data);
  }

  updateDeliveryRule(storeId: number, body: Record<string, unknown>) {
    return this.request('POST', `/api/merchant/trade/stores/${storeId}/delivery-rule`, body).then(
      (r) => r.data,
    );
  }

  createCatalogItem(body: Record<string, unknown>) {
    return this.request<{ id: number; title?: string; status?: string }>(
      'POST',
      '/api/merchant/catalog/items',
      body,
    ).then((r) => r.data);
  }

  catalogItems(filters = '') {
    return this.request<Array<{ id: number; title?: string; status?: string }>>(
      'GET',
      `/api/merchant/catalog/items${filters}`,
    ).then((r) => r.data);
  }

  catalogItem(itemId: number) {
    return this.request<{ id: number; title?: string; status?: string }>(
      'GET',
      `/api/merchant/catalog/items/${itemId}`,
    ).then((r) => r.data);
  }

  updateCatalogItemStatus(itemId: number, status: string) {
    return this.request('POST', `/api/merchant/catalog/items/${itemId}/status`, { status }).then(
      (r) => r.data,
    );
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
    return this.request('POST', `/api/merchant/trade/orders/${orderId}/booking/confirm`, { remark }).then(
      (r) => r.data,
    );
  }
}

export function newApi(token = '') {
  const origin = process.env.E2E_API_ORIGIN ?? 'http://127.0.0.1:8080';
  return new E2EApi(origin, token);
}
