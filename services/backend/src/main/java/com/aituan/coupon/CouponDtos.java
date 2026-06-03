package com.aituan.coupon;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// 我的优惠券
@JsonInclude(JsonInclude.Include.NON_NULL)
record UserCouponView(
    Long id,
    Long templateId,
    String name,
    String type,
    BigDecimal faceValue,
    BigDecimal thresholdAmount,
    String status,
    String discountDesc,
    String thresholdDesc,
    LocalDateTime claimedAt,
    LocalDateTime expireAt,
    LocalDateTime usedAt,
    Long usedOrderId) {}

// 可领取的优惠券
@JsonInclude(JsonInclude.Include.NON_NULL)
record AvailableCouponView(
    Long templateId,
    String name,
    String type,
    BigDecimal faceValue,
    BigDecimal thresholdAmount,
    String discountDesc,
    String thresholdDesc,
    String validDesc,
    Integer remaining,
    boolean claimable,
    String reason) {}

// 下单时的可用券选项
@JsonInclude(JsonInclude.Include.NON_NULL)
record OrderCouponOptionView(
    Long userCouponId,
    String name,
    String discountDesc,
    BigDecimal discountAmount,
    boolean usable,
    String reason) {}

// 后台优惠券模板
@JsonInclude(JsonInclude.Include.NON_NULL)
record CouponTemplateView(
    Long id,
    String name,
    String type,
    BigDecimal faceValue,
    BigDecimal thresholdAmount,
    String businessScope,
    String validKind,
    LocalDateTime validStart,
    LocalDateTime validEnd,
    Integer validDays,
    int totalQty,
    int issuedQty,
    int perUserLimit,
    String status) {}

// 后台新增/编辑优惠券模板
record CouponTemplateUpsertRequest(
    @NotBlank String name,
    @NotBlank String type,
    @NotNull BigDecimal faceValue,
    BigDecimal thresholdAmount,
    String businessScope,
    @NotBlank String validKind,
    LocalDateTime validStart,
    LocalDateTime validEnd,
    Integer validDays,
    Integer totalQty,
    Integer perUserLimit,
    String status) {}
