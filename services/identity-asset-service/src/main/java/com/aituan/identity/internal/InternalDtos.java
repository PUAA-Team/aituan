package com.aituan.identity.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record UserSummaryView(Long userId, Long accountId, String nickname, String avatarUrl, String phone, String email, String status, String memberLevelName) {}

record AddressSnapshotView(
    Long addressId,
    Long userId,
    String contactName,
    String contactPhone,
    String province,
    String city,
    String district,
    String detailAddress,
    Double longitude,
    Double latitude,
    String deliveryNote) {}

record UserHomeSummaryView(Long userId, String nickname, String memberLevelName, int growthValue, long unreadMessageCount, long usableCouponCount) {}

record UserPreferenceSignalsView(Long userId, List<FavoriteSignal> favorites) {}

record FavoriteSignal(String favoriteType, Long targetId, String targetName) {}

record CouponQuoteRequest(@NotNull Long userId, Long couponId, @NotNull BigDecimal orderAmount) {}

record CouponQuoteView(boolean usable, BigDecimal discountAmount, String reason) {}

record CouponUseRequest(@NotNull Long userId, @NotNull Long orderId, @NotNull BigDecimal orderAmount) {}

record CouponCommandResult(boolean success, String status, String message) {}

record MemberGrowthRequest(@NotBlank String sourceType, @NotNull Long sourceId, @NotNull Integer delta, String reason) {}

record MessagePublishRequest(
    @NotNull Long userId,
    @NotBlank String type,
    @NotBlank String title,
    @NotBlank String content,
    String badgeText,
    Long relatedOrderId,
    String relatedTargetType,
    Long relatedTargetId) {}

record MerchantAccountProvisionRequest(@NotBlank String loginName, String phone, String email, @NotBlank String password, String displayName) {}

record MerchantAccountProvisionView(Long accountId, String accountNo, String loginName, String status) {}

record PlatformUserMetricsView(long userCount, long activeUserCount, long memberUserCount) {}
