package com.aituan.merchantcatalog;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.enums.BusinessType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class MerchantCatalogService {
  private static final BigDecimal DEFAULT_MAX_DELIVERY_DISTANCE_KM = BigDecimal.valueOf(5).setScale(2);
  private static final String ACCEPT_MODE_AUTO = "auto";
  private static final String ACCEPT_MODE_MANUAL = "manual";

  private final MerchantCatalogRepository repository;
  private final FileStorageService fileStorageService;
  private final MapDistanceService mapDistanceService;
  private final InternalServiceClient internalClient;
  private final Map<String, InventoryResultView> inventoryIdempotency = new ConcurrentHashMap<>();

  MerchantCatalogService(
      MerchantCatalogRepository repository,
      FileStorageService fileStorageService,
      MapDistanceService mapDistanceService,
      InternalServiceClient internalClient) {
    this.repository = repository;
    this.fileStorageService = fileStorageService;
    this.mapDistanceService = mapDistanceService;
    this.internalClient = internalClient;
  }

  @Transactional
  MerchantApplicationView submitApplication(MerchantApplicationSubmitRequest request) {
    String businessType = normalizeBusinessType(request.businessType());
    long id = repository.insertApplication("APP" + System.currentTimeMillis(), request, businessType);
    return repository.findApplication(id).map(this::toApplicationView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  MerchantProfileView profile() {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    List<MerchantStoreView> stores = repository.listMerchantStores(merchant.id()).stream().map(this::toStoreView).toList();
    Long currentStoreId = stores.isEmpty() ? null : stores.get(0).id();
    return new MerchantProfileView(merchant.id(), merchant.accountId(), merchant.merchantName(), merchant.contactName(), merchant.contactPhone(), merchant.licenseNo(), merchant.status(), merchant.auditStatus(), currentStoreId, stores);
  }

  @Transactional
  MerchantProfileView updateProfile(MerchantProfileUpdateRequest request) {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    repository.updateMerchant(merchant.id(), new AdminMerchantUpsertRequest(merchant.accountId(), request.merchantName(), request.contactName(), request.contactPhone(), merchant.licenseNo(), merchant.status(), merchant.auditStatus()), merchant.status(), merchant.auditStatus());
    return profile();
  }

  MerchantStoreView currentStore() {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    return repository.listMerchantStores(merchant.id()).stream().findFirst().map(this::toStoreView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  MerchantStoreView updateCurrentStore(MerchantStoreUpdateRequest request) {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    MerchantCatalogRepository.StoreRow store = repository.listMerchantStores(merchant.id()).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    StoreLocation location = resolveLocation(request, store);
    repository.updateMerchantStore(store.id(), request, location.longitude(), location.latitude());
    return repository.findStore(merchant.id(), store.id()).map(this::toStoreView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  MerchantStoreView uploadCurrentStoreCover(MultipartFile file) {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    MerchantCatalogRepository.StoreRow store = repository.listMerchantStores(merchant.id()).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    FileStorageService.FileAssetView asset = fileStorageService.save(file, "store");
    repository.updateStoreCover(store.id(), asset.publicUrl());
    return repository.findStore(merchant.id(), store.id()).map(this::toStoreView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  MerchantCertificationView certification() {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    return new MerchantCertificationView(merchant.auditStatus(), merchant.licenseNo(), certificationMaterials());
  }

  MerchantDashboardView merchantDashboard() {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    List<MerchantCatalogRepository.StoreRow> stores = repository.listMerchantStores(merchant.id());
    long todayOrders = 0;
    BigDecimal todayRevenue = BigDecimal.ZERO;
    long pendingReviews = 0;
    long openSessions = 0;
    BigDecimal ratingTotal = BigDecimal.ZERO;
    long ratingStores = 0;
    for (MerchantCatalogRepository.StoreRow store : stores) {
      StoreOrderMetricsView orderMetrics = internalClient.orderMetrics(store.id());
      StoreEngagementMetricsView engagementMetrics = internalClient.engagementMetrics(store.id());
      todayOrders += orderMetrics.orderCount();
      todayRevenue = todayRevenue.add(orderMetrics.amount());
      pendingReviews += engagementMetrics.pendingReplyCount();
      openSessions += engagementMetrics.activeSessionCount();
      if (engagementMetrics.rating() != null && engagementMetrics.rating().compareTo(BigDecimal.ZERO) > 0) {
        ratingTotal = ratingTotal.add(engagementMetrics.rating());
        ratingStores++;
      }
    }
    double averageRating = ratingStores == 0 ? 0.0 : round1(ratingTotal.divide(BigDecimal.valueOf(ratingStores), 2, java.math.RoundingMode.HALF_UP).doubleValue());
    return new MerchantDashboardView(todayOrders, todayRevenue, pendingReviews, openSessions, averageRating, weeklyOrders(todayOrders));
  }

  List<CertificationMaterialView> certificationMaterials() {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    return repository.listMaterialsByMerchant(merchant.id()).stream().map(this::toMaterialView).toList();
  }

  @Transactional
  CertificationMaterialView uploadCertificationMaterial(String materialType, String materialName, MultipartFile file) {
    MerchantCatalogRepository.MerchantRow merchant = currentMerchant();
    String normalizedType = normalizeMaterialType(materialType);
    FileStorageService.FileAssetView asset = fileStorageService.save(file, "merchant-certification");
    long id = repository.insertMaterial(merchant.id(), normalizedType, cleanOrDefault(materialName, materialTypeLabel(normalizedType)), asset.publicUrl());
    return repository.findMaterial(id).map(this::toMaterialView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  List<CatalogItemView> merchantItems(String businessType, String status, String keyword) {
    CurrentUser current = requireMerchant();
    return repository.listMerchantItems(current.accountId(), normalizeBusinessTypeOrNull(businessType), normalizeStatusOrNull(status), keyword).stream().map(this::toItemView).toList();
  }

  PageResponse<CatalogItemView> adminItems(Long storeId, String businessType, String status, String keyword, int page, int pageSize) {
    requireAdmin();
    String normalizedBusinessType = normalizeBusinessTypeOrNull(businessType);
    String normalizedStatus = normalizeStatusOrNull(status);
    long total = repository.countAdminItems(storeId, normalizedBusinessType, normalizedStatus, keyword);
    List<CatalogItemView> list = repository.listAdminItems(storeId, normalizedBusinessType, normalizedStatus, keyword, (page - 1) * pageSize, pageSize).stream().map(this::toItemView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  CatalogItemView item(long itemId) {
    MerchantCatalogRepository.CatalogItemRow item = requireCatalogItem(itemId);
    ensureItemAccess(item);
    return toItemView(item);
  }

  @Transactional
  CatalogItemView createMerchantItem(CatalogItemUpsertRequest request) {
    CurrentUser current = requireMerchant();
    long storeId = resolveMerchantStore(current.accountId(), request.storeId());
    return createItem(storeId, request);
  }

  @Transactional
  CatalogItemView createAdminItem(CatalogItemUpsertRequest request) {
    requireAdmin();
    if (request.storeId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "后台新增商品必须指定门店");
    return createItem(request.storeId(), request);
  }

  @Transactional
  CatalogItemView updateItem(long itemId, CatalogItemUpsertRequest request) {
    MerchantCatalogRepository.CatalogItemRow current = requireCatalogItem(itemId);
    ensureItemAccess(current);
    long storeId = resolveStoreForUpdate(current, request.storeId());
    long categoryId = resolveCategory(storeId, request);
    validatePrice(request.price());
    repository.updateItem(itemId, request, categoryId, normalizeItemStatus(request.status()));
    return toItemView(requireCatalogItem(itemId));
  }

  @Transactional
  CatalogItemView updateItemStatus(long itemId, CatalogItemStatusRequest request) {
    MerchantCatalogRepository.CatalogItemRow item = requireCatalogItem(itemId);
    ensureItemAccess(item);
    repository.updateItemStatus(itemId, normalizeItemStatus(request.status()));
    return toItemView(requireCatalogItem(itemId));
  }

  @Transactional
  CatalogItemView uploadItemCover(long itemId, MultipartFile file) {
    MerchantCatalogRepository.CatalogItemRow item = requireCatalogItem(itemId);
    ensureItemAccess(item);
    FileStorageService.FileAssetView asset = fileStorageService.save(file, "item");
    repository.updateItemCover(itemId, asset.publicUrl());
    return toItemView(requireCatalogItem(itemId));
  }

  List<CatalogCategoryView> merchantCategories(String businessType) {
    CurrentUser current = requireMerchant();
    Long storeId = repository.firstStoreByAccount(current.accountId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return repository.listCategories(storeId, normalizeBusinessTypeOrNull(businessType)).stream().map(this::toCategoryView).toList();
  }

  List<CatalogCategoryView> adminCategories(Long storeId, String businessType) {
    requireAdmin();
    return repository.listCategories(storeId, normalizeBusinessTypeOrNull(businessType)).stream().map(this::toCategoryView).toList();
  }

  @Transactional
  CatalogCategoryView createMerchantCategory(CatalogCategoryUpsertRequest request) {
    CurrentUser current = requireMerchant();
    long storeId = resolveMerchantStore(current.accountId(), request.storeId());
    return createCategory(storeId, request);
  }

  @Transactional
  CatalogCategoryView createAdminCategory(CatalogCategoryUpsertRequest request) {
    requireAdmin();
    if (request.storeId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "后台新增分类必须指定门店");
    return createCategory(request.storeId(), request);
  }

  @Transactional
  CatalogCategoryView updateCategory(long categoryId, CatalogCategoryUpsertRequest request) {
    MerchantCatalogRepository.CategoryRow category = repository.findCategory(categoryId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    ensureStoreAccess(category.storeId());
    repository.updateCategory(categoryId, request);
    return repository.findCategory(categoryId).map(this::toCategoryView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  void deleteCategory(long categoryId) {
    MerchantCatalogRepository.CategoryRow category = repository.findCategory(categoryId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    ensureStoreAccess(category.storeId());
    if (category.storeId() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "系统分类不允许删除");
    if (repository.countItemsByCategory(categoryId) > 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "分类下存在商品，请先调整商品分类或删除商品");
    repository.softDeleteCategory(categoryId);
  }

  PageResponse<AdminMerchantView> merchants(String keyword, int page, int pageSize) {
    requireAdmin();
    long total = repository.countMerchants(keyword);
    List<AdminMerchantView> list = repository.listMerchants(keyword, (page - 1) * pageSize, pageSize).stream().map(this::toAdminMerchantView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  PageResponse<AdminMerchantApplicationView> merchantApplications(String status, int page, int pageSize) {
    requireAdmin();
    String normalizedStatus = normalizeNullableApplicationStatus(status);
    long total = repository.countApplications(normalizedStatus);
    List<AdminMerchantApplicationView> list = repository.listApplications(normalizedStatus, (page - 1) * pageSize, pageSize).stream().map(this::toAdminApplicationView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  AdminMerchantApplicationView approveMerchantApplication(long id, AdminMerchantApplicationAuditRequest request) {
    CurrentUser current = requireAdmin();
    MerchantCatalogRepository.ApplicationRow application = pendingApplication(id);
    String idempotencyKey = "merchant-catalog-service:merchant-account-provision:application-" + id + ":v1";
    ServiceCommandResult account = internalClient.provisionMerchantAccount(
        new MerchantAccountProvisionRequest(application.applicationNo(), application.merchantName(), application.contactName(), application.contactPhone()),
        idempotencyKey);
    if (!account.success() || account.accountId() == null) {
      String remark = auditRemark(request, account.message() == null ? "账号服务暂不可用，待重试创建商家账号" : account.message());
      repository.updateApplicationAudit(id, "account_pending", application.accountId(), current.accountId(), remark);
      repository.insertMerchantAuditLog("application", id, "approve", "account_pending", remark, current.accountId());
      internalClient.writeAuditLog(new AuditLogRequest("admin", current.accountId(), "merchant_application_account_pending", "merchant_application", id, remark), "merchant-catalog-service:audit:application-" + id + ":account-pending");
      return repository.findApplication(id).map(this::toAdminApplicationView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    String remark = auditRemark(request, "审核通过，商家账号已创建");
    long merchantId = repository.insertMerchantFromApplication("MCH" + System.currentTimeMillis(), account.accountId(), application);
    repository.insertStoreFromApplication(merchantId, application);
    repository.updateApplicationAudit(id, "approved", account.accountId(), current.accountId(), remark);
    repository.insertMerchantAuditLog("application", id, "approve", "approved", remark, current.accountId());
    internalClient.writeAuditLog(new AuditLogRequest("admin", current.accountId(), "merchant_application_approve", "merchant_application", id, application.applicationNo() + " 入驻申请已通过"), "merchant-catalog-service:audit:application-" + id + ":approved");
    return repository.findApplication(id).map(this::toAdminApplicationView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminMerchantApplicationView rejectMerchantApplication(long id, AdminMerchantApplicationAuditRequest request) {
    CurrentUser current = requireAdmin();
    MerchantCatalogRepository.ApplicationRow application = pendingApplication(id);
    String remark = auditRemark(request, "资料不完整，请补充后重新提交");
    repository.updateApplicationAudit(id, "rejected", application.accountId(), current.accountId(), remark);
    repository.insertMerchantAuditLog("application", id, "reject", "rejected", remark, current.accountId());
    internalClient.writeAuditLog(new AuditLogRequest("admin", current.accountId(), "merchant_application_reject", "merchant_application", id, application.applicationNo() + " 入驻申请已驳回"), "merchant-catalog-service:audit:application-" + id + ":rejected");
    return repository.findApplication(id).map(this::toAdminApplicationView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<AdminCertificationMaterialView> certificationMaterials(String status, int page, int pageSize) {
    requireAdmin();
    String normalizedStatus = normalizeNullableAuditStatus(status);
    long total = repository.countCertificationMaterials(normalizedStatus);
    List<AdminCertificationMaterialView> list = repository.listCertificationMaterials(normalizedStatus, (page - 1) * pageSize, pageSize).stream().map(this::toAdminMaterialView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  AdminCertificationMaterialView updateCertificationMaterialStatus(long id, AdminCertificationMaterialAuditRequest request) {
    CurrentUser current = requireAdmin();
    String status = normalizeAuditStatus(request.status());
    String remark = request.rejectReason() == null || request.rejectReason().isBlank() ? ("approved".equals(status) ? "材料审核通过" : "材料审核未通过") : request.rejectReason().trim();
    repository.updateCertificationMaterialStatus(id, status, remark, current.accountId());
    repository.insertMerchantAuditLog("certification_material", id, "audit", status, remark, current.accountId());
    internalClient.writeAuditLog(new AuditLogRequest("admin", current.accountId(), "merchant_material_audit", "certification_material", id, remark), "merchant-catalog-service:audit:material-" + id + ":" + status);
    return repository.findMaterial(id).map(this::toAdminMaterialView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<AdminStoreView> stores(Long merchantId, String businessType, String status, int page, int pageSize) {
    requireAdmin();
    long total = repository.countStores(merchantId, businessType, status);
    List<AdminStoreView> list = repository.listStores(merchantId, businessType, status, (page - 1) * pageSize, pageSize).stream().map(this::toAdminStoreView).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  AdminMerchantView createMerchant(AdminMerchantUpsertRequest request) {
    requireAdmin();
    long id = repository.insertMerchant("MCH" + System.currentTimeMillis(), request, normalizeGeneralStatus(defaultValue(request.status(), "normal")), normalizeAuditStatus(defaultValue(request.auditStatus(), "approved")));
    return repository.findMerchant(id).map(this::toAdminMerchantView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminMerchantView updateMerchant(long merchantId, AdminMerchantUpsertRequest request) {
    requireAdmin();
    repository.updateMerchant(merchantId, request, normalizeGeneralStatus(defaultValue(request.status(), "normal")), normalizeAuditStatus(defaultValue(request.auditStatus(), "approved")));
    return repository.findMerchant(merchantId).map(this::toAdminMerchantView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminMerchantView updateMerchantStatus(long merchantId, AdminStatusRequest request) {
    requireAdmin();
    repository.updateMerchantStatus(merchantId, normalizeGeneralStatus(request.status()));
    return repository.findMerchant(merchantId).map(this::toAdminMerchantView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminStoreView createStore(AdminStoreUpsertRequest request) {
    requireAdmin();
    long id = repository.insertStore(request, normalizeBusinessType(request.businessType()), normalizeStoreStatus(defaultValue(request.status(), "open")));
    return repository.findStoreAdmin(id).map(this::toAdminStoreView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminStoreView updateStore(long storeId, AdminStoreUpsertRequest request) {
    requireAdmin();
    repository.updateStore(storeId, request, normalizeBusinessType(request.businessType()), normalizeStoreStatus(defaultValue(request.status(), "open")));
    return repository.findStoreAdmin(storeId).map(this::toAdminStoreView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminStoreView updateStoreStatus(long storeId, AdminStatusRequest request) {
    requireAdmin();
    repository.updateStoreStatus(storeId, normalizeStoreStatus(request.status()));
    return repository.findStoreAdmin(storeId).map(this::toAdminStoreView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  AdminStoreView uploadStoreCover(long storeId, MultipartFile file) {
    requireAdmin();
    FileStorageService.FileAssetView asset = fileStorageService.save(file, "store");
    repository.updateStoreCover(storeId, asset.publicUrl());
    return repository.findStoreAdmin(storeId).map(this::toAdminStoreView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  HomeView home(Double latitude, Double longitude) {
    List<ModuleView> modules = repository.listModules().stream().map(row -> new ModuleView(row.id(), row.code(), row.name(), row.businessType(), row.name() + "精选推荐", moduleIcon(row.businessType()))).toList();
    PageResponse<ItemCardView> recommendations = recommendations(1, 12, "personalized", latitude, longitude);
    long unread = CurrentUserContext.optional().filter(currentUser -> currentUser.isUser() && currentUser.userId() != null).map(currentUser -> internalClient.homeSummary(currentUser.userId()).unreadMessageCount()).orElse(0L);
    return new HomeView(modules, recommendations, unread);
  }

  PageResponse<ItemCardView> recommendations(int page, int pageSize, String sort, Double latitude, Double longitude) {
    int safePageSize = Math.min(Math.max(pageSize, 1), 50);
    long total = repository.countRecommendations();
    LocationContext location = locationContext(latitude, longitude);
    UserPreference preference = userPreference();
    RecommendationSort recommendationSort = RecommendationSort.from(sort);
    List<MerchantCatalogRepository.ItemRow> sorted = sortItems(repository.listRecommendations(0, (int) Math.min(total, Integer.MAX_VALUE)), location, preference, recommendationSort);
    List<MerchantCatalogRepository.ItemRow> spaced = spaceItemsByStore(sorted, 4);
    int fromIndex = Math.max(0, (page - 1) * safePageSize);
    int toIndex = Math.min(spaced.size(), fromIndex + safePageSize);
    List<ItemCardView> pageItems = fromIndex >= spaced.size() ? List.of() : spaced.subList(fromIndex, toIndex).stream().map(row -> toItemCard(row, itemReason(row, preference, location, recommendationSort))).toList();
    return PageResponse.of(pageItems, page, safePageSize, total);
  }

  ModulePageView module(String moduleCode, Double latitude, Double longitude) {
    BusinessType businessType = businessTypeByModuleCode(moduleCode);
    LocationContext location = locationContext(latitude, longitude);
    List<MerchantCatalogRepository.StoreRow> storeRows = sortStores(repository.listStoresByBusinessType(businessType.code(), 50), location);
    List<StoreCardView> stores = storeRows.stream().limit(10).map(row -> toStoreCard(row, location, true)).toList();
    List<ItemCardView> featuredItems = repository.listItemsByBusinessType(businessType.code(), 12).stream().map(this::toItemCard).toList();
    return new ModulePageView(moduleCode, businessType.code(), stores, PageResponse.of(featuredItems, 1, 12, featuredItems.size()));
  }

  PageResponse<StoreCardView> search(String keyword, int page, int pageSize, String sort, String businessType, Double latitude, Double longitude) {
    int safePageSize = Math.min(Math.max(pageSize, 1), 50);
    LocationContext location = locationContext(latitude, longitude);
    SearchSort searchSort = SearchSort.from(sort);
    String normalizedBusinessType = normalizeBusinessTypeNullable(businessType);
    List<String> inferredBusinessTypes = normalizedBusinessType == null ? inferredBusinessTypes(keyword) : List.of();
    String effectiveKeyword = inferredBusinessTypes.isEmpty() ? keyword : "";
    List<MerchantCatalogRepository.StoreRow> stores = sortStores(inferredBusinessTypes.isEmpty() ? repository.searchStores(keyword, normalizedBusinessType, 100) : repository.searchStoresByBusinessTypes(inferredBusinessTypes, 100), location, searchSort, effectiveKeyword);
    List<StoreCardView> mapped = stores.stream().map(store -> {
      List<MerchantCatalogRepository.ItemRow> matchedRows = new ArrayList<>(repository.searchItems(store.id(), effectiveKeyword, 6));
      if (matchedRows.size() < 6) {
        List<Long> excluded = matchedRows.stream().map(MerchantCatalogRepository.ItemRow::id).toList();
        List<Long> preferredCategories = matchedRows.stream().map(MerchantCatalogRepository.ItemRow::categoryId).distinct().toList();
        matchedRows.addAll(repository.listStoreItemsForFill(store.id(), excluded, preferredCategories, 6 - matchedRows.size()));
      }
      List<ItemCardView> matchedItems = matchedRows.stream().limit(6).map(row -> toItemCard(row, itemSearchReason(row, effectiveKeyword))).toList();
      return toStoreCard(store, matchedItems, location, true, storeReason(store, location, searchSort, effectiveKeyword));
    }).toList();
    int fromIndex = Math.max(0, (page - 1) * safePageSize);
    int toIndex = Math.min(mapped.size(), fromIndex + safePageSize);
    List<StoreCardView> pageItems = fromIndex >= mapped.size() ? List.of() : mapped.subList(fromIndex, toIndex);
    return PageResponse.of(pageItems, page, safePageSize, mapped.size());
  }

  StoreDetailView storeDetail(long storeId, Double latitude, Double longitude) {
    LocationContext location = locationContext(latitude, longitude);
    MerchantCatalogRepository.StoreRow storeRow = repository.findStore(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    List<CategoryView> categories = repository.listStoreCategories(storeId).stream().map(row -> new CategoryView(row.id(), row.name(), row.sortOrder())).toList();
    List<MerchantCatalogRepository.ItemRow> itemRows = repository.listStoreItems(storeId);
    List<ItemGroupView> itemGroups = itemRows.stream().collect(Collectors.groupingBy(MerchantCatalogRepository.ItemRow::categoryId, Collectors.toList())).entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> new ItemGroupView(entry.getKey(), entry.getValue().get(0).categoryName(), entry.getValue().stream().map(this::toItemCard).toList())).toList();
    ReviewSummaryView reviewSummary = internalClient.reviewSummary(storeId);
    MerchantCatalogRepository.DeliveryRuleRow deliveryRuleRow = repository.findDeliveryRule(storeId).orElse(null);
    DeliveryRuleView deliveryRule = deliveryRuleRow == null ? null : new DeliveryRuleView(deliveryRuleRow.deliveryFee(), deliveryRuleRow.estimatedMinutes(), deliveryRuleRow.startPrice(), deliveryRuleRow.packageFeeFixed(), deliveryRuleRow.packageFeePerItem(), deliveryRuleRow.packageFeeMode(), deliveryRuleRow.distanceExtraThresholdKm(), deliveryRuleRow.distanceExtraFee(), deliveryRuleRow.distanceExtraStepKm(), deliveryRuleRow.deliveryText());
    return new StoreDetailView(toStoreCard(storeRow, location), storeRow.businessType(), categories, itemGroups, reviewSummary, deliveryRule);
  }

  ItemDetailView itemDetail(long itemId, Double latitude, Double longitude) {
    LocationContext location = locationContext(latitude, longitude);
    MerchantCatalogRepository.ItemRow itemRow = repository.findItem(itemId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    MerchantCatalogRepository.StoreRow storeRow = repository.findStore(itemRow.storeId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    StoreCardView store = toStoreCard(storeRow, location);
    List<CategoryView> categories = repository.listStoreCategories(storeRow.id()).stream().map(row -> new CategoryView(row.id(), row.name(), row.sortOrder())).toList();
    List<ItemGroupView> itemGroups = repository.listStoreItems(storeRow.id()).stream().collect(Collectors.groupingBy(MerchantCatalogRepository.ItemRow::categoryId, Collectors.toList())).entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> new ItemGroupView(entry.getKey(), entry.getValue().get(0).categoryName(), entry.getValue().stream().map(this::toItemCard).toList())).toList();
    return new ItemDetailView(toItemCard(itemRow), store, categories, itemGroups);
  }

  MapDistanceService.ReverseGeocodeResult reverseGeocode(double longitude, double latitude) {
    return mapDistanceService.reverseGeocode(longitude, latitude);
  }

  TakeawaySettingView getTakeawaySetting(long storeId) {
    MerchantCatalogRepository.TakeawaySettingRow setting = requireTakeawaySettingStore(storeId);
    return new TakeawaySettingView(setting.storeId(), setting.storeName(), setting.acceptMode());
  }

  @Transactional
  TakeawaySettingView updateTakeawaySetting(long storeId, TakeawaySettingRequest request) {
    MerchantCatalogRepository.TakeawaySettingRow setting = requireTakeawaySettingStore(storeId);
    String acceptMode = normalizeAcceptMode(request.acceptMode());
    CurrentUser current = CurrentUserContext.required();
    repository.upsertTakeawaySetting(storeId, acceptMode, current.accountId());
    return new TakeawaySettingView(setting.storeId(), setting.storeName(), acceptMode);
  }

  List<MerchantItemView> listTakeawayItems(long storeId, String status) {
    requireTakeawaySettingStore(storeId);
    return repository.listTakeawayItems(storeId, normalizeItemStatusFilter(status)).stream().map(this::toMerchantItemView).toList();
  }

  @Transactional
  MerchantItemView updateTakeawayItem(long storeId, long itemId, MerchantItemUpdateRequest request) {
    requireTakeawaySettingStore(storeId);
    MerchantCatalogRepository.MerchantItemRow item = requireMerchantItem(storeId, itemId);
    validatePrice(request.price());
    repository.updateMerchantItem(item.id(), request.title().trim(), request.subtitle() == null ? "" : request.subtitle().trim(), request.price(), request.stock(), normalizeItemStatus(request.status()));
    return toMerchantItemView(requireMerchantItem(storeId, itemId));
  }

  @Transactional
  MerchantItemView updateTakeawayItemStatus(long storeId, long itemId, MerchantItemStatusRequest request) {
    requireTakeawaySettingStore(storeId);
    MerchantCatalogRepository.MerchantItemRow item = requireMerchantItem(storeId, itemId);
    repository.updateMerchantItemStatus(item.id(), normalizeItemStatus(request.status()));
    return toMerchantItemView(requireMerchantItem(storeId, itemId));
  }

  DeliveryRuleOpsView getDeliveryRule(long storeId) {
    requireTakeawaySettingStore(storeId);
    return toDeliveryRuleOpsView(storeId, repository.findDeliveryRule(storeId).orElse(defaultDeliveryRule()));
  }

  @Transactional
  DeliveryRuleOpsView updateDeliveryRule(long storeId, DeliveryRuleUpdateRequest request) {
    requireTakeawaySettingStore(storeId);
    if (request.deliveryFee().compareTo(BigDecimal.ZERO) < 0 || request.startPrice().compareTo(BigDecimal.ZERO) < 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "配送费和起送价不能为负数");
    if (request.maxDeliveryDistanceKm().compareTo(BigDecimal.ZERO) <= 0 || request.maxDeliveryDistanceKm().compareTo(BigDecimal.valueOf(100)) > 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "可配送范围需大于 0 且不超过 100km");
    String packageFeeMode = normalizePackageFeeMode(request.packageFeeMode());
    BigDecimal distanceExtraStepKm = request.distanceExtraStepKm() == null || request.distanceExtraStepKm().compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : request.distanceExtraStepKm();
    repository.upsertDeliveryRule(storeId, request.deliveryFee(), request.startPrice(), request.estimatedMinutes(), request.maxDeliveryDistanceKm(), packageFeeMode, nonNegative(request.packageFeeFixed()), nonNegative(request.packageFeePerItem()), nonNegative(request.distanceExtraThresholdKm()), nonNegative(request.distanceExtraFee()), distanceExtraStepKm, request.deliveryText() == null ? "" : request.deliveryText().trim());
    return getDeliveryRule(storeId);
  }

  StoreSnapshotView storeSnapshot(long storeId) {
    MerchantCatalogRepository.StoreRow store = repository.findStore(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    MerchantCatalogRepository.MerchantRow merchant = repository.findMerchant(store.merchantId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return new StoreSnapshotView(store.id(), store.merchantId(), merchant.accountId(), merchant.merchantName(), store.storeName(), store.businessType(), store.summary(), store.address(), store.status(), store.businessHoursText(), store.contactPhone(), store.coverUrl(), store.longitude(), store.latitude());
  }

  MerchantAccountView merchantByAccount(long accountId) {
    MerchantCatalogRepository.MerchantRow merchant = repository.findMerchantByAccount(accountId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    Long currentStoreId = repository.listMerchantStores(merchant.id()).stream().findFirst().map(MerchantCatalogRepository.StoreRow::id).orElse(null);
    return new MerchantAccountView(merchant.id(), merchant.accountId(), merchant.merchantName(), merchant.status(), currentStoreId);
  }

  CatalogItemSnapshotView itemSnapshot(long itemId) {
    MerchantCatalogRepository.CatalogItemRow item = requireCatalogItem(itemId);
    return new CatalogItemSnapshotView(item.id(), item.storeId(), item.storeName(), item.title(), item.subtitle(), item.businessType(), item.categoryId(), item.categoryName(), item.price(), item.coverUrl(), item.status(), item.usageRules(), item.refundPolicy());
  }

  FulfillmentRulesView fulfillmentRules(long storeId) {
    MerchantCatalogRepository.StoreRow store = repository.findStore(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    MerchantCatalogRepository.DeliveryRuleRow rule = repository.findDeliveryRule(storeId).orElse(defaultDeliveryRule());
    String acceptMode = repository.findTakeawaySetting(storeId).map(MerchantCatalogRepository.TakeawaySettingRow::acceptMode).orElse(ACCEPT_MODE_MANUAL);
    return new FulfillmentRulesView(store.id(), store.businessType(), acceptMode, rule.deliveryFee(), rule.startPrice(), rule.estimatedMinutes(), rule.maxDeliveryDistanceKm(), rule.packageFeeMode(), rule.packageFeeFixed(), rule.packageFeePerItem(), rule.distanceExtraThresholdKm(), rule.distanceExtraFee(), rule.distanceExtraStepKm(), rule.deliveryText());
  }

  CheckoutQuoteView checkoutQuote(CheckoutQuoteRequest request) {
    MerchantCatalogRepository.StoreRow store = repository.findStore(request.storeId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    List<CheckoutQuoteItemView> items = request.items().stream().map(line -> checkoutLine(store.id(), line)).toList();
    BigDecimal amount = items.stream().map(CheckoutQuoteItemView::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CheckoutQuoteView(store.id(), store.storeName(), store.businessType(), items, amount);
  }

  @Transactional
  InventoryResultView deductInventory(InventoryDeductRequest request, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    return inventoryIdempotency.computeIfAbsent("deduct:" + idempotencyKey, key -> {
      List<InventoryLineResultView> lines = new ArrayList<>();
      for (InventoryItemRequest item : request.items()) {
        MerchantCatalogRepository.SkuRow sku = repository.findSku(item.skuId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!sku.itemId().equals(item.itemId())) throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 与商品不匹配");
        int updated = repository.decreaseSkuStock(item.skuId(), item.quantity());
        if (updated == 0) throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
        lines.add(new InventoryLineResultView(item.skuId(), item.itemId(), item.quantity(), "deducted"));
      }
      return new InventoryResultView(idempotencyKey, "deducted", lines);
    });
  }

  @Transactional
  InventoryResultView restoreInventory(InventoryRestoreRequest request, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    return inventoryIdempotency.computeIfAbsent("restore:" + idempotencyKey, key -> {
      List<InventoryLineResultView> lines = new ArrayList<>();
      for (InventoryItemRequest item : request.items()) {
        MerchantCatalogRepository.SkuRow sku = repository.findSku(item.skuId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!sku.itemId().equals(item.itemId())) throw new BusinessException(ErrorCode.BAD_REQUEST, "SKU 与商品不匹配");
        repository.increaseSkuStock(item.skuId(), item.quantity());
        lines.add(new InventoryLineResultView(item.skuId(), item.itemId(), item.quantity(), "restored"));
      }
      return new InventoryResultView(idempotencyKey, "restored", lines);
    });
  }

  PlatformMerchantMetricsView platformMerchantMetrics() {
    return new PlatformMerchantMetricsView(
        repository.count("select count(1) from merchant_profile where is_deleted = 0"),
        repository.count("select count(1) from merchant_store where is_deleted = 0"),
        repository.count("select count(1) from catalog_item where is_deleted = 0 and status = 'on_sale'"));
  }

  private CheckoutQuoteItemView checkoutLine(long storeId, CheckoutQuoteItemRequest line) {
    MerchantCatalogRepository.CatalogItemRow item = requireCatalogItem(line.itemId());
    if (!item.storeId().equals(storeId)) throw new BusinessException(ErrorCode.BAD_REQUEST, "商品不属于当前门店");
    if (!"on_sale".equals(item.status())) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "商品已下架");
    MerchantCatalogRepository.SkuRow sku = repository.findDefaultSku(item.id()).orElseThrow(() -> new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH));
    if (!"on_sale".equals(sku.status()) || sku.stock() < line.quantity()) throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
    BigDecimal totalPrice = sku.price().multiply(BigDecimal.valueOf(line.quantity()));
    return new CheckoutQuoteItemView(item.id(), sku.id(), item.title(), item.subtitle(), item.businessType(), item.categoryId(), item.categoryName(), line.quantity(), sku.price(), totalPrice, item.coverUrl(), sku.stock(), item.status(), item.usageRules(), item.refundPolicy());
  }

  private MerchantCatalogRepository.MerchantRow currentMerchant() {
    CurrentUser current = CurrentUserContext.required();
    return repository.findMerchantByAccount(current.accountId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private CurrentUser requireMerchant() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.MERCHANT) throw new BusinessException(ErrorCode.FORBIDDEN);
    return current;
  }

  private CurrentUser requireAdmin() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.ADMIN) throw new BusinessException(ErrorCode.FORBIDDEN);
    return current;
  }

  private void ensureStoreAccess(Long storeId) {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() == AccountType.ADMIN) return;
    if (current.accountType() == AccountType.MERCHANT && storeId != null && repository.isStoreOwnedByAccount(storeId, current.accountId())) return;
    throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  private void ensureItemAccess(MerchantCatalogRepository.CatalogItemRow item) {
    ensureStoreAccess(item.storeId());
  }

  private MerchantCatalogRepository.TakeawaySettingRow requireTakeawaySettingStore(long storeId) {
    MerchantCatalogRepository.TakeawaySettingRow setting = repository.findTakeawaySetting(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    ensureStoreAccess(storeId);
    return setting;
  }

  private MerchantCatalogRepository.MerchantItemRow requireMerchantItem(long storeId, long itemId) {
    MerchantCatalogRepository.MerchantItemRow item = repository.findMerchantItem(itemId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!item.storeId().equals(storeId)) throw new BusinessException(ErrorCode.FORBIDDEN);
    return item;
  }

  private MerchantCatalogRepository.CatalogItemRow requireCatalogItem(long itemId) {
    return repository.findCatalogItem(itemId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private long resolveMerchantStore(long accountId, Long storeId) {
    long resolved = storeId == null ? repository.firstStoreByAccount(accountId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)) : storeId;
    if (!repository.isStoreOwnedByAccount(resolved, accountId)) throw new BusinessException(ErrorCode.FORBIDDEN);
    return resolved;
  }

  private long resolveStoreForUpdate(MerchantCatalogRepository.CatalogItemRow item, Long requestStoreId) {
    if (requestStoreId == null || requestStoreId.equals(item.storeId())) return item.storeId();
    ensureStoreAccess(requestStoreId);
    return requestStoreId;
  }

  private long resolveCategory(long storeId, CatalogItemUpsertRequest request) {
    String businessType = normalizeBusinessType(request.businessType());
    if (request.categoryId() == null) return repository.getOrCreateDefaultCategory(storeId, businessType);
    MerchantCatalogRepository.CategoryRow category = repository.findCategory(request.categoryId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (category.storeId() == null || !category.storeId().equals(storeId)) throw new BusinessException(ErrorCode.BAD_REQUEST, "分类不属于当前门店");
    return category.id();
  }

  private CatalogItemView createItem(long storeId, CatalogItemUpsertRequest request) {
    ensureStoreAccess(storeId);
    validatePrice(request.price());
    String businessType = normalizeBusinessType(request.businessType());
    long categoryId = resolveCategory(storeId, request);
    CatalogItemUpsertRequest normalized = new CatalogItemUpsertRequest(storeId, businessType, categoryId, request.title(), request.subtitle(), request.price(), request.originalPrice(), request.stock(), request.status(), request.coverUrl(), request.tagText(), request.businessAttributes(), request.usageRules(), request.refundPolicy(), request.notice(), request.validityDays());
    long itemId = repository.insertItem(storeId, normalized, categoryId, normalizeItemStatus(request.status()));
    return toItemView(requireCatalogItem(itemId));
  }

  private CatalogCategoryView createCategory(long storeId, CatalogCategoryUpsertRequest request) {
    ensureStoreAccess(storeId);
    String businessType = normalizeBusinessType(request.businessType());
    long categoryId = repository.insertCategory(storeId, new CatalogCategoryUpsertRequest(storeId, businessType, request.categoryName(), request.sortOrder(), request.status()));
    return repository.findCategory(categoryId).map(this::toCategoryView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private MerchantCatalogRepository.ApplicationRow pendingApplication(long id) {
    MerchantCatalogRepository.ApplicationRow application = repository.findApplication(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"pending".equals(application.status()) && !"account_pending".equals(application.status())) throw new BusinessException(ErrorCode.BAD_REQUEST, "申请已处理");
    return application;
  }

  private StoreLocation resolveLocation(MerchantStoreUpdateRequest request, MerchantCatalogRepository.StoreRow store) {
    if (request.longitude() != null || request.latitude() != null) {
      validateLocation(request.longitude(), request.latitude());
      return new StoreLocation(request.longitude(), request.latitude());
    }
    MapDistanceService.GeocodeResult geocoded = mapDistanceService.geocode(request.address());
    if (geocoded != null) return new StoreLocation(geocoded.longitude(), geocoded.latitude());
    return new StoreLocation(store.longitude(), store.latitude());
  }

  private void validateLocation(BigDecimal longitude, BigDecimal latitude) {
    if (longitude == null || latitude == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "经纬度需要同时填写");
    if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0 || longitude.compareTo(BigDecimal.valueOf(180)) > 0 || latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "经纬度范围不正确");
  }

  private void validatePrice(BigDecimal price) {
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "价格必须大于 0");
  }

  private void requireIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "内部写接口必须携带 Idempotency-Key");
  }

  private String normalizeBusinessTypeOrNull(String businessType) {
    return businessType == null || businessType.isBlank() ? null : normalizeBusinessType(businessType);
  }

  private String normalizeBusinessTypeNullable(String businessType) {
    return businessType == null || businessType.isBlank() ? null : normalizeBusinessType(businessType);
  }

  private String normalizeBusinessType(String businessType) {
    String value = businessType == null ? "" : businessType.trim().toLowerCase();
    return switch (value) {
      case "group", "groupbuy" -> "group_buy";
      case "fun" -> "entertainment";
      case "takeaway", "group_buy", "hotel", "entertainment", "movie", "beauty", "ticket", "massage" -> value;
      default -> BusinessType.fromCode(value).code();
    };
  }

  private String normalizeStatusOrNull(String status) {
    return status == null || status.isBlank() ? null : normalizeItemStatus(status);
  }

  private String normalizeItemStatusFilter(String status) {
    return status == null || status.isBlank() ? null : normalizeItemStatus(status);
  }

  private String normalizeItemStatus(String status) {
    String value = status == null || status.isBlank() ? "on_sale" : status.trim().toLowerCase();
    return switch (value) {
      case "on_sale", "online", "上架", "售卖" -> "on_sale";
      case "off_sale", "offline", "下架", "停售" -> "off_sale";
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "商品状态只能是 on_sale 或 off_sale");
    };
  }

  private String normalizeGeneralStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "normal", "disabled", "blocked" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "状态值不正确");
    };
  }

  private String normalizeStoreStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "open", "closed" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "门店状态只能是 open 或 closed");
    };
  }

  private String normalizeAuditStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "pending", "approved", "rejected" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "审核状态不正确");
    };
  }

  private String normalizeNullableAuditStatus(String status) {
    return status == null || status.isBlank() ? null : normalizeAuditStatus(status);
  }

  private String normalizeNullableApplicationStatus(String status) {
    if (status == null || status.isBlank()) return null;
    String value = status.trim().toLowerCase();
    return switch (value) {
      case "pending", "approved", "rejected", "account_pending" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "申请状态不正确");
    };
  }

  private String normalizeMaterialType(String materialType) {
    String value = materialType == null ? "" : materialType.trim().toLowerCase();
    return switch (value) {
      case "business_license", "food_license", "identity", "other" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "材料类型不正确");
    };
  }

  private String normalizeAcceptMode(String acceptMode) {
    String value = acceptMode == null ? "" : acceptMode.trim().toLowerCase();
    return switch (value) {
      case "auto", "automatic", "自动", "自动接单" -> ACCEPT_MODE_AUTO;
      case "manual", "手动", "手动接单" -> ACCEPT_MODE_MANUAL;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "接单模式只能是 auto 或 manual");
    };
  }

  private String normalizePackageFeeMode(String mode) {
    String value = mode == null ? "none" : mode.trim().toLowerCase();
    return switch (value) {
      case "none", "fixed", "per_item" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "打包费模式不支持");
    };
  }

  private BigDecimal nonNegative(BigDecimal value) {
    if (value == null) return BigDecimal.ZERO;
    if (value.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "费用不能为负数");
    return value;
  }

  private String defaultValue(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private List<DailyCountView> weeklyOrders(long todayOrders) {
    LocalDate today = LocalDate.now();
    List<DailyCountView> weekly = new ArrayList<>();
    for (int i = 6; i >= 0; i--) {
      LocalDate date = today.minusDays(i);
      weekly.add(new DailyCountView(date, i == 0 ? todayOrders : 0));
    }
    return weekly;
  }

  private double round1(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  private String auditRemark(AdminMerchantApplicationAuditRequest request, String fallback) {
    return request == null || request.remark() == null || request.remark().isBlank() ? fallback : request.remark().trim();
  }

  private String cleanOrDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String materialTypeLabel(String materialType) {
    return switch (materialType) {
      case "business_license" -> "营业执照";
      case "food_license" -> "食品经营许可证";
      case "identity" -> "法人身份证明";
      default -> "其他材料";
    };
  }

  private MerchantProfileView toProfileView(MerchantCatalogRepository.MerchantRow merchant) {
    List<MerchantStoreView> stores = repository.listMerchantStores(merchant.id()).stream().map(this::toStoreView).toList();
    return new MerchantProfileView(merchant.id(), merchant.accountId(), merchant.merchantName(), merchant.contactName(), merchant.contactPhone(), merchant.licenseNo(), merchant.status(), merchant.auditStatus(), stores.isEmpty() ? null : stores.get(0).id(), stores);
  }

  private MerchantStoreView toStoreView(MerchantCatalogRepository.StoreRow row) {
    return new MerchantStoreView(row.id(), row.merchantId(), row.storeName(), row.businessType(), row.summary(), row.address(), row.rating(), row.monthlySales(), row.avgPrice(), row.status(), row.businessHoursText(), row.tagText(), row.coverUrl(), row.contactPhone(), row.announcement(), row.longitude(), row.latitude(), row.updatedAt());
  }

  private MerchantApplicationView toApplicationView(MerchantCatalogRepository.ApplicationRow row) {
    return new MerchantApplicationView(row.id(), row.applicationNo(), row.accountId(), row.merchantName(), row.contactName(), row.contactPhone(), row.businessType(), row.storeName(), row.address(), row.status(), row.auditRemark(), row.submittedAt(), row.auditedAt());
  }

  private CertificationMaterialView toMaterialView(MerchantCatalogRepository.CertificationMaterialRow row) {
    return new CertificationMaterialView(row.id(), row.merchantId(), row.applicationId(), row.materialType(), row.materialName(), row.fileUrl(), row.status(), row.rejectReason(), row.submittedAt(), row.auditedAt());
  }

  private CatalogItemView toItemView(MerchantCatalogRepository.CatalogItemRow row) {
    return new CatalogItemView(row.id(), row.storeId(), row.storeName(), row.businessType(), row.categoryId(), row.categoryName(), row.title(), row.subtitle(), row.price(), row.originalPrice(), row.stock(), row.status(), row.coverUrl(), row.tagText(), row.salesCount(), row.businessAttributes(), row.usageRules(), row.refundPolicy(), row.notice(), row.validityDays(), row.updatedAt());
  }

  private CatalogCategoryView toCategoryView(MerchantCatalogRepository.CategoryRow row) {
    return new CatalogCategoryView(row.id(), row.storeId(), row.businessType(), row.categoryCode(), row.categoryName(), row.sortOrder(), row.status(), row.updatedAt());
  }

  private AdminMerchantView toAdminMerchantView(MerchantCatalogRepository.MerchantRow row) {
    return new AdminMerchantView(row.id(), row.accountId(), row.merchantName(), row.contactName(), row.contactPhone(), row.licenseNo(), row.status(), row.auditStatus(), row.storeCount(), row.itemCount(), row.settledAt());
  }

  private AdminMerchantApplicationView toAdminApplicationView(MerchantCatalogRepository.ApplicationRow row) {
    return new AdminMerchantApplicationView(row.id(), row.applicationNo(), row.accountId(), row.merchantName(), row.contactName(), row.contactPhone(), row.businessType(), row.storeName(), row.address(), row.status(), row.auditRemark(), row.submittedAt(), row.auditedBy(), row.auditedAt());
  }

  private AdminCertificationMaterialView toAdminMaterialView(MerchantCatalogRepository.CertificationMaterialRow row) {
    return new AdminCertificationMaterialView(row.id(), row.merchantId(), row.applicationId(), row.merchantName(), row.materialType(), row.materialName(), row.fileUrl(), row.status(), row.rejectReason(), row.submittedAt(), row.auditedBy(), row.auditedAt());
  }

  private AdminStoreView toAdminStoreView(MerchantCatalogRepository.StoreAdminRow row) {
    return new AdminStoreView(row.storeId(), row.merchantId(), row.merchantName(), row.storeName(), row.businessType(), row.summary(), row.address(), row.status(), row.businessHoursText(), row.tagText(), row.coverUrl(), row.contactPhone(), row.announcement(), row.updatedAt());
  }

  private MerchantItemView toMerchantItemView(MerchantCatalogRepository.MerchantItemRow row) {
    return new MerchantItemView(row.id(), row.storeId(), row.title(), row.subtitle(), row.categoryName(), row.price(), row.originalPrice(), row.stock(), row.status(), row.salesCount());
  }

  private DeliveryRuleOpsView toDeliveryRuleOpsView(long storeId, MerchantCatalogRepository.DeliveryRuleRow row) {
    return new DeliveryRuleOpsView(storeId, row.deliveryFee(), row.startPrice(), row.estimatedMinutes(), row.maxDeliveryDistanceKm(), row.packageFeeMode(), row.packageFeeFixed(), row.packageFeePerItem(), row.distanceExtraThresholdKm(), row.distanceExtraFee(), row.distanceExtraStepKm(), row.deliveryText());
  }

  private MerchantCatalogRepository.DeliveryRuleRow defaultDeliveryRule() {
    return new MerchantCatalogRepository.DeliveryRuleRow(BigDecimal.ZERO, BigDecimal.ZERO, 35, DEFAULT_MAX_DELIVERY_DISTANCE_KM, "none", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, "");
  }

  private StoreCardView toStoreCard(MerchantCatalogRepository.StoreRow row) {
    return toStoreCard(row, List.of(), null, false, "");
  }

  private StoreCardView toStoreCard(MerchantCatalogRepository.StoreRow row, LocationContext location) {
    return toStoreCard(row, List.of(), location, false, "");
  }

  private StoreCardView toStoreCard(MerchantCatalogRepository.StoreRow row, LocationContext location, boolean localOnly) {
    return toStoreCard(row, List.of(), location, localOnly, storeReason(row, location, SearchSort.DEFAULT, ""));
  }

  private StoreCardView toStoreCard(MerchantCatalogRepository.StoreRow row, List<ItemCardView> matchedItems, LocationContext location, boolean localOnly, String recommendReason) {
    MapDistanceService.DistanceEstimate estimate = estimate(row, location, localOnly);
    return new StoreCardView(row.id(), row.storeName(), row.businessType(), estimate == null ? row.distanceText() : estimate.distanceText(), row.rating(), row.monthlySales(), row.avgPrice(), row.status(), row.businessHoursText(), row.summary(), row.address(), splitTags(row.tagText()), row.coverUrl(), recommendReason, estimate == null ? "" : estimate.estimatedTimeText(), row.longitude(), row.latitude(), matchedItems);
  }

  private ItemCardView toItemCard(MerchantCatalogRepository.ItemRow row) {
    return toItemCard(row, "");
  }

  private ItemCardView toItemCard(MerchantCatalogRepository.ItemRow row, String recommendReason) {
    return new ItemCardView(row.id(), row.title(), row.subtitle(), row.businessType(), row.categoryId(), row.categoryName(), row.price(), row.originalPrice(), row.coverUrl(), splitTags(row.tagText()), recommendReason, row.stock(), row.skuStatus(), row.stock() <= 0 || !"on_sale".equals(row.skuStatus()), row.storeId(), row.storeName(), row.businessAttributes(), row.usageRules(), row.refundPolicy(), row.notice(), row.validityDays());
  }

  private MapDistanceService.DistanceEstimate estimate(MerchantCatalogRepository.StoreRow row, LocationContext location, boolean localOnly) {
    if (location == null || row.latitude() == null || row.longitude() == null) return null;
    if (localOnly) return mapDistanceService.estimateLocally(location.latitude(), location.longitude(), row.latitude().doubleValue(), row.longitude().doubleValue());
    return mapDistanceService.estimate(location.latitude(), location.longitude(), row.latitude().doubleValue(), row.longitude().doubleValue());
  }

  private double distanceForSort(MerchantCatalogRepository.StoreRow row, LocationContext location) {
    if (location == null || row.latitude() == null || row.longitude() == null) return Double.MAX_VALUE;
    return mapDistanceService.distanceKm(location.latitude(), location.longitude(), row.latitude().doubleValue(), row.longitude().doubleValue());
  }

  private double itemDistanceForSort(MerchantCatalogRepository.ItemRow row, LocationContext location) {
    if (location == null || row.storeLatitude() == null || row.storeLongitude() == null) return Double.MAX_VALUE;
    return mapDistanceService.distanceKm(location.latitude(), location.longitude(), row.storeLatitude().doubleValue(), row.storeLongitude().doubleValue());
  }

  private List<MerchantCatalogRepository.StoreRow> sortStores(List<MerchantCatalogRepository.StoreRow> stores, LocationContext location) {
    return sortStores(stores, location, SearchSort.DEFAULT, "");
  }

  private List<MerchantCatalogRepository.StoreRow> sortStores(List<MerchantCatalogRepository.StoreRow> stores, LocationContext location, SearchSort sort, String keyword) {
    Comparator<MerchantCatalogRepository.StoreRow> base = Comparator.comparingInt((MerchantCatalogRepository.StoreRow row) -> keywordScore(row, keyword)).thenComparing(MerchantCatalogRepository.StoreRow::monthlySales, Comparator.reverseOrder()).thenComparing(MerchantCatalogRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(MerchantCatalogRepository.StoreRow::id);
    Comparator<MerchantCatalogRepository.StoreRow> comparator = switch (sort) {
      case DISTANCE -> location == null ? base : Comparator.comparingDouble((MerchantCatalogRepository.StoreRow row) -> distanceForSort(row, location)).thenComparing(base);
      case RATING -> Comparator.comparing(MerchantCatalogRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(MerchantCatalogRepository.StoreRow::monthlySales, Comparator.reverseOrder()).thenComparing(MerchantCatalogRepository.StoreRow::id);
      case SALES -> Comparator.comparing(MerchantCatalogRepository.StoreRow::monthlySales, Comparator.reverseOrder()).thenComparing(MerchantCatalogRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(MerchantCatalogRepository.StoreRow::id);
      case PRICE_ASC -> Comparator.comparing(MerchantCatalogRepository.StoreRow::avgPrice, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(MerchantCatalogRepository.StoreRow::rating, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(MerchantCatalogRepository.StoreRow::id);
      case DEFAULT -> location == null ? base : base.thenComparingDouble(row -> distanceForSort(row, location));
    };
    return stores.stream().sorted(comparator).toList();
  }

  private List<MerchantCatalogRepository.ItemRow> sortItems(List<MerchantCatalogRepository.ItemRow> items, LocationContext location, UserPreference preference, RecommendationSort sort) {
    Comparator<MerchantCatalogRepository.ItemRow> hot = Comparator.comparing(MerchantCatalogRepository.ItemRow::salesCount, Comparator.reverseOrder()).thenComparing(MerchantCatalogRepository.ItemRow::storeMonthlySales, Comparator.reverseOrder()).thenComparing(MerchantCatalogRepository.ItemRow::storeRating, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(MerchantCatalogRepository.ItemRow::sortOrder).thenComparing(MerchantCatalogRepository.ItemRow::id);
    Comparator<MerchantCatalogRepository.ItemRow> comparator = switch (sort) {
      case PERSONALIZED -> Comparator.comparingInt((MerchantCatalogRepository.ItemRow row) -> preference.score(row)).reversed().thenComparing(location == null ? hot : Comparator.comparingDouble((MerchantCatalogRepository.ItemRow row) -> itemDistanceForSort(row, location)).thenComparing(hot));
      case DISTANCE -> location == null ? hot : Comparator.comparingDouble((MerchantCatalogRepository.ItemRow row) -> itemDistanceForSort(row, location)).thenComparing(hot);
      case RATING -> Comparator.comparing(MerchantCatalogRepository.ItemRow::storeRating, Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(MerchantCatalogRepository.ItemRow::storeMonthlySales, Comparator.reverseOrder()).thenComparing(MerchantCatalogRepository.ItemRow::salesCount, Comparator.reverseOrder()).thenComparing(MerchantCatalogRepository.ItemRow::id);
      case SALES -> hot;
    };
    return items.stream().sorted(comparator).toList();
  }

  private UserPreference userPreference() {
    return CurrentUserContext.optional().filter(currentUser -> currentUser.isUser() && currentUser.userId() != null).map(currentUser -> UserPreference.from(internalClient.preferenceSignals(currentUser.userId()))).orElseGet(UserPreference::empty);
  }

  private String itemReason(MerchantCatalogRepository.ItemRow row, UserPreference preference, LocationContext location, RecommendationSort sort) {
    if (sort == RecommendationSort.PERSONALIZED) {
      String reason = preference.reason(row);
      if (!reason.isBlank()) return reason;
    }
    return switch (sort) {
      case DISTANCE -> location == null ? fallbackItemReason(row) : "离你更近";
      case RATING -> ratingReason(row.storeRating());
      case SALES -> salesReason(row.salesCount());
      case PERSONALIZED -> fallbackItemReason(row);
    };
  }

  private String itemSearchReason(MerchantCatalogRepository.ItemRow row, String keyword) {
    String normalized = normalizeText(keyword);
    if (!normalized.isEmpty()) {
      if (normalizeText(row.title()).contains(normalized)) return "商品名匹配";
      if (normalizeText(row.tagText()).contains(normalized)) return "标签匹配";
      if (normalizeText(row.subtitle()).contains(normalized)) return "描述匹配";
    }
    return salesReason(row.salesCount());
  }

  private String storeReason(MerchantCatalogRepository.StoreRow row, LocationContext location, SearchSort sort, String keyword) {
    return switch (sort) {
      case DISTANCE -> location == null ? fallbackStoreReason(row) : "距离最近优先";
      case RATING -> ratingReason(row.rating());
      case SALES -> "月售 " + row.monthlySales();
      case PRICE_ASC -> row.avgPrice() == null ? fallbackStoreReason(row) : "人均 ￥" + row.avgPrice().stripTrailingZeros().toPlainString();
      case DEFAULT -> defaultSearchReason(row, location, keyword);
    };
  }

  private String defaultSearchReason(MerchantCatalogRepository.StoreRow row, LocationContext location, String keyword) {
    int score = keywordScore(row, keyword);
    if (score == 0) return "店名匹配";
    if (score == 1) return "标签匹配";
    if (score == 2) return "描述匹配";
    if (location != null) return "附近热门";
    return fallbackStoreReason(row);
  }

  private int keywordScore(MerchantCatalogRepository.StoreRow row, String keyword) {
    String normalized = normalizeText(keyword);
    if (normalized.isEmpty()) return 4;
    if (normalizeText(row.storeName()).contains(normalized)) return 0;
    if (normalizeText(row.tagText()).contains(normalized)) return 1;
    if (normalizeText(row.summary()).contains(normalized)) return 2;
    if (normalizeText(row.address()).contains(normalized)) return 3;
    return 4;
  }

  private String fallbackItemReason(MerchantCatalogRepository.ItemRow row) {
    if (row.salesCount() > 0) return salesReason(row.salesCount());
    if (row.storeRating() != null && row.storeRating().doubleValue() >= 4.5) return ratingReason(row.storeRating());
    return businessLabel(row.businessType()) + "精选";
  }

  private String fallbackStoreReason(MerchantCatalogRepository.StoreRow row) {
    if (row.monthlySales() > 0) return "月售 " + row.monthlySales();
    if (row.rating() != null && row.rating().doubleValue() >= 4.5) return ratingReason(row.rating());
    return businessLabel(row.businessType()) + "精选";
  }

  private String salesReason(int salesCount) {
    return salesCount > 0 ? "热卖 " + salesCount + " 单" : "热门推荐";
  }

  private String ratingReason(BigDecimal rating) {
    return rating == null ? "高分好店" : "评分 " + rating.stripTrailingZeros().toPlainString();
  }

  private String businessLabel(String businessType) {
    try {
      return BusinessType.fromCode(businessType).label();
    } catch (IllegalArgumentException ex) {
      return "爱团";
    }
  }

  private List<MerchantCatalogRepository.ItemRow> spaceItemsByStore(List<MerchantCatalogRepository.ItemRow> items, int windowSize) {
    List<MerchantCatalogRepository.ItemRow> remaining = new ArrayList<>(items);
    List<MerchantCatalogRepository.ItemRow> result = new ArrayList<>(items.size());
    List<Long> recentStores = new ArrayList<>();
    while (!remaining.isEmpty()) {
      int selectedIndex = -1;
      for (int i = 0; i < remaining.size(); i++) {
        if (!recentStores.contains(remaining.get(i).storeId())) {
          selectedIndex = i;
          break;
        }
      }
      if (selectedIndex < 0) selectedIndex = 0;
      MerchantCatalogRepository.ItemRow selected = remaining.remove(selectedIndex);
      result.add(selected);
      recentStores.add(selected.storeId());
      if (recentStores.size() > windowSize) recentStores.remove(0);
    }
    return result;
  }

  private LocationContext locationContext(Double latitude, Double longitude) {
    return latitude == null || longitude == null ? null : new LocationContext(latitude, longitude);
  }

  private List<String> splitTags(String tagText) {
    if (tagText == null || tagText.isBlank()) return List.of();
    return java.util.Arrays.stream(tagText.split(",")).map(String::trim).filter(tag -> !tag.isBlank()).toList();
  }

  private BusinessType businessTypeByModuleCode(String moduleCode) {
    return switch (moduleCode) {
      case "group" -> BusinessType.GROUP_BUY;
      case "fun" -> BusinessType.ENTERTAINMENT;
      default -> BusinessType.fromCode(moduleCode);
    };
  }

  private List<String> inferredBusinessTypes(String keyword) {
    String normalized = normalizeText(keyword);
    if (normalized.isEmpty()) return List.of();
    if (containsAny(normalized, "休闲玩乐", "休闲娱乐", "附近玩", "周边玩", "玩乐", "娱乐")) return List.of("movie", "ticket", "massage", "entertainment");
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
    PERSONALIZED, SALES, RATING, DISTANCE;
    static RecommendationSort from(String value) {
      if (value == null || value.isBlank()) return PERSONALIZED;
      return switch (value.trim().toLowerCase(Locale.ROOT)) {
        case "sales" -> SALES;
        case "rating" -> RATING;
        case "distance" -> DISTANCE;
        default -> PERSONALIZED;
      };
    }
  }

  private enum SearchSort {
    DEFAULT, DISTANCE, RATING, SALES, PRICE_ASC;
    static SearchSort from(String value) {
      if (value == null || value.isBlank()) return DEFAULT;
      return switch (value.trim().toLowerCase(Locale.ROOT)) {
        case "distance" -> DISTANCE;
        case "rating" -> RATING;
        case "sales" -> SALES;
        case "price_asc" -> PRICE_ASC;
        default -> DEFAULT;
      };
    }
  }

  private record StoreLocation(BigDecimal longitude, BigDecimal latitude) {}
  private record LocationContext(double latitude, double longitude) {}

  private record UserPreference(Map<String, Integer> businessTypeWeights, Map<Long, Integer> categoryWeights, Map<Long, Integer> storeWeights, Map<Long, Integer> itemWeights) {
    static UserPreference empty() {
      return new UserPreference(Map.of(), Map.of(), Map.of(), Map.of());
    }

    static UserPreference from(List<PreferenceSignalView> signals) {
      Map<String, Integer> businessTypeWeights = new HashMap<>();
      Map<Long, Integer> categoryWeights = new HashMap<>();
      Map<Long, Integer> storeWeights = new HashMap<>();
      Map<Long, Integer> itemWeights = new HashMap<>();
      for (PreferenceSignalView signal : signals) {
        add(businessTypeWeights, signal.businessType(), signal.weight());
        add(categoryWeights, signal.categoryId(), signal.weight());
        add(storeWeights, signal.storeId(), signal.weight());
        add(itemWeights, signal.itemId(), signal.weight());
      }
      return new UserPreference(businessTypeWeights, categoryWeights, storeWeights, itemWeights);
    }

    int score(MerchantCatalogRepository.ItemRow row) {
      return businessTypeWeights.getOrDefault(row.businessType(), 0) + categoryWeights.getOrDefault(row.categoryId(), 0) + storeWeights.getOrDefault(row.storeId(), 0) + itemWeights.getOrDefault(row.id(), 0);
    }

    String reason(MerchantCatalogRepository.ItemRow row) {
      if (itemWeights.containsKey(row.id())) return "收藏后常看";
      if (storeWeights.containsKey(row.storeId())) return "常逛这家店";
      if (categoryWeights.containsKey(row.categoryId())) return "常买同类";
      if (businessTypeWeights.containsKey(row.businessType())) return "偏好" + label(row.businessType());
      return "";
    }

    private static <T> void add(Map<T, Integer> map, T key, int weight) {
      if (key != null) map.merge(key, weight, Integer::sum);
    }

    private static String label(String businessType) {
      try {
        return BusinessType.fromCode(businessType).label();
      } catch (IllegalArgumentException ex) {
        return "同类内容";
      }
    }
  }
}
