package com.aituan.discovery;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/discovery")
@Validated
class DiscoveryController {
  private final DiscoveryService discoveryService;

  DiscoveryController(DiscoveryService discoveryService) {
    this.discoveryService = discoveryService;
  }

  @GetMapping("/home")
  ApiResponse<HomeView> home() {
    return ApiResponse.ok(discoveryService.home());
  }

  @GetMapping("/recommendations")
  ApiResponse<PageResponse<ItemCardView>> recommendations(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "12") @Min(1) int pageSize) {
    return ApiResponse.ok(discoveryService.recommendations(page, pageSize));
  }

  @GetMapping("/modules/{moduleCode}")
  ApiResponse<ModulePageView> module(@PathVariable @NotBlank String moduleCode) {
    return ApiResponse.ok(discoveryService.module(moduleCode));
  }

  @GetMapping("/stores/search")
  ApiResponse<PageResponse<StoreCardView>> search(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "12") @Min(1) int pageSize) {
    return ApiResponse.ok(discoveryService.search(keyword, page, pageSize));
  }

  @GetMapping("/stores/{storeId}")
  ApiResponse<StoreDetailView> storeDetail(@PathVariable long storeId) {
    return ApiResponse.ok(discoveryService.storeDetail(storeId));
  }

  @GetMapping("/stores/{storeId}/items")
  ApiResponse<StoreDetailView> storeItems(@PathVariable long storeId) {
    return ApiResponse.ok(discoveryService.storeDetail(storeId));
  }

  @GetMapping("/items/{itemId}")
  ApiResponse<ItemDetailView> itemDetail(@PathVariable long itemId) {
    return ApiResponse.ok(discoveryService.itemDetail(itemId));
  }
}
