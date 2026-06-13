package com.aituan.message;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

record MessageView(
    Long id,
    String type,
    String title,
    String content,
    String badgeText,
    boolean unread,
    Long relatedOrderId,
    String relatedTargetType,
    Long relatedTargetId,
    LocalDateTime createdAt) {}

record MessageBatchRequest(
    @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> messageIds) {}
