package com.aituan.merchant;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/merchant")
@Validated
class MerchantController {
  private final MerchantService merchantService;

  MerchantController(MerchantService merchantService) {
    this.merchantService = merchantService;
  }

  @GetMapping("/profile/me")
  ApiResponse<MerchantProfileView> profile() {
    return ApiResponse.ok(merchantService.profile());
  }

  @PutMapping("/profile/me")
  ApiResponse<MerchantProfileView> updateProfile(@Valid @RequestBody MerchantProfileUpdateRequest request) {
    return ApiResponse.ok(merchantService.updateProfile(request));
  }

  @GetMapping("/stores/current")
  ApiResponse<MerchantStoreView> currentStore() {
    return ApiResponse.ok(merchantService.currentStore());
  }

  @PutMapping("/stores/current")
  ApiResponse<MerchantStoreView> updateCurrentStore(@Valid @RequestBody MerchantStoreUpdateRequest request) {
    return ApiResponse.ok(merchantService.updateCurrentStore(request));
  }

  @PostMapping(value = "/stores/current/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ApiResponse<MerchantStoreView> uploadCurrentStoreCover(@RequestParam("file") MultipartFile file) {
    return ApiResponse.ok(merchantService.uploadCurrentStoreCover(file));
  }
}
