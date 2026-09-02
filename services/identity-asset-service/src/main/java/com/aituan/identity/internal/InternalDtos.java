package com.aituan.identity.internal;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

record UserSummaryView(Long userId, Long accountId, String nickname, String avatarUrl, String phone, String email, String status, String memberLevelName) {}

record AddressSnapshotView(
    Long id,
    Long addressId,
    Long userId,
    String contactName,
    String contactPhone,
    String province,
    String city,
    String district,
    String detailAddress,
    BigDecimal longitude,
    BigDecimal latitude,
    String deliveryNote) {}

record UserHomeSummaryView(Long userId, String nickname, String memberLevelName, int growthValue, long unreadMessageCount, long usableCouponCount) {}

record PreferenceSignalView(
    String favoriteType,
    Long targetId,
    String targetName,
    String businessType,
    Long categoryId,
    Long storeId,
    Long itemId,
    int weight,
    String source) {}

record CouponQuoteRequest(@NotNull Long userId, Long couponId, @NotNull BigDecimal orderAmount) {}

record CouponQuoteView(boolean usable, BigDecimal discountAmount, String reason) {}

record CouponUseRequest(@NotNull Long userId, @NotNull Long orderId, @NotNull BigDecimal orderAmount) {}

record CouponCommandResult(boolean success, String status, String message) {}

record MemberGrowthRequest(
    @NotBlank String sourceType,
    @NotNull Long sourceId,
    @JsonAlias("growth") @NotNull Integer delta,
    String reason) {}

record MessagePublishRequest(
    @NotNull Long userId,
    @JsonAlias("messageType") @NotBlank String type,
    @NotBlank String title,
    @NotBlank String content,
    String badgeText,
    Long relatedOrderId,
    String relatedTargetType,
    Long relatedTargetId) {}

record MerchantAccountProvisionRequest(
    @NotBlank String loginName,
    String merchantName,
    String contactName,
    String contactPhone) {}

record MerchantAccountProvisionView(
    boolean success,
    Long accountId,
    String accountNo,
    String loginName,
    String status,
    String message) {}

record PlatformUserMetricsView(long userCount, long activeUserCount, long memberUserCount) {}
