package com.aituan.catalog;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.enums.BusinessType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.file.FileAssetView;
import com.aituan.common.file.FileStorageService;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class CatalogService {
  private final CatalogRepository catalogRepository;
  private final FileStorageService fileStorageService;

  CatalogService(CatalogRepository catalogRepository, FileStorageService fileStorageService) {
    this.catalogRepository = catalogRepository;
    this.fileStorageService = fileStorageService;
  }

  List<CatalogItemView> merchantItems(String businessType, String status, String keyword) {
    CurrentUser current = requireMerchant();
    return catalogRepository.listMerchantItems(current.accountId(), normalizeBusinessTypeOrNull(businessType), normalizeStatusOrNull(status), keyword)
        .stream()
        .map(this::toItemView)
        .toList();
  }

  PageResponse<CatalogItemView> adminItems(Long storeId, String businessType, String status, String keyword, int page, int pageSize) {
    requireAdmin();
    String normalizedBusinessType = normalizeBusinessTypeOrNull(businessType);
    String normalizedStatus = normalizeStatusOrNull(status);
    long total = catalogRepository.countAdminItems(storeId, normalizedBusinessType, normalizedStatus, keyword);
    List<CatalogItemView> list = catalogRepository.listAdminItems(storeId, normalizedBusinessType, normalizedStatus, keyword, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toItemView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  CatalogItemView item(long itemId) {
    CatalogRepository.CatalogItemRow item = requireItem(itemId);
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
    if (request.storeId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "后台新增商品必须指定门店");
    }
    return createItem(request.storeId(), request);
  }

  @Transactional
  CatalogItemView updateItem(long itemId, CatalogItemUpsertRequest request) {
    CatalogRepository.CatalogItemRow current = requireItem(itemId);
    ensureItemAccess(current);
    long storeId = resolveStoreForUpdate(current, request.storeId());
    long categoryId = resolveCategory(storeId, request);
    validatePrice(request.price());
    catalogRepository.updateItem(itemId, request, categoryId, normalizeItemStatus(request.status()));
    return toItemView(requireItem(itemId));
  }

  @Transactional
  CatalogItemView updateItemStatus(long itemId, CatalogItemStatusRequest request) {
    CatalogRepository.CatalogItemRow item = requireItem(itemId);
    ensureItemAccess(item);
    catalogRepository.updateItemStatus(itemId, normalizeItemStatus(request.status()));
    return toItemView(requireItem(itemId));
  }

  @Transactional
  CatalogItemView uploadItemCover(long itemId, MultipartFile file) {
    CatalogRepository.CatalogItemRow item = requireItem(itemId);
    ensureItemAccess(item);
    FileAssetView asset = fileStorageService.save(file, "item");
    catalogRepository.updateItemCover(itemId, asset.publicUrl());
    return toItemView(requireItem(itemId));
  }

  List<CatalogCategoryView> merchantCategories(String businessType) {
    CurrentUser current = requireMerchant();
    Long storeId = catalogRepository.firstStoreByAccount(current.accountId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return catalogRepository.listCategories(storeId, normalizeBusinessTypeOrNull(businessType)).stream().map(this::toCategoryView).toList();
  }

  List<CatalogCategoryView> adminCategories(Long storeId, String businessType) {
    requireAdmin();
    return catalogRepository.listCategories(storeId, normalizeBusinessTypeOrNull(businessType)).stream().map(this::toCategoryView).toList();
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
    if (request.storeId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "后台新增分类必须指定门店");
    }
    return createCategory(request.storeId(), request);
  }

  @Transactional
  CatalogCategoryView updateCategory(long categoryId, CatalogCategoryUpsertRequest request) {
    CatalogRepository.CategoryRow category = catalogRepository.findCategory(categoryId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    ensureStoreAccess(category.storeId());
    catalogRepository.updateCategory(categoryId, request);
    return catalogRepository.findCategory(categoryId).map(this::toCategoryView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  void deleteCategory(long categoryId) {
    CatalogRepository.CategoryRow category = catalogRepository.findCategory(categoryId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    ensureStoreAccess(category.storeId());
    if (category.storeId() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "系统分类不允许删除");
    }
    if (catalogRepository.countItemsByCategory(categoryId) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "分类下存在商品，请先调整商品分类或删除商品");
    }
    catalogRepository.softDeleteCategory(categoryId);
  }

  private CatalogItemView createItem(long storeId, CatalogItemUpsertRequest request) {
    ensureStoreAccess(storeId);
    validatePrice(request.price());
    String businessType = normalizeBusinessType(request.businessType());
    long categoryId = resolveCategory(storeId, request);
    long itemId = catalogRepository.insertItem(storeId, new CatalogItemUpsertRequest(
        storeId,
        businessType,
        categoryId,
        request.title(),
        request.subtitle(),
        request.price(),
        request.originalPrice(),
        request.stock(),
        request.status(),
        request.coverUrl(),
        request.tagText()), categoryId, normalizeItemStatus(request.status()));
    return toItemView(requireItem(itemId));
  }

  private CatalogCategoryView createCategory(long storeId, CatalogCategoryUpsertRequest request) {
    ensureStoreAccess(storeId);
    String businessType = normalizeBusinessType(request.businessType());
    long categoryId = catalogRepository.insertCategory(storeId, new CatalogCategoryUpsertRequest(storeId, businessType, request.categoryName(), request.sortOrder(), request.status()));
    return catalogRepository.findCategory(categoryId).map(this::toCategoryView).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private long resolveCategory(long storeId, CatalogItemUpsertRequest request) {
    String businessType = normalizeBusinessType(request.businessType());
    if (request.categoryId() == null) {
      return catalogRepository.getOrCreateDefaultCategory(storeId, businessType);
    }
    CatalogRepository.CategoryRow category = catalogRepository.findCategory(request.categoryId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (category.storeId() == null || !category.storeId().equals(storeId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "分类不属于当前门店");
    }
    return category.id();
  }

  private long resolveMerchantStore(long accountId, Long storeId) {
    long resolved = storeId == null
        ? catalogRepository.firstStoreByAccount(accountId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
        : storeId;
    if (!catalogRepository.isStoreOwnedByAccount(resolved, accountId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return resolved;
  }

  private long resolveStoreForUpdate(CatalogRepository.CatalogItemRow item, Long requestStoreId) {
    if (requestStoreId == null || requestStoreId.equals(item.storeId())) {
      return item.storeId();
    }
    ensureStoreAccess(requestStoreId);
    return requestStoreId;
  }

  private void ensureItemAccess(CatalogRepository.CatalogItemRow item) {
    ensureStoreAccess(item.storeId());
  }

  private void ensureStoreAccess(Long storeId) {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() == AccountType.ADMIN) {
      return;
    }
    if (current.accountType() == AccountType.MERCHANT && storeId != null && catalogRepository.isStoreOwnedByAccount(storeId, current.accountId())) {
      return;
    }
    throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  private CurrentUser requireMerchant() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.MERCHANT) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return current;
  }

  private void requireAdmin() {
    if (CurrentUserContext.required().accountType() != AccountType.ADMIN) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
  }

  private CatalogRepository.CatalogItemRow requireItem(long itemId) {
    return catalogRepository.findItem(itemId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private void validatePrice(BigDecimal price) {
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "价格必须大于 0");
    }
  }

  private String normalizeBusinessTypeOrNull(String businessType) {
    return businessType == null || businessType.isBlank() ? null : normalizeBusinessType(businessType);
  }

  private String normalizeBusinessType(String businessType) {
    String value = businessType.trim().toLowerCase();
    return switch (value) {
      case "group", "groupbuy" -> "group_buy";
      case "fun" -> "entertainment";
      default -> BusinessType.fromCode(value).code();
    };
  }

  private String normalizeStatusOrNull(String status) {
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

  private CatalogItemView toItemView(CatalogRepository.CatalogItemRow row) {
    return new CatalogItemView(
        row.id(),
        row.storeId(),
        row.storeName(),
        row.businessType(),
        row.categoryId(),
        row.categoryName(),
        row.title(),
        row.subtitle(),
        row.price(),
        row.originalPrice(),
        row.stock(),
        row.status(),
        row.coverUrl(),
        row.tagText(),
        row.salesCount(),
        row.updatedAt());
  }

  private CatalogCategoryView toCategoryView(CatalogRepository.CategoryRow row) {
    return new CatalogCategoryView(row.id(), row.storeId(), row.businessType(), row.categoryCode(), row.categoryName(), row.sortOrder(), row.status(), row.updatedAt());
  }
}
