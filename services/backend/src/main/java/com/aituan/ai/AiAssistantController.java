package com.aituan.ai;

import com.aituan.common.api.ApiResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/app/ai/assistant")
class AiAssistantController {
  private final AiAgentService aiAgentService;

  AiAssistantController(AiAgentService aiAgentService) {
    this.aiAgentService = aiAgentService;
  }

  @PostMapping("/message")
  ApiResponse<AiAssistantResponse> message(@Valid @RequestBody AiAssistantMessageRequest request) {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.USER) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return ApiResponse.ok(aiAgentService.userAssistant(current, request));
  }
}
