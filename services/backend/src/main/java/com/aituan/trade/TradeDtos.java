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
    String remark) {}

record CreateOrderRequest(
    @NotNull Long storeId,
    @NotBlank String businessType,
    Long addressId,
    @NotEmpty List<@Valid CheckoutItemRequest> items,
    String remark,
    String idempotencyKey) {}

record PayOrderRequest(@NotBlank String paymentMode) {}

record TakeawayOrderActionRequest(String remark) {}

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

record DeliveryRuleOpsView(Long storeId, BigDecimal deliveryFee, BigDecimal startPrice, Integer estimatedMinutes, String deliveryText) {}

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
