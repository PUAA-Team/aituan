package com.aituan.complaint;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ComplaintService {
  private static final List<String> ALLOWED_CATEGORIES = List.of("service", "quality", "delivery", "other");
  private static final List<String> ALLOWED_STATUS = List.of("pending", "processing", "resolved", "closed");

  private final ComplaintRepository complaintRepository;

  ComplaintService(ComplaintRepository complaintRepository) {
    this.complaintRepository = complaintRepository;
  }

  // ============ 用户端 ============

  PageResponse<ComplaintView> myTickets(String statusFilter, int page, int pageSize) {
    long userId = requireUser().userId();
    String normalized = normalizeStatus(statusFilter);
    long total = complaintRepository.countUserTickets(userId, normalized);
    List<ComplaintView> list = complaintRepository.listUserTickets(userId, normalized, (page - 1) * pageSize, pageSize)
        .stream().map(row -> toView(row, false)).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  ComplaintDetailView myTicketDetail(long id) {
    long userId = requireUser().userId();
    ComplaintRepository.TicketRow row = complaintRepository.findById(id)
        .filter(r -> r.userId() == userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    List<ComplaintLogView> logs = complaintRepository.listLogs(id).stream().map(this::toLogView).toList();
    return new ComplaintDetailView(toView(row, false), logs);
  }

  @Transactional
  ComplaintView submit(ComplaintCreateRequest request) {
    CurrentUser current = requireUser();
    String category = normalizeCategory(request.category());
    String title = require(request.title(), "标题");
    String detail = require(request.detail(), "详细描述");
    Long orderId = request.orderId();
    Long storeId = null;
    Long merchantId = null;
    String evidence = joinList(request.evidenceUrls());
    if (orderId != null) {
      ComplaintRepository.OrderRefRow order = complaintRepository.findOrderForComplaint(current.userId(), orderId)
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在或无权访问"));
      storeId = order.storeId();
      merchantId = complaintRepository.findStoreMerchantId(storeId).orElse(null);
    }
    String ticketNo = "CP" + System.currentTimeMillis() + "-" + current.userId();
    Long id = complaintRepository.insertTicket(ticketNo, current.userId(), orderId, storeId, merchantId,
        category, title, detail, evidence);
    complaintRepository.insertLog(id, "submit", "user", current.userId(), null);
    complaintRepository.insertSysAuditLog("user", current.accountId(), "complaint_submit", "complaint", id,
        "用户提交投诉：" + title);
    return complaintRepository.findById(id).map(r -> toView(r, false))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  // ============ 后台端 ============

  PageResponse<ComplaintView> adminTickets(String statusFilter, String categoryFilter, int page, int pageSize) {
    requireAdmin();
    String s = normalizeStatus(statusFilter);
    String c = categoryFilter == null ? null : normalizeCategory(categoryFilter);
    long total = complaintRepository.countAdminTickets(s, c);
    List<ComplaintView> list = complaintRepository.listAdminTickets(s, c, (page - 1) * pageSize, pageSize)
        .stream().map(row -> toView(row, true)).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  ComplaintView accept(long id, ComplaintActionRequest request) {
    return advance(id, "accept", request, "pending", "processing", "complaint_accept", "受理投诉");
  }

  @Transactional
  ComplaintView resolve(long id, ComplaintActionRequest request) {
    return advance(id, "resolve", request, "processing", "resolved", "complaint_resolve", "处理完成");
  }

  @Transactional
  ComplaintView close(long id, ComplaintActionRequest request) {
    return advance(id, "close", request, null, "closed", "complaint_close", "关闭工单");
  }

  private ComplaintView advance(long id, String action, ComplaintActionRequest request,
                                String expectedFrom, String toStatus,
                                String auditAction, String auditDetailPrefix) {
    CurrentUser admin = requireAdmin();
    ComplaintRepository.TicketRow row = complaintRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if ("closed".equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "工单已关闭");
    }
    if (expectedFrom != null && !expectedFrom.equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前状态不允许该操作");
    }
    switch (action) {
      case "accept" -> complaintRepository.accept(id, admin.accountId());
      case "resolve" -> complaintRepository.resolve(id, admin.accountId());
      case "close" -> complaintRepository.close(id);
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST);
    }
    String remark = request == null ? null : request.remark();
    complaintRepository.insertLog(id, action, "admin", admin.accountId(), remark);
    complaintRepository.insertSysAuditLog("admin", admin.accountId(), auditAction, "complaint", id,
        auditDetailPrefix + " #" + row.ticketNo());
    return complaintRepository.findById(id).map(r -> toView(r, true))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  // ============ 视图映射 ============

  private ComplaintView toView(ComplaintRepository.TicketRow row, boolean adminScope) {
    return new ComplaintView(
        row.id(), row.ticketNo(), row.orderId(), row.orderNo(),
        row.storeId(), row.storeName(),
        row.category(), row.title(), row.detail(),
        splitList(row.evidenceUrls()),
        row.status(),
        adminScope ? maskNickname(row.userNickname()) : null,
        row.acceptedBy(), row.acceptedAt(),
        row.resolvedBy(), row.resolvedAt(),
        row.closedAt(), row.createdAt());
  }

  private ComplaintLogView toLogView(ComplaintRepository.LogRow row) {
    return new ComplaintLogView(row.id(), row.action(), row.operatorType(), row.operatorId(), row.remark(), row.createdAt());
  }

  // ============ 鉴权工具 ============

  private CurrentUser requireUser() {
    CurrentUser c = CurrentUserContext.required();
    if (c.accountType() != AccountType.USER) throw new BusinessException(ErrorCode.FORBIDDEN);
    return c;
  }

  private CurrentUser requireAdmin() {
    CurrentUser c = CurrentUserContext.required();
    if (c.accountType() != AccountType.ADMIN) throw new BusinessException(ErrorCode.FORBIDDEN);
    return c;
  }

  // ============ 字符串工具 ============

  private String normalizeStatus(String value) {
    if (value == null || value.isBlank()) return null;
    String v = value.trim().toLowerCase();
    if (!ALLOWED_STATUS.contains(v)) throw new BusinessException(ErrorCode.BAD_REQUEST, "状态不正确");
    return v;
  }

  private String normalizeCategory(String value) {
    String v = value == null ? "" : value.trim().toLowerCase();
    if (!ALLOWED_CATEGORIES.contains(v)) throw new BusinessException(ErrorCode.BAD_REQUEST, "分类不正确");
    return v;
  }

  private String require(String value, String label) {
    if (value == null || value.trim().isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, label + "不能为空");
    }
    return value.trim();
  }

  private List<String> splitList(String value) {
    if (value == null || value.isBlank()) return List.of();
    return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  private String joinList(List<String> values) {
    if (values == null || values.isEmpty()) return null;
    return values.stream().map(v -> v == null ? "" : v.trim()).filter(s -> !s.isEmpty())
        .distinct().reduce((a, b) -> a + "," + b).orElse(null);
  }

  private String maskNickname(String nickname) {
    if (nickname == null || nickname.isBlank()) return "匿名用户";
    if (nickname.length() <= 2) return nickname.charAt(0) + "*";
    return nickname.charAt(0) + "***" + nickname.charAt(nickname.length() - 1);
  }
}
