package com.aituan.engagementplatform.ai;

import static com.aituan.engagementplatform.ai.AiSkillSupport.limit;
import static com.aituan.engagementplatform.ai.AiSkillSupport.params;
import static com.aituan.engagementplatform.ai.AiSkillSupport.shouldRun;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class SupportLookupSkill implements AiSkill {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");
  private static final List<String> WORDS = List.of("客服", "人工", "平台介入", "商家客服", "平台客服", "会话", "没人回", "转人工");

  private final JdbcTemplate jdbcTemplate;

  SupportLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "support_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户真实客服会话、平台介入状态、AI/人工模式和最近消息";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<SessionRow> rows = jdbcTemplate.query(
        """
        select s.id, s.session_no, s.topic, s.status, s.service_scope, s.assistant_mode,
               s.platform_intervention_status, s.user_unread_count, s.store_name, s.related_order_no as order_no,
               s.last_message_at,
               (select content from support_message m where m.session_id = s.id and m.is_deleted = 0 order by m.id desc limit 1) as last_content
        from support_session s
        where s.user_id = ? and s.is_deleted = 0
        order by coalesce(s.last_message_at, s.created_at) desc, s.id desc
        limit 5
        """,
        this::mapSession,
        context.currentUser().userId());
    StringBuilder summary = new StringBuilder();
    if (rows.isEmpty()) {
      summary.append("当前账号暂无客服会话。可以发起平台客服或从订单/店铺进入商家客服。");
    } else {
      summary.append("当前用户客服会话：");
      for (SessionRow row : rows) {
        summary.append("\n- ").append(row.topic()).append("，")
            .append(scopeLabel(row.scope())).append("/").append(modeLabel(row.mode()))
            .append("，状态 ").append(row.status())
            .append("，平台介入 ").append(row.interventionStatus())
            .append(row.storeName() == null ? "" : "，店铺 " + row.storeName())
            .append(row.orderNo() == null ? "" : "，订单 " + row.orderNo())
            .append("，未读 ").append(row.unreadCount())
            .append(row.lastMessageAt() == null ? "" : "，" + row.lastMessageAt())
            .append(row.lastContent() == null ? "" : "，最后消息：" + limit(row.lastContent(), 40));
      }
    }
    return Optional.of(new AiSkillResult(
        name(),
        "客服会话查询",
        summary.toString(),
        List.of(new AiAssistantCard("support", "平台客服", "AI 会先整理问题，用户可随时转人工。", "打开客服", "/support/sessions", params())),
        List.of(
            new AiAssistantAction("打开客服", null, "/support/sessions", params()),
            new AiAssistantAction("转平台人工", "我要转人工", "/support/sessions", params()))));
  }

  private String scopeLabel(String scope) {
    return "platform".equals(scope) ? "平台客服" : "商家客服";
  }

  private String modeLabel(String mode) {
    return "ai".equals(mode) ? "AI" : "人工";
  }

  private SessionRow mapSession(ResultSet rs, int rowNum) throws SQLException {
    Timestamp lastMessageAt = rs.getTimestamp("last_message_at");
    return new SessionRow(
        rs.getLong("id"), rs.getString("session_no"), rs.getString("topic"), rs.getString("status"),
        rs.getString("service_scope"), rs.getString("assistant_mode"), rs.getString("platform_intervention_status"),
        rs.getInt("user_unread_count"), rs.getString("store_name"), rs.getString("order_no"),
        lastMessageAt == null ? null : TIME.format(lastMessageAt.toLocalDateTime()), rs.getString("last_content"));
  }

  record SessionRow(long id, String sessionNo, String topic, String status, String scope, String mode,
                    String interventionStatus, int unreadCount, String storeName, String orderNo,
                    String lastMessageAt, String lastContent) {}
}
