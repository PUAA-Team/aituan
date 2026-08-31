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
}
