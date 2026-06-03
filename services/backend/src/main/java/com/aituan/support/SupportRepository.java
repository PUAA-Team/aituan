package com.aituan.support;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class SupportRepository {
  private final JdbcTemplate jdbcTemplate;

  SupportRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final String SESSION_SELECT = """
      select s.id, s.session_no, s.user_id, s.store_id, s.merchant_id, s.topic, s.status,
             s.related_order_id, s.last_message_id, s.last_message_at,
             s.user_unread_count, s.merchant_unread_count,
             s.closed_at, s.close_reason, s.created_at,
             ms.store_name as store_name,
             o.order_no as related_order_no,
             up.nickname as user_nickname
      from support_session s
      left join merchant_store ms on ms.id = s.store_id and ms.is_deleted = 0
      left join order_main o on o.id = s.related_order_id and o.is_deleted = 0
      left join user_profile up on up.id = s.user_id and up.is_deleted = 0
      """;

  // ============ 会话查询 ============

  Optional<SessionRow> findById(long id) {
    List<SessionRow> rows = jdbcTemplate.query(
        SESSION_SELECT + " where s.id = ? and s.is_deleted = 0 limit 1",
        this::mapSession, id);
    return rows.stream().findFirst();
  }

  long countUserSessions(long userId, String statusFilter) {
    if (statusFilter == null) {
      Long c = jdbcTemplate.queryForObject(
          "select count(1) from support_session where user_id = ? and is_deleted = 0",
          Long.class, userId);
      return c == null ? 0 : c;
    }
    Long c = jdbcTemplate.queryForObject(
        "select count(1) from support_session where user_id = ? and status = ? and is_deleted = 0",
        Long.class, userId, statusFilter);
    return c == null ? 0 : c;
  }

  List<SessionRow> listUserSessions(long userId, String statusFilter, int offset, int limit) {
    String tail = " order by coalesce(s.last_message_at, s.created_at) desc, s.id desc limit ? offset ?";
    if (statusFilter == null) {
      return jdbcTemplate.query(
          SESSION_SELECT + " where s.user_id = ? and s.is_deleted = 0" + tail,
          this::mapSession, userId, limit, offset);
    }
    return jdbcTemplate.query(
        SESSION_SELECT + " where s.user_id = ? and s.status = ? and s.is_deleted = 0" + tail,
        this::mapSession, userId, statusFilter, limit, offset);
  }

  long countMerchantSessions(long merchantId, String statusFilter) {
    if (statusFilter == null) {
      Long c = jdbcTemplate.queryForObject(
          "select count(1) from support_session where merchant_id = ? and is_deleted = 0",
          Long.class, merchantId);
      return c == null ? 0 : c;
    }
    Long c = jdbcTemplate.queryForObject(
        "select count(1) from support_session where merchant_id = ? and status = ? and is_deleted = 0",
        Long.class, merchantId, statusFilter);
    return c == null ? 0 : c;
  }

  List<SessionRow> listMerchantSessions(long merchantId, String statusFilter, int offset, int limit) {
    String tail = " order by coalesce(s.last_message_at, s.created_at) desc, s.id desc limit ? offset ?";
    if (statusFilter == null) {
      return jdbcTemplate.query(
          SESSION_SELECT + " where s.merchant_id = ? and s.is_deleted = 0" + tail,
          this::mapSession, merchantId, limit, offset);
    }
    return jdbcTemplate.query(
        SESSION_SELECT + " where s.merchant_id = ? and s.status = ? and s.is_deleted = 0" + tail,
        this::mapSession, merchantId, statusFilter, limit, offset);
  }

  // ============ 写入 ============

  Long insertSession(String sessionNo, long userId, long storeId, long merchantId, String topic, Long relatedOrderId) {
    jdbcTemplate.update(
        """
        insert into support_session(session_no, user_id, store_id, merchant_id, topic, status, related_order_id)
        values (?, ?, ?, ?, ?, 'open', ?)
        """,
        sessionNo, userId, storeId, merchantId, topic, relatedOrderId);
    return jdbcTemplate.queryForObject(
        "select id from support_session where session_no = ?", Long.class, sessionNo);
  }

  Long insertMessage(long sessionId, String senderType, long senderId, String content, String messageKind) {
    jdbcTemplate.update(
        """
        insert into support_message(session_id, sender_type, sender_id, content, message_kind)
        values (?, ?, ?, ?, ?)
        """,
        sessionId, senderType, senderId, content, messageKind);
    return jdbcTemplate.queryForObject(
        "select max(id) from support_message where session_id = ?", Long.class, sessionId);
  }

  void updateLastMessage(long sessionId, long messageId, String senderType) {
    // 发消息者对侧未读+1，发送者侧不动
    String column = "user".equals(senderType) ? "merchant_unread_count" : "user_unread_count";
    jdbcTemplate.update(
        "update support_session set last_message_id = ?, last_message_at = current_timestamp, "
            + column + " = " + column + " + 1, updated_at = current_timestamp where id = ?",
        messageId, sessionId);
  }

  void clearUnread(long sessionId, String viewerType) {
    String column = "user".equals(viewerType) ? "user_unread_count" : "merchant_unread_count";
    jdbcTemplate.update(
        "update support_session set " + column + " = 0, updated_at = current_timestamp where id = ?",
        sessionId);
  }

  void closeSession(long sessionId, String closedByType, long closedById, String reason) {
    jdbcTemplate.update(
        """
        update support_session set status = 'closed', closed_at = current_timestamp,
          closed_by_type = ?, closed_by_id = ?, close_reason = ?, updated_at = current_timestamp
        where id = ?
        """,
        closedByType, closedById, reason, sessionId);
  }

  // ============ 消息查询 ============

  Optional<MessageRow> findMessageById(long messageId) {
    List<MessageRow> rows = jdbcTemplate.query(
        "select id, session_id, sender_type, sender_id, content, message_kind, created_at "
            + "from support_message where id = ? and is_deleted = 0 limit 1",
        this::mapMessage, messageId);
    return rows.stream().findFirst();
  }

  List<MessageRow> listMessages(long sessionId, int offset, int limit) {
    return jdbcTemplate.query(
        """
        select id, session_id, sender_type, sender_id, content, message_kind, created_at
        from support_message
        where session_id = ? and is_deleted = 0
        order by id asc
        limit ? offset ?
        """,
        this::mapMessage, sessionId, limit, offset);
  }

  String lastMessageContent(long sessionId) {
    List<String> rows = jdbcTemplate.queryForList(
        "select content from support_message where session_id = ? and is_deleted = 0 order by id desc limit 1",
        String.class, sessionId);
    return rows.isEmpty() ? null : rows.get(0);
  }

  // ============ 工具 ============

  Optional<Long> findStoreMerchantId(long storeId) {
    List<Long> rows = jdbcTemplate.queryForList(
        "select merchant_id from merchant_store where id = ? and is_deleted = 0", Long.class, storeId);
    return rows.stream().findFirst();
  }

  Optional<String> findStoreName(long storeId) {
    List<String> rows = jdbcTemplate.queryForList(
        "select store_name from merchant_store where id = ? and is_deleted = 0", String.class, storeId);
    return rows.stream().findFirst();
  }

  List<String> listSupportTemplates() {
    return jdbcTemplate.queryForList(
        "select dict_value from sys_dict where dict_type = 'support_template' and status = 'normal' order by sort_order asc",
        String.class);
  }

  void insertSysAuditLog(String actorType, long actorId, String actionType, String targetType, long targetId, String detail) {
    jdbcTemplate.update(
        """
        insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail)
        values (?, ?, ?, ?, ?, ?)
        """,
        actorType, actorId, actionType, targetType, targetId, detail);
  }

  // ============ 行映射 ============

  private SessionRow mapSession(ResultSet rs, int rowNum) throws SQLException {
    long relatedOrderRaw = rs.getLong("related_order_id");
    Long relatedOrderId = rs.wasNull() ? null : relatedOrderRaw;
    long storeId = rs.getLong("store_id");
    long merchantId = rs.getLong("merchant_id");
    Timestamp lastAt = rs.getTimestamp("last_message_at");
    Timestamp closedAt = rs.getTimestamp("closed_at");
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new SessionRow(
        rs.getLong("id"), rs.getString("session_no"),
        rs.getLong("user_id"), storeId, merchantId,
        rs.getString("topic"), rs.getString("status"),
        relatedOrderId, rs.getString("related_order_no"),
        storeId == 0 ? "平台客服" : rs.getString("store_name"),
        rs.getString("user_nickname"),
        rs.getInt("user_unread_count"), rs.getInt("merchant_unread_count"),
        lastAt == null ? null : lastAt.toLocalDateTime(),
        closedAt == null ? null : closedAt.toLocalDateTime(),
        rs.getString("close_reason"),
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  private MessageRow mapMessage(ResultSet rs, int rowNum) throws SQLException {
    Timestamp t = rs.getTimestamp("created_at");
    return new MessageRow(
        rs.getLong("id"), rs.getLong("session_id"),
        rs.getString("sender_type"), rs.getLong("sender_id"),
        rs.getString("content"), rs.getString("message_kind"),
        t == null ? null : t.toLocalDateTime());
  }

  record SessionRow(Long id, String sessionNo, Long userId, Long storeId, Long merchantId,
                    String topic, String status,
                    Long relatedOrderId, String relatedOrderNo,
                    String storeName, String userNickname,
                    int userUnreadCount, int merchantUnreadCount,
                    LocalDateTime lastMessageAt, LocalDateTime closedAt, String closeReason,
                    LocalDateTime createdAt) {}

  record MessageRow(Long id, Long sessionId, String senderType, Long senderId,
                    String content, String messageKind, LocalDateTime createdAt) {}
}
