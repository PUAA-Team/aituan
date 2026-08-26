package com.aituan.complaint;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

record ComplaintCreateRequest(
    Long orderId,
    @NotBlank String category,
    @NotBlank String title,
    @NotBlank String detail,
    List<String> evidenceUrls) {}

record ComplaintActionRequest(String remark) {}

record ComplaintSupplementRequest(@NotBlank String content) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ComplaintView(
    Long id,
    String ticketNo,
    Long orderId,
    String orderNo,
    Long storeId,
    String storeName,
    Long merchantId,
    String category,
    String title,
    String detail,
    List<String> evidenceUrls,
    String status,
    String userMaskedNickname,
    Long acceptedBy,
    LocalDateTime acceptedAt,
    Long resolvedBy,
    LocalDateTime resolvedAt,
    LocalDateTime closedAt,
    LocalDateTime createdAt) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ComplaintLogView(
    Long id,
    String action,
    String operatorType,
    Long operatorId,
    String remark,
    LocalDateTime createdAt) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record ComplaintDetailView(
    ComplaintView complaint,
    List<ComplaintLogView> logs) {}
