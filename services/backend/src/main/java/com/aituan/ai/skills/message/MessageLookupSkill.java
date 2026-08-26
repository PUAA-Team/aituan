package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.limit;
import static com.aituan.ai.AiSkillSupport.params;
import static com.aituan.ai.AiSkillSupport.shouldRun;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class MessageLookupSkill implements AiSkill {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");
  private static final List<String> WORDS = List.of("消息", "通知", "站内信", "未读", "提醒", "系统通知");

  private final JdbcTemplate jdbcTemplate;

  MessageLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "message_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户真实站内消息、未读状态和关联目标";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<MessageRow> rows = jdbcTemplate.query(
        """
        select id, message_type, title, content, badge_text, read_status, related_order_id,
               related_target_type, related_target_id, created_at
        from support_station_message
        where user_id = ? and is_deleted = 0
        order by case read_status when 'unread' then 0 else 1 end, created_at desc, id desc
        limit 5
        """,
        this::mapMessage,
        context.currentUser().userId());
    if (rows.isEmpty()) return Optional.of(AiSkillResult.text(name(), "消息查询", "当前账号暂无站内消息。"));
    StringBuilder summary = new StringBuilder("当前用户站内消息：");
    for (MessageRow row : rows) {
      summary.append("\n- ").append(row.title()).append("，").append(statusLabel(row.readStatus()))
          .append("，").append(row.type())
          .append(row.badge() == null ? "" : "，" + row.badge())
          .append(row.createdAt() == null ? "" : "，" + row.createdAt())
          .append("：").append(limit(row.content(), 56));
    }
    return Optional.of(new AiSkillResult(
        name(), "消息查询", summary.toString(),
        List.of(), List.of(new AiAssistantAction("消息中心", null, "/messages", params()))));
  }

  private String statusLabel(String status) {
    return "unread".equals(status) ? "未读" : "已读";
  }

  private MessageRow mapMessage(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new MessageRow(rs.getLong("id"), rs.getString("message_type"), rs.getString("title"),
        rs.getString("content"), rs.getString("badge_text"), rs.getString("read_status"),
        rs.getString("related_target_type"), rs.getLong("related_target_id"),
        createdAt == null ? null : TIME.format(createdAt.toLocalDateTime()));
  }

  record MessageRow(long id, String type, String title, String content, String badge, String readStatus,
                    String relatedTargetType, Long relatedTargetId, String createdAt) {}
}
