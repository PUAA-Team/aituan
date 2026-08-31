package com.aituan.tradefulfillment.trade.api;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import com.aituan.tradefulfillment.trade.TradeService;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.BookingConfirmRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.BookingView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OpsBookingView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OpsOrderSummaryView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OpsVoucherView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderDetailView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderStatusCountView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.RefundRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.TakeawayOrderActionRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.VoucherLookupView;
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
@RequestMapping({"/api/merchant/trade", "/api/admin/trade"})
@Validated
public class TradeOpsController {
  private final TradeService tradeService;

  public TradeOpsController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @GetMapping("/orders")
  public ApiResponse<PageResponse<OpsOrderSummaryView>> listOrders(
      @RequestParam(required = false) String displayStatus,
      @RequestParam(required = false) String fulfillmentStatus,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(tradeService.listOpsOrders(displayStatus, fulfillmentStatus, page, pageSize));
  }

  @GetMapping("/orders/stats")
  public ApiResponse<List<OrderStatusCountView>> stats() {
    return ApiResponse.ok(tradeService.opsOrderStats());
  }

  @GetMapping("/orders/{orderId}")
  public ApiResponse<OrderDetailView> orderDetail(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.getOrderDetailForStaff(orderId));
  }

  @PostMapping("/orders/{orderId}/accept")
  public ApiResponse<OrderDetailView> accept(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.acceptTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/reject")
  public ApiResponse<OrderDetailView> reject(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.rejectTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/prepare")
  public ApiResponse<OrderDetailView> prepare(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.prepareTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/ready")
  public ApiResponse<OrderDetailView> ready(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.readyTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/delivery/advance")
  public ApiResponse<OrderDetailView> advanceDelivery(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.advanceDelivery(orderId));
  }

  @PostMapping("/orders/{orderId}/complete")
  public ApiResponse<OrderDetailView> complete(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.completeTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/abnormal")
  public ApiResponse<OrderDetailView> abnormal(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.markTakeawayAbnormal(orderId, request));
  }

  @PostMapping("/orders/{orderId}/refund")
  public ApiResponse<OrderDetailView> refund(@PathVariable long orderId, @RequestBody(required = false) RefundRequest request) {
    return ApiResponse.ok(tradeService.refundOrderForStaff(orderId, request));
  }

  @PostMapping("/vouchers/{voucherCode}/redeem")
  public ApiResponse<OrderDetailView> redeemVoucher(@PathVariable String voucherCode) {
    return ApiResponse.ok(tradeService.redeemVoucher(voucherCode));
  }

  @GetMapping("/vouchers/{voucherCode}")
  public ApiResponse<VoucherLookupView> lookupVoucher(@PathVariable String voucherCode) {
    return ApiResponse.ok(tradeService.lookupVoucher(voucherCode));
  }

  @GetMapping("/vouchers")
  public ApiResponse<PageResponse<OpsVoucherView>> listVouchers(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(tradeService.listOpsVouchers(status, keyword, page, pageSize));
  }

  @GetMapping("/bookings")
  public ApiResponse<PageResponse<OpsBookingView>> listBookings(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String businessType,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(tradeService.listOpsBookings(status, businessType, page, pageSize));
  }

  @PostMapping("/orders/{orderId}/booking/confirm")
  public ApiResponse<BookingView> confirmBooking(@PathVariable long orderId, @RequestBody(required = false) BookingConfirmRequest request) {
    return ApiResponse.ok(tradeService.confirmBookingForStaff(orderId, request));
  }
}
