package com.aituan.support;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/ops/sessions")
@Validated
class SupportMerchantController {
  private final SupportService supportService;

  SupportMerchantController(SupportService supportService) {
    this.supportService = supportService;
  }

  @GetMapping
  ApiResponse<PageResponse<SupportSessionView>> sessions(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(supportService.merchantSessions(status, page, pageSize));
  }

  @GetMapping("/{id}")
  ApiResponse<SupportSessionDetailView> detail(@PathVariable long id) {
    return ApiResponse.ok(supportService.merchantSessionDetail(id));
  }

  @PostMapping("/{id}/messages")
  ApiResponse<SupportMessageView> send(@PathVariable long id, @Valid @RequestBody SupportMessageCreateRequest request) {
    return ApiResponse.ok(supportService.merchantSendMessage(id, request));
  }

  @PostMapping("/{id}/close")
  ApiResponse<SupportSessionView> close(@PathVariable long id, @RequestBody(required = false) SupportSessionCloseRequest request) {
    return ApiResponse.ok(supportService.merchantCloseSession(id, request));
  }

  @GetMapping("/templates")
  ApiResponse<List<String>> templates() {
    return ApiResponse.ok(supportService.merchantTemplates());
  }
}
