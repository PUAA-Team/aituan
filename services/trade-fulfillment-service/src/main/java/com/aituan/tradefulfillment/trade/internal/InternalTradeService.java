package com.aituan.tradefulfillment.trade.internal;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.tradefulfillment.trade.repository.TradeRepository;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InternalTradeService {
  private final TradeRepository tradeRepository;

  InternalTradeService(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  OrderSnapshotView orderSnapshot(long orderId) {
    return snapshot(requireOrder(orderId));
  }

  ReviewEligibilityView reviewEligibility(long orderId) {
    OrderRow order = requireOrder(orderId);
    String reason = reviewReason(order);
    return new ReviewEligibilityView(reason == null, reason, snapshot(order));
  }

  @Transactional
  InternalCommandResult markReviewed(long orderId, long reviewId) {
    requireOrder(orderId);
    if (!tradeRepository.markOrderReviewed(orderId, reviewId)) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单已关联其他评价");
    }
    return new InternalCommandResult(true, "reviewed", "订单已标记为已评价");
  }

  PurchaseSignalsView purchaseSignals(long userId) {
    return new PurchaseSignalsView(
        tradeRepository.countOrders(userId, null),
        tradeRepository.countCompletedOrders(userId),
        tradeRepository.topStoreIds(userId, 10),
        tradeRepository.topCategoryIds(userId, 10));
  }

  StoreOrderMetricsView storeMetrics(long storeId) {
    var metrics = tradeRepository.storeOrderMetrics(storeId);
    return new StoreOrderMetricsView(metrics.orderCount(), metrics.amount(), metrics.pendingCount());
  }

  PlatformOrderMetricsView platformMetrics() {
    var metrics = tradeRepository.platformOrderMetrics();
    return new PlatformOrderMetricsView(
        metrics.orderCount(), metrics.paidOrderCount(), metrics.refundedOrderCount(), metrics.totalAmount());
  }

  private OrderRow requireOrder(long orderId) {
    return tradeRepository.findOrderById(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private OrderSnapshotView snapshot(OrderRow order) {
    return new OrderSnapshotView(
        order.id(), order.orderNo(), order.title(), order.userId(), order.storeId(), order.merchantId(),
        order.storeName(), order.displayStatus(), order.paymentStatus(), order.fulfillmentStatus(), order.orderType());
  }

  private String reviewReason(OrderRow order) {
    if (order.reviewId() != null) return "订单已经评价";
    if (!"paid".equals(order.paymentStatus())) return "订单尚未支付";
    if (!"none".equals(order.refundStatus())) return "退款订单不能评价";
    if (!"used".equals(order.displayStatus())) return "订单完成或券码核销后才能评价";
    return null;
  }
}
