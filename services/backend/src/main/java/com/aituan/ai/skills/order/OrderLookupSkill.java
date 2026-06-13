package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.limit;
import static com.aituan.ai.AiSkillSupport.money;
import static com.aituan.ai.AiSkillSupport.params;
import static com.aituan.ai.AiSkillSupport.shouldRun;

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
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");
  private static final List<String> WORDS = List.of(
      "订单", "下单", "买了", "支付", "付款", "实付", "退款", "退单", "取消", "催单", "配送", "骑手", "到哪", "取餐", "券码", "核销", "预约");

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
    return "读取当前用户真实订单、订单商品、退款、配送、券码和预约信息";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<OrderRow> orders = jdbcTemplate.query(
        """
        select o.id, o.order_no, o.store_id, o.store_name, o.order_type, o.title,
               o.display_status, o.payment_status, o.fulfillment_status, o.refund_status,
               o.payable_amount, o.address_snapshot, o.voucher_summary, o.remark, o.created_at,
               dt.current_stage_text, dt.eta_minutes,
               br.booking_date, br.booking_time_slot, br.store_confirm_status,
               rr.status as latest_refund_status, rr.reason as latest_refund_reason
        from order_main o
        left join delivery_task dt on dt.order_id = o.id and dt.is_deleted = 0
        left join order_booking_record br on br.order_id = o.id and br.is_deleted = 0
        left join order_refund_record rr on rr.order_id = o.id and rr.is_deleted = 0
        where o.user_id = ? and o.is_deleted = 0
        order by o.created_at desc, o.id desc
        limit 5
        """,
        this::mapOrder,
        context.currentUser().userId());
    if (orders.isEmpty()) {
      return Optional.of(AiSkillResult.text(name(), "订单查询", "当前账号没有查到订单记录。请确认登录账号是否为下单账号。"));
    }

    StringBuilder summary = new StringBuilder("当前用户最近订单：");
    for (OrderRow order : orders) {
      summary.append("\n- ")
          .append(order.orderNo()).append("，")
          .append(order.storeName()).append("，")
          .append(statusText(order)).append("，实付 ")
          .append(money(order.payableAmount()))
          .append(order.createdAt() == null ? "" : "，" + TIME.format(order.createdAt()));
      String itemText = orderItemSummary(order.id());
      if (!itemText.isBlank()) summary.append("，商品：").append(itemText);
      if (order.currentStageText() != null) {
        summary.append("，配送：").append(order.currentStageText()).append("，预计 ").append(order.etaMinutes()).append(" 分钟");
      }
      if (order.bookingDate() != null) {
        summary.append("，预约：").append(order.bookingDate()).append(" ").append(order.bookingTimeSlot())
            .append("，确认状态 ").append(order.bookingStatus());
      }
      if (order.latestRefundStatus() != null) {
        summary.append("，退款 ").append(order.latestRefundStatus())
            .append(order.latestRefundReason() == null ? "" : "（" + order.latestRefundReason() + "）");
      }
    }

    List<AiAssistantCard> cards = orders.stream()
        .map(order -> new AiAssistantCard(
            "order",
            order.storeName(),
            order.orderNo() + " · " + statusText(order) + " · 实付 " + money(order.payableAmount()),
            "查看订单",
            "/order/detail",
            params("orderId", order.id(), "storeId", order.storeId(), "orderType", order.orderType())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "订单查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("查看全部订单", null, "/orders", params()))));
  }

  private String orderItemSummary(long orderId) {
    List<String> items = jdbcTemplate.query(
        """
        select item_name, quantity
        from order_item
        where order_id = ? and is_deleted = 0
        order by id
        limit 4
        """,
        (rs, rowNum) -> rs.getString("item_name") + "x" + rs.getInt("quantity"),
        orderId);
    return limit(String.join("、", items), 80);
  }

  private String statusText(OrderRow row) {
    String refund = row.refundStatus() == null || "none".equals(row.refundStatus()) ? "" : "/" + row.refundStatus();
    return row.displayStatus() + "/" + row.paymentStatus() + "/" + row.fulfillmentStatus() + refund;
  }

  private OrderRow mapOrder(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new OrderRow(
        rs.getLong("id"),
        rs.getString("order_no"),
        rs.getLong("store_id"),
        rs.getString("store_name"),
        rs.getString("order_type"),
        rs.getString("title"),
        rs.getString("display_status"),
        rs.getString("payment_status"),
        rs.getString("fulfillment_status"),
        rs.getString("refund_status"),
        rs.getBigDecimal("payable_amount"),
        rs.getString("address_snapshot"),
        rs.getString("voucher_summary"),
        rs.getString("remark"),
        createdAt == null ? null : createdAt.toLocalDateTime(),
        rs.getString("current_stage_text"),
        rs.getInt("eta_minutes"),
        rs.getString("booking_date"),
        rs.getString("booking_time_slot"),
        rs.getString("store_confirm_status"),
        rs.getString("latest_refund_status"),
        rs.getString("latest_refund_reason"));
  }

  record OrderRow(
      long id,
      String orderNo,
      long storeId,
      String storeName,
      String orderType,
      String title,
      String displayStatus,
      String paymentStatus,
      String fulfillmentStatus,
      String refundStatus,
      BigDecimal payableAmount,
      String addressSnapshot,
      String voucherSummary,
      String remark,
      LocalDateTime createdAt,
      String currentStageText,
      int etaMinutes,
      String bookingDate,
      String bookingTimeSlot,
      String bookingStatus,
      String latestRefundStatus,
      String latestRefundReason) {}
}
