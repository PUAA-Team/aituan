import type { APIRequestContext, APIResponse } from '@playwright/test';

interface ApiEnvelope<T> {
  code: number;
  message: string;
  data: T;
}

interface RequestOptions {
  token?: string;
  data?: unknown;
  params?: Record<string, string | number | boolean | undefined>;
}

export interface AuthSession {
  token: string;
  expiresIn: number;
  profile: {
    id: number;
    nickname?: string;
  };
}

export interface PageResponse<T> {
  list: T[];
  page: number;
  pageSize: number;
  total: number;
}

export interface OrderDetail {
  id: number;
  orderNo: string;
  orderKind: string;
  displayStatus: string;
  paymentStatus: string;
  fulfillmentStatus: string;
  storeId: number;
  storeName: string;
  title: string;
  amount: number;
  discountAmount: number;
  payableAmount: number;
  refundStatus?: string;
  refundAmount?: number;
  refundReason?: string;
  items: Array<{
    itemId: number;
    itemName: string;
    quantity: number;
  }>;
  voucher?: {
    voucherCode: string;
    status: string;
  };
  booking?: BookingView;
}

export interface BookingView {
  orderId: number;
  orderNo: string;
  bookingDate: string;
  bookingTimeSlot: string;
  storeConfirmStatus: string;
  storeConfirmRemark?: string;
}

export class E2eApi {
  constructor(private readonly request: APIRequestContext) {}

  get<T>(path: string, options: Omit<RequestOptions, 'data'> = {}) {
    return this.call<T>('GET', path, options);
  }

  post<T>(path: string, options: RequestOptions = {}) {
    return this.call<T>('POST', path, options);
  }

  put<T>(path: string, options: RequestOptions = {}) {
    return this.call<T>('PUT', path, options);
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

  private async call<T>(method: 'GET' | 'POST' | 'PUT', path: string, options: RequestOptions) {
    const response = await this.request.fetch(path, {
      method,
      headers: options.token ? { Authorization: `Bearer ${options.token}` } : undefined,
      data: options.data,
      params: cleanParams(options.params),
    });
    return readSuccessfulEnvelope<T>(response, method, path);
  }
}

async function readSuccessfulEnvelope<T>(response: APIResponse, method: string, path: string) {
  const body = await response.text();
  if (!response.ok()) {
    throw new Error(`${method} ${path} 返回 HTTP ${response.status()}：${body}`);
  }
  let envelope: ApiEnvelope<T>;
  try {
    envelope = JSON.parse(body) as ApiEnvelope<T>;
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
