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
@RequestMapping("/api/admin/governance/reviews")
@Validated
class InteractionAdminController {
  private final InteractionService interactionService;

  InteractionAdminController(InteractionService interactionService) {
    this.interactionService = interactionService;
  }

  @GetMapping
  ApiResponse<PageResponse<ReviewView>> reviews(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean reported,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(interactionService.adminReviews(status, reported, page, pageSize));
  }

  @PostMapping("/{id}/audit")
  ApiResponse<ReviewView> audit(@PathVariable long id, @Valid @RequestBody AdminReviewAuditRequest request) {
    return ApiResponse.ok(interactionService.adminAudit(id, request));
  }
}
