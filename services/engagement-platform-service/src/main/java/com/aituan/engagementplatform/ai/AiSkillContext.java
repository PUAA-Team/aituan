package com.aituan.engagementplatform.ai;

import com.aituan.common.security.CurrentUser;

public record AiSkillContext(
    CurrentUser currentUser,
    String message,
    String memoryText,
    Long conversationId,
    Long supportSessionId,
    Long relatedOrderId,
    String channel) {
  String normalizedMessage() {
    return message == null ? "" : message.trim();
  }

  public String memoryText() {
    return memoryText == null ? "" : memoryText.trim();
  }

  String searchableText() {
    String current = normalizedMessage();
    String memory = memoryText();
    if (memory.isBlank()) return current;
    return current + "\n" + memory;
  }
}

