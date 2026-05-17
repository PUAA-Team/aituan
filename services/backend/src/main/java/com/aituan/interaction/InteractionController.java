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
@RequestMapping("/api/app/interaction")
@Validated
class InteractionController {
  private final InteractionService interactionService;

  InteractionController(InteractionService interactionService) {
    this.interactionService = interactionService;
  }

  @GetMapping("/reviews")
  ApiResponse<PageResponse<ReviewView>> reviews(
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(interactionService.reviews(page, pageSize));
  }

  @GetMapping("/orders/{orderId}/review")
  ApiResponse<ReviewView> review(@PathVariable long orderId) {
    return ApiResponse.ok(interactionService.review(orderId));
  }

  @PostMapping("/orders/{orderId}/review")
  ApiResponse<ReviewView> submitReview(@PathVariable long orderId, @Valid @RequestBody ReviewCreateRequest request) {
    return ApiResponse.ok(interactionService.submitReview(orderId, request));
  }
}
