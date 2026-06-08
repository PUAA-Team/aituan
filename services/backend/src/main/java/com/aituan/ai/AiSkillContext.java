package com.aituan.ai;

import com.aituan.common.security.CurrentUser;

public record AiSkillContext(
    CurrentUser currentUser,
    String message,
    Long supportSessionId,
    Long relatedOrderId,
    String channel) {
  String normalizedMessage() {
    return message == null ? "" : message.trim();
  }
}
