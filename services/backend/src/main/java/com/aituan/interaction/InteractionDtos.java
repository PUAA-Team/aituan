package com.aituan.interaction;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

record ReviewCreateRequest(
    @Min(1) @Max(5) Integer rating,
    @NotBlank String content,
    List<@NotBlank String> labels) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ReviewView(
    Long id,
    Long orderId,
    String orderNo,
    String orderTitle,
    Long storeId,
    String storeName,
    Integer rating,
    String content,
    List<String> labels,
    String status,
    boolean replied,
    String replyContent,
    LocalDateTime repliedAt,
    LocalDateTime createdAt) {}
