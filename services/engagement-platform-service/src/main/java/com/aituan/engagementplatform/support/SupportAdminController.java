package com.aituan.engagementplatform.support;

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
@RequestMapping("/api/admin/governance/support/sessions")
@Validated
class SupportAdminController {
  private final SupportService supportService;

  SupportAdminController(SupportService supportService) {
    this.supportService = supportService;
  }

  @GetMapping
  ApiResponse<PageResponse<SupportSessionView>> sessions(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(supportService.adminPlatformSessions(status, page, pageSize));
  }

  @GetMapping("/{id}")
  ApiResponse<SupportSessionDetailView> detail(@PathVariable long id) {
    return ApiResponse.ok(supportService.adminPlatformSessionDetail(id));
  }

  @PostMapping("/{id}/messages")
  ApiResponse<SupportMessageView> send(@PathVariable long id, @Valid @RequestBody SupportMessageCreateRequest request) {
    return ApiResponse.ok(supportService.adminSendPlatformMessage(id, request));
  }
}

