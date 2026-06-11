package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.limit;
import static com.aituan.ai.AiSkillSupport.params;
import static com.aituan.ai.AiSkillSupport.shouldRun;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class ComplaintLookupSkill implements AiSkill {
  private static final List<String> WORDS = List.of("投诉", "工单", "处理进度", "平台处理", "纠纷", "证据", "食品安全", "服务态度", "差评");

  private final JdbcTemplate jdbcTemplate;

  ComplaintLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "complaint_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户真实投诉工单、证据、关联订单和处理日志摘要";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<TicketRow> tickets = jdbcTemplate.query(
        """
        select t.id, t.ticket_no, t.title, t.category, t.detail, t.evidence_urls, t.status, ms.store_name, o.order_no,
               (select count(1) from complaint_log l where l.ticket_id = t.id) as log_count
        from complaint_ticket t
        left join merchant_store ms on ms.id = t.store_id and ms.is_deleted = 0
        left join order_main o on o.id = t.order_id and o.is_deleted = 0
        where t.user_id = ? and t.is_deleted = 0
        order by t.created_at desc, t.id desc
        limit 5
        """,
        this::mapTicket,
        context.currentUser().userId());
    String content;
    if (tickets.isEmpty()) {
      content = "当前账号暂无投诉工单。涉及退款、食品安全、服务态度或评价争议时，可提交投诉并上传图片证据。";
    } else {
      StringBuilder builder = new StringBuilder("当前用户投诉工单：");
      for (TicketRow ticket : tickets) {
        builder.append("\n- ").append(ticket.ticketNo()).append("，")
            .append(ticket.title()).append("，").append(ticket.category())
            .append("，状态 ").append(ticket.status())
            .append(ticket.storeName() == null ? "" : "，店铺 " + ticket.storeName())
            .append(ticket.orderNo() == null ? "" : "，订单 " + ticket.orderNo())
            .append("，证据 ").append(imageCount(ticket.evidenceUrls())).append(" 张")
            .append("，处理记录 ").append(ticket.logCount()).append(" 条")
            .append("，问题：").append(limit(ticket.detail(), 48));
      }
      content = builder.toString();
    }
    return Optional.of(new AiSkillResult(
        name(),
        "投诉工单查询",
        content,
        List.of(new AiAssistantCard("complaint", "投诉与建议", "查看工单处理进度，或补充图片证据和说明。", "查看投诉", "/complaint/list", params())),
        List.of(
            new AiAssistantAction("查看投诉", null, "/complaint/list", params()),
            new AiAssistantAction("提交投诉", null, "/complaint/submit", params()))));
  }

  private int imageCount(String urls) {
    if (urls == null || urls.isBlank()) return 0;
    return (int) java.util.Arrays.stream(urls.split(",")).filter(s -> !s.isBlank()).count();
  }

  private TicketRow mapTicket(ResultSet rs, int rowNum) throws SQLException {
    return new TicketRow(
        rs.getLong("id"),
        rs.getString("ticket_no"),
        rs.getString("title"),
        rs.getString("category"),
        rs.getString("detail"),
        rs.getString("evidence_urls"),
        rs.getString("status"),
        rs.getString("store_name"),
        rs.getString("order_no"),
        rs.getInt("log_count"));
  }

  record TicketRow(long id, String ticketNo, String title, String category, String detail, String evidenceUrls,
                   String status, String storeName, String orderNo, int logCount) {}
}
