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
@RequestMapping("/api/app/support")
@Validated
class SupportController {
  private final SupportService supportService;

  SupportController(SupportService supportService) {
    this.supportService = supportService;
  }

  @GetMapping("/sessions")
  ApiResponse<PageResponse<SupportSessionView>> sessions(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) int pageSize) {
    return ApiResponse.ok(supportService.myUserSessions(status, page, pageSize));
  }

  @PostMapping("/sessions")
  ApiResponse<SupportSessionView> create(@Valid @RequestBody SupportSessionCreateRequest request) {
    return ApiResponse.ok(supportService.createUserSession(request));
  }

  @GetMapping("/sessions/{id}")
  ApiResponse<SupportSessionDetailView> detail(@PathVariable long id) {
    return ApiResponse.ok(supportService.userSessionDetail(id));
  }

  @PostMapping("/sessions/{id}/messages")
  ApiResponse<SupportMessageView> send(@PathVariable long id, @Valid @RequestBody SupportMessageCreateRequest request) {
    return ApiResponse.ok(supportService.userSendMessage(id, request));
  }

  @PostMapping("/sessions/{id}/handoff")
  ApiResponse<SupportSessionView> handoff(@PathVariable long id) {
    return ApiResponse.ok(supportService.userHandoffToHuman(id));
  }

  @PostMapping("/sessions/{id}/platform-intervention")
  ApiResponse<SupportSessionView> requestPlatformIntervention(@PathVariable long id) {
    return ApiResponse.ok(supportService.userRequestPlatformIntervention(id));
  }

  @PostMapping("/sessions/{id}/close")
  ApiResponse<SupportSessionView> close(@PathVariable long id, @RequestBody(required = false) SupportSessionCloseRequest request) {
    return ApiResponse.ok(supportService.userCloseSession(id, request));
  }
}

