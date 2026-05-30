export type AdminPage = 'dashboard' | 'orders' | 'merchants' | 'users' | 'catalog' | 'delivery' | 'vouchers' | 'bookings' | 'announcements' | 'settings';

export interface AuthSession {
  token: string;
  profile: {
    nickname: string;
  };
}

export interface PageResponse<T> {
  list: T[];
  page: number;
  pageSize: number;
  total: number;
  hasNext: boolean;
}

export interface DashboardView {
  todayOrders: number;
  todayAmount: number;
  abnormalOrders: number;
  merchantCount: number;
  userCount: number;
  itemCount: number;
  deliveringTasks: number;
}

export interface AdminMerchant {
  merchantId: number;
  accountId?: number;
  merchantName: string;
  contactName: string;
  contactPhone: string;
  licenseNo: string;
  status: string;
  auditStatus: string;
  storeCount: number;
  itemCount: number;
  settledAt: string;
}

export interface AdminMerchantForm {
  accountId?: number;
  merchantName: string;
  contactName: string;
  contactPhone: string;
  licenseNo: string;
  status: string;
  auditStatus: string;
}

export interface AdminMerchantApplication {
  id: number;
  applicationNo: string;
  accountId?: number;
  merchantName: string;
  contactName: string;
  contactPhone: string;
  businessType: string;
  storeName: string;
  address: string;
  status: string;
  auditRemark?: string;
  submittedAt: string;
  auditedBy?: number;
  auditedAt?: string;
}

export interface AdminCertificationMaterial {
  id: number;
  merchantId: number;
  applicationId?: number;
  merchantName: string;
  materialType: string;
  materialName: string;
  fileUrl: string;
  status: string;
  rejectReason?: string;
  submittedAt: string;
  auditedBy?: number;
  auditedAt?: string;
}

export interface AdminStore {
  storeId: number;
  merchantId: number;
  merchantName: string;
  storeName: string;
  businessType: string;
  summary: string;
  address: string;
  status: string;
  businessHoursText: string;
  tagText: string;
  coverUrl: string;
  contactPhone: string;
  announcement: string;
  updatedAt: string;
}

export interface AdminStoreForm {
  merchantId?: number;
  storeName: string;
  businessType: string;
  summary: string;
  address: string;
  status: string;
  businessHoursText: string;
  tagText: string;
  coverUrl: string;
  contactPhone: string;
  announcement: string;
}

export interface AdminUser {
  accountId: number;
  userId: number;
  nickname: string;
  avatarUrl: string;
  phone: string;
  email: string;
  status: string;
  addressCount: number;
  orderCount: number;
  createdAt: string;
}

export interface AdminUserForm {
  nickname: string;
  avatarUrl: string;
  phone: string;
  email: string;
  status: string;
}

export interface OpsOrder {
  id: number;
  orderNo: string;
  orderKind: string;
  displayStatus: string;
  paymentStatus: string;
  fulfillmentStatus: string;
  currentStage: string;
  currentStageText: string;
  storeName: string;
  title: string;
  amount: number;
  createdAt: string;
}

export interface OrderLineItem {
  itemId: number;
  itemName: string;
  subtitle: string;
  categoryName: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface TimelineNode {
  code: string;
  text: string;
  reachedAt?: string;
}

export interface OrderDetail {
  id: number;
  orderNo: string;
  orderKind: string;
  displayStatus: string;
  paymentStatus: string;
  fulfillmentStatus: string;
  storeName: string;
  title: string;
  amount: number;
  deliveryFee: number;
  discountAmount: number;
  payableAmount: number;
  addressSnapshot?: string;
  remark?: string;
  createdAt: string;
  paidAt?: string;
  completedAt?: string;
  items: OrderLineItem[];
  deliveryTimeline?: {
    currentStage: string;
    nodes: TimelineNode[];
  };
}

export interface OrderStatusCount {
  status: string;
  label: string;
  count: number;
}

export interface CatalogItem {
  id: number;
  storeId: number;
  storeName: string;
  businessType: string;
  categoryId?: number;
  categoryName: string;
  title: string;
  subtitle: string;
  price: number;
  originalPrice?: number;
  stock: number;
  status: 'on_sale' | 'off_sale';
  coverUrl: string;
  tagText: string;
  salesCount: number;
  updatedAt: string;
}

export interface CatalogItemForm {
  storeId?: number;
  businessType: string;
  categoryId?: number;
  title: string;
  subtitle: string;
  price: number;
  originalPrice?: number;
  stock: number;
  status: 'on_sale' | 'off_sale';
  coverUrl: string;
  tagText: string;
}

export interface CatalogCategory {
  id: number;
  storeId: number;
  businessType: string;
  categoryCode: string;
  categoryName: string;
  sortOrder: number;
  status: string;
  updatedAt: string;
}

export interface DeliveryTask {
  taskId: number;
  orderId: number;
  orderNo: string;
  storeName: string;
  currentStage: string;
  currentStageText: string;
  autoAdvanceEnabled: boolean;
  pausedAt?: string;
  abnormalReason?: string;
  nextTickAt?: string;
  completedAt?: string;
  updatedAt: string;
}

export interface DeliverySetting {
  autoAdvanceEnabled: boolean;
  tickMinutes: number;
}

export interface Announcement {
  id: number;
  title: string;
  content: string;
  targetClient: string;
  coverUrl: string;
  status: 'draft' | 'published' | 'offline';
  startAt?: string;
  endAt?: string;
  sortOrder: number;
  createdBy?: number;
  updatedAt: string;
}

export interface AnnouncementForm {
  title: string;
  content: string;
  targetClient: string;
  coverUrl: string;
  status: 'draft' | 'published' | 'offline';
  startAt?: string;
  endAt?: string;
  sortOrder: number;
}

export interface AdminConfig {
  configKey: string;
  configValue: string;
  remark: string;
  updatedAt: string;
}

export interface AuditLog {
  id: number;
  actorType: string;
  actorId: number;
  actionType: string;
  targetType: string;
  targetId: number;
  detail: string;
  createdAt: string;
}

// Stage5-D：券码与预约治理
export interface OpsVoucher {
  voucherCode: string;
  qrPayload: string;
  status: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  verifiedAt?: string;
  verifiedBy?: number;
  orderId: number;
  orderNo: string;
  orderTitle: string;
  storeName: string;
  businessType: string;
  payableAmount: number;
  displayStatus: string;
  orderCreatedAt?: string;
}

export interface BookingView {
  orderId: number;
  orderNo: string;
  storeName: string;
  businessType: string;
  contactName?: string;
  contactPhone?: string;
  bookingDate?: string;
  bookingTimeSlot?: string;
  guestCount: number;
  storeConfirmStatus: string;
  storeConfirmRemark?: string;
  confirmedAt?: string;
  createdAt?: string;
}

export interface OpsBooking {
  booking: BookingView;
  orderTitle: string;
  displayStatus: string;
  paymentStatus: string;
  payableAmount: number;
}
