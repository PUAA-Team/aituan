package com.aituan.merchantcatalog;

import com.aituan.common.api.PageResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

record MerchantProfileView(Long merchantId, Long accountId, String merchantName, String contactName, String contactPhone, String licenseNo, String status, String auditStatus, Long currentStoreId, List<MerchantStoreView> stores) {}
record MerchantStoreView(Long id, Long merchantId, String storeName, String businessType, String summary, String address, BigDecimal rating, Integer monthlySales, BigDecimal avgPrice, String status, String businessHoursText, String tagText, String coverUrl, String contactPhone, String announcement, BigDecimal longitude, BigDecimal latitude, LocalDateTime updatedAt) {}
record MerchantApplicationSubmitRequest(@NotBlank String merchantName, @NotBlank String contactName, @NotBlank String contactPhone, @NotBlank String businessType, @NotBlank String storeName, @NotBlank String address) {}
record MerchantApplicationView(Long id, String applicationNo, Long accountId, String merchantName, String contactName, String contactPhone, String businessType, String storeName, String address, String status, String auditRemark, LocalDateTime submittedAt, LocalDateTime auditedAt) {}
record MerchantCertificationView(String auditStatus, String licenseNo, List<CertificationMaterialView> materials) {}
record CertificationMaterialView(Long id, Long merchantId, Long applicationId, String materialType, String materialName, String fileUrl, String status, String rejectReason, LocalDateTime submittedAt, LocalDateTime auditedAt) {}
record MerchantProfileUpdateRequest(@NotBlank String merchantName, @NotBlank String contactName, @NotBlank String contactPhone) {}
record MerchantStoreUpdateRequest(@NotBlank String storeName, @NotBlank String summary, @NotBlank String address, String businessHoursText, String tagText, String contactPhone, String announcement, String status, BigDecimal longitude, BigDecimal latitude) {}

record CatalogItemView(Long id, Long storeId, String storeName, String businessType, Long categoryId, String categoryName, String title, String subtitle, BigDecimal price, BigDecimal originalPrice, Integer stock, String status, String coverUrl, String tagText, Integer salesCount, String businessAttributes, String usageRules, String refundPolicy, String notice, Integer validityDays, LocalDateTime updatedAt) {}
record CatalogItemUpsertRequest(Long storeId, @NotBlank String businessType, Long categoryId, @NotBlank String title, String subtitle, @NotNull BigDecimal price, BigDecimal originalPrice, @Min(0) Integer stock, String status, String coverUrl, String tagText, String businessAttributes, String usageRules, String refundPolicy, String notice, @Min(1) Integer validityDays) {}
record CatalogItemStatusRequest(@NotBlank String status) {}
record CatalogCategoryView(Long id, Long storeId, String businessType, String categoryCode, String categoryName, Integer sortOrder, String status, LocalDateTime updatedAt) {}
record CatalogCategoryUpsertRequest(Long storeId, @NotBlank String businessType, @NotBlank String categoryName, Integer sortOrder, String status) {}

record AdminMerchantView(Long merchantId, Long accountId, String merchantName, String contactName, String contactPhone, String licenseNo, String status, String auditStatus, Long storeCount, Long itemCount, LocalDateTime settledAt) {}
record AdminMerchantApplicationView(Long id, String applicationNo, Long accountId, String merchantName, String contactName, String contactPhone, String businessType, String storeName, String address, String status, String auditRemark, LocalDateTime submittedAt, Long auditedBy, LocalDateTime auditedAt) {}
record AdminMerchantApplicationAuditRequest(String remark) {}
record AdminCertificationMaterialView(Long id, Long merchantId, Long applicationId, String merchantName, String materialType, String materialName, String fileUrl, String status, String rejectReason, LocalDateTime submittedAt, Long auditedBy, LocalDateTime auditedAt) {}
record AdminCertificationMaterialAuditRequest(@NotBlank String status, String rejectReason) {}
record AdminStoreView(Long storeId, Long merchantId, String merchantName, String storeName, String businessType, String summary, String address, String status, String businessHoursText, String tagText, String coverUrl, String contactPhone, String announcement, LocalDateTime updatedAt) {}
record AdminStatusRequest(@NotBlank String status) {}
record AdminMerchantUpsertRequest(Long accountId, @NotBlank String merchantName, String contactName, String contactPhone, String licenseNo, String status, String auditStatus) {}
record AdminStoreUpsertRequest(@NotNull Long merchantId, @NotBlank String storeName, @NotBlank String businessType, @NotBlank String summary, @NotBlank String address, String status, String businessHoursText, String tagText, String coverUrl, String contactPhone, String announcement) {}

record ModuleView(Long id, String code, String name, String businessType, String summary, String icon) {}
record ItemCardView(Long id, String title, String subtitle, String businessType, Long categoryId, String categoryName, BigDecimal price, BigDecimal originalPrice, String coverUrl, List<String> tags, String recommendReason, Integer stock, String saleStatus, Boolean soldOut, Long storeId, String storeName, String businessAttributes, String usageRules, String refundPolicy, String notice, Integer validityDays) {}
record StoreCardView(Long id, String name, String businessType, String distanceText, BigDecimal rating, Integer monthlySales, BigDecimal avgPrice, String status, String businessHoursText, String summary, String address, List<String> tags, String coverUrl, String recommendReason, String estimatedTimeText, BigDecimal longitude, BigDecimal latitude, List<ItemCardView> matchedItems) {}
record SearchResultView(PageResponse<StoreCardView> page) {}
record CategoryView(Long id, String name, Integer sortOrder) {}
record ItemGroupView(Long categoryId, String categoryName, List<ItemCardView> items) {}
record ReviewSummaryView(BigDecimal rating, long count, List<String> highlights) {}
record DeliveryRuleView(BigDecimal deliveryFee, Integer estimatedMinutes, BigDecimal startPrice, BigDecimal packageFeeFixed, BigDecimal packageFeePerItem, String packageFeeMode, BigDecimal distanceExtraThresholdKm, BigDecimal distanceExtraFee, BigDecimal distanceExtraStepKm, String deliveryText) {}
@JsonInclude(JsonInclude.Include.NON_NULL)
record StoreDetailView(StoreCardView store, String businessType, List<CategoryView> categories, List<ItemGroupView> itemGroups, ReviewSummaryView reviewSummary, DeliveryRuleView deliveryRule) {}
record HomeView(List<ModuleView> modules, PageResponse<ItemCardView> recommendations, long unreadMessageCount) {}
record ModulePageView(String moduleCode, String businessType, List<StoreCardView> stores, PageResponse<ItemCardView> featuredItems) {}
record ItemDetailView(ItemCardView item, StoreCardView store, List<CategoryView> categories, List<ItemGroupView> itemGroups) {}
record ReverseGeocodeQuery(@DecimalMin("-180") @DecimalMax("180") double longitude, @DecimalMin("-90") @DecimalMax("90") double latitude) {}

record TakeawaySettingRequest(@NotBlank String acceptMode) {}
record MerchantItemUpdateRequest(@NotBlank String title, String subtitle, @NotNull BigDecimal price, @NotNull @Min(0) Integer stock, @NotBlank String status) {}
record MerchantItemStatusRequest(@NotBlank String status) {}
record DeliveryRuleUpdateRequest(@NotNull BigDecimal deliveryFee, @NotNull BigDecimal startPrice, @Min(1) Integer estimatedMinutes, @NotNull BigDecimal maxDeliveryDistanceKm, String packageFeeMode, BigDecimal packageFeeFixed, BigDecimal packageFeePerItem, BigDecimal distanceExtraThresholdKm, BigDecimal distanceExtraFee, BigDecimal distanceExtraStepKm, String deliveryText) {}
record TakeawaySettingView(Long storeId, String storeName, String acceptMode) {}
record MerchantItemView(Long id, Long storeId, String title, String subtitle, String categoryName, BigDecimal price, BigDecimal originalPrice, Integer stock, String status, Integer salesCount) {}
record DeliveryRuleOpsView(Long storeId, BigDecimal deliveryFee, BigDecimal startPrice, Integer estimatedMinutes, BigDecimal maxDeliveryDistanceKm, String packageFeeMode, BigDecimal packageFeeFixed, BigDecimal packageFeePerItem, BigDecimal distanceExtraThresholdKm, BigDecimal distanceExtraFee, BigDecimal distanceExtraStepKm, String deliveryText) {}

record CheckoutQuoteRequest(@NotNull Long storeId, @NotEmpty List<@Valid CheckoutQuoteItemRequest> items) {}
record CheckoutQuoteItemRequest(@NotNull Long itemId, @Min(1) Integer quantity) {}
record CheckoutQuoteView(Long storeId, String storeName, String businessType, List<CheckoutQuoteItemView> items, BigDecimal amount) {}
record CheckoutQuoteItemView(Long itemId, Long skuId, String itemName, String subtitle, String businessType, Long categoryId, String categoryName, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice, String coverUrl, Integer stock, String status, String usageRules, String refundPolicy) {}
record InventoryDeductRequest(@NotNull Long orderId, @NotEmpty List<@Valid InventoryItemRequest> items) {}
record InventoryRestoreRequest(@NotNull Long orderId, @NotEmpty List<@Valid InventoryItemRequest> items) {}
record InventoryItemRequest(@NotNull Long skuId, @NotNull Long itemId, @Min(1) Integer quantity) {}
record InventoryResultView(String idempotencyKey, String status, List<InventoryLineResultView> lines) {}
record InventoryLineResultView(Long skuId, Long itemId, Integer quantity, String status) {}

record StoreSnapshotView(Long storeId, Long merchantId, Long accountId, String merchantName, String storeName, String businessType, String summary, String address, String status, String businessHoursText, String contactPhone, String coverUrl, BigDecimal longitude, BigDecimal latitude) {}
record MerchantAccountView(Long merchantId, Long accountId, String merchantName, String status, Long currentStoreId) {}
record CatalogItemSnapshotView(Long itemId, Long storeId, String storeName, String itemName, String subtitle, String businessType, Long categoryId, String categoryName, BigDecimal price, String coverUrl, String status, String usageRules, String refundPolicy) {}
record FulfillmentRulesView(Long storeId, String businessType, String acceptMode, BigDecimal deliveryFee, BigDecimal startPrice, Integer estimatedMinutes, BigDecimal maxDeliveryDistanceKm, String packageFeeMode, BigDecimal packageFeeFixed, BigDecimal packageFeePerItem, BigDecimal distanceExtraThresholdKm, BigDecimal distanceExtraFee, BigDecimal distanceExtraStepKm, String deliveryText) {}
record PlatformMerchantMetricsView(long merchantCount, long storeCount, long onSaleItemCount) {}
record InternalApiResponse<T>(int code, String message, T data) {}

record ServiceCommandResult(boolean success, Long accountId, String message) {}
record MerchantAccountProvisionRequest(String loginName, String merchantName, String contactName, String contactPhone) {}
record AuditLogRequest(String actorType, Long actorId, String actionType, String targetType, Long targetId, String detail) {}
record PreferenceSignalView(String businessType, Long categoryId, Long storeId, Long itemId, int weight, String source) {}
record HomeSummaryView(long unreadMessageCount) {}
record StoreOrderMetricsView(long orderCount, BigDecimal amount, long pendingCount) {}
record StoreEngagementMetricsView(BigDecimal rating, long reviewCount, long pendingReplyCount, long activeSessionCount) {}
record DailyCountView(LocalDate date, long count) {}
record MerchantDashboardView(long todayOrders, BigDecimal todayRevenue, long pendingReviews, long openSessions, double averageRating, List<DailyCountView> weeklyOrders) {}
