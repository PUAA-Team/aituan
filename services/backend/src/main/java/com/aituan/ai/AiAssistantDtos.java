package com.aituan.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

record AiAssistantMessageRequest(@NotBlank String content, String conversationId) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record AiAssistantResponse(
    String conversationId,
    String reply,
    List<AiAssistantCard> cards,
    List<AiAssistantAction> quickActions,
    List<String> usedSkills,
    boolean modelUsed) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record AiAssistantCard(
    String type,
    String title,
    String content,
    String actionLabel,
    String route,
    Map<String, Object> payload) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record AiAssistantAction(
    String label,
    String message,
    String route,
    Map<String, Object> payload) {}
