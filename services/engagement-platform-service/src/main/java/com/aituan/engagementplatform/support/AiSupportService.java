package com.aituan.engagementplatform.support;

import com.aituan.engagementplatform.ai.AiAgentService;
import com.aituan.common.security.CurrentUser;
import org.springframework.stereotype.Service;

@Service
class AiSupportService {
  private final AiAgentService aiAgentService;

  AiSupportService(AiAgentService aiAgentService) {
    this.aiAgentService = aiAgentService;
  }

  String reply(CurrentUser currentUser, SupportRepository.SessionRow session, String content) {
    return aiAgentService.platformSupportReply(currentUser, session.id(), session.relatedOrderId(), content);
  }

  String localKeywordReply(String content) {
    return aiAgentService.localKeywordReply(content);
  }
}
