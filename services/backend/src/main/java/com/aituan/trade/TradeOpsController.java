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
@RequestMapping({"/api/merchant/trade", "/api/admin/trade"})
@Validated
class TradeOpsController {
  private final TradeService tradeService;

  TradeOpsController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @GetMapping("/orders")
  ApiResponse<PageResponse<OpsOrderSummaryView>> listOrders(
      @RequestParam(required = false) String displayStatus,
      @RequestParam(required = false) String fulfillmentStatus,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(tradeService.listOpsOrders(displayStatus, fulfillmentStatus, page, pageSize));
  }

  @GetMapping("/orders/stats")
  ApiResponse<List<OrderStatusCountView>> stats() {
    return ApiResponse.ok(tradeService.opsOrderStats());
  }

  @GetMapping("/orders/{orderId}")
  ApiResponse<OrderDetailView> orderDetail(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.getOrderDetailForStaff(orderId));
  }

  @PostMapping("/orders/{orderId}/accept")
  ApiResponse<OrderDetailView> accept(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.acceptTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/reject")
  ApiResponse<OrderDetailView> reject(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.rejectTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/prepare")
  ApiResponse<OrderDetailView> prepare(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.prepareTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/ready")
  ApiResponse<OrderDetailView> ready(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.readyTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/delivery/advance")
  ApiResponse<OrderDetailView> advanceDelivery(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.advanceDelivery(orderId));
  }

  @PostMapping("/orders/{orderId}/complete")
  ApiResponse<OrderDetailView> complete(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.completeTakeawayOrder(orderId, request));
  }

  @PostMapping("/orders/{orderId}/abnormal")
  ApiResponse<OrderDetailView> abnormal(@PathVariable long orderId, @RequestBody(required = false) TakeawayOrderActionRequest request) {
    return ApiResponse.ok(tradeService.markTakeawayAbnormal(orderId, request));
  }

  @GetMapping("/stores/{storeId}/takeaway-setting")
  ApiResponse<TakeawaySettingView> getTakeawaySetting(@PathVariable long storeId) {
    return ApiResponse.ok(tradeService.getTakeawaySetting(storeId));
  }

  @PostMapping("/stores/{storeId}/takeaway-setting")
  ApiResponse<TakeawaySettingView> updateTakeawaySetting(@PathVariable long storeId, @Valid @RequestBody TakeawaySettingRequest request) {
    return ApiResponse.ok(tradeService.updateTakeawaySetting(storeId, request));
  }

  @GetMapping("/stores/{storeId}/items")
  ApiResponse<List<MerchantItemView>> listItems(@PathVariable long storeId, @RequestParam(required = false) String status) {
    return ApiResponse.ok(tradeService.listTakeawayItems(storeId, status));
  }

  @PostMapping("/stores/{storeId}/items/{itemId}")
  ApiResponse<MerchantItemView> updateItem(
      @PathVariable long storeId,
      @PathVariable long itemId,
      @Valid @RequestBody MerchantItemUpdateRequest request) {
    return ApiResponse.ok(tradeService.updateTakeawayItem(storeId, itemId, request));
  }

  @PostMapping("/stores/{storeId}/items/{itemId}/status")
  ApiResponse<MerchantItemView> updateItemStatus(
      @PathVariable long storeId,
      @PathVariable long itemId,
      @Valid @RequestBody MerchantItemStatusRequest request) {
    return ApiResponse.ok(tradeService.updateTakeawayItemStatus(storeId, itemId, request));
  }

  @GetMapping("/stores/{storeId}/delivery-rule")
  ApiResponse<DeliveryRuleOpsView> getDeliveryRule(@PathVariable long storeId) {
    return ApiResponse.ok(tradeService.getDeliveryRule(storeId));
  }

  @PostMapping("/stores/{storeId}/delivery-rule")
  ApiResponse<DeliveryRuleOpsView> updateDeliveryRule(@PathVariable long storeId, @Valid @RequestBody DeliveryRuleUpdateRequest request) {
    return ApiResponse.ok(tradeService.updateDeliveryRule(storeId, request));
  }

  @PostMapping("/vouchers/{voucherCode}/redeem")
  ApiResponse<OrderDetailView> redeemVoucher(@PathVariable String voucherCode) {
    return ApiResponse.ok(tradeService.redeemVoucher(voucherCode));
  }
}
