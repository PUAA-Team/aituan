package com.aituan.engagementplatform.ai;

import java.util.List;

record AiSkillResult(
    String name,
    String title,
    String content,
    List<AiAssistantCard> cards,
    List<AiAssistantAction> actions) {
  static AiSkillResult text(String name, String title, String content) {
    return new AiSkillResult(name, title, content, List.of(), List.of());
  }
}

