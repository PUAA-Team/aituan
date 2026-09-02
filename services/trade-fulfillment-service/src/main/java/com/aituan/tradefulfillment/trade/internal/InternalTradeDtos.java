package com.aituan.tradefulfillment.trade.internal;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

record OrderSnapshotView(
    Long orderId,
    String orderNo,
    String title,
    Long userId,
    Long storeId,
    Long merchantId,
    String storeName,
    String displayStatus,
    String paymentStatus,
    String fulfillmentStatus,
    String orderType) {}

record ReviewEligibilityView(boolean eligible, String reason, OrderSnapshotView order) {}

record OrderReviewedRequest(@NotNull Long reviewId) {}

record InternalCommandResult(boolean success, String status, String message) {}

record PurchaseSignalsView(
    long orderCount,
    long completedOrderCount,
    List<Long> storeIds,
    List<Long> categoryIds) {}

record StoreOrderMetricsView(long orderCount, BigDecimal amount, long pendingCount) {}

record PlatformOrderMetricsView(
    long orderCount,
    long paidOrderCount,
    long refundedOrderCount,
    BigDecimal totalAmount) {}
