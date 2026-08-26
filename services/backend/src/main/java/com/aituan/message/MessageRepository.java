package com.aituan.message;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository {
  private final JdbcTemplate jdbcTemplate;

  public MessageRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long countUnreadMessages(long userId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from support_station_message where user_id = ? and is_deleted = 0 and read_status = 'unread'",
        Long.class,
        userId);
    return count == null ? 0 : count;
  }

  public void insertStationMessage(
      long userId,
      String type,
      String title,
      String content,
      String badgeText,
      Long relatedOrderId,
      String relatedTargetType,
      Long relatedTargetId) {
    jdbcTemplate.update(
        """
        insert into support_station_message(user_id, message_type, title, content, badge_text, read_status,
                                            related_order_id, related_target_type, related_target_id)
        values (?, ?, ?, ?, ?, 'unread', ?, ?, ?)
        """,
        userId,
        text(type, 40, "system"),
        text(title, 120, "系统通知"),
        text(content, 500, ""),
        nullableText(badgeText, 40),
        relatedOrderId,
        nullableText(relatedTargetType, 16),
        relatedTargetId);
  }

  long countMessages(long userId, String type) {
    if (type == null || type.isBlank()) {
      Long count = jdbcTemplate.queryForObject(
          "select count(1) from support_station_message where user_id = ? and is_deleted = 0",
          Long.class,
          userId);
      return count == null ? 0 : count;
    }
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from support_station_message where user_id = ? and message_type = ? and is_deleted = 0",
        Long.class,
        userId,
        type.trim());
    return count == null ? 0 : count;
  }

  List<MessageRow> listMessages(long userId, String type, int offset, int limit) {
    if (type == null || type.isBlank()) {
      return jdbcTemplate.query(
          """
          select id, message_type, title, content, badge_text, read_status, related_order_id,
                 related_target_type, related_target_id, created_at
          from support_station_message
          where user_id = ? and is_deleted = 0
          order by created_at desc, id desc
          limit ? offset ?
          """,
          this::mapMessage,
          userId,
          limit,
          offset);
    }
    return jdbcTemplate.query(
        """
        select id, message_type, title, content, badge_text, read_status, related_order_id,
               related_target_type, related_target_id, created_at
        from support_station_message
        where user_id = ? and message_type = ? and is_deleted = 0
        order by created_at desc, id desc
        limit ? offset ?
        """,
        this::mapMessage,
        userId,
        type.trim(),
        limit,
        offset);
  }

  void markRead(long userId, long messageId) {
    jdbcTemplate.update(
        "update support_station_message set read_status = 'read', updated_at = current_timestamp where id = ? and user_id = ? and is_deleted = 0",
        messageId,
        userId);
  }

  void markRead(long userId, List<Long> messageIds) {
    updateMessageStatus(userId, messageIds, "read");
  }

  void markUnread(long userId, List<Long> messageIds) {
    updateMessageStatus(userId, messageIds, "unread");
  }

  void softDelete(long userId, List<Long> messageIds) {
    if (messageIds.isEmpty()) {
      return;
    }
    jdbcTemplate.update(
        "update support_station_message set is_deleted = 1, updated_at = current_timestamp where user_id = ? and is_deleted = 0 and id in (" + placeholders(messageIds) + ")",
        params(userId, messageIds));
  }

  void markAllRead(long userId) {
    jdbcTemplate.update(
        "update support_station_message set read_status = 'read', updated_at = current_timestamp where user_id = ? and read_status = 'unread' and is_deleted = 0",
        userId);
  }

  private void updateMessageStatus(long userId, List<Long> messageIds, String readStatus) {
    if (messageIds.isEmpty()) {
      return;
    }
    jdbcTemplate.update(
        "update support_station_message set read_status = ?, updated_at = current_timestamp where user_id = ? and is_deleted = 0 and id in (" + placeholders(messageIds) + ")",
        params(readStatus, userId, messageIds));
  }

  private String placeholders(List<Long> messageIds) {
    return "?, ".repeat(messageIds.size() - 1) + "?";
  }

  private Object[] params(long userId, List<Long> messageIds) {
    Object[] params = new Object[messageIds.size() + 1];
    params[0] = userId;
    for (int i = 0; i < messageIds.size(); i++) {
      params[i + 1] = messageIds.get(i);
    }
    return params;
  }

  private Object[] params(String readStatus, long userId, List<Long> messageIds) {
    Object[] params = new Object[messageIds.size() + 2];
    params[0] = readStatus;
    params[1] = userId;
    for (int i = 0; i < messageIds.size(); i++) {
      params[i + 2] = messageIds.get(i);
    }
    return params;
  }

  private MessageRow mapMessage(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    Long relatedOrderId = nullableLong(rs, "related_order_id");
    Long relatedTargetId = nullableLong(rs, "related_target_id");
    return new MessageRow(
        rs.getLong("id"),
        rs.getString("message_type"),
        rs.getString("title"),
        rs.getString("content"),
        rs.getString("badge_text"),
        rs.getString("read_status"),
        relatedOrderId,
        rs.getString("related_target_type"),
        relatedTargetId,
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private String nullableText(String value, int maxLength) {
    String text = value == null ? null : value.trim();
    if (text == null || text.isEmpty()) {
      return null;
    }
    return text.length() <= maxLength ? text : text.substring(0, maxLength);
  }

  private String text(String value, int maxLength, String fallback) {
    String text = value == null ? "" : value.trim();
    if (text.isEmpty()) {
      text = fallback;
    }
    return text.length() <= maxLength ? text : text.substring(0, maxLength);
  }

  record MessageRow(Long id, String type, String title, String content, String badgeText, String readStatus,
                    Long relatedOrderId, String relatedTargetType, Long relatedTargetId, LocalDateTime createdAt) {}
}
