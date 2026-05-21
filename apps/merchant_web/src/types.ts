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

export interface OpsOrder {
  id: number;
  orderNo: string;
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

export interface TakeawaySetting {
  storeId: number;
  storeName: string;
  acceptMode: 'manual' | 'auto';
}

export interface MerchantItem {
  id: number;
  storeId: number;
  title: string;
  subtitle: string;
  categoryName: string;
  price: number;
  originalPrice?: number;
  stock: number;
  status: 'on_sale' | 'off_sale';
  salesCount: number;
}

export interface DeliveryRule {
  storeId: number;
  deliveryFee: number;
  startPrice: number;
  estimatedMinutes: number;
  deliveryText: string;
}
