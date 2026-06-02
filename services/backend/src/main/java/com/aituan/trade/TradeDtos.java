package com.aituan.trade;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record CheckoutItemRequest(@NotNull Long itemId, @Min(1) Integer quantity) {}

record CartItemRequest(@NotNull Long storeId, @NotNull Long itemId, @Min(1) Integer quantity) {}

record CartItemQuantityRequest(@NotNull Long storeId, @Min(0) Integer quantity) {}

record CartLineView(
    Long itemId,
    String itemName,
    String subtitle,
    String categoryName,
    BigDecimal unitPrice,
    Integer quantity,
    BigDecimal totalPrice,
    Integer stock,
    String status,
    Boolean soldOut) {}

record CartView(Long storeId, String storeName, BigDecimal amount, List<CartLineView> items) {}

record CheckoutPreviewRequest(
    @NotNull Long storeId,
    @NotBlank String businessType,
    Long addressId,
    @NotEmpty List<@Valid CheckoutItemRequest> items,
    String remark,
    String tablewareOption,
    Integer tablewareCount) {}

record CreateOrderRequest(
    @NotNull Long storeId,
    @NotBlank String businessType,
    Long addressId,
    @NotEmpty List<@Valid CheckoutItemRequest> items,
    String remark,
    String tablewareOption,
    Integer tablewareCount,
    String idempotencyKey) {}

record PayOrderRequest(@NotBlank String paymentMode) {}

record TakeawayOrderActionRequest(String remark) {}

record OrderAddressUpdateRequest(@NotNull Long addressId) {}

record TakeawaySettingRequest(@NotBlank String acceptMode) {}

record MerchantItemUpdateRequest(
    @NotBlank String title,
    String subtitle,
    @NotNull BigDecimal price,
    @NotNull @Min(0) Integer stock,
    @NotBlank String status) {}

record MerchantItemStatusRequest(@NotBlank String status) {}

record DeliveryRuleUpdateRequest(
    @NotNull BigDecimal deliveryFee,
    @NotNull BigDecimal startPrice,
    @Min(1) Integer estimatedMinutes,
    @NotNull BigDecimal maxDeliveryDistanceKm,
    String packageFeeMode,
    BigDecimal packageFeeFixed,
    BigDecimal packageFeePerItem,
    BigDecimal distanceExtraThresholdKm,
    BigDecimal distanceExtraFee,
    BigDecimal distanceExtraStepKm,
    String deliveryText) {}

record TakeawaySettingView(Long storeId, String storeName, String acceptMode) {}

record MerchantItemView(
    Long id,
    Long storeId,
    String title,
    String subtitle,
    String categoryName,
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stock,
    String status,
    Integer salesCount) {}

record DeliveryRuleOpsView(
    Long storeId,
    BigDecimal deliveryFee,
    BigDecimal startPrice,
    Integer estimatedMinutes,
    BigDecimal maxDeliveryDistanceKm,
    String packageFeeMode,
    BigDecimal packageFeeFixed,
    BigDecimal packageFeePerItem,
    BigDecimal distanceExtraThresholdKm,
    BigDecimal distanceExtraFee,
    BigDecimal distanceExtraStepKm,
    String deliveryText) {}

record PaymentMethodView(String code, String name, boolean enabled) {}

record CheckoutItemView(
    Long itemId,
    String itemName,
    String subtitle,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    Long categoryId,
    String categoryName) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record CheckoutPreviewView(
    Long storeId,
    String storeName,
    String businessType,
    String addressSnapshot,
    BigDecimal deliveryFee,
    BigDecimal packageFee,
    BigDecimal distanceExtraFee,
    BigDecimal amount,
    BigDecimal payableAmount,
    BigDecimal discountAmount,
    BigDecimal startPrice,
    BigDecimal startPriceMissing,
    Boolean minimumOrderMet,
    BigDecimal deliveryDistanceKm,
    BigDecimal maxDeliveryDistanceKm,
    Integer estimatedDeliveryMinutes,
    LocalDateTime estimatedArrivalAt,
    String estimatedArrivalText,
    Boolean deliverable,
    String unavailableReason,
    String tablewareOption,
    Integer tablewareCount,
    String tablewareText,
    List<CheckoutItemView> items,
    String note) {}

record OrderItemView(
    Long itemId,
    String itemName,
    String subtitle,
    String businessType,
    Long categoryId,
    String categoryName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    String coverUrl) {}

record OrderSummaryView(
    Long id,
    String orderNo,
    String orderKind,
    String displayStatus,
    String fulfillmentStatus,
    String storeName,
    String title,
    BigDecimal amount,
    LocalDateTime createdAt) {}

record OpsOrderSummaryView(
    Long id,
    String orderNo,
    String orderKind,
    String displayStatus,
    String paymentStatus,
    String fulfillmentStatus,
    String currentStage,
    String currentStageText,
    String storeName,
    String title,
    BigDecimal amount,
    LocalDateTime createdAt) {}

record OrderStatusCountView(String status, String label, long count) {}

record TimelineNodeView(String code, String text, LocalDateTime reachedAt) {}

record DeliveryTimelineView(String orderNo, String currentStage, List<TimelineNodeView> nodes) {}

record VoucherView(String voucherCode, String qrPayload, String status, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {}

record BookingRequest(
    String contactName,
    String contactPhone,
    String bookingDate,
    String bookingTimeSlot,
    @Min(1) Integer guestCount,
    String remark) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record BookingView(
    Long orderId,
    String orderNo,
    String storeName,
    String businessType,
    String contactName,
    String contactPhone,
    String bookingDate,
    String bookingTimeSlot,
    Integer guestCount,
    String storeConfirmStatus,
    String storeConfirmRemark,
    LocalDateTime confirmedAt,
    LocalDateTime createdAt) {}

record BookingConfirmRequest(String remark) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpsBookingView(
    BookingView booking,
    String orderTitle,
    String displayStatus,
    String paymentStatus,
    java.math.BigDecimal payableAmount) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OpsVoucherView(
    String voucherCode,
    String qrPayload,
    String status,
    LocalDateTime effectiveFrom,
    LocalDateTime effectiveTo,
    LocalDateTime verifiedAt,
    Long verifiedBy,
    Long orderId,
    String orderNo,
    String orderTitle,
    String storeName,
    String businessType,
    java.math.BigDecimal payableAmount,
    String displayStatus,
    LocalDateTime orderCreatedAt) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record VoucherLookupView(
    String voucherCode,
    String qrPayload,
    String status,
    LocalDateTime effectiveFrom,
    LocalDateTime effectiveTo,
    String orderNo,
    String orderTitle,
    String storeName,
    String businessType,
    java.math.BigDecimal payableAmount,
    String usageRulesSnapshot) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record OrderDetailView(
    Long id,
    String orderNo,
    String orderKind,
    String displayStatus,
    String paymentStatus,
    String fulfillmentStatus,
    String paymentMethod,
    String storeName,
    String title,
    BigDecimal amount,
    BigDecimal deliveryFee,
    BigDecimal packageFee,
    BigDecimal discountAmount,
    BigDecimal payableAmount,
    String addressSnapshot,
    BigDecimal deliveryDistanceKm,
    LocalDateTime estimatedArrivalAt,
    String estimatedArrivalText,
    String deliveryCompletionText,
    String voucherSummary,
    String tablewareOption,
    Integer tablewareCount,
    String tablewareText,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime paidAt,
    LocalDateTime completedAt,
    List<OrderItemView> items,
    DeliveryTimelineView deliveryTimeline,
    VoucherView voucher,
    BookingView booking) {}
