package com.aituan.engagementplatform.support;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.engagementplatform.client.PlatformRemoteClient;
import com.aituan.engagementplatform.client.StationMessageClient;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SupportService {
  private static final int DEFAULT_MESSAGE_LIMIT = 50;

  private final SupportRepository supportRepository;
  private final PlatformRemoteClient remoteClient;
  private final AiSupportService aiSupportService;
  private final StationMessageClient stationMessageClient;

  SupportService(
      SupportRepository supportRepository,
      PlatformRemoteClient remoteClient,
      AiSupportService aiSupportService,
      StationMessageClient stationMessageClient) {
    this.supportRepository = supportRepository;
    this.remoteClient = remoteClient;
    this.aiSupportService = aiSupportService;
    this.stationMessageClient = stationMessageClient;
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
    PlatformRemoteClient.StoreSnapshot store = platformSession
        ? new PlatformRemoteClient.StoreSnapshot(0L, 0L, "平台客服")
        : remoteClient.storeSnapshot(storeId);
    long merchantId = store.merchantId();
    String relatedOrderNo = null;
    if (request.relatedOrderId() != null) {
      PlatformRemoteClient.OrderSnapshot order = remoteClient.orderSnapshot(request.relatedOrderId());
      if (order.userId() != current.userId()) throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在或无权访问");
      relatedOrderNo = order.orderNo();
    }
    String userNickname = remoteClient.userSummary(current.userId()).nickname();
    String topic = platformSession ? "平台客服" : "商家客服咨询";
    if (request.relatedOrderId() != null) topic = topic + " · 订单咨询";
    String sessionNo = "SS" + System.currentTimeMillis() + "-" + current.userId();
    Long id = supportRepository.insertSession(
        sessionNo, current.userId(), storeId, merchantId, topic, request.relatedOrderId(),
        relatedOrderNo, store.storeName(), userNickname,
        platformSession ? "platform" : "merchant",
        platformSession ? "ai" : "human");
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
        .filter(r -> Objects.equals(r.userId(), current.userId()))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"open".equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "会话已关闭");
    }
    Long messageId = supportRepository.insertMessage(sessionId, "user", current.userId(), request.content().trim(), "text");
    supportRepository.updateLastMessage(sessionId, messageId, "user");
    String content = request.content().trim();
    if (shouldTransferToHuman(content) && "platform".equals(row.serviceScope())) {
      transferToHuman(row);
    } else {
      autoReplyIfMatched(row, content, current);
    }
    return supportRepository.findMessageById(messageId)
        .map(this::toMessageView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  SupportSessionView userHandoffToHuman(long sessionId) {
    CurrentUser current = requireUser();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> Objects.equals(r.userId(), current.userId()))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"open".equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "会话已关闭");
    }
    if (!"platform".equals(row.serviceScope())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅平台客服会话支持转人工");
    }
    transferToHuman(row);
    return supportRepository.findById(sessionId)
        .map(r -> toSessionView(r, "user"))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  SupportSessionView userRequestPlatformIntervention(long sessionId) {
    CurrentUser current = requireUser();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> Objects.equals(r.userId(), current.userId()))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"open".equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "会话已关闭");
    }
    if (!"merchant".equals(row.serviceScope())) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "仅商家客服会话支持申请平台介入");
    }
    supportRepository.requestPlatformIntervention(sessionId);
    Long messageId = supportRepository.insertMessage(sessionId, "platform", 0L,
        "用户已申请平台客服介入，平台人工客服将协助处理本次商家客服会话。", "platform_intervention");
    supportRepository.updateLastMessage(sessionId, messageId, "platform");
    supportRepository.insertSysAuditLog("user", current.accountId(), "support_platform_intervention", "support_session", sessionId,
        "用户申请平台客服介入商家会话");
    return supportRepository.findById(sessionId)
        .map(r -> toSessionView(r, "user"))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  SupportSessionView userCloseSession(long sessionId, SupportSessionCloseRequest request) {
    CurrentUser current = requireUser();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> Objects.equals(r.userId(), current.userId()))
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
    stationMessageClient.support(
        row.userId(),
        "商家客服回复了你",
        row.storeName() + " 客服回复：" + request.content().trim(),
        row.relatedOrderId(),
        sessionId,
        "客服");
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
    stationMessageClient.support(
        row.userId(),
        "商家客服会话已关闭",
        row.storeName() + " 已关闭本次客服会话。" + (reason == null || reason.isBlank() ? "" : " 原因：" + reason.trim()),
        row.relatedOrderId(),
        sessionId,
        "客服");
    return supportRepository.findById(sessionId)
        .map(r -> toSessionView(r, "merchant"))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  List<String> merchantTemplates() {
    requireMerchant();
    return supportRepository.listSupportTemplates();
  }

  List<SupportAutoReplyRuleView> merchantAutoReplyRules() {
    long merchantId = currentMerchantId();
    return supportRepository.listAutoReplyRules(merchantId).stream()
        .map(this::toAutoReplyRuleView)
        .toList();
  }

  @Transactional
  SupportAutoReplyRuleView createMerchantAutoReplyRule(SupportAutoReplyRuleUpsertRequest request) {
    long merchantId = currentMerchantId();
    Long id = supportRepository.insertAutoReplyRule(
        merchantId,
        cleanRequired(request.keywords(), "触发关键词不能为空"),
        cleanRequired(request.replyContent(), "回复内容不能为空"),
        request.enabled() == null || request.enabled());
    return supportRepository.findAutoReplyRule(id)
        .map(this::toAutoReplyRuleView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  SupportAutoReplyRuleView updateMerchantAutoReplyRule(long ruleId, SupportAutoReplyRuleUpsertRequest request) {
    long merchantId = currentMerchantId();
    SupportRepository.AutoReplyRuleRow row = supportRepository.findAutoReplyRule(ruleId)
        .filter(rule -> rule.merchantId() == merchantId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    supportRepository.updateAutoReplyRule(
        row.id(),
        cleanRequired(request.keywords(), "触发关键词不能为空"),
        cleanRequired(request.replyContent(), "回复内容不能为空"),
        request.enabled() == null || request.enabled());
    return supportRepository.findAutoReplyRule(ruleId)
        .map(this::toAutoReplyRuleView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  void deleteMerchantAutoReplyRule(long ruleId) {
    long merchantId = currentMerchantId();
    SupportRepository.AutoReplyRuleRow row = supportRepository.findAutoReplyRule(ruleId)
        .filter(rule -> rule.merchantId() == merchantId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    supportRepository.deleteAutoReplyRule(row.id());
  }

  @Transactional
  SupportSessionView requestPlatformIntervention(long sessionId) {
    CurrentUser current = requireMerchant();
    long merchantId = currentMerchantId();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> r.merchantId() == merchantId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"open".equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "会话已关闭");
    }
    supportRepository.requestPlatformIntervention(sessionId);
    Long messageId = supportRepository.insertMessage(sessionId, "platform", 0L,
        "平台客服已介入，本次会话将由平台人工客服继续协助处理。", "platform_intervention");
    supportRepository.updateLastMessage(sessionId, messageId, "platform");
    stationMessageClient.support(
        row.userId(),
        "平台客服已介入",
        "平台客服已介入你的会话，将继续协助处理。",
        row.relatedOrderId(),
        sessionId,
        "平台");
    supportRepository.insertSysAuditLog("merchant", current.accountId(), "support_platform_intervention", "support_session", sessionId,
        "商家申请平台客服介入");
    return supportRepository.findById(sessionId)
        .map(r -> toSessionView(r, "merchant"))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  // ============ 后台平台客服 ============

  PageResponse<SupportSessionView> adminPlatformSessions(String statusFilter, int page, int pageSize) {
    requireAdmin();
    String normalized = normalizeStatus(statusFilter);
    long total = supportRepository.countAdminPlatformSessions(normalized);
    List<SupportSessionView> list = supportRepository.listAdminPlatformSessions(normalized, (page - 1) * pageSize, pageSize)
        .stream().map(row -> toSessionView(row, "admin")).toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  SupportSessionDetailView adminPlatformSessionDetail(long sessionId) {
    requireAdmin();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> "platform".equals(r.serviceScope()))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    SupportRepository.SessionRow refreshed = supportRepository.findById(sessionId).orElse(row);
    List<SupportMessageView> messages = supportRepository.listMessages(sessionId, 0, DEFAULT_MESSAGE_LIMIT)
        .stream().map(this::toMessageView).toList();
    return new SupportSessionDetailView(toSessionView(refreshed, "admin"), messages);
  }

  @Transactional
  SupportMessageView adminSendPlatformMessage(long sessionId, SupportMessageCreateRequest request) {
    CurrentUser admin = requireAdmin();
    SupportRepository.SessionRow row = supportRepository.findById(sessionId)
        .filter(r -> "platform".equals(r.serviceScope()))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"open".equals(row.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "会话已关闭");
    }
    if ("ai".equals(row.assistantMode())) {
      supportRepository.markHumanHandoff(sessionId);
    }
    Long messageId = supportRepository.insertMessage(sessionId, "platform", admin.accountId(), request.content().trim(), "text");
    supportRepository.updateLastMessage(sessionId, messageId, "platform");
    stationMessageClient.support(
        row.userId(),
        "平台客服回复了你",
        "平台客服回复：" + request.content().trim(),
        row.relatedOrderId(),
        sessionId,
        "客服");
    supportRepository.insertSysAuditLog("admin", admin.accountId(), "support_platform_reply", "support_session", sessionId,
        "平台人工客服回复");
    return supportRepository.findMessageById(messageId)
        .map(this::toMessageView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
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
        row.createdAt(), row.closedAt(), row.closeReason(),
        row.serviceScope(), row.assistantMode(), row.platformInterventionStatus(),
        row.humanRequestedAt(), row.platformIntervenedAt());
  }

  private SupportMessageView toMessageView(SupportRepository.MessageRow row) {
    return new SupportMessageView(
        row.id(), row.sessionId(),
        row.senderType(), row.senderId(),
        row.content(), row.messageKind(),
        row.createdAt());
  }

  private SupportAutoReplyRuleView toAutoReplyRuleView(SupportRepository.AutoReplyRuleRow row) {
    return new SupportAutoReplyRuleView(
        row.id(), row.merchantId(), row.keywords(), row.replyContent(),
        row.enabled(), row.createdAt(), row.updatedAt());
  }

  private void autoReplyIfMatched(SupportRepository.SessionRow row, String content, CurrentUser currentUser) {
    String normalized = content == null ? "" : content.trim();
    if (normalized.isEmpty()) return;
    String reply = null;
    String senderType = "merchant";
    long senderId = row.merchantId();
    if ("platform".equals(row.serviceScope())) {
      if (!"ai".equals(row.assistantMode())) return;
      senderType = "platform";
      senderId = 0L;
      reply = aiSupportService.reply(currentUser, row, normalized);
    } else {
      for (SupportRepository.AutoReplyRuleRow rule : supportRepository.listEnabledAutoReplyRules(row.merchantId())) {
        if (matchesRule(normalized, rule.keywords())) {
          reply = rule.replyContent();
          break;
        }
      }
    }
    if (reply == null) return;
    Long autoId = supportRepository.insertMessage(row.id(), senderType, senderId, reply, "auto_reply");
    supportRepository.updateLastMessage(row.id(), autoId, senderType);
  }

  private void transferToHuman(SupportRepository.SessionRow row) {
    if ("human".equals(row.assistantMode())) return;
    supportRepository.markHumanHandoff(row.id());
    Long handoffId = supportRepository.insertMessage(row.id(), "platform", 0L,
        "已为您转接平台人工客服，请继续补充问题细节。", "handoff");
    supportRepository.updateLastMessage(row.id(), handoffId, "platform");
  }

  private boolean shouldTransferToHuman(String content) {
    return containsAny(content == null ? "" : content, "转人工", "人工客服", "找人工", "人工处理");
  }

  private boolean matchesRule(String content, String keywords) {
    if (keywords == null || keywords.isBlank()) return false;
    return Arrays.stream(keywords.split("[,，/、\\s]+"))
        .map(String::trim)
        .filter(keyword -> !keyword.isEmpty())
        .anyMatch(content::contains);
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

  private CurrentUser requireAdmin() {
    CurrentUser c = CurrentUserContext.required();
    if (c.accountType() != AccountType.ADMIN) throw new BusinessException(ErrorCode.FORBIDDEN);
    return c;
  }

  private long currentMerchantId() {
    long accountId = requireMerchant().accountId();
    return remoteClient.merchantIdByAccount(accountId);
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

  private String cleanRequired(String value, String message) {
    String cleaned = value == null ? "" : value.trim();
    if (cleaned.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    return cleaned;
  }
}
