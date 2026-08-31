package com.aituan.tradefulfillment.trade.api;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import com.aituan.tradefulfillment.trade.TradeService;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.BookingRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.BookingView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartItemQuantityRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartItemRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutPreviewRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutPreviewView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CreateOrderRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.DeliveryTimelineView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderDetailView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderSummaryView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.PayOrderRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.PaymentMethodView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.RefundRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/trade")
@Validated
public class TradeController {
  private final TradeService tradeService;

  public TradeController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @GetMapping("/payment-methods")
  public ApiResponse<List<PaymentMethodView>> paymentMethods() {
    return ApiResponse.ok(tradeService.paymentMethods());
  }

  @GetMapping("/cart")
  public ApiResponse<CartView> cart(@RequestParam @Min(1) long storeId) {
    return ApiResponse.ok(tradeService.getCart(storeId));
  }

  @PostMapping("/cart/items")
  public ApiResponse<CartView> addCartItem(@Valid @RequestBody CartItemRequest request) {
    return ApiResponse.ok(tradeService.addCartItem(request));
  }

  @PutMapping("/cart/items/{itemId}")
  public ApiResponse<CartView> updateCartItem(@PathVariable long itemId, @Valid @RequestBody CartItemQuantityRequest request) {
    return ApiResponse.ok(tradeService.updateCartItem(itemId, request));
  }

  @DeleteMapping("/cart/items/{itemId}")
  public ApiResponse<CartView> removeCartItem(@PathVariable long itemId, @RequestParam @Min(1) long storeId) {
    return ApiResponse.ok(tradeService.removeCartItem(storeId, itemId));
  }

  @DeleteMapping("/cart")
  public ApiResponse<CartView> clearCart(@RequestParam @Min(1) long storeId) {
    return ApiResponse.ok(tradeService.clearCart(storeId));
  }

  @PostMapping("/checkout/preview")
  public ApiResponse<CheckoutPreviewView> preview(@Valid @RequestBody CheckoutPreviewRequest request) {
    return ApiResponse.ok(tradeService.preview(request));
  }

  @PostMapping("/orders")
  public ApiResponse<OrderDetailView> createOrder(@Valid @RequestBody CreateOrderRequest request) {
    return ApiResponse.ok(tradeService.createOrder(request));
  }

  @PostMapping("/orders/{orderId}/pay")
  public ApiResponse<OrderDetailView> pay(@PathVariable long orderId, @Valid @RequestBody PayOrderRequest request) {
    return ApiResponse.ok(tradeService.pay(orderId, request));
  }

  @PostMapping("/orders/{orderId}/refund")
  public ApiResponse<OrderDetailView> refund(@PathVariable long orderId, @RequestBody(required = false) RefundRequest request) {
    return ApiResponse.ok(tradeService.refundOrderForUser(orderId, request));
  }

  @GetMapping("/orders")
  public ApiResponse<PageResponse<OrderSummaryView>> listOrders(
      @RequestParam(required = false) String displayStatus,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(tradeService.listOrders(displayStatus, page, pageSize));
  }

  @GetMapping("/orders/{orderId}")
  public ApiResponse<OrderDetailView> orderDetail(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.getOrderDetail(orderId));
  }

  @GetMapping("/orders/{orderId}/delivery/timeline")
  public ApiResponse<DeliveryTimelineView> deliveryTimeline(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.deliveryTimeline(orderId));
  }

  @GetMapping("/orders/{orderId}/booking")
  public ApiResponse<BookingView> getBooking(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.getBookingForUser(orderId));
  }

  @PostMapping("/orders/{orderId}/booking")
  public ApiResponse<BookingView> upsertBooking(@PathVariable long orderId, @Valid @RequestBody BookingRequest request) {
    return ApiResponse.ok(tradeService.upsertBooking(orderId, request));
  }
}
