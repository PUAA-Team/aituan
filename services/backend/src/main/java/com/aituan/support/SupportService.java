package com.aituan.support;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SupportService {
  private static final int DEFAULT_MESSAGE_LIMIT = 50;

  private final SupportRepository supportRepository;
  private final JdbcTemplate jdbcTemplate;

  SupportService(SupportRepository supportRepository, JdbcTemplate jdbcTemplate) {
    this.supportRepository = supportRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  // ============ 用户端 ============

  PageResponse<SupportSessionView> myUserSessions(String statusFilter, int page, int pageSize) {
    long userId = requireUser().userId();
    String normalized = normalizeStatus(statusFilter);
    long total = supportRepository.countUserSessions(userId, normalized);
    List<SupportSessionView> list = supportRepository.listUserSessions(userId, normalized, (page - 1) * pageSize, pageSize)
        .stream()
        .map(row -> toSessionView(row, "user"))
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  @Transactional
  SupportSessionView createUserSession(SupportSessionCreateRequest request) {
    CurrentUser current = requireUser();
    boolean platformSession = request.storeId() == null || request.storeId() <= 0;
    long storeId = platformSession ? 0L : request.storeId();
    long merchantId = platformSession ? 0L : supportRepository.findStoreMerchantId(storeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "门店不存在"));
    String topic = platformSession ? "平台客服" : "商家客服咨询";
    if (request.relatedOrderId() != null) topic = topic + " · 订单咨询";
    String sessionNo = "SS" + System.currentTimeMillis() + "-" + current.userId();
    Long id = supportRepository.insertSession(sessionNo, current.userId(), storeId, merchantId, topic, request.relatedOrderId());
    if (platformSession) {
      Long welcomeId = supportRepository.insertMessage(id, "platform", 0L,
          "您好，我是平台客服助手。订单、投诉、退款相关问题都可以在这里描述，我会先帮您整理并转交平台处理。", "ai");
      supportRepository.updateLastMessage(id, welcomeId, "platform");
    }
    supportRepository.insertSysAuditLog("user", current.accountId(), "support_session_create", "support_session", id,
        "用户发起咨询：" + topic);
    return supportRepository.findById(id)
        .map(row -> toSessionView(row, "user"))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  SupportSessionDetailView userSessionDetail(long sessionId) {
    long userId = requireUser().userId();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> r.userId() == userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    supportRepository.clearUnread(sessionId, "user");
    SupportRepository.SessionRow refreshed = supportRepository.findById(sessionId).orElse(row);
    List<SupportMessageView> messages = supportRepository.listMessages(sessionId, 0, DEFAULT_MESSAGE_LIMIT)
        .stream().map(this::toMessageView).toList();
    return new SupportSessionDetailView(toSessionView(refreshed, "user"), messages);
  }

  @Transactional
  SupportMessageView userSendMessage(long sessionId, SupportMessageCreateRequest request) {
    CurrentUser current = requireUser();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> r.userId() == current.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"open".equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "会话已关闭");
    }
    Long messageId = supportRepository.insertMessage(sessionId, "user", current.userId(), request.content().trim(), "text");
    supportRepository.updateLastMessage(sessionId, messageId, "user");
    autoReplyIfMatched(row, request.content().trim());
    return supportRepository.findMessageById(messageId)
        .map(this::toMessageView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  SupportSessionView userCloseSession(long sessionId, SupportSessionCloseRequest request) {
    CurrentUser current = requireUser();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> r.userId() == current.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if ("closed".equals(row.status())) {
      return toSessionView(row, "user");
    }
    String reason = request == null ? null : request.reason();
    supportRepository.closeSession(sessionId, "user", current.userId(), reason);
    return supportRepository.findById(sessionId)
        .map(r -> toSessionView(r, "user"))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  // ============ 商家端 ============

  PageResponse<SupportSessionView> merchantSessions(String statusFilter, int page, int pageSize) {
    long merchantId = currentMerchantId();
    String normalized = normalizeStatus(statusFilter);
    long total = supportRepository.countMerchantSessions(merchantId, normalized);
    List<SupportSessionView> list = supportRepository.listMerchantSessions(merchantId, normalized, (page - 1) * pageSize, pageSize)
        .stream().map(row -> toSessionView(row, "merchant")).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  SupportSessionDetailView merchantSessionDetail(long sessionId) {
    long merchantId = currentMerchantId();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> r.merchantId() == merchantId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    supportRepository.clearUnread(sessionId, "merchant");
    SupportRepository.SessionRow refreshed = supportRepository.findById(sessionId).orElse(row);
    List<SupportMessageView> messages = supportRepository.listMessages(sessionId, 0, DEFAULT_MESSAGE_LIMIT)
        .stream().map(this::toMessageView).toList();
    return new SupportSessionDetailView(toSessionView(refreshed, "merchant"), messages);
  }

  @Transactional
  SupportMessageView merchantSendMessage(long sessionId, SupportMessageCreateRequest request) {
    CurrentUser current = requireMerchant();
    long merchantId = currentMerchantId();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> r.merchantId() == merchantId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"open".equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "会话已关闭");
    }
    Long messageId = supportRepository.insertMessage(sessionId, "merchant", current.accountId(), request.content().trim(), "text");
    supportRepository.updateLastMessage(sessionId, messageId, "merchant");
    return supportRepository.findMessageById(messageId)
        .map(this::toMessageView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  SupportSessionView merchantCloseSession(long sessionId, SupportSessionCloseRequest request) {
    CurrentUser current = requireMerchant();
    long merchantId = currentMerchantId();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> r.merchantId() == merchantId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if ("closed".equals(row.status())) {
      return toSessionView(row, "merchant");
    }
    String reason = request == null ? null : request.reason();
    supportRepository.closeSession(sessionId, "merchant", current.accountId(), reason);
    return supportRepository.findById(sessionId)
        .map(r -> toSessionView(r, "merchant"))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  List<String> merchantTemplates() {
    requireMerchant();
    return supportRepository.listSupportTemplates();
  }

  // ============ 视图映射 ============

  private SupportSessionView toSessionView(SupportRepository.SessionRow row, String viewer) {
    int unread = "user".equals(viewer) ? row.userUnreadCount() : row.merchantUnreadCount();
    String maskedNickname = "merchant".equals(viewer) ? maskNickname(row.userNickname()) : null;
    String lastMessage = supportRepository.lastMessageContent(row.id());
    return new SupportSessionView(
        row.id(), row.sessionNo(), row.storeId(), row.storeName(),
        row.topic(), row.status(),
        row.relatedOrderId(), row.relatedOrderNo(),
        lastMessage, row.lastMessageAt(),
        unread, maskedNickname,
        row.createdAt(), row.closedAt(), row.closeReason());
  }

  private SupportMessageView toMessageView(SupportRepository.MessageRow row) {
    return new SupportMessageView(
        row.id(), row.sessionId(),
        row.senderType(), row.senderId(),
        row.content(), row.messageKind(),
        row.createdAt());
  }

  private void autoReplyIfMatched(SupportRepository.SessionRow row, String content) {
    String normalized = content == null ? "" : content.trim();
    if (normalized.isEmpty()) return;
    String reply = null;
    String senderType = "merchant";
    long senderId = row.merchantId();
    if (row.storeId() == 0) {
      senderType = "platform";
      senderId = 0L;
      reply = "平台客服助手已收到。我会优先整理您的问题，涉及投诉、退款或订单异常时会同步给平台处理人员。";
    } else if (containsAny(normalized, "配送", "多久", "时间", "催", "慢")) {
      reply = "您好，系统已收到您的催单/时效问题，商家会尽快确认处理。";
    } else if (containsAny(normalized, "退款", "退单", "取消")) {
      reply = "您好，退款/取消问题建议先说明订单情况，商家会结合订单状态回复。";
    } else if (containsAny(normalized, "发票", "票据")) {
      reply = "您好，发票或票据问题已记录，请补充抬头和联系方式。";
    }
    if (reply == null) return;
    Long autoId = supportRepository.insertMessage(row.id(), senderType, senderId, reply, "auto_reply");
    supportRepository.updateLastMessage(row.id(), autoId, senderType);
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  // ============ 鉴权工具 ============

  private CurrentUser requireUser() {
    CurrentUser c = CurrentUserContext.required();
    if (c.accountType() != AccountType.USER) throw new BusinessException(ErrorCode.FORBIDDEN);
    return c;
  }

  private CurrentUser requireMerchant() {
    CurrentUser c = CurrentUserContext.required();
    if (c.accountType() != AccountType.MERCHANT) throw new BusinessException(ErrorCode.FORBIDDEN);
    return c;
  }

  private long currentMerchantId() {
    long accountId = requireMerchant().accountId();
    Long merchantId = jdbcTemplate.query(
        "select id from merchant_profile where account_id = ? and is_deleted = 0 limit 1",
        rs -> rs.next() ? rs.getLong("id") : null,
        accountId);
    if (merchantId == null) throw new BusinessException(ErrorCode.NOT_FOUND, "商家资料不存在");
    return merchantId;
  }

  private String normalizeStatus(String value) {
    if (value == null || value.isBlank()) return null;
    String v = value.trim().toLowerCase();
    return switch (v) {
      case "open", "closed" -> v;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "状态不正确");
    };
  }

  private String maskNickname(String nickname) {
    if (nickname == null || nickname.isBlank()) return "匿名用户";
    if (nickname.length() <= 2) return nickname.charAt(0) + "*";
    return nickname.charAt(0) + "***" + nickname.charAt(nickname.length() - 1);
  }
}
