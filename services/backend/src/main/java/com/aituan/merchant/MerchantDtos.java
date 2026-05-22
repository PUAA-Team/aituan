package com.aituan.merchant;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

record MerchantProfileView(
    Long merchantId,
    String merchantName,
    String contactName,
    String contactPhone,
    String licenseNo,
    String status,
    String auditStatus,
    Long currentStoreId,
    List<MerchantStoreView> stores) {}

record MerchantStoreView(
    Long id,
    Long merchantId,
    String storeName,
    String businessType,
    String summary,
    String address,
    BigDecimal rating,
    Integer monthlySales,
    BigDecimal avgPrice,
    String status,
    String businessHoursText,
    String tagText,
    String coverUrl,
    String contactPhone,
    String announcement,
    LocalDateTime updatedAt) {}

record MerchantProfileUpdateRequest(
    @NotBlank String merchantName,
    @NotBlank String contactName,
    @NotBlank String contactPhone) {}

record MerchantStoreUpdateRequest(
    @NotBlank String storeName,
    @NotBlank String summary,
    @NotBlank String address,
    String businessHoursText,
    String tagText,
    String contactPhone,
    String announcement,
    String status) {}
