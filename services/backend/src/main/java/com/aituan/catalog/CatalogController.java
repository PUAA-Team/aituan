package com.aituan.catalog;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/merchant/catalog")
@Validated
class MerchantCatalogController {
  private final CatalogService catalogService;

  MerchantCatalogController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/items")
  ApiResponse<List<CatalogItemView>> items(
      @RequestParam(required = false) String businessType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword) {
    return ApiResponse.ok(catalogService.merchantItems(businessType, status, keyword));
  }

  @PostMapping("/items")
  ApiResponse<CatalogItemView> createItem(@Valid @RequestBody CatalogItemUpsertRequest request) {
    return ApiResponse.ok(catalogService.createMerchantItem(request));
  }

  @GetMapping("/items/{itemId}")
  ApiResponse<CatalogItemView> item(@PathVariable long itemId) {
    return ApiResponse.ok(catalogService.item(itemId));
  }

  @PutMapping("/items/{itemId}")
  ApiResponse<CatalogItemView> updateItem(@PathVariable long itemId, @Valid @RequestBody CatalogItemUpsertRequest request) {
    return ApiResponse.ok(catalogService.updateItem(itemId, request));
  }

  @PostMapping("/items/{itemId}/status")
  ApiResponse<CatalogItemView> updateItemStatus(@PathVariable long itemId, @Valid @RequestBody CatalogItemStatusRequest request) {
    return ApiResponse.ok(catalogService.updateItemStatus(itemId, request));
  }

  @PostMapping(value = "/items/{itemId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<CatalogItemView> uploadItemCover(@PathVariable long itemId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(catalogService.uploadItemCover(itemId, file));
  }

  @GetMapping("/categories")
  ApiResponse<List<CatalogCategoryView>> categories(@RequestParam(required = false) String businessType) {
    return ApiResponse.ok(catalogService.merchantCategories(businessType));
  }

  @PostMapping("/categories")
  ApiResponse<CatalogCategoryView> createCategory(@Valid @RequestBody CatalogCategoryUpsertRequest request) {
    return ApiResponse.ok(catalogService.createMerchantCategory(request));
  }

  @PutMapping("/categories/{categoryId}")
  ApiResponse<CatalogCategoryView> updateCategory(@PathVariable long categoryId, @Valid @RequestBody CatalogCategoryUpsertRequest request) {
    return ApiResponse.ok(catalogService.updateCategory(categoryId, request));
  }
}

@RestController
@RequestMapping("/api/admin/catalog")
@Validated
class AdminCatalogController {
  private final CatalogService catalogService;

  AdminCatalogController(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @GetMapping("/items")
  ApiResponse<PageResponse<CatalogItemView>> items(
      @RequestParam(required = false) Long storeId,
      @RequestParam(required = false) String businessType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(catalogService.adminItems(storeId, businessType, status, keyword, page, pageSize));
  }

  @PostMapping("/items")
  ApiResponse<CatalogItemView> createItem(@Valid @RequestBody CatalogItemUpsertRequest request) {
    return ApiResponse.ok(catalogService.createAdminItem(request));
  }

  @GetMapping("/items/{itemId}")
  ApiResponse<CatalogItemView> item(@PathVariable long itemId) {
    return ApiResponse.ok(catalogService.item(itemId));
  }

  @PutMapping("/items/{itemId}")
  ApiResponse<CatalogItemView> updateItem(@PathVariable long itemId, @Valid @RequestBody CatalogItemUpsertRequest request) {
    return ApiResponse.ok(catalogService.updateItem(itemId, request));
  }

  @PostMapping("/items/{itemId}/status")
  ApiResponse<CatalogItemView> updateItemStatus(@PathVariable long itemId, @Valid @RequestBody CatalogItemStatusRequest request) {
    return ApiResponse.ok(catalogService.updateItemStatus(itemId, request));
  }

  @PostMapping(value = "/items/{itemId}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<CatalogItemView> uploadItemCover(@PathVariable long itemId, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(catalogService.uploadItemCover(itemId, file));
  }

  @GetMapping("/categories")
  ApiResponse<List<CatalogCategoryView>> categories(@RequestParam(required = false) Long storeId, @RequestParam(required = false) String businessType) {
    return ApiResponse.ok(catalogService.adminCategories(storeId, businessType));
  }

  @PostMapping("/categories")
  ApiResponse<CatalogCategoryView> createCategory(@Valid @RequestBody CatalogCategoryUpsertRequest request) {
    return ApiResponse.ok(catalogService.createAdminCategory(request));
  }

  @PutMapping("/categories/{categoryId}")
  ApiResponse<CatalogCategoryView> updateCategory(@PathVariable long categoryId, @Valid @RequestBody CatalogCategoryUpsertRequest request) {
    return ApiResponse.ok(catalogService.updateCategory(categoryId, request));
  }
}
