package com.aituan.ai;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class OrderLookupSkill implements AiSkill {
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
  private final JdbcTemplate jdbcTemplate;

  OrderLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "order_lookup";
  }

  @Override
  public String description() {
    return "读取用户最近订单、支付和配送状态，辅助回答订单、退款、催单问题";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    String text = context.normalizedMessage();
    if (!containsAny(text, "订单", "配送", "退款", "取消", "催单", "骑手", "到哪", "支付")) {
      return Optional.empty();
    }
    List<OrderRow> orders = jdbcTemplate.query(
        """
        select id, order_no, store_name, display_status, fulfillment_status, refund_status,
               payable_amount, created_at
        from order_main
        where user_id = ? and is_deleted = 0
        order by created_at desc, id desc
        limit 3
        """,
        this::mapOrder,
        context.currentUser().userId());
    if (orders.isEmpty()) {
      return Optional.of(AiSkillResult.text(
          name(),
          "订单查询",
          "未查询到该用户近期订单。可引导用户确认是否使用当前账号下单，或转人工补充手机号/订单号。"));
    }
    StringBuilder summary = new StringBuilder("用户最近订单：");
    for (OrderRow order : orders) {
      summary.append("\n- ")
          .append(order.orderNo())
          .append("，")
          .append(order.storeName())
          .append("，状态 ")
          .append(order.displayStatus())
          .append("/")
          .append(order.fulfillmentStatus())
          .append(order.refundStatus() == null ? "" : "/" + order.refundStatus())
          .append("，实付 ")
          .append(order.payableAmount())
          .append("，")
          .append(order.createdAt() == null ? "时间未知" : TIME_FORMATTER.format(order.createdAt()));
    }
    List<AiAssistantCard> cards = orders.stream()
        .map(order -> new AiAssistantCard(
            "order",
            order.storeName(),
            order.orderNo() + " · " + order.displayStatus() + " · 实付 " + order.payableAmount(),
            "查看订单",
            "/order/detail",
            java.util.Map.of("orderId", order.id())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "订单查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("查看全部订单", null, "/orders", java.util.Map.of()))));
  }

  private OrderRow mapOrder(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new OrderRow(
        rs.getLong("id"),
        rs.getString("order_no"),
        rs.getString("store_name"),
        rs.getString("display_status"),
        rs.getString("fulfillment_status"),
        safeString(rs, "refund_status"),
        rs.getBigDecimal("payable_amount"),
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  private String safeString(ResultSet rs, String column) {
    try {
      return rs.getString(column);
    } catch (SQLException ignored) {
      return null;
    }
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  record OrderRow(
      long id,
      String orderNo,
      String storeName,
      String displayStatus,
      String fulfillmentStatus,
      String refundStatus,
      BigDecimal payableAmount,
      LocalDateTime createdAt) {}
}
