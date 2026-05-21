package com.aituan.trade;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/trade")
@Validated
class TradeController {
  private final TradeService tradeService;

  TradeController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @GetMapping("/payment-methods")
  ApiResponse<List<PaymentMethodView>> paymentMethods() {
    return ApiResponse.ok(tradeService.paymentMethods());
  }

  @PostMapping("/checkout/preview")
  ApiResponse<CheckoutPreviewView> preview(@Valid @RequestBody CheckoutPreviewRequest request) {
    return ApiResponse.ok(tradeService.preview(request));
  }

  @PostMapping("/orders")
  ApiResponse<OrderDetailView> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return ApiResponse.ok(tradeService.createOrder(request));
  }

  @PostMapping("/orders/{orderId}/pay")
  ApiResponse<OrderDetailView> pay(@PathVariable long orderId, @Valid @RequestBody PayOrderRequest request) {
    return ApiResponse.ok(tradeService.pay(orderId, request));
  }

  @GetMapping("/orders")
  ApiResponse<PageResponse<OrderSummaryView>> listOrders(
      @RequestParam(required = false) String displayStatus,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(tradeService.listOrders(displayStatus, page, pageSize));
  }

  @GetMapping("/orders/{orderId}")
  ApiResponse<OrderDetailView> orderDetail(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.getOrderDetail(orderId));
  }

  @GetMapping("/orders/{orderId}/delivery/timeline")
  ApiResponse<DeliveryTimelineView> deliveryTimeline(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.deliveryTimeline(orderId));
  }
}
