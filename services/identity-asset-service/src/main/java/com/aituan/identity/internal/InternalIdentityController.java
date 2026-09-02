package com.aituan.identity.internal;

import com.aituan.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
class InternalIdentityController {
  private final InternalIdentityService internalIdentityService;
  private final InternalServiceGuard internalServiceGuard;

  InternalIdentityController(InternalIdentityService internalIdentityService, InternalServiceGuard internalServiceGuard) {
    this.internalIdentityService = internalIdentityService;
    this.internalServiceGuard = internalServiceGuard;
  }

  @GetMapping("/users/{userId}/summary")
  ApiResponse<UserSummaryView> userSummary(@PathVariable long userId, HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.userSummary(userId));
  }

  @GetMapping("/users/{userId}/addresses/{addressId}/snapshot")
  ApiResponse<AddressSnapshotView> addressSnapshot(@PathVariable long userId, @PathVariable long addressId, HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.addressSnapshot(userId, addressId));
  }

  @GetMapping("/users/{userId}/home-summary")
  ApiResponse<UserHomeSummaryView> homeSummary(@PathVariable long userId, HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.homeSummary(userId));
  }

  @GetMapping("/users/{userId}/preference-signals")
  ApiResponse<List<PreferenceSignalView>> preferenceSignals(@PathVariable long userId, HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.preferenceSignals(userId));
  }

  @PostMapping("/coupons/quote")
  ApiResponse<CouponQuoteView> quote(
      @Valid @RequestBody CouponQuoteRequest requestBody,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.quote(requestBody, idempotencyKey));
  }

  @PostMapping("/coupons/{couponId}/use")
  ApiResponse<CouponCommandResult> useCoupon(
      @PathVariable long couponId,
      @Valid @RequestBody CouponUseRequest requestBody,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.useCoupon(couponId, requestBody, idempotencyKey));
  }

  @PostMapping("/coupons/{couponId}/release")
  ApiResponse<CouponCommandResult> releaseCoupon(
      @PathVariable long couponId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.releaseCoupon(couponId, idempotencyKey));
  }

  @PostMapping("/members/{userId}/growth")
  ApiResponse<CouponCommandResult> addGrowth(
      @PathVariable long userId,
      @Valid @RequestBody MemberGrowthRequest requestBody,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.addGrowth(userId, requestBody, idempotencyKey));
  }

  @PostMapping("/messages")
  ApiResponse<CouponCommandResult> publishMessage(
      @Valid @RequestBody MessagePublishRequest requestBody,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.publishMessage(requestBody, idempotencyKey));
  }

  @PostMapping("/merchant-accounts/provision")
  ApiResponse<MerchantAccountProvisionView> provisionMerchantAccount(
      @Valid @RequestBody MerchantAccountProvisionRequest requestBody,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.provisionMerchantAccount(requestBody, idempotencyKey));
  }

  @PostMapping("/merchant-accounts/{accountId}/deactivate")
  ApiResponse<CouponCommandResult> deactivateMerchantAccount(
      @PathVariable long accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.deactivateMerchantAccount(accountId, idempotencyKey));
  }

  @GetMapping("/metrics/platform/users")
  ApiResponse<PlatformUserMetricsView> userMetrics(HttpServletRequest request) {
    requireInternal(request);
    return ApiResponse.ok(internalIdentityService.userMetrics());
  }

  private void requireInternal(HttpServletRequest request) {
    internalServiceGuard.require(request.getHeader("X-Request-Id"), request.getHeader("X-Caller-Service"), request.getHeader("X-Service-Token"));
  }
}
