package com.aituan.tradefulfillment.trade.internal;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
class InternalTradeController {
  private final InternalTradeService internalTradeService;

  InternalTradeController(InternalTradeService internalTradeService) {
    this.internalTradeService = internalTradeService;
  }

  @GetMapping("/orders/{orderId}/snapshot")
  ApiResponse<OrderSnapshotView> orderSnapshot(@PathVariable long orderId) {
    return ApiResponse.ok(internalTradeService.orderSnapshot(orderId));
  }

  @GetMapping("/orders/{orderId}/review-eligibility")
  ApiResponse<ReviewEligibilityView> reviewEligibility(@PathVariable long orderId) {
    return ApiResponse.ok(internalTradeService.reviewEligibility(orderId));
  }

  @PostMapping("/orders/{orderId}/reviewed")
  ApiResponse<InternalCommandResult> markReviewed(
      @PathVariable long orderId,
      @Valid @RequestBody OrderReviewedRequest request) {
    return ApiResponse.ok(internalTradeService.markReviewed(orderId, request.reviewId()));
  }

  @GetMapping("/users/{userId}/purchase-signals")
  ApiResponse<PurchaseSignalsView> purchaseSignals(@PathVariable long userId) {
    return ApiResponse.ok(internalTradeService.purchaseSignals(userId));
  }

  @GetMapping("/metrics/stores/{storeId}/orders")
  ApiResponse<StoreOrderMetricsView> storeMetrics(@PathVariable long storeId) {
    return ApiResponse.ok(internalTradeService.storeMetrics(storeId));
  }

  @GetMapping("/metrics/platform/orders")
  ApiResponse<PlatformOrderMetricsView> platformMetrics() {
    return ApiResponse.ok(internalTradeService.platformMetrics());
  }
}
