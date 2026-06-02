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
        type);
    return count == null ? 0 : count;
  }

  List<MessageRow> listMessages(long userId, String type, int offset, int limit) {
    if (type == null || type.isBlank()) {
      return jdbcTemplate.query(
          messageSelect() + " where user_id = ? and is_deleted = 0 order by created_at desc, id desc limit ? offset ?",
          this::mapMessage,
          userId,
          limit,
          offset);
    }
    return jdbcTemplate.query(
        messageSelect() + " where user_id = ? and message_type = ? and is_deleted = 0 order by created_at desc, id desc limit ? offset ?",
        this::mapMessage,
        userId,
        type,
        limit,
        offset);
  }

  void markRead(long userId, long messageId) {
    jdbcTemplate.update(
        "update support_station_message set read_status = 'read', updated_at = current_timestamp where id = ? and user_id = ? and is_deleted = 0",
        messageId,
        userId);
  }

  int markAllRead(long userId) {
    return jdbcTemplate.update(
        "update support_station_message set read_status = 'read', updated_at = current_timestamp where user_id = ? and read_status = 'unread' and is_deleted = 0",
        userId);
  }

  private String messageSelect() {
    return """
        select id, message_type, title, content, badge_text, read_status, related_order_id, related_target_type, related_target_id, created_at
        from support_station_message
        """;
  }

  private MessageRow mapMessage(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new MessageRow(
        rs.getLong("id"),
        rs.getString("message_type"),
        rs.getString("title"),
        rs.getString("content"),
        rs.getString("badge_text"),
        rs.getString("read_status"),
        rs.getObject("related_order_id", Long.class),
        rs.getString("related_target_type"),
        rs.getObject("related_target_id", Long.class),
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  record MessageRow(Long id, String type, String title, String content, String badgeText, String readStatus,
                    Long relatedOrderId, String relatedTargetType, Long relatedTargetId, LocalDateTime createdAt) {}
}
