export type AdminPage = 'dashboard' | 'orders' | 'merchants' | 'users' | 'catalog' | 'delivery' | 'announcements' | 'settings';

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
  merchantName: string;
  contactName: string;
  contactPhone: string;
  status: string;
  auditStatus: string;
  storeCount: number;
  itemCount: number;
  settledAt: string;
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
  coverUrl: string;
  contactPhone: string;
  announcement: string;
  updatedAt: string;
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
