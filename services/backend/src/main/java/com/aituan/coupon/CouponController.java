package com.aituan.coupon;

import com.aituan.common.api.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 用户端优惠券
@RestController
@RequestMapping("/api/app/account/coupons")
class CouponController {
  private final CouponService couponService;

  CouponController(CouponService couponService) {
    this.couponService = couponService;
  }

  @GetMapping
  ApiResponse<List<UserCouponView>> myCoupons(@RequestParam(defaultValue = "usable") String status) {
    return ApiResponse.ok(couponService.myCoupons(status));
  }

  @GetMapping("/available")
  ApiResponse<List<AvailableCouponView>> available() {
    return ApiResponse.ok(couponService.availableTemplates());
  }

  @PostMapping("/{templateId}/claim")
  ApiResponse<Void> claim(@PathVariable long templateId) {
    couponService.claim(templateId);
    return ApiResponse.ok(null);
  }

  @GetMapping("/usable-for-order")
  ApiResponse<List<OrderCouponOptionView>> usableForOrder(@RequestParam BigDecimal orderAmount) {
    return ApiResponse.ok(couponService.usableForOrder(orderAmount));
  }
}
