package com.aituan.tradefulfillment.trade.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class TradeDtos {
  private TradeDtos() {}

  public record PaymentMethodView(String code, String name, boolean enabled) {}

  public record CheckoutItemRequest(@NotNull Long itemId, @Min(1) Integer quantity) {}

  public record CartItemRequest(@NotNull Long storeId, @NotNull Long itemId, @Min(1) Integer quantity) {}

  public record CartItemQuantityRequest(@NotNull Long storeId, @Min(0) Integer quantity) {}

  public record CartLineView(
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

  public record CartView(Long storeId, String storeName, BigDecimal amount, List<CartLineView> items) {}

  public record CheckoutPreviewRequest(
      @NotNull Long storeId,
      @NotBlank String businessType,
      Long addressId,
      @NotEmpty List<@Valid CheckoutItemRequest> items,
      String remark,
      String tablewareOption,
      Integer tablewareCount,
      Long couponId) {}

  public record CreateOrderRequest(
      @NotNull Long storeId,
      @NotBlank String businessType,
      Long addressId,
      @NotEmpty List<@Valid CheckoutItemRequest> items,
      String remark,
      String tablewareOption,
      Integer tablewareCount,
      Long couponId,
      String idempotencyKey) {}

  public record PayOrderRequest(@NotBlank String paymentMode) {}

  public record RefundRequest(String reason) {}

  public record OrderAddressUpdateRequest(@NotNull Long addressId) {}

  public record BookingRequest(
      String contactName,
      String contactPhone,
      String bookingDate,
      String bookingTimeSlot,
      @Min(1) Integer guestCount,
      String remark) {}

  public record CheckoutItemView(
      Long itemId,
      String itemName,
      String subtitle,
      Integer quantity,
      BigDecimal unitPrice,
      BigDecimal totalPrice,
      Long categoryId,
      String categoryName) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record CheckoutPreviewView(
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

  public record OrderItemView(
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

  public record OrderSummaryView(
      Long id,
      String orderNo,
      String orderKind,
      String displayStatus,
      String fulfillmentStatus,
      String refundStatus,
      String storeName,
      String title,
      BigDecimal amount,
      LocalDateTime createdAt) {}

  public record OpsOrderSummaryView(
      Long id,
      String orderNo,
      String orderKind,
      String displayStatus,
      String paymentStatus,
      String fulfillmentStatus,
      String refundStatus,
      String currentStage,
      String currentStageText,
      String storeName,
      String title,
      BigDecimal amount,
      LocalDateTime createdAt) {}

  public record OrderStatusCountView(String status, String label, long count) {}

  public record TakeawayOrderActionRequest(String remark) {}

  public record TimelineNodeView(String code, String text, LocalDateTime reachedAt) {}

  public record DeliveryTimelineView(String orderNo, String currentStage, List<TimelineNodeView> nodes) {}

  public record VoucherView(String voucherCode, String qrPayload, String status, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record BookingView(
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

  public record BookingConfirmRequest(String remark) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record OpsBookingView(
      BookingView booking,
      String orderTitle,
      String displayStatus,
      String paymentStatus,
      String refundStatus,
      BigDecimal payableAmount,
      Boolean refundableByStaff) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record OpsVoucherView(
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
      BigDecimal payableAmount,
      String displayStatus,
      String refundStatus,
      Boolean refundableByStaff,
      LocalDateTime orderCreatedAt) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record VoucherLookupView(
      String voucherCode,
      String qrPayload,
      String status,
      LocalDateTime effectiveFrom,
      LocalDateTime effectiveTo,
      Long orderId,
      String orderNo,
      String orderTitle,
      String storeName,
      String businessType,
      BigDecimal payableAmount,
      String usageRulesSnapshot) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record AdminDeliveryTaskView(
      Long taskId,
      Long orderId,
      String orderNo,
      String storeName,
      String currentStage,
      String currentStageText,
      Boolean autoAdvanceEnabled,
      LocalDateTime pausedAt,
      String abnormalReason,
      LocalDateTime nextTickAt,
      LocalDateTime completedAt,
      LocalDateTime updatedAt) {}

  public record DeliveryActionRequest(String remark, String reason) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record OrderDetailView(
      Long id,
      String orderNo,
      String orderKind,
      String displayStatus,
      String paymentStatus,
      String fulfillmentStatus,
      String paymentMethod,
      Long storeId,
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
      String refundStatus,
      BigDecimal refundAmount,
      String refundReason,
      LocalDateTime refundedAt,
      Boolean refundableByUser,
      Boolean refundableByStaff,
      String refundHint,
      LocalDateTime createdAt,
      LocalDateTime paidAt,
      LocalDateTime completedAt,
      List<OrderItemView> items,
      DeliveryTimelineView deliveryTimeline,
      VoucherView voucher,
      BookingView booking) {}
}
