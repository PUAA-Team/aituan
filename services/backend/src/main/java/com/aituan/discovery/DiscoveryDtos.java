package com.aituan.discovery;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.BusinessType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

record ModuleView(Long id, String code, String name, String businessType, String summary, String icon) {}

record ItemCardView(
    Long id,
    String title,
    String subtitle,
    String businessType,
    Long categoryId,
    String categoryName,
    BigDecimal price,
    BigDecimal originalPrice,
    String coverUrl,
    List<String> tags,
    Integer stock,
    String saleStatus,
    Boolean soldOut,
    Long storeId,
    String storeName) {}

record StoreCardView(
    Long id,
    String name,
    String businessType,
    String distanceText,
    BigDecimal rating,
    Integer monthlySales,
    BigDecimal avgPrice,
    String status,
    String businessHoursText,
    String summary,
    String address,
    List<String> tags,
    String coverUrl,
    List<ItemCardView> matchedItems) {}

record SearchResultView(PageResponse<StoreCardView> page) {}

record CategoryView(Long id, String name, Integer sortOrder) {}

record ItemGroupView(Long categoryId, String categoryName, List<ItemCardView> items) {}

record ReviewSummaryView(BigDecimal rating, long count, List<String> highlights) {}

record DeliveryRuleView(BigDecimal deliveryFee, Integer estimatedMinutes, BigDecimal startPrice, String deliveryText) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record StoreDetailView(
    StoreCardView store,
    String businessType,
    List<CategoryView> categories,
    List<ItemGroupView> itemGroups,
    ReviewSummaryView reviewSummary,
    DeliveryRuleView deliveryRule) {}

record HomeView(List<ModuleView> modules, PageResponse<ItemCardView> recommendations, long unreadMessageCount) {}

record ModulePageView(String moduleCode, String businessType, List<StoreCardView> stores, PageResponse<ItemCardView> featuredItems) {}

record ItemDetailView(ItemCardView item, StoreCardView store, List<CategoryView> categories, List<ItemGroupView> itemGroups) {}
