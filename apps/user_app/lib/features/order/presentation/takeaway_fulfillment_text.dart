import '../../../shared/enums/business_type.dart';

String takeawayStatusLabel(OrderStatus status, String fulfillmentStatus) {
  if (status == OrderStatus.unpaid) return '待付款';
  if (status == OrderStatus.cancelled) return '已取消';
  if (status == OrderStatus.refunded) return '已退款';
  return switch (fulfillmentStatus) {
    'merchant_pending' => '待商家接单',
    'accepted' => '商家已接单',
    'preparing' => '备餐中',
    'ready_for_delivery' => '待配送',
    'delivering' => '配送中',
    'delivered' => '已送达',
    'completed' => '已完成',
    'merchant_rejected' => '商家已拒单',
    'cancelled' => '已取消',
    'abnormal' => '异常处理中',
    _ => status.labelForKind(OrderKind.takeaway),
  };
}

String takeawayStatusDescription(OrderStatus status, String fulfillmentStatus) {
  if (status == OrderStatus.unpaid) return '订单已创建，请尽快完成支付。';
  if (status == OrderStatus.cancelled) return '订单已取消，本单不会继续配送。';
  if (status == OrderStatus.refunded) return '订单已退款，本单不会继续配送。';
  return switch (fulfillmentStatus) {
    'merchant_pending' => '支付已完成，正在等待商家接单。',
    'accepted' => '商家已接单，正在安排制作。',
    'preparing' => '商家正在备餐，请耐心等待。',
    'ready_for_delivery' => '餐品已出餐，等待配送。',
    'delivering' => '骑手正在配送，请留意收货电话。',
    'delivered' => '订单已送达，请及时查收。',
    'completed' => '本单已完成，可对本次服务进行评价。',
    'merchant_rejected' => '商家暂时无法接单，平台将继续处理。',
    'cancelled' => '订单已取消。',
    'abnormal' => '订单异常处理中，可等待平台处理。',
    _ => status == OrderStatus.used ? '订单已完成，可对本次服务进行评价。' : '订单正在履约中。',
  };
}

String takeawayStatusTag(OrderStatus status, String fulfillmentStatus) {
  if (status == OrderStatus.unpaid) return '等待付款';
  if (status == OrderStatus.cancelled) return '已取消';
  if (status == OrderStatus.refunded) return '已退款';
  return switch (fulfillmentStatus) {
    'merchant_pending' => '待接单',
    'accepted' => '已接单',
    'preparing' => '备餐',
    'ready_for_delivery' => '待配送',
    'delivering' => '配送中',
    'delivered' => '已送达',
    'completed' => '已完成',
    'merchant_rejected' => '已拒单',
    'cancelled' => '已取消',
    'abnormal' => '异常',
    _ => status.labelForKind(OrderKind.takeaway),
  };
}

String formatTimelineTime(DateTime? reachedAt) {
  if (reachedAt == null) return '未到达';
  return '${reachedAt.hour.toString().padLeft(2, '0')}:${reachedAt.minute.toString().padLeft(2, '0')}';
}
