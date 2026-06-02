package com.aituan.discovery;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.BusinessType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.message.MessageRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
class DiscoveryService {
  private final DiscoveryRepository discoveryRepository;
  private final MessageRepository messageRepository;
  private final MapDistanceService mapDistanceService;

  DiscoveryService(
      DiscoveryRepository discoveryRepository,
      MessageRepository messageRepository,
      MapDistanceService mapDistanceService) {
    this.discoveryRepository = discoveryRepository;
    this.messageRepository = messageRepository;
    this.mapDistanceService = mapDistanceService;
  }

  HomeView home(Double latitude, Double longitude) {
    List<ModuleView> modules = discoveryRepository.listModules().stream()
        .map(row -> new ModuleView(
            row.id(),
            row.code(),
            row.name(),
            row.businessType(),
            row.name() + "精选推荐",
            moduleIcon(row.businessType())))
        .toList();
    PageResponse<ItemCardView> recommendations = recommendations(1, 12, latitude, longitude);
    long unread = CurrentUserContext.optional()
        .map(currentUser -> messageRepository.countUnreadMessages(currentUser.userId()))
        .orElseGet(() -> discoveryRepository.countUnreadMessages(1L));
    return new HomeView(modules, recommendations, unread);
  }

  PageResponse<ItemCardView> recommendations(int page, int pageSize) {
    return recommendations(page, pageSize, null, null);
  }

  PageResponse<ItemCardView> recommendations(int page, int pageSize, Double latitude, Double longitude) {
    int safePageSize = Math.min(Math.max(pageSize, 1), 50);
    long total = discoveryRepository.countRecommendations();
    LocationContext location = locationContext(latitude, longitude);
    List<DiscoveryRepository.ItemRow> sorted = sortItems(
        discoveryRepository.listRecommendations(0, (int) Math.min(total, Integer.MAX_VALUE)),
        location);
    List<DiscoveryRepository.ItemRow> spaced = spaceItemsByStore(sorted, 4);
    int fromIndex = Math.max(0, (page - 1) * safePageSize);
    int toIndex = Math.min(spaced.size(), fromIndex + safePageSize);
    List<ItemCardView> pageItems = fromIndex >= spaced.size()
        ? List.of()
        : spaced.subList(fromIndex, toIndex).stream().map(this::toItemCard).toList();
    return PageResponse.of(pageItems, page, safePageSize, total);
  }

  ModulePageView module(String moduleCode, Double latitude, Double longitude) {
    BusinessType businessType = businessTypeByModuleCode(moduleCode);
    LocationContext location = locationContext(latitude, longitude);
    List<DiscoveryRepository.StoreRow> storeRows = sortStores(discoveryRepository.listStoresByBusinessType(businessType.code(), 50), location);
    List<StoreCardView> stores = storeRows.stream().limit(10).map(row -> toStoreCard(row, location, true)).toList();
    List<ItemCardView> featuredItems = discoveryRepository.listItemsByBusinessType(businessType.code(), 12).stream()
        .map(this::toItemCard)
        .toList();
    return new ModulePageView(
        moduleCode,
        businessType.code(),
        stores,
        PageResponse.of(featuredItems, 1, 12, featuredItems.size()));
  }

  PageResponse<StoreCardView> search(String keyword, int page, int pageSize, Double latitude, Double longitude) {
    LocationContext location = locationContext(latitude, longitude);
    List<DiscoveryRepository.StoreRow> stores = sortStores(discoveryRepository.searchStores(keyword, 50), location);
    List<StoreCardView> mapped = stores.stream().map(store -> {
      List<DiscoveryRepository.ItemRow> matchedRows = new java.util.ArrayList<>(discoveryRepository.searchItems(store.id(), keyword, 6));
      if (matchedRows.size() < 6) {
        List<Long> excluded = matchedRows.stream().map(DiscoveryRepository.ItemRow::id).toList();
        List<Long> preferredCategories = matchedRows.stream().map(DiscoveryRepository.ItemRow::categoryId).distinct().toList();
        matchedRows.addAll(discoveryRepository.listStoreItemsForFill(store.id(), excluded, preferredCategories, 6 - matchedRows.size()));
      }
      List<ItemCardView> matchedItems = matchedRows.stream().limit(6).map(this::toItemCard).toList();
      return toStoreCard(store, matchedItems, location, true);
    }).toList();
    int fromIndex = Math.max(0, (page - 1) * pageSize);
    int toIndex = Math.min(mapped.size(), fromIndex + pageSize);
    List<StoreCardView> pageItems = fromIndex >= mapped.size() ? List.of() : mapped.subList(fromIndex, toIndex);
    return PageResponse.of(pageItems, page, pageSize, mapped.size());
  }

  StoreDetailView storeDetail(long storeId, Double latitude, Double longitude) {
    LocationContext location = locationContext(latitude, longitude);
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
        deliveryRuleRow.packageFeeFixed(),
        deliveryRuleRow.packageFeePerItem(),
        deliveryRuleRow.packageFeeMode(),
        deliveryRuleRow.distanceExtraThresholdKm(),
        deliveryRuleRow.distanceExtraFee(),
        deliveryRuleRow.distanceExtraStepKm(),
        deliveryRuleRow.deliveryText());
    return new StoreDetailView(toStoreCard(storeRow, location), storeRow.businessType(), categories, itemGroups, reviewSummary, deliveryRule);
  }

  ItemDetailView itemDetail(long itemId, Double latitude, Double longitude) {
    LocationContext location = locationContext(latitude, longitude);
    DiscoveryRepository.ItemRow itemRow = discoveryRepository.findItem(itemId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    DiscoveryRepository.StoreRow storeRow = discoveryRepository.findStore(itemRow.storeId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    StoreCardView store = toStoreCard(storeRow, location);
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
    return toStoreCard(row, List.of(), null, false);
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row, LocationContext location) {
    return toStoreCard(row, List.of(), location, false);
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row, List<ItemCardView> matchedItems, LocationContext location) {
    return toStoreCard(row, matchedItems, location, false);
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row, LocationContext location, boolean localOnly) {
    return toStoreCard(row, List.of(), location, localOnly);
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row, List<ItemCardView> matchedItems, LocationContext location, boolean localOnly) {
    MapDistanceService.DistanceEstimate estimate = estimate(row, location, localOnly);
    return new StoreCardView(
        row.id(),
        row.name(),
        row.businessType(),
        estimate == null ? row.distanceText() : estimate.distanceText(),
        row.rating(),
        row.monthlySales(),
        row.avgPrice(),
        row.status(),
        row.businessHoursText(),
        row.summary(),
        row.address(),
        splitTags(row.tagText()),
        row.coverUrl(),
        estimate == null ? "" : estimate.estimatedTimeText(),
        row.longitude(),
        row.latitude(),
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

  private MapDistanceService.DistanceEstimate estimate(DiscoveryRepository.StoreRow row, LocationContext location, boolean localOnly) {
    if (location == null || row.latitude() == null || row.longitude() == null) {
      return null;
    }
    if (localOnly) {
      return mapDistanceService.estimateLocally(
          location.latitude(),
          location.longitude(),
          row.latitude().doubleValue(),
          row.longitude().doubleValue());
    }
    return mapDistanceService.estimate(
        location.latitude(),
        location.longitude(),
        row.latitude().doubleValue(),
        row.longitude().doubleValue());
  }

  private double distanceForSort(DiscoveryRepository.StoreRow row, LocationContext location) {
    if (location == null || row.latitude() == null || row.longitude() == null) {
      return Double.MAX_VALUE;
    }
    return mapDistanceService.distanceKm(
        location.latitude(),
        location.longitude(),
        row.latitude().doubleValue(),
        row.longitude().doubleValue());
  }

  private List<DiscoveryRepository.StoreRow> sortStores(List<DiscoveryRepository.StoreRow> stores, LocationContext location) {
    if (location == null) {
      return stores;
    }
    return stores.stream()
        .sorted(Comparator
            .comparingDouble((DiscoveryRepository.StoreRow row) -> distanceForSort(row, location))
            .thenComparing(DiscoveryRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(DiscoveryRepository.StoreRow::monthlySales, Comparator.reverseOrder())
            .thenComparing(DiscoveryRepository.StoreRow::id))
        .toList();
  }

  private double itemDistanceForSort(DiscoveryRepository.ItemRow row, LocationContext location) {
    if (location == null || row.storeLatitude() == null || row.storeLongitude() == null) {
      return Double.MAX_VALUE;
    }
    return mapDistanceService.distanceKm(
        location.latitude(),
        location.longitude(),
        row.storeLatitude().doubleValue(),
        row.storeLongitude().doubleValue());
  }

  private List<DiscoveryRepository.ItemRow> sortItems(List<DiscoveryRepository.ItemRow> items, LocationContext location) {
    Comparator<DiscoveryRepository.ItemRow> comparator = Comparator
        .comparing(DiscoveryRepository.ItemRow::salesCount, Comparator.reverseOrder())
        .thenComparing(DiscoveryRepository.ItemRow::storeMonthlySales, Comparator.reverseOrder())
        .thenComparing(DiscoveryRepository.ItemRow::storeRating, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(DiscoveryRepository.ItemRow::sortOrder)
        .thenComparing(DiscoveryRepository.ItemRow::id);
    if (location != null) {
      comparator = Comparator
          .comparingDouble((DiscoveryRepository.ItemRow row) -> itemDistanceForSort(row, location))
          .thenComparing(comparator);
    }
    return items.stream().sorted(comparator).toList();
  }

  private List<DiscoveryRepository.ItemRow> spaceItemsByStore(List<DiscoveryRepository.ItemRow> items, int windowSize) {
    List<DiscoveryRepository.ItemRow> remaining = new ArrayList<>(items);
    List<DiscoveryRepository.ItemRow> result = new ArrayList<>(items.size());
    List<Long> recentStores = new ArrayList<>();
    while (!remaining.isEmpty()) {
      int selectedIndex = -1;
      for (int i = 0; i < remaining.size(); i++) {
        if (!recentStores.contains(remaining.get(i).storeId())) {
          selectedIndex = i;
          break;
        }
      }
      if (selectedIndex < 0) {
        selectedIndex = 0;
      }
      DiscoveryRepository.ItemRow selected = remaining.remove(selectedIndex);
      result.add(selected);
      recentStores.add(selected.storeId());
      if (recentStores.size() > windowSize) {
        recentStores.remove(0);
      }
    }
    return result;
  }

  private LocationContext locationContext(Double latitude, Double longitude) {
    if (latitude == null || longitude == null) {
      return null;
    }
    return new LocationContext(latitude, longitude);
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

  private record LocationContext(double latitude, double longitude) {}
}
