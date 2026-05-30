package com.aituan.discovery;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.BusinessType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.message.MessageRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class DiscoveryService {
  private final DiscoveryRepository discoveryRepository;
  private final MessageRepository messageRepository;

  DiscoveryService(DiscoveryRepository discoveryRepository, MessageRepository messageRepository) {
    this.discoveryRepository = discoveryRepository;
    this.messageRepository = messageRepository;
  }

  HomeView home() {
    List<ModuleView> modules = discoveryRepository.listModules().stream()
        .map(row -> new ModuleView(
            row.id(),
            row.code(),
            row.name(),
            row.businessType(),
            row.name() + "精选推荐",
            moduleIcon(row.businessType())))
        .toList();
    PageResponse<ItemCardView> recommendations = recommendations(1, 12);
    long unread = CurrentUserContext.optional()
        .map(currentUser -> messageRepository.countUnreadMessages(currentUser.userId()))
        .orElseGet(() -> discoveryRepository.countUnreadMessages(1L));
    return new HomeView(modules, recommendations, unread);
  }

  PageResponse<ItemCardView> recommendations(int page, int pageSize) {
    long total = discoveryRepository.countRecommendations();
    List<ItemCardView> list = discoveryRepository.listRecommendations((page - 1) * pageSize, pageSize).stream()
        .map(this::toItemCard)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  ModulePageView module(String moduleCode) {
    BusinessType businessType = businessTypeByModuleCode(moduleCode);
    List<DiscoveryRepository.StoreRow> storeRows = discoveryRepository.listStoresByBusinessType(businessType.code(), 10);
    List<StoreCardView> stores = storeRows.stream().map(this::toStoreCard).toList();
    List<ItemCardView> featuredItems = discoveryRepository.listItemsByBusinessType(businessType.code(), 12).stream()
        .map(this::toItemCard)
        .toList();
    return new ModulePageView(
        moduleCode,
        businessType.code(),
        stores,
        PageResponse.of(featuredItems, 1, 12, featuredItems.size()));
  }

  PageResponse<StoreCardView> search(String keyword, int page, int pageSize) {
    List<DiscoveryRepository.StoreRow> stores = discoveryRepository.searchStores(keyword, 50);
    List<StoreCardView> mapped = stores.stream().map(store -> {
      List<ItemCardView> matchedItems = discoveryRepository.searchItems(store.id(), keyword, 6).stream()
          .map(this::toItemCard)
          .toList();
      return toStoreCard(store, matchedItems);
    }).toList();
    int fromIndex = Math.max(0, (page - 1) * pageSize);
    int toIndex = Math.min(mapped.size(), fromIndex + pageSize);
    List<StoreCardView> pageItems = fromIndex >= mapped.size() ? List.of() : mapped.subList(fromIndex, toIndex);
    return PageResponse.of(pageItems, page, pageSize, mapped.size());
  }

  StoreDetailView storeDetail(long storeId) {
    DiscoveryRepository.StoreRow storeRow = discoveryRepository.findStore(storeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    List<DiscoveryRepository.CategoryRow> categoryRows = discoveryRepository.listStoreCategories(storeId);
    List<DiscoveryRepository.ItemRow> itemRows = discoveryRepository.listStoreItems(storeId);
    List<CategoryView> categories = categoryRows.stream()
        .map(row -> new CategoryView(row.id(), row.name(), row.sortOrder()))
        .toList();
    List<ItemGroupView> itemGroups = itemRows.stream()
        .collect(Collectors.groupingBy(DiscoveryRepository.ItemRow::categoryId, Collectors.toList()))
        .entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> new ItemGroupView(
            entry.getKey(),
            entry.getValue().get(0).categoryName(),
            entry.getValue().stream().map(this::toItemCard).toList()))
        .toList();
    DiscoveryRepository.ReviewSummaryRow reviewSummaryRow = discoveryRepository.reviewSummary(storeId);
    ReviewSummaryView reviewSummary = new ReviewSummaryView(
        reviewSummaryRow.rating(),
        reviewSummaryRow.count(),
        discoveryRepository.reviewHighlights(storeId));
    DiscoveryRepository.DeliveryRuleRow deliveryRuleRow = discoveryRepository.findDeliveryRule(storeId).orElse(null);
    DeliveryRuleView deliveryRule = deliveryRuleRow == null ? null : new DeliveryRuleView(
        deliveryRuleRow.deliveryFee(),
        deliveryRuleRow.estimatedMinutes(),
        deliveryRuleRow.startPrice(),
        deliveryRuleRow.deliveryText());
    return new StoreDetailView(toStoreCard(storeRow), storeRow.businessType(), categories, itemGroups, reviewSummary, deliveryRule);
  }

  ItemDetailView itemDetail(long itemId) {
    DiscoveryRepository.ItemRow itemRow = discoveryRepository.findItem(itemId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    DiscoveryRepository.StoreRow storeRow = discoveryRepository.findStore(itemRow.storeId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    StoreCardView store = toStoreCard(storeRow);
    List<CategoryView> categories = discoveryRepository.listStoreCategories(storeRow.id()).stream()
        .map(row -> new CategoryView(row.id(), row.name(), row.sortOrder()))
        .toList();
    List<ItemGroupView> itemGroups = discoveryRepository.listStoreItems(storeRow.id()).stream()
        .collect(Collectors.groupingBy(DiscoveryRepository.ItemRow::categoryId, Collectors.toList()))
        .entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> new ItemGroupView(
            entry.getKey(),
            entry.getValue().get(0).categoryName(),
            entry.getValue().stream().map(this::toItemCard).toList()))
        .toList();
    return new ItemDetailView(toItemCard(itemRow), store, categories, itemGroups);
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row) {
    return toStoreCard(row, List.of());
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row, List<ItemCardView> matchedItems) {
    return new StoreCardView(
        row.id(),
        row.name(),
        row.businessType(),
        row.distanceText(),
        row.rating(),
        row.monthlySales(),
        row.avgPrice(),
        row.status(),
        row.businessHoursText(),
        row.summary(),
        row.address(),
        splitTags(row.tagText()),
        row.coverUrl(),
        matchedItems);
  }

  private ItemCardView toItemCard(DiscoveryRepository.ItemRow row) {
    return new ItemCardView(
        row.id(),
        row.title(),
        row.subtitle(),
        row.businessType(),
        row.categoryId(),
        row.categoryName(),
        row.price(),
        row.originalPrice(),
        row.coverUrl(),
        splitTags(row.tagText()),
        row.stock(),
        row.skuStatus(),
        row.stock() <= 0 || !"on_sale".equals(row.skuStatus()),
        row.storeId(),
        row.storeName(),
        row.businessAttributes(),
        row.usageRules(),
        row.refundPolicy(),
        row.notice(),
        row.validityDays());
  }

  private List<String> splitTags(String tagText) {
    if (tagText == null || tagText.isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(tagText.split(","))
        .map(String::trim)
        .filter(tag -> !tag.isBlank())
        .toList();
  }

  private BusinessType businessTypeByModuleCode(String moduleCode) {
    return switch (moduleCode) {
      case "group" -> BusinessType.GROUP_BUY;
      case "fun" -> BusinessType.ENTERTAINMENT;
      default -> BusinessType.fromCode(moduleCode);
    };
  }

  private String moduleIcon(String businessType) {
    return switch (businessType) {
      case "takeaway" -> "外";
      case "group_buy" -> "团";
      case "hotel" -> "酒";
      case "entertainment" -> "娱";
      case "movie" -> "影";
      case "beauty" -> "丽";
      case "ticket" -> "票";
      case "massage" -> "足";
      default -> "爱";
    };
  }
}
