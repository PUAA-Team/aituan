package com.aituan.trade;

import com.aituan.common.api.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/merchant/trade", "/api/admin/trade"})
@Validated
class TradeOpsController {
  private final TradeService tradeService;

  TradeOpsController(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @GetMapping("/orders/{orderId}")
  ApiResponse<OrderDetailView> orderDetail(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.getOrderDetailForStaff(orderId));
  }

  @PostMapping("/orders/{orderId}/delivery/advance")
  ApiResponse<OrderDetailView> advanceDelivery(@PathVariable long orderId) {
    return ApiResponse.ok(tradeService.advanceDelivery(orderId));
  }

  @PostMapping("/vouchers/{voucherCode}/redeem")
  ApiResponse<OrderDetailView> redeemVoucher(@PathVariable String voucherCode) {
    return ApiResponse.ok(tradeService.redeemVoucher(voucherCode));
  }
}
