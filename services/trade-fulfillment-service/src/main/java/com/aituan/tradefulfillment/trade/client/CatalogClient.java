package com.aituan.tradefulfillment.trade.client;

import java.math.BigDecimal;
import java.util.Optional;

public interface CatalogClient {

  Optional<StoreSnapshot> findStore(long storeId);

  Optional<ItemSnapshot> findItem(long itemId);

  DeliveryRuleSnapshot deliveryRule(long storeId);

  record StoreSnapshot(
      Long id,
      String storeName,
      String businessType,
      BigDecimal longitude,
      BigDecimal latitude) {}

  record ItemSnapshot(
      Long id,
      Long storeId,
      String itemName,
      String subtitle,
      String businessType,
      Long categoryId,
      String categoryName,
      BigDecimal price,
      String status,
      Integer stock) {}

  record DeliveryRuleSnapshot(
      BigDecimal deliveryFee,
      BigDecimal startPrice,
      Integer estimatedMinutes,
      BigDecimal maxDeliveryDistanceKm,
      String packageFeeMode,
      BigDecimal packageFeeFixed,
      BigDecimal packageFeePerItem,
      BigDecimal distanceExtraThresholdKm,
      BigDecimal distanceExtraFee,
      BigDecimal distanceExtraStepKm) {}
}
