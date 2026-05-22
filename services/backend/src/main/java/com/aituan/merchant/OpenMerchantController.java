package com.aituan.merchant;

import com.aituan.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/open/merchant")
@Validated
class OpenMerchantController {
  private final MerchantService merchantService;

  OpenMerchantController(MerchantService merchantService) {
    this.merchantService = merchantService;
  }

  @PostMapping("/applications")
  ApiResponse<MerchantApplicationView> submitApplication(@Valid @RequestBody MerchantApplicationSubmitRequest request) {
    return ApiResponse.ok(merchantService.submitApplication(request));
  }
}
