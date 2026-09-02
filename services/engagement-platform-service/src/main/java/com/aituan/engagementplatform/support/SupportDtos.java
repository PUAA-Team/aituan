package com.aituan.engagementplatform.support;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

record SupportSessionCreateRequest(
    Long storeId,
    String topic,
    Long relatedOrderId) {}

record SupportMessageCreateRequest(
    @NotBlank String content) {}

record SupportSessionCloseRequest(String reason) {}

record SupportAutoReplyRuleUpsertRequest(
    @NotBlank String keywords,
    @NotBlank String replyContent,
    Boolean enabled) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record SupportSessionView(
    Long id,
    String sessionNo,
    Long storeId,
    String storeName,
    String topic,
    String status,
    Long relatedOrderId,
    String relatedOrderNo,
    String lastMessage,
    LocalDateTime lastMessageAt,
    int unreadCount,
    String userMaskedNickname,
    LocalDateTime createdAt,
    LocalDateTime closedAt,
    String closeReason,
    String serviceScope,
    String assistantMode,
    String platformInterventionStatus,
    LocalDateTime humanRequestedAt,
    LocalDateTime platformIntervenedAt) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record SupportMessageView(
    Long id,
    Long sessionId,
    String senderType,
    Long senderId,
    String content,
    String messageKind,
    LocalDateTime createdAt) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record SupportSessionDetailView(
    SupportSessionView session,
    List<SupportMessageView> messages) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record SupportAutoReplyRuleView(
    Long id,
    Long merchantId,
    String keywords,
    String replyContent,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
