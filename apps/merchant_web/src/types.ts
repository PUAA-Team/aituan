export type ConsolePage = 'orders' | 'catalog' | 'fulfillment' | 'store';

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

export interface MerchantProfile {
  merchantId: number;
  merchantName: string;
  contactName: string;
  contactPhone: string;
  licenseNo: string;
  status: string;
  auditStatus: string;
  currentStoreId?: number;
  stores: MerchantStore[];
}

export interface MerchantStore {
  id: number;
  merchantId: number;
  storeName: string;
  businessType: string;
  summary: string;
  address: string;
  rating: number;
  monthlySales: number;
  avgPrice: number;
  status: string;
  businessHoursText: string;
  tagText: string;
  coverUrl: string;
  contactPhone: string;
  announcement: string;
  longitude?: number | null;
  latitude?: number | null;
  updatedAt: string;
}

export interface MerchantApplicationForm {
  merchantName: string;
  contactName: string;
  contactPhone: string;
  businessType: string;
  storeName: string;
  address: string;
}

export interface MerchantApplicationView extends MerchantApplicationForm {
  id: number;
  applicationNo: string;
  accountId?: number;
  status: string;
  auditRemark?: string;
  submittedAt: string;
  auditedAt?: string;
}

export interface MerchantCertification {
  auditStatus: string;
  licenseNo: string;
  materials: CertificationMaterial[];
}

export interface CertificationMaterial {
  id: number;
  merchantId: number;
  applicationId?: number;
  materialType: string;
  materialName: string;
  fileUrl: string;
  status: string;
  rejectReason?: string;
  submittedAt: string;
  auditedAt?: string;
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
  coverUrl?: string;
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

export interface TakeawaySetting {
  storeId: number;
  storeName: string;
  acceptMode: 'manual' | 'auto';
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

export interface DeliveryRule {
  storeId: number;
  deliveryFee: number;
  startPrice: number;
  estimatedMinutes: number;
  deliveryText: string;
}
