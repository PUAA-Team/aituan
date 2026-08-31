package com.aituan.identity.coupon;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 后台优惠券模板配置
@RestController
@RequestMapping("/api/admin/operation/coupon-templates")
@Validated
class CouponAdminController {
  private final CouponService couponService;

  CouponAdminController(CouponService couponService) {
    this.couponService = couponService;
  }

  @GetMapping
  ApiResponse<List<CouponTemplateView>> list() {
    return ApiResponse.ok(couponService.listTemplates());
  }

  @PostMapping
  ApiResponse<CouponTemplateView> create(@Valid @RequestBody CouponTemplateUpsertRequest request) {
    return ApiResponse.ok(couponService.createTemplate(request));
  }

  @PutMapping("/{id}")
  ApiResponse<CouponTemplateView> update(@PathVariable long id, @Valid @RequestBody CouponTemplateUpsertRequest request) {
    return ApiResponse.ok(couponService.updateTemplate(id, request));
  }
}
