package com.aituan.catalog;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

record CatalogItemView(
    Long id,
    Long storeId,
    String storeName,
    String businessType,
    Long categoryId,
    String categoryName,
    String title,
    String subtitle,
    BigDecimal price,
    BigDecimal originalPrice,
    Integer stock,
    String status,
    String coverUrl,
    String tagText,
    Integer salesCount,
    LocalDateTime updatedAt) {}

record CatalogItemUpsertRequest(
    Long storeId,
    @NotBlank String businessType,
    Long categoryId,
    @NotBlank String title,
    String subtitle,
    @NotNull BigDecimal price,
    BigDecimal originalPrice,
    @Min(0) Integer stock,
    String status,
    String coverUrl,
    String tagText) {}

record CatalogItemStatusRequest(@NotBlank String status) {}

record CatalogCategoryView(
    Long id,
    Long storeId,
    String businessType,
    String categoryCode,
    String categoryName,
    Integer sortOrder,
    String status,
    LocalDateTime updatedAt) {}

record CatalogCategoryUpsertRequest(
    Long storeId,
    @NotBlank String businessType,
    @NotBlank String categoryName,
    Integer sortOrder,
    String status) {}
