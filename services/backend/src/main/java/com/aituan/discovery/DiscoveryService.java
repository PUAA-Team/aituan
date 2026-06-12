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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        .filter(currentUser -> currentUser.isUser() && currentUser.userId() != null)
        .map(currentUser -> messageRepository.countUnreadMessages(currentUser.userId()))
        .orElseGet(() -> discoveryRepository.countUnreadMessages(1L));
    return new HomeView(modules, recommendations, unread);
  }

  PageResponse<ItemCardView> recommendations(int page, int pageSize) {
    return recommendations(page, pageSize, "personalized", null, null);
  }

  PageResponse<ItemCardView> recommendations(int page, int pageSize, Double latitude, Double longitude) {
    return recommendations(page, pageSize, "personalized", latitude, longitude);
  }

  PageResponse<ItemCardView> recommendations(int page, int pageSize, String sort, Double latitude, Double longitude) {
    int safePageSize = Math.min(Math.max(pageSize, 1), 50);
    long total = discoveryRepository.countRecommendations();
    LocationContext location = locationContext(latitude, longitude);
    UserPreference preference = userPreference();
    RecommendationSort recommendationSort = RecommendationSort.from(sort);
    List<DiscoveryRepository.ItemRow> sorted = sortItems(
        discoveryRepository.listRecommendations(0, (int) Math.min(total, Integer.MAX_VALUE)),
        location,
        preference,
        recommendationSort);
    List<DiscoveryRepository.ItemRow> spaced = spaceItemsByStore(sorted, 4);
    int fromIndex = Math.max(0, (page - 1) * safePageSize);
    int toIndex = Math.min(spaced.size(), fromIndex + safePageSize);
    List<ItemCardView> pageItems = fromIndex >= spaced.size()
        ? List.of()
        : spaced.subList(fromIndex, toIndex).stream()
            .map(row -> toItemCard(row, itemReason(row, preference, location, recommendationSort)))
            .toList();
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
    return search(keyword, page, pageSize, "default", null, latitude, longitude);
  }

  PageResponse<StoreCardView> search(String keyword, int page, int pageSize, String sort, String businessType, Double latitude, Double longitude) {
    int safePageSize = Math.min(Math.max(pageSize, 1), 50);
    LocationContext location = locationContext(latitude, longitude);
    SearchSort searchSort = SearchSort.from(sort);
    String normalizedBusinessType = normalizeBusinessType(businessType);
    List<String> inferredBusinessTypes = normalizedBusinessType == null ? inferredBusinessTypes(keyword) : List.of();
    String effectiveKeyword = inferredBusinessTypes.isEmpty() ? keyword : "";
    List<DiscoveryRepository.StoreRow> stores = sortStores(
        inferredBusinessTypes.isEmpty()
            ? discoveryRepository.searchStores(keyword, normalizedBusinessType, 100)
            : discoveryRepository.searchStoresByBusinessTypes(inferredBusinessTypes, 100),
        location,
        searchSort,
        effectiveKeyword);
    List<StoreCardView> mapped = stores.stream().map(store -> {
      List<DiscoveryRepository.ItemRow> matchedRows = new java.util.ArrayList<>(discoveryRepository.searchItems(store.id(), effectiveKeyword, 6));
      if (matchedRows.size() < 6) {
        List<Long> excluded = matchedRows.stream().map(DiscoveryRepository.ItemRow::id).toList();
        List<Long> preferredCategories = matchedRows.stream().map(DiscoveryRepository.ItemRow::categoryId).distinct().toList();
        matchedRows.addAll(discoveryRepository.listStoreItemsForFill(store.id(), excluded, preferredCategories, 6 - matchedRows.size()));
      }
      List<ItemCardView> matchedItems = matchedRows.stream().limit(6)
          .map(row -> toItemCard(row, itemSearchReason(row, effectiveKeyword)))
          .toList();
      return toStoreCard(store, matchedItems, location, true, storeReason(store, location, searchSort, effectiveKeyword));
    }).toList();
    int fromIndex = Math.max(0, (page - 1) * safePageSize);
    int toIndex = Math.min(mapped.size(), fromIndex + safePageSize);
    List<StoreCardView> pageItems = fromIndex >= mapped.size() ? List.of() : mapped.subList(fromIndex, toIndex);
    return PageResponse.of(pageItems, page, safePageSize, mapped.size());
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
    return toStoreCard(row, List.of(), null, false, "");
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row, LocationContext location) {
    return toStoreCard(row, List.of(), location, false, "");
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row, List<ItemCardView> matchedItems, LocationContext location) {
    return toStoreCard(row, matchedItems, location, false, "");
  }

  private StoreCardView toStoreCard(DiscoveryRepository.StoreRow row, LocationContext location, boolean localOnly) {
    return toStoreCard(row, List.of(), location, localOnly, storeReason(row, location, SearchSort.DEFAULT, ""));
  }

  private StoreCardView toStoreCard(
      DiscoveryRepository.StoreRow row,
      List<ItemCardView> matchedItems,
      LocationContext location,
      boolean localOnly,
      String recommendReason) {
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
        recommendReason,
        estimate == null ? "" : estimate.estimatedTimeText(),
        row.longitude(),
        row.latitude(),
        matchedItems);
  }

  private ItemCardView toItemCard(DiscoveryRepository.ItemRow row) {
    return toItemCard(row, "");
  }

  private ItemCardView toItemCard(DiscoveryRepository.ItemRow row, String recommendReason) {
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
        recommendReason,
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
    return sortStores(stores, location, SearchSort.DEFAULT, "");
  }

  private List<DiscoveryRepository.StoreRow> sortStores(
      List<DiscoveryRepository.StoreRow> stores,
      LocationContext location,
      SearchSort sort,
      String keyword) {
    Comparator<DiscoveryRepository.StoreRow> base = Comparator
        .comparingInt((DiscoveryRepository.StoreRow row) -> keywordScore(row, keyword))
        .thenComparing(DiscoveryRepository.StoreRow::monthlySales, Comparator.reverseOrder())
        .thenComparing(DiscoveryRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(DiscoveryRepository.StoreRow::id);
    Comparator<DiscoveryRepository.StoreRow> comparator = switch (sort) {
      case DISTANCE -> location == null
          ? base
          : Comparator.comparingDouble((DiscoveryRepository.StoreRow row) -> distanceForSort(row, location))
              .thenComparing(base);
      case RATING -> Comparator
          .comparing(DiscoveryRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder()))
          .thenComparing(DiscoveryRepository.StoreRow::monthlySales, Comparator.reverseOrder())
          .thenComparing(DiscoveryRepository.StoreRow::id);
      case SALES -> Comparator
          .comparing(DiscoveryRepository.StoreRow::monthlySales, Comparator.reverseOrder())
          .thenComparing(DiscoveryRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder()))
          .thenComparing(DiscoveryRepository.StoreRow::id);
      case PRICE_ASC -> Comparator
          .comparing(DiscoveryRepository.StoreRow::avgPrice, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(DiscoveryRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder()))
          .thenComparing(DiscoveryRepository.StoreRow::id);
      case DEFAULT -> location == null
          ? base
          : base.thenComparingDouble(row -> distanceForSort(row, location));
    };
    return stores.stream().sorted(comparator).toList();
  }

  private int keywordScore(DiscoveryRepository.StoreRow row, String keyword) {
    String normalized = normalizeText(keyword);
    if (normalized.isEmpty()) {
      return 4;
    }
    if (normalizeText(row.name()).contains(normalized)) {
      return 0;
    }
    if (normalizeText(row.tagText()).contains(normalized)) {
      return 1;
    }
    if (normalizeText(row.summary()).contains(normalized)) {
      return 2;
    }
    if (normalizeText(row.address()).contains(normalized)) {
      return 3;
    }
    return 4;
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

  private List<DiscoveryRepository.ItemRow> sortItems(
      List<DiscoveryRepository.ItemRow> items,
      LocationContext location,
      UserPreference preference,
      RecommendationSort sort) {
    Comparator<DiscoveryRepository.ItemRow> hot = Comparator
        .comparing(DiscoveryRepository.ItemRow::salesCount, Comparator.reverseOrder())
        .thenComparing(DiscoveryRepository.ItemRow::storeMonthlySales, Comparator.reverseOrder())
        .thenComparing(DiscoveryRepository.ItemRow::storeRating, Comparator.nullsLast(Comparator.reverseOrder()))
        .thenComparing(DiscoveryRepository.ItemRow::sortOrder)
        .thenComparing(DiscoveryRepository.ItemRow::id);
    Comparator<DiscoveryRepository.ItemRow> comparator = switch (sort) {
      case PERSONALIZED -> Comparator
          .comparingInt((DiscoveryRepository.ItemRow row) -> preference.score(row)).reversed()
          .thenComparing(location == null
              ? hot
              : Comparator.comparingDouble((DiscoveryRepository.ItemRow row) -> itemDistanceForSort(row, location)).thenComparing(hot));
      case DISTANCE -> location == null
          ? hot
          : Comparator.comparingDouble((DiscoveryRepository.ItemRow row) -> itemDistanceForSort(row, location)).thenComparing(hot);
      case RATING -> Comparator
          .comparing(DiscoveryRepository.ItemRow::storeRating, Comparator.nullsLast(Comparator.reverseOrder()))
          .thenComparing(DiscoveryRepository.ItemRow::storeMonthlySales, Comparator.reverseOrder())
          .thenComparing(DiscoveryRepository.ItemRow::salesCount, Comparator.reverseOrder())
          .thenComparing(DiscoveryRepository.ItemRow::id);
      case SALES -> hot;
    };
    return items.stream().sorted(comparator).toList();
  }

  private UserPreference userPreference() {
    return CurrentUserContext.optional()
        .filter(currentUser -> currentUser.isUser() && currentUser.userId() != null)
        .map(currentUser -> UserPreference.from(discoveryRepository.userPreferenceSignals(currentUser.userId())))
        .orElseGet(UserPreference::empty);
  }

  private String itemReason(
      DiscoveryRepository.ItemRow row,
      UserPreference preference,
      LocationContext location,
      RecommendationSort sort) {
    if (sort == RecommendationSort.PERSONALIZED) {
      String reason = preference.reason(row);
      if (!reason.isBlank()) {
        return reason;
      }
    }
    return switch (sort) {
      case DISTANCE -> location == null ? fallbackItemReason(row) : "离你更近";
      case RATING -> ratingReason(row.storeRating());
      case SALES -> salesReason(row.salesCount());
      case PERSONALIZED -> fallbackItemReason(row);
    };
  }

  private String itemSearchReason(DiscoveryRepository.ItemRow row, String keyword) {
    String normalized = normalizeText(keyword);
    if (!normalized.isEmpty()) {
      if (normalizeText(row.title()).contains(normalized)) {
        return "商品名匹配";
      }
      if (normalizeText(row.tagText()).contains(normalized)) {
        return "标签匹配";
      }
      if (normalizeText(row.subtitle()).contains(normalized)) {
        return "描述匹配";
      }
    }
    return salesReason(row.salesCount());
  }

  private String storeReason(DiscoveryRepository.StoreRow row, LocationContext location, SearchSort sort, String keyword) {
    return switch (sort) {
      case DISTANCE -> location == null ? fallbackStoreReason(row) : "距离最近优先";
      case RATING -> ratingReason(row.rating());
      case SALES -> "月售 " + row.monthlySales();
      case PRICE_ASC -> row.avgPrice() == null ? fallbackStoreReason(row) : "人均 ￥" + row.avgPrice().stripTrailingZeros().toPlainString();
      case DEFAULT -> defaultSearchReason(row, location, keyword);
    };
  }

  private String defaultSearchReason(DiscoveryRepository.StoreRow row, LocationContext location, String keyword) {
    int score = keywordScore(row, keyword);
    if (score == 0) {
      return "店名匹配";
    }
    if (score == 1) {
      return "标签匹配";
    }
    if (score == 2) {
      return "描述匹配";
    }
    if (location != null) {
      return "附近热门";
    }
    return fallbackStoreReason(row);
  }

  private String fallbackItemReason(DiscoveryRepository.ItemRow row) {
    if (row.salesCount() > 0) {
      return salesReason(row.salesCount());
    }
    if (row.storeRating() != null && row.storeRating().doubleValue() >= 4.5) {
      return ratingReason(row.storeRating());
    }
    return businessLabel(row.businessType()) + "精选";
  }

  private String fallbackStoreReason(DiscoveryRepository.StoreRow row) {
    if (row.monthlySales() > 0) {
      return "月售 " + row.monthlySales();
    }
    if (row.rating() != null && row.rating().doubleValue() >= 4.5) {
      return ratingReason(row.rating());
    }
    return businessLabel(row.businessType()) + "精选";
  }

  private String salesReason(int salesCount) {
    return salesCount > 0 ? "热卖 " + salesCount + " 单" : "热门推荐";
  }

  private String ratingReason(BigDecimal rating) {
    return rating == null ? "高分好店" : "评分 " + rating.stripTrailingZeros().toPlainString();
  }

  private String normalizeBusinessType(String businessType) {
    String normalized = businessType == null ? "" : businessType.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    try {
      return BusinessType.fromCode(normalized).code();
    } catch (IllegalArgumentException ex) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "业务类型不正确");
    }
  }

  private List<String> inferredBusinessTypes(String keyword) {
    String normalized = normalizeText(keyword);
    if (normalized.isEmpty()) return List.of();
    if (containsAny(normalized, "休闲玩乐", "休闲娱乐", "附近玩", "周边玩", "玩乐", "娱乐")) {
      return List.of("movie", "ticket", "massage", "entertainment");
    }
    if (containsAny(normalized, "电影", "影院", "剧场", "演出")) return List.of("movie");
    if (containsAny(normalized, "景点", "门票", "观景", "游玩")) return List.of("ticket");
    if (containsAny(normalized, "按摩", "足疗", "足道", "洗脚", "肩颈")) return List.of("massage");
    if (containsAny(normalized, "密室", "电玩城")) return List.of("entertainment");
    if (containsAny(normalized, "spa", "丽人", "美容", "医美", "护理")) return List.of("beauty");
    return List.of();
  }

  private boolean containsAny(String text, String... words) {
    for (String word : words) {
      if (text.contains(word)) return true;
    }
    return false;
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String businessLabel(String businessType) {
    try {
      return BusinessType.fromCode(businessType).label();
    } catch (IllegalArgumentException ex) {
      return "爱团";
    }
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

  private enum RecommendationSort {
    PERSONALIZED,
    SALES,
    RATING,
    DISTANCE;

    static RecommendationSort from(String value) {
      if (value == null || value.isBlank()) {
        return PERSONALIZED;
      }
      return switch (value.trim().toLowerCase(Locale.ROOT)) {
        case "sales" -> SALES;
        case "rating" -> RATING;
        case "distance" -> DISTANCE;
        default -> PERSONALIZED;
      };
    }
  }

  private enum SearchSort {
    DEFAULT,
    DISTANCE,
    RATING,
    SALES,
    PRICE_ASC;

    static SearchSort from(String value) {
      if (value == null || value.isBlank()) {
        return DEFAULT;
      }
      return switch (value.trim().toLowerCase(Locale.ROOT)) {
        case "distance" -> DISTANCE;
        case "rating" -> RATING;
        case "sales" -> SALES;
        case "price_asc" -> PRICE_ASC;
        default -> DEFAULT;
      };
    }
  }

  private record UserPreference(
      Map<String, Integer> businessTypeWeights,
      Map<Long, Integer> categoryWeights,
      Map<Long, Integer> storeWeights,
      Map<Long, Integer> itemWeights) {

    static UserPreference empty() {
      return new UserPreference(Map.of(), Map.of(), Map.of(), Map.of());
    }

    static UserPreference from(List<DiscoveryRepository.PreferenceSignalRow> signals) {
      Map<String, Integer> businessTypeWeights = new HashMap<>();
      Map<Long, Integer> categoryWeights = new HashMap<>();
      Map<Long, Integer> storeWeights = new HashMap<>();
      Map<Long, Integer> itemWeights = new HashMap<>();
      for (DiscoveryRepository.PreferenceSignalRow signal : signals) {
        add(businessTypeWeights, signal.businessType(), signal.weight());
        add(categoryWeights, signal.categoryId(), signal.weight());
        add(storeWeights, signal.storeId(), signal.weight());
        add(itemWeights, signal.itemId(), signal.weight());
      }
      return new UserPreference(businessTypeWeights, categoryWeights, storeWeights, itemWeights);
    }

    int score(DiscoveryRepository.ItemRow row) {
      return businessTypeWeights.getOrDefault(row.businessType(), 0)
          + categoryWeights.getOrDefault(row.categoryId(), 0)
          + storeWeights.getOrDefault(row.storeId(), 0)
          + itemWeights.getOrDefault(row.id(), 0);
    }

    String reason(DiscoveryRepository.ItemRow row) {
      if (itemWeights.containsKey(row.id())) {
        return "收藏后常看";
      }
      if (storeWeights.containsKey(row.storeId())) {
        return "常逛这家店";
      }
      if (categoryWeights.containsKey(row.categoryId())) {
        return "常买同类";
      }
      if (businessTypeWeights.containsKey(row.businessType())) {
        return "偏好" + label(row.businessType());
      }
      return "";
    }

    private static <T> void add(Map<T, Integer> map, T key, int weight) {
      if (key != null) {
        map.merge(key, weight, Integer::sum);
      }
    }

    private static String label(String businessType) {
      try {
        return BusinessType.fromCode(businessType).label();
      } catch (IllegalArgumentException ex) {
        return "同类内容";
      }
    }
  }

  private record LocationContext(double latitude, double longitude) {}
}
