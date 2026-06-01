package com.aituan.interaction;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/ops/reviews")
@Validated
class InteractionMerchantController {
  private final InteractionService interactionService;

  InteractionMerchantController(InteractionService interactionService) {
    this.interactionService = interactionService;
  }

  @GetMapping
  ApiResponse<PageResponse<ReviewView>> reviews(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean replied,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(interactionService.merchantReviews(status, replied, page, pageSize));
  }

  @GetMapping("/{id}")
  ApiResponse<ReviewView> detail(@PathVariable long id) {
    return ApiResponse.ok(interactionService.merchantReviewDetail(id));
  }

  @PostMapping("/{id}/reply")
  ApiResponse<ReviewView> reply(@PathVariable long id, @Valid @RequestBody MerchantReviewReplyRequest request) {
    return ApiResponse.ok(interactionService.merchantReply(id, request));
  }
}
