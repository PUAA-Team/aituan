package com.aituan.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AiAssistantRepository {
  private static final TypeReference<List<AiAssistantCard>> CARD_LIST = new TypeReference<>() {};
  private static final TypeReference<List<AiAssistantAction>> ACTION_LIST = new TypeReference<>() {};
  private static final TypeReference<List<AiAssistantStep>> STEP_LIST = new TypeReference<>() {};
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  AiAssistantRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  Optional<ConversationRow> findCurrentConversation(long userId) {
    List<ConversationRow> rows = jdbcTemplate.query(
        """
        select id, conversation_no, user_id, title, status, last_message_id, last_message_at, created_at
        from ai_assistant_conversation
        where user_id = ? and status = 'active' and is_deleted = 0
        order by coalesce(last_message_at, created_at) desc, id desc
        limit 1
        """,
        this::mapConversation,
        userId);
    return rows.stream().findFirst();
  }

  Optional<ConversationRow> findConversation(long userId, String conversationNo) {
    List<ConversationRow> rows = jdbcTemplate.query(
        """
        select id, conversation_no, user_id, title, status, last_message_id, last_message_at, created_at
        from ai_assistant_conversation
        where user_id = ? and conversation_no = ? and is_deleted = 0
        limit 1
        """,
        this::mapConversation,
        userId,
        conversationNo);
    return rows.stream().findFirst();
  }

  ConversationRow createConversation(long userId, String conversationNo, String title) {
    jdbcTemplate.update(
        """
        insert into ai_assistant_conversation(conversation_no, user_id, title, status)
        values (?, ?, ?, 'active')
        """,
        conversationNo,
        userId,
        title);
    return findConversation(userId, conversationNo).orElseThrow();
  }

  long insertMessage(
      long conversationId,
      long userId,
      String role,
      String content,
      List<AiAssistantCard> cards,
      List<AiAssistantAction> actions,
      List<AiAssistantStep> steps,
      List<String> usedSkills,
      boolean modelUsed) {
    jdbcTemplate.update(
        """
        insert into ai_assistant_message(
          conversation_id, user_id, role, content, cards_json, actions_json, steps_json, used_skills_json, model_used)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        conversationId,
        userId,
        role,
        content,
        writeJson(cards),
        writeJson(actions),
        writeJson(steps),
        writeJson(usedSkills),
        modelUsed ? 1 : 0);
    Long id = jdbcTemplate.queryForObject(
        "select max(id) from ai_assistant_message where conversation_id = ? and user_id = ?",
        Long.class,
        conversationId,
        userId);
    return id == null ? 0L : id;
  }

  void touchConversation(long conversationId, long messageId, String title) {
    jdbcTemplate.update(
        """
        update ai_assistant_conversation
        set last_message_id = ?, last_message_at = current_timestamp,
            title = case when title = '新对话' then ? else title end,
            updated_at = current_timestamp
        where id = ?
        """,
        messageId,
        title,
        conversationId);
  }

  List<MessageRow> listMessages(long conversationId, long userId, int limit) {
    return jdbcTemplate.query(
        """
        select id, conversation_id, user_id, role, content, cards_json, actions_json, steps_json,
               used_skills_json, model_used, created_at
        from ai_assistant_message
        where conversation_id = ? and user_id = ? and is_deleted = 0
        order by id asc
        limit ?
        """,
        this::mapMessage,
        conversationId,
        userId,
        limit);
  }

  private ConversationRow mapConversation(ResultSet rs, int rowNum) throws SQLException {
    Timestamp lastMessageAt = rs.getTimestamp("last_message_at");
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new ConversationRow(
        rs.getLong("id"),
        rs.getString("conversation_no"),
        rs.getLong("user_id"),
        rs.getString("title"),
        rs.getString("status"),
        nullableLong(rs, "last_message_id"),
        lastMessageAt == null ? null : lastMessageAt.toLocalDateTime(),
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  private MessageRow mapMessage(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new MessageRow(
        rs.getLong("id"),
        rs.getLong("conversation_id"),
        rs.getLong("user_id"),
        rs.getString("role"),
        rs.getString("content"),
        readJson(rs.getString("cards_json"), CARD_LIST),
        readJson(rs.getString("actions_json"), ACTION_LIST),
        readJson(rs.getString("steps_json"), STEP_LIST),
        readJson(rs.getString("used_skills_json"), STRING_LIST),
        rs.getInt("model_used") == 1,
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (JsonProcessingException ex) {
      return "[]";
    }
  }

  private <T> T readJson(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json == null || json.isBlank() ? "[]" : json, type);
    } catch (JsonProcessingException ex) {
      try {
        return objectMapper.readValue("[]", type);
      } catch (JsonProcessingException impossible) {
        throw new IllegalStateException(impossible);
      }
    }
  }

  record ConversationRow(
      long id,
      String conversationNo,
      long userId,
      String title,
      String status,
      Long lastMessageId,
      LocalDateTime lastMessageAt,
      LocalDateTime createdAt) {}

  record MessageRow(
      long id,
      long conversationId,
      long userId,
      String role,
      String content,
      List<AiAssistantCard> cards,
      List<AiAssistantAction> actions,
      List<AiAssistantStep> steps,
      List<String> usedSkills,
      boolean modelUsed,
      LocalDateTime createdAt) {}
}
