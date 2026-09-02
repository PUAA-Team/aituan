package com.aituan.merchantcatalog;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/open/merchant")
@Validated
class OpenMerchantController {
  private final MerchantCatalogService service;

  OpenMerchantController(MerchantCatalogService service) {
    this.service = service;
  }

  @PostMapping("/applications")
  ApiResponse<MerchantApplicationView> submitApplication(@Valid @RequestBody MerchantApplicationSubmitRequest request) {
    return ApiResponse.ok(service.submitApplication(request));
  }
}

@RestController
@RequestMapping("/api/merchant")
@Validated
class MerchantController {
  private final MerchantCatalogService service;

  MerchantController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/profile/me")
  ApiResponse<MerchantProfileView> profile() {
    return ApiResponse.ok(service.profile());
  }

  @PutMapping("/profile/me")
  ApiResponse<MerchantProfileView> updateProfile(@Valid @RequestBody MerchantProfileUpdateRequest request) {
    return ApiResponse.ok(service.updateProfile(request));
  }

  @GetMapping("/stores/current")
  ApiResponse<MerchantStoreView> currentStore() {
    return ApiResponse.ok(service.currentStore());
  }

  @PutMapping("/stores/current")
  ApiResponse<MerchantStoreView> updateCurrentStore(@Valid @RequestBody MerchantStoreUpdateRequest request) {
    return ApiResponse.ok(service.updateCurrentStore(request));
  }

  @PostMapping(value = "/stores/current/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<MerchantStoreView> uploadCurrentStoreCover(@RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadCurrentStoreCover(file));
  }

  @GetMapping("/certification")
  ApiResponse<MerchantCertificationView> certification() {
    return ApiResponse.ok(service.certification());
  }

  @GetMapping("/certification/materials")
  ApiResponse<List<CertificationMaterialView>> certificationMaterials() {
    return ApiResponse.ok(service.certificationMaterials());
  }

  @PostMapping(value = "/certification/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<CertificationMaterialView> uploadCertificationMaterial(
      @RequestParam String materialType,
      @RequestParam(required = false) String materialName,
      @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadCertificationMaterial(materialType, materialName, file));
  }
}

@RestController
@RequestMapping("/api/merchant/ops")
@Validated
class MerchantOpsController {
  private final MerchantCatalogService service;

  MerchantOpsController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/dashboard")
  ApiResponse<MerchantDashboardView> dashboard() {
    return ApiResponse.ok(service.merchantDashboard());
  }
}

@RestController
@RequestMapping("/api/merchant/catalog")
@Validated
class MerchantCatalogController {
  private final MerchantCatalogService service;

  MerchantCatalogController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/items")
  ApiResponse<List<CatalogItemView>> items(
      @RequestParam(required = false) String businessType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(service.merchantItems(businessType, status, keyword));
  }

  @PostMapping("/items")
  ApiResponse<CatalogItemView> createItem(@Valid @RequestBody CatalogItemUpsertRequest request) {
    return ApiResponse.ok(service.createMerchantItem(request));
  }

  @GetMapping("/items/{itemId}")
  ApiResponse<CatalogItemView> item(@PathVariable long itemId) {
    return ApiResponse.ok(service.item(itemId));
  }

  @PutMapping("/items/{itemId}")
  ApiResponse<CatalogItemView> updateItem(@PathVariable long itemId, @Valid @RequestBody CatalogItemUpsertRequest request) {
    return ApiResponse.ok(service.updateItem(itemId, request));
  }

  @PostMapping("/items/{itemId}/status")
  ApiResponse<CatalogItemView> updateItemStatus(@PathVariable long itemId, @Valid @RequestBody CatalogItemStatusRequest request) {
    return ApiResponse.ok(service.updateItemStatus(itemId, request));
  }

  @PostMapping(value = "/items/{itemId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<CatalogItemView> uploadItemCover(@PathVariable long itemId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadItemCover(itemId, file));
  }

  @GetMapping("/categories")
  ApiResponse<List<CatalogCategoryView>> categories(@RequestParam(required = false) String businessType) {
    return ApiResponse.ok(service.merchantCategories(businessType));
  }

  @PostMapping("/categories")
  ApiResponse<CatalogCategoryView> createCategory(@Valid @RequestBody CatalogCategoryUpsertRequest request) {
    return ApiResponse.ok(service.createMerchantCategory(request));
  }

  @PutMapping("/categories/{categoryId}")
  ApiResponse<CatalogCategoryView> updateCategory(@PathVariable long categoryId, @Valid @RequestBody CatalogCategoryUpsertRequest request) {
    return ApiResponse.ok(service.updateCategory(categoryId, request));
  }

  @DeleteMapping("/categories/{categoryId}")
  ApiResponse<Void> deleteCategory(@PathVariable long categoryId) {
    service.deleteCategory(categoryId);
    return ApiResponse.ok(null);
  }
}

@RestController
@RequestMapping("/api/admin/catalog")
@Validated
class AdminCatalogController {
  private final MerchantCatalogService service;

  AdminCatalogController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/items")
  ApiResponse<PageResponse<CatalogItemView>> items(
      @RequestParam(required = false) Long storeId,
      @RequestParam(required = false) String businessType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(service.adminItems(storeId, businessType, status, keyword, page, pageSize));
  }

  @PostMapping("/items")
  ApiResponse<CatalogItemView> createItem(@Valid @RequestBody CatalogItemUpsertRequest request) {
    return ApiResponse.ok(service.createAdminItem(request));
  }

  @GetMapping("/items/{itemId}")
  ApiResponse<CatalogItemView> item(@PathVariable long itemId) {
    return ApiResponse.ok(service.item(itemId));
  }

  @PutMapping("/items/{itemId}")
  ApiResponse<CatalogItemView> updateItem(@PathVariable long itemId, @Valid @RequestBody CatalogItemUpsertRequest request) {
    return ApiResponse.ok(service.updateItem(itemId, request));
  }

  @PostMapping("/items/{itemId}/status")
  ApiResponse<CatalogItemView> updateItemStatus(@PathVariable long itemId, @Valid @RequestBody CatalogItemStatusRequest request) {
    return ApiResponse.ok(service.updateItemStatus(itemId, request));
  }

  @PostMapping(value = "/items/{itemId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<CatalogItemView> uploadItemCover(@PathVariable long itemId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadItemCover(itemId, file));
  }

  @GetMapping("/categories")
  ApiResponse<List<CatalogCategoryView>> categories(@RequestParam(required = false) Long storeId, @RequestParam(required = false) String businessType) {
    return ApiResponse.ok(service.adminCategories(storeId, businessType));
  }

  @PostMapping("/categories")
  ApiResponse<CatalogCategoryView> createCategory(@Valid @RequestBody CatalogCategoryUpsertRequest request) {
    return ApiResponse.ok(service.createAdminCategory(request));
  }

  @PutMapping("/categories/{categoryId}")
  ApiResponse<CatalogCategoryView> updateCategory(@PathVariable long categoryId, @Valid @RequestBody CatalogCategoryUpsertRequest request) {
    return ApiResponse.ok(service.updateCategory(categoryId, request));
  }
}

@RestController
@RequestMapping("/api/admin")
@Validated
class AdminMerchantStoreController {
  private final MerchantCatalogService service;

  AdminMerchantStoreController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/merchants")
  ApiResponse<PageResponse<AdminMerchantView>> merchants(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(service.merchants(keyword, page, pageSize));
  }

  @PostMapping("/merchants")
  ApiResponse<AdminMerchantView> createMerchant(@Valid @RequestBody AdminMerchantUpsertRequest request) {
    return ApiResponse.ok(service.createMerchant(request));
  }

  @PutMapping("/merchants/{merchantId}")
  ApiResponse<AdminMerchantView> updateMerchant(@PathVariable long merchantId, @Valid @RequestBody AdminMerchantUpsertRequest request) {
    return ApiResponse.ok(service.updateMerchant(merchantId, request));
  }

  @PostMapping("/merchants/{merchantId}/status")
  ApiResponse<AdminMerchantView> updateMerchantStatus(@PathVariable long merchantId, @Valid @RequestBody AdminStatusRequest request) {
    return ApiResponse.ok(service.updateMerchantStatus(merchantId, request));
  }

  @GetMapping("/merchants/applications")
  ApiResponse<PageResponse<AdminMerchantApplicationView>> merchantApplications(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(service.merchantApplications(status, page, pageSize));
  }

  @PostMapping("/merchants/applications/{id}/approve")
  ApiResponse<AdminMerchantApplicationView> approveMerchantApplication(@PathVariable long id, @RequestBody(required = false) AdminMerchantApplicationAuditRequest request) {
    return ApiResponse.ok(service.approveMerchantApplication(id, request));
  }

  @PostMapping("/merchants/applications/{id}/reject")
  ApiResponse<AdminMerchantApplicationView> rejectMerchantApplication(@PathVariable long id, @RequestBody(required = false) AdminMerchantApplicationAuditRequest request) {
    return ApiResponse.ok(service.rejectMerchantApplication(id, request));
  }

  @GetMapping("/merchants/certification-materials")
  ApiResponse<PageResponse<AdminCertificationMaterialView>> certificationMaterials(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(service.certificationMaterials(status, page, pageSize));
  }

  @PostMapping("/merchants/certification-materials/{id}/status")
  ApiResponse<AdminCertificationMaterialView> updateCertificationMaterialStatus(@PathVariable long id, @Valid @RequestBody AdminCertificationMaterialAuditRequest request) {
    return ApiResponse.ok(service.updateCertificationMaterialStatus(id, request));
  }

  @GetMapping("/stores")
  ApiResponse<PageResponse<AdminStoreView>> stores(
      @RequestParam(required = false) Long merchantId,
      @RequestParam(required = false) String businessType,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(service.stores(merchantId, businessType, status, page, pageSize));
  }

  @PostMapping("/stores")
  ApiResponse<AdminStoreView> createStore(@Valid @RequestBody AdminStoreUpsertRequest request) {
    return ApiResponse.ok(service.createStore(request));
  }

  @PutMapping("/stores/{storeId}")
  ApiResponse<AdminStoreView> updateStore(@PathVariable long storeId, @Valid @RequestBody AdminStoreUpsertRequest request) {
    return ApiResponse.ok(service.updateStore(storeId, request));
  }

  @PostMapping("/stores/{storeId}/status")
  ApiResponse<AdminStoreView> updateStoreStatus(@PathVariable long storeId, @Valid @RequestBody AdminStatusRequest request) {
    return ApiResponse.ok(service.updateStoreStatus(storeId, request));
  }

  @PostMapping(value = "/stores/{storeId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<AdminStoreView> uploadStoreCover(@PathVariable long storeId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(service.uploadStoreCover(storeId, file));
  }
}

@RestController
@RequestMapping("/api/app/discovery")
@Validated
class DiscoveryController {
  private final MerchantCatalogService service;

  DiscoveryController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/home")
  ApiResponse<HomeView> home(@RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude) {
    return ApiResponse.ok(service.home(latitude, longitude));
  }

  @GetMapping("/recommendations")
  ApiResponse<PageResponse<ItemCardView>> recommendations(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "12") @Min(1) int pageSize,
      @RequestParam(defaultValue = "personalized") String sort,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude) {
    return ApiResponse.ok(service.recommendations(page, pageSize, sort, latitude, longitude));
  }

  @GetMapping("/modules/{moduleCode}")
  ApiResponse<ModulePageView> module(@PathVariable @NotBlank String moduleCode, @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude) {
    return ApiResponse.ok(service.module(moduleCode, latitude, longitude));
  }

  @GetMapping("/stores/search")
  ApiResponse<PageResponse<StoreCardView>> search(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "12") @Min(1) int pageSize,
      @RequestParam(defaultValue = "default") String sort,
      @RequestParam(required = false) String businessType,
      @RequestParam(required = false) Double latitude,
      @RequestParam(required = false) Double longitude) {
    return ApiResponse.ok(service.search(keyword, page, pageSize, sort, businessType, latitude, longitude));
  }

  @GetMapping("/stores/{storeId}")
  ApiResponse<StoreDetailView> storeDetail(@PathVariable long storeId, @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude) {
    return ApiResponse.ok(service.storeDetail(storeId, latitude, longitude));
  }

  @GetMapping("/stores/{storeId}/items")
  ApiResponse<StoreDetailView> storeItems(@PathVariable long storeId, @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude) {
    return ApiResponse.ok(service.storeDetail(storeId, latitude, longitude));
  }

  @GetMapping("/items/{itemId}")
  ApiResponse<ItemDetailView> itemDetail(@PathVariable long itemId, @RequestParam(required = false) Double latitude, @RequestParam(required = false) Double longitude) {
    return ApiResponse.ok(service.itemDetail(itemId, latitude, longitude));
  }
}

@RestController
@RequestMapping("/api/app/location")
@Validated
class LocationController {
  private final MerchantCatalogService service;

  LocationController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/reverse-geocode")
  ApiResponse<MapDistanceService.ReverseGeocodeResult> reverseGeocode(
      @RequestParam @DecimalMin("-180") @DecimalMax("180") double longitude,
      @RequestParam @DecimalMin("-90") @DecimalMax("90") double latitude) {
    return ApiResponse.ok(service.reverseGeocode(longitude, latitude));
  }
}

@RestController
@RequestMapping({"/api/merchant/trade", "/api/admin/trade"})
@Validated
class MerchantTradeStoreController {
  private final MerchantCatalogService service;

  MerchantTradeStoreController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/stores/{storeId}/takeaway-setting")
  ApiResponse<TakeawaySettingView> getTakeawaySetting(@PathVariable long storeId) {
    return ApiResponse.ok(service.getTakeawaySetting(storeId));
  }

  @PostMapping("/stores/{storeId}/takeaway-setting")
  ApiResponse<TakeawaySettingView> updateTakeawaySetting(@PathVariable long storeId, @Valid @RequestBody TakeawaySettingRequest request) {
    return ApiResponse.ok(service.updateTakeawaySetting(storeId, request));
  }

  @GetMapping("/stores/{storeId}/items")
  ApiResponse<List<MerchantItemView>> listItems(@PathVariable long storeId, @RequestParam(required = false) String status) {
    return ApiResponse.ok(service.listTakeawayItems(storeId, status));
  }

  @PostMapping("/stores/{storeId}/items/{itemId}")
  ApiResponse<MerchantItemView> updateItem(@PathVariable long storeId, @PathVariable long itemId, @Valid @RequestBody MerchantItemUpdateRequest request) {
    return ApiResponse.ok(service.updateTakeawayItem(storeId, itemId, request));
  }

  @PostMapping("/stores/{storeId}/items/{itemId}/status")
  ApiResponse<MerchantItemView> updateItemStatus(@PathVariable long storeId, @PathVariable long itemId, @Valid @RequestBody MerchantItemStatusRequest request) {
    return ApiResponse.ok(service.updateTakeawayItemStatus(storeId, itemId, request));
  }

  @GetMapping("/stores/{storeId}/delivery-rule")
  ApiResponse<DeliveryRuleOpsView> getDeliveryRule(@PathVariable long storeId) {
    return ApiResponse.ok(service.getDeliveryRule(storeId));
  }

  @PostMapping("/stores/{storeId}/delivery-rule")
  ApiResponse<DeliveryRuleOpsView> updateDeliveryRule(@PathVariable long storeId, @Valid @RequestBody DeliveryRuleUpdateRequest request) {
    return ApiResponse.ok(service.updateDeliveryRule(storeId, request));
  }
}

@RestController
@RequestMapping("/internal")
@Validated
class MerchantCatalogInternalController {
  private final MerchantCatalogService service;

  MerchantCatalogInternalController(MerchantCatalogService service) {
    this.service = service;
  }

  @GetMapping("/stores/{storeId}/snapshot")
  ApiResponse<StoreSnapshotView> storeSnapshot(@PathVariable long storeId) {
    return ApiResponse.ok(service.storeSnapshot(storeId));
  }

  @GetMapping("/merchants/by-account/{accountId}")
  ApiResponse<MerchantAccountView> merchantByAccount(@PathVariable long accountId) {
    return ApiResponse.ok(service.merchantByAccount(accountId));
  }

  @GetMapping("/catalog/items/{itemId}/snapshot")
  ApiResponse<CatalogItemSnapshotView> itemSnapshot(@PathVariable long itemId) {
    return ApiResponse.ok(service.itemSnapshot(itemId));
  }

  @GetMapping("/stores/{storeId}/fulfillment-rules")
  ApiResponse<FulfillmentRulesView> fulfillmentRules(@PathVariable long storeId) {
    return ApiResponse.ok(service.fulfillmentRules(storeId));
  }

  @PostMapping("/catalog/checkout-quote")
  ApiResponse<CheckoutQuoteView> checkoutQuote(@Valid @RequestBody CheckoutQuoteRequest request) {
    return ApiResponse.ok(service.checkoutQuote(request));
  }

  @PostMapping("/inventory/deduct")
  ApiResponse<InventoryResultView> deductInventory(@Valid @RequestBody InventoryDeductRequest request, @RequestHeader(value = "X-Caller-Service", required = false) String callerService, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ApiResponse.ok(service.deductInventory(request, callerService, idempotencyKey));
  }

  @PostMapping("/inventory/restore")
  ApiResponse<InventoryResultView> restoreInventory(@Valid @RequestBody InventoryRestoreRequest request, @RequestHeader(value = "X-Caller-Service", required = false) String callerService, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    return ApiResponse.ok(service.restoreInventory(request, callerService, idempotencyKey));
  }

  @GetMapping("/metrics/platform/merchants")
  ApiResponse<PlatformMerchantMetricsView> platformMerchantMetrics() {
    return ApiResponse.ok(service.platformMerchantMetrics());
  }
}
