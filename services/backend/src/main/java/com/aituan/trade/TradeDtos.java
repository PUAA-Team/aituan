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

record CheckoutPreviewRequest(
    @NotNull Long storeId,
    @NotBlank String businessType,
    Long addressId,
    @NotEmpty List<@Valid CheckoutItemRequest> items,
    String remark) {}

record CreateOrderRequest(
    @NotNull Long storeId,
    @NotBlank String businessType,
    Long addressId,
    @NotEmpty List<@Valid CheckoutItemRequest> items,
    String remark,
    String idempotencyKey) {}

record PayOrderRequest(@NotBlank String paymentMode) {}

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
    BigDecimal amount,
    BigDecimal payableAmount,
    BigDecimal discountAmount,
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
    String storeName,
    String title,
    BigDecimal amount,
    LocalDateTime createdAt) {}

record TimelineNodeView(String code, String text, LocalDateTime reachedAt) {}

record DeliveryTimelineView(String orderNo, String currentStage, List<TimelineNodeView> nodes) {}

record VoucherView(String voucherCode, String qrPayload, String status, LocalDateTime effectiveFrom, LocalDateTime effectiveTo) {}

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
    BigDecimal discountAmount,
    BigDecimal payableAmount,
    String addressSnapshot,
    String voucherSummary,
    String remark,
    LocalDateTime createdAt,
    LocalDateTime paidAt,
    LocalDateTime completedAt,
    List<OrderItemView> items,
    DeliveryTimelineView deliveryTimeline,
    VoucherView voucher) {}
