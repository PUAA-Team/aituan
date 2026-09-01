package com.aituan.engagementplatform.interaction;

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
@RequestMapping("/api/app/interaction")
@Validated
class InteractionController {
  private final InteractionService interactionService;

  InteractionController(InteractionService interactionService) {
    this.interactionService = interactionService;
  }

  @GetMapping("/reviews/me")
  ApiResponse<PageResponse<ReviewView>> myReviews(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(interactionService.myReviews(status, page, pageSize));
  }

  @GetMapping("/reviews/{id}")
  ApiResponse<ReviewView> reviewDetail(@PathVariable long id) {
    return ApiResponse.ok(interactionService.reviewDetail(id));
  }

  @GetMapping("/orders/{orderId}/review")
  ApiResponse<ReviewView> reviewByOrder(@PathVariable long orderId) {
    return ApiResponse.ok(interactionService.reviewByOrder(orderId));
  }

  @GetMapping("/stores/{storeId}/reviews")
  ApiResponse<PageResponse<ReviewView>> storeReviews(
      @PathVariable long storeId,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(interactionService.storeReviews(storeId, page, pageSize));
  }

  @PostMapping("/orders/{orderId}/review")
  ApiResponse<ReviewView> submitReview(@PathVariable long orderId, @Valid @RequestBody ReviewCreateRequest request) {
    return ApiResponse.ok(interactionService.submitReview(orderId, request));
  }

  @PostMapping("/reviews/{id}/helpful")
  ApiResponse<ReviewHelpfulView> toggleHelpful(@PathVariable long id) {
    return ApiResponse.ok(interactionService.toggleHelpful(id));
  }

  @PostMapping("/reviews/{id}/report")
  ApiResponse<ReviewReportView> reportReview(@PathVariable long id, @Valid @RequestBody ReviewReportRequest request) {
    return ApiResponse.ok(interactionService.reportReview(id, request));
  }
}
