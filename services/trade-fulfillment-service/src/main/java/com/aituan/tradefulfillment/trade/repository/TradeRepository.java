package com.aituan.tradefulfillment.trade.repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TradeRepository {
  private final JdbcTemplate jdbcTemplate;

  public TradeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public long getOrCreateCart(long userId, long storeId) {
    Optional<Long> existing = findCartId(userId, storeId);
    if (existing.isPresent()) return existing.get();
    jdbcTemplate.update("insert into cart(user_id, store_id) values (?, ?)", userId, storeId);
    return findCartId(userId, storeId).orElseThrow();
  }

  public int findCartItemQuantity(long cartId, long itemId) {
    List<Integer> rows = jdbcTemplate.query(
        "select quantity from cart_item where cart_id = ? and item_id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> rs.getInt("quantity"), cartId, itemId);
    return rows.stream().findFirst().orElse(0);
  }

  public List<CartItemRow> listCartItems(long cartId) {
    return jdbcTemplate.query(
        """
        select item_id, quantity
        from cart_item
        where cart_id = ? and is_deleted = 0
        order by updated_at desc, id desc
        """,
        (rs, rowNum) -> new CartItemRow(rs.getLong("item_id"), rs.getInt("quantity")), cartId);
  }

  public void upsertCartItem(long cartId, long itemId, int quantity) {
    int updated = jdbcTemplate.update(
        "update cart_item set quantity = quantity + ?, is_deleted = 0, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        quantity, cartId, itemId);
    if (updated == 0) {
      jdbcTemplate.update("insert into cart_item(cart_id, item_id, quantity) values (?, ?, ?)", cartId, itemId, quantity);
    }
    touchCart(cartId);
  }

  public void setCartItemQuantity(long cartId, long itemId, int quantity) {
    if (quantity <= 0) {
      removeCartItem(cartId, itemId);
      return;
    }
    int updated = jdbcTemplate.update(
        "update cart_item set quantity = ?, is_deleted = 0, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        quantity, cartId, itemId);
    if (updated == 0) {
      jdbcTemplate.update("insert into cart_item(cart_id, item_id, quantity) values (?, ?, ?)", cartId, itemId, quantity);
    }
    touchCart(cartId);
  }

  public void removeCartItem(long cartId, long itemId) {
    jdbcTemplate.update(
        "update cart_item set quantity = 0, is_deleted = 1, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        cartId, itemId);
    touchCart(cartId);
  }

  public void clearCart(long cartId) {
    jdbcTemplate.update(
        "update cart_item set quantity = 0, is_deleted = 1, updated_at = current_timestamp where cart_id = ? and is_deleted = 0",
        cartId);
    touchCart(cartId);
  }

  public Long insertOrder(OrderInsertRow order) {
    jdbcTemplate.update(
        """
        insert into order_main(order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status,
                               fulfillment_status, payment_method, amount, delivery_fee, package_fee, discount_amount, payable_amount,
                               address_snapshot, delivery_distance_km, estimated_arrival_at, voucher_summary, tableware_option, tableware_count, remark, idempotency_key)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        order.orderNo(), order.userId(), order.storeId(), order.storeName(), order.orderType(), order.title(),
        order.displayStatus(), order.paymentStatus(), order.fulfillmentStatus(), order.paymentMethod(), order.amount(),
        order.deliveryFee(), order.packageFee(), order.discountAmount(), order.payableAmount(), order.addressSnapshot(),
        order.deliveryDistanceKm(), toTimestamp(order.estimatedArrivalAt()), order.voucherSummary(), order.tablewareOption(),
        order.tablewareCount(), order.remark(), order.idempotencyKey());
    return jdbcTemplate.queryForObject("select max(id) from order_main", Long.class);
  }

  public void insertOrderItem(long orderId, OrderItemInsertRow item) {
    jdbcTemplate.update(
        """
        insert into order_item(order_id, item_id, item_name, item_subtitle, business_type, category_id, quantity, unit_price, total_price, cover_url)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        orderId, item.itemId(), item.itemName(), item.subtitle(), item.businessType(), item.categoryId(), item.quantity(),
        item.unitPrice(), item.totalPrice(), item.coverUrl());
  }

  public Optional<OrderRow> findOrderById(long orderId) {
    return queryOrders("where id = ? and is_deleted = 0 limit 1", orderId).stream().findFirst();
  }

  public Optional<OrderRow> findOrderByIdempotency(long userId, String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) return Optional.empty();
    return queryOrders("where user_id = ? and idempotency_key = ? and is_deleted = 0 limit 1", userId, idempotencyKey)
        .stream().findFirst();
  }

  public List<OrderRow> listOrders(long userId, String displayStatus, int offset, int limit) {
    if (displayStatus == null || displayStatus.isBlank()) {
      return queryOrders("where user_id = ? and is_deleted = 0 order by created_at desc, id desc limit ? offset ?", userId, limit, offset);
    }
    return queryOrders("where user_id = ? and display_status = ? and is_deleted = 0 order by created_at desc, id desc limit ? offset ?", userId, displayStatus, limit, offset);
  }

  public long countOrders(long userId, String displayStatus) {
    Long count = displayStatus == null || displayStatus.isBlank()
        ? jdbcTemplate.queryForObject("select count(1) from order_main where user_id = ? and is_deleted = 0", Long.class, userId)
        : jdbcTemplate.queryForObject("select count(1) from order_main where user_id = ? and display_status = ? and is_deleted = 0", Long.class, userId, displayStatus);
    return count == null ? 0 : count;
  }

  public List<OrderItemRow> listOrderItems(long orderId) {
    return jdbcTemplate.query(
        """
        select id, order_id, item_id, item_name, item_subtitle, business_type, category_id, quantity, unit_price, total_price, cover_url, is_reviewed
        from order_item where order_id = ? and is_deleted = 0 order by id
        """, this::mapOrderItem, orderId);
  }

  public void markPaymentSuccess(long orderId, String paymentMethod, BigDecimal amount) {
    jdbcTemplate.update(
        "update order_main set payment_status = 'paid', payment_method = ?, paid_at = current_timestamp, updated_at = current_timestamp where id = ?",
        paymentMethod, orderId);
    jdbcTemplate.update(
        "insert into order_payment_record(order_id, payment_no, payment_method, amount, status, provider_trade_no, paid_at) values (?, ?, ?, ?, 'paid', ?, current_timestamp)",
        orderId, "PAY" + orderId + System.currentTimeMillis(), paymentMethod, amount, "MOCK" + orderId + System.currentTimeMillis());
  }

  public void updateTakeawayAfterPaid(long orderId, String fulfillmentStatus) {
    jdbcTemplate.update("update order_main set display_status = 'pending', fulfillment_status = ?, updated_at = current_timestamp where id = ?", fulfillmentStatus, orderId);
  }

  public void updateServiceAfterPaid(long orderId, String voucherSummary) {
    jdbcTemplate.update("update order_main set display_status = 'unused', fulfillment_status = 'voucher_unused', voucher_summary = ?, updated_at = current_timestamp where id = ?", voucherSummary, orderId);
  }

  public void insertVoucher(long orderId, String voucherCode, String qrPayload, LocalDateTime effectiveTo) {
    jdbcTemplate.update(
        "insert into order_voucher(order_id, voucher_code, qr_payload, status, effective_from, effective_to) values (?, ?, ?, 'unused', current_timestamp, ?)",
        orderId, voucherCode, qrPayload, toTimestamp(effectiveTo));
  }

  public Optional<VoucherRow> findVoucher(long orderId) {
    List<VoucherRow> rows = jdbcTemplate.query(
        "select id, order_id, voucher_code, qr_payload, status, effective_from, effective_to, verified_at, verified_by from order_voucher where order_id = ? and is_deleted = 0 limit 1",
        this::mapVoucher, orderId);
    return rows.stream().findFirst();
  }

  public void insertDeliveryTask(long orderId, String stage, String text, int etaMinutes) {
    jdbcTemplate.update(
        "insert into delivery_task(order_id, current_stage, current_stage_text, eta_minutes) values (?, ?, ?, ?)",
        orderId, stage, text, etaMinutes);
    Long taskId = jdbcTemplate.queryForObject("select id from delivery_task where order_id = ?", Long.class, orderId);
    insertDeliveryNode(taskId, 1, "merchant_pending", "待商家接单", "merchant_pending".equals(stage));
    insertDeliveryNode(taskId, 2, "accepted", "商家已接单", "accepted".equals(stage));
    insertDeliveryNode(taskId, 3, "preparing", "商家正在备餐", false);
    insertDeliveryNode(taskId, 4, "ready_for_delivery", "餐品已出餐，待配送", false);
    insertDeliveryNode(taskId, 5, "delivering", "骑手正在配送", false);
    insertDeliveryNode(taskId, 6, "delivered", "订单已送达", false);
    insertDeliveryNode(taskId, 7, "completed", "订单已完成", false);
  }

  public Optional<DeliveryTaskRow> findDeliveryTask(long orderId) {
    List<DeliveryTaskRow> rows = jdbcTemplate.query(
        """
        select id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at,
               auto_advance_enabled, paused_at, abnormal_reason, updated_at
        from delivery_task where order_id = ? and is_deleted = 0 limit 1
        """,
        this::mapDeliveryTask, orderId);
    return rows.stream().findFirst();
  }

  public List<TimelineRow> listDeliveryTimeline(long orderId) {
    return jdbcTemplate.query(
        """
        select n.node_code, n.node_text, n.reached_at
        from delivery_track_node n
        join delivery_task t on t.id = n.delivery_task_id
        where t.order_id = ? and t.is_deleted = 0 and n.is_deleted = 0
        order by n.node_order
        """,
        (rs, rowNum) -> new TimelineRow(rs.getString("node_code"), rs.getString("node_text"), toLocalDateTime(rs.getTimestamp("reached_at"))), orderId);
  }

  public void insertRefundRecord(long orderId, String refundNo, long userId, long storeId, BigDecimal amount, String initiatorType, Long initiatorId, String reason) {
    jdbcTemplate.update(
        """
        insert into order_refund_record(refund_no, order_id, user_id, store_id, refund_amount, status, initiator_type, initiator_id, reason, provider_refund_no, completed_at)
        values (?, ?, ?, ?, ?, 'succeeded', ?, ?, ?, ?, current_timestamp)
        """,
        refundNo, orderId, userId, storeId, amount, initiatorType, initiatorId, reason, "MOCK-REFUND-" + refundNo);
  }

  public void markOrderRefunded(long orderId, BigDecimal amount, String reason, String initiatorType, Long initiatorId) {
    jdbcTemplate.update(
        """
        update order_main
        set display_status = 'refunded', payment_status = 'refunded', fulfillment_status = 'refunded', refund_status = 'succeeded',
            refund_amount = ?, refund_reason = ?, refunded_at = current_timestamp, refund_initiator_type = ?, refund_initiator_id = ?,
            completed_at = coalesce(completed_at, current_timestamp), updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """, amount, reason, initiatorType, initiatorId, orderId);
    jdbcTemplate.update("update order_payment_record set status = 'refunded', updated_at = current_timestamp where order_id = ? and is_deleted = 0 and status = 'paid'", orderId);
  }

  public void markVoucherRefunded(long orderId) {
    jdbcTemplate.update("update order_voucher set status = 'refunded', updated_at = current_timestamp where order_id = ? and is_deleted = 0", orderId);
  }

  public void markDeliveryTaskRefunded(long orderId) {
    jdbcTemplate.update("update delivery_task set current_stage = 'refunded', current_stage_text = '订单已退款', next_tick_at = null, completed_at = current_timestamp, updated_at = current_timestamp where order_id = ? and is_deleted = 0", orderId);
  }

  public void upsertBooking(long orderId, String businessType, String contactName, String contactPhone, String bookingDate, String bookingTimeSlot, int guestCount, String remark) {
    int updated = jdbcTemplate.update(
        """
        update order_booking_record
        set contact_name = ?, contact_phone = ?, booking_date = ?, booking_time_slot = ?, guest_count = ?, store_confirm_remark = ?, updated_at = current_timestamp
        where order_id = ? and is_deleted = 0
        """, contactName, contactPhone, toDate(bookingDate), bookingTimeSlot, guestCount, remark, orderId);
    if (updated == 0) {
      jdbcTemplate.update(
          """
          insert into order_booking_record(order_id, business_type, contact_name, contact_phone, booking_date, booking_time_slot, guest_count, store_confirm_status, store_confirm_remark)
          values (?, ?, ?, ?, ?, ?, ?, 'pending', ?)
          """, orderId, businessType, contactName, contactPhone, toDate(bookingDate), bookingTimeSlot, guestCount, remark);
    }
  }

  public Optional<BookingRow> findBookingByOrder(long orderId) {
    List<BookingRow> rows = jdbcTemplate.query(
        """
        select id, order_id, business_type, contact_name, contact_phone, booking_date, booking_time_slot, guest_count, store_confirm_status, store_confirm_remark, confirmed_at, confirmed_by, created_at
        from order_booking_record where order_id = ? and is_deleted = 0 limit 1
        """, this::mapBooking, orderId);
    return rows.stream().findFirst();
  }

  public List<OrderRow> listOpsOrders(String displayStatus, String fulfillmentStatus, int offset, int limit) {
    StringBuilder where = new StringBuilder("where is_deleted = 0");
    List<Object> args = new java.util.ArrayList<>();
    if (displayStatus != null && !displayStatus.isBlank()) {
      where.append(" and display_status = ?");
      args.add(displayStatus);
    }
    if (fulfillmentStatus != null && !fulfillmentStatus.isBlank()) {
      where.append(" and fulfillment_status = ?");
      args.add(fulfillmentStatus);
    }
    where.append(" order by created_at desc, id desc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return queryOrders(where.toString(), args.toArray());
  }

  public long countOpsOrders(String displayStatus, String fulfillmentStatus) {
    StringBuilder sql = new StringBuilder("select count(1) from order_main where is_deleted = 0");
    List<Object> args = new java.util.ArrayList<>();
    if (displayStatus != null && !displayStatus.isBlank()) {
      sql.append(" and display_status = ?");
      args.add(displayStatus);
    }
    if (fulfillmentStatus != null && !fulfillmentStatus.isBlank()) {
      sql.append(" and fulfillment_status = ?");
      args.add(fulfillmentStatus);
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return count == null ? 0 : count;
  }

  public List<StatusCountRow> orderStatusCounts() {
    return jdbcTemplate.query(
        "select display_status, count(1) total from order_main where is_deleted = 0 group by display_status order by display_status",
        (rs, rowNum) -> new StatusCountRow(rs.getString("display_status"), rs.getLong("total")));
  }

  public int updateTakeawayStage(long orderId, String displayStatus, String fulfillmentStatus, boolean completed) {
    return jdbcTemplate.update(
        """
        update order_main
        set display_status = ?, fulfillment_status = ?, completed_at = case when ? = 1 then current_timestamp else completed_at end, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """, displayStatus, fulfillmentStatus, completed ? 1 : 0, orderId);
  }

  public int updateDeliveryTaskStage(long orderId, String expectedStage, String nextStage, String nextText, boolean completed) {
    int updated = jdbcTemplate.update(
        """
        update delivery_task
        set current_stage = ?, current_stage_text = ?, completed_at = case when ? = 1 then current_timestamp else completed_at end,
            next_tick_at = null, last_advanced_at = current_timestamp, updated_at = current_timestamp
        where order_id = ? and current_stage = ? and is_deleted = 0
        """, nextStage, nextText, completed ? 1 : 0, orderId, expectedStage);
    if (updated > 0) {
      jdbcTemplate.update(
          """
          update delivery_track_node
          set reached_at = coalesce(reached_at, current_timestamp), updated_at = current_timestamp
          where delivery_task_id = (select id from delivery_task where order_id = ? and is_deleted = 0 limit 1)
            and node_code = ? and is_deleted = 0
          """, orderId, nextStage);
    }
    return updated;
  }

  public void insertOrderStateLog(long orderId, String fromStatus, String toStatus, String actionType, String operatorType, Long operatorId, String remark) {
    jdbcTemplate.update(
        "insert into order_state_log(order_id, from_status, to_status, action_type, operator_type, operator_id, remark) values (?, ?, ?, ?, ?, ?, ?)",
        orderId, fromStatus, toStatus, actionType, operatorType, operatorId, remark);
  }

  public Optional<VoucherRow> findVoucherByCode(String voucherCode) {
    List<VoucherRow> rows = jdbcTemplate.query(
        "select id, order_id, voucher_code, qr_payload, status, effective_from, effective_to, verified_at, verified_by from order_voucher where voucher_code = ? and is_deleted = 0 limit 1",
        this::mapVoucher, voucherCode);
    return rows.stream().findFirst();
  }

  public void setVoucherUsed(long orderId, long operatorId) {
    jdbcTemplate.update("update order_voucher set status = 'used', verified_at = current_timestamp, verified_by = ?, updated_at = current_timestamp where order_id = ? and status = 'unused' and is_deleted = 0", operatorId, orderId);
    jdbcTemplate.update("update order_main set display_status = 'used', fulfillment_status = 'voucher_used', completed_at = current_timestamp, updated_at = current_timestamp where id = ? and is_deleted = 0", orderId);
  }

  public List<OpsVoucherRow> listOpsVouchers(String status, String keyword, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select v.id, v.order_id, v.voucher_code, v.qr_payload, v.status, v.effective_from, v.effective_to, v.verified_at, v.verified_by,
               o.order_no, o.title, o.store_name, o.order_type, o.payable_amount, o.display_status, o.refund_status, o.created_at
        from order_voucher v join order_main o on o.id = v.order_id
        where v.is_deleted = 0 and o.is_deleted = 0
        """);
    List<Object> args = new java.util.ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and v.status = ?");
      args.add(status);
    }
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and (v.voucher_code like ? or o.order_no like ? or o.title like ?)");
      String like = "%" + keyword + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    sql.append(" order by o.created_at desc, v.id desc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapOpsVoucher, args.toArray());
  }

  public long countOpsVouchers(String status, String keyword) {
    StringBuilder sql = new StringBuilder("select count(1) from order_voucher v join order_main o on o.id = v.order_id where v.is_deleted = 0 and o.is_deleted = 0");
    List<Object> args = new java.util.ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and v.status = ?");
      args.add(status);
    }
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and (v.voucher_code like ? or o.order_no like ? or o.title like ?)");
      String like = "%" + keyword + "%";
      args.add(like);
      args.add(like);
      args.add(like);
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return count == null ? 0 : count;
  }

  public List<OpsBookingRow> listOpsBookings(String status, String businessType, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select b.id, b.order_id, b.business_type, b.contact_name, b.contact_phone, b.booking_date, b.booking_time_slot, b.guest_count,
               b.store_confirm_status, b.store_confirm_remark, b.confirmed_at, b.confirmed_by, b.created_at,
               o.order_no, o.title, o.store_name, o.display_status, o.payment_status, o.refund_status, o.payable_amount
        from order_booking_record b join order_main o on o.id = b.order_id
        where b.is_deleted = 0 and o.is_deleted = 0
        """);
    List<Object> args = new java.util.ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and b.store_confirm_status = ?");
      args.add(status);
    }
    if (businessType != null && !businessType.isBlank()) {
      sql.append(" and b.business_type = ?");
      args.add(businessType);
    }
    sql.append(" order by b.created_at desc, b.id desc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapOpsBooking, args.toArray());
  }

  public long countOpsBookings(String status, String businessType) {
    StringBuilder sql = new StringBuilder("select count(1) from order_booking_record b join order_main o on o.id = b.order_id where b.is_deleted = 0 and o.is_deleted = 0");
    List<Object> args = new java.util.ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and b.store_confirm_status = ?");
      args.add(status);
    }
    if (businessType != null && !businessType.isBlank()) {
      sql.append(" and b.business_type = ?");
      args.add(businessType);
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return count == null ? 0 : count;
  }

  public void confirmBooking(long orderId, Long operatorId, String remark) {
    jdbcTemplate.update("update order_booking_record set store_confirm_status = 'confirmed', store_confirm_remark = ?, confirmed_at = current_timestamp, confirmed_by = ?, updated_at = current_timestamp where order_id = ? and is_deleted = 0", remark, operatorId, orderId);
  }

  public Optional<DeliveryTaskRow> findDeliveryTaskById(long taskId) {
    List<DeliveryTaskRow> rows = jdbcTemplate.query(
        """
        select id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at,
               auto_advance_enabled, paused_at, abnormal_reason, updated_at
        from delivery_task where id = ? and is_deleted = 0 limit 1
        """, this::mapDeliveryTask, taskId);
    return rows.stream().findFirst();
  }

  public List<DeliveryTaskRow> listDeliveryTasks(String stage, int offset, int limit) {
    if (stage == null || stage.isBlank()) {
      return jdbcTemplate.query(
          """
          select id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at,
                 auto_advance_enabled, paused_at, abnormal_reason, updated_at
          from delivery_task where is_deleted = 0 order by updated_at desc, id desc limit ? offset ?
          """, this::mapDeliveryTask, limit, offset);
    }
    return jdbcTemplate.query(
        """
        select id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at,
               auto_advance_enabled, paused_at, abnormal_reason, updated_at
        from delivery_task where current_stage = ? and is_deleted = 0 order by updated_at desc, id desc limit ? offset ?
        """, this::mapDeliveryTask, stage, limit, offset);
  }

  public long countDeliveryTasks(String stage) {
    Long count = stage == null || stage.isBlank()
        ? jdbcTemplate.queryForObject("select count(1) from delivery_task where is_deleted = 0", Long.class)
        : jdbcTemplate.queryForObject("select count(1) from delivery_task where current_stage = ? and is_deleted = 0", Long.class, stage);
    return count == null ? 0 : count;
  }

  public void pauseDeliveryTask(long taskId) {
    jdbcTemplate.update("update delivery_task set auto_advance_enabled = 0, paused_at = current_timestamp, updated_at = current_timestamp where id = ? and is_deleted = 0", taskId);
  }

  public void resumeDeliveryTask(long taskId) {
    jdbcTemplate.update("update delivery_task set auto_advance_enabled = 1, paused_at = null, updated_at = current_timestamp where id = ? and is_deleted = 0", taskId);
  }

  public void markDeliveryTaskAbnormal(long taskId, String reason) {
    jdbcTemplate.update("update delivery_task set current_stage = 'abnormal', current_stage_text = '订单异常，待处理', abnormal_reason = ?, auto_advance_enabled = 0, updated_at = current_timestamp where id = ? and is_deleted = 0", reason, taskId);
  }

  private Optional<Long> findCartId(long userId, long storeId) {
    List<Long> rows = jdbcTemplate.query("select id from cart where user_id = ? and store_id = ? and is_deleted = 0 limit 1", (rs, rowNum) -> rs.getLong("id"), userId, storeId);
    return rows.stream().findFirst();
  }

  private List<OrderRow> queryOrders(String where, Object... args) {
    return jdbcTemplate.query(
        """
        select id, order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status, fulfillment_status,
               payment_method, amount, delivery_fee, package_fee, discount_amount, payable_amount, address_snapshot, delivery_distance_km,
               estimated_arrival_at, voucher_summary, tableware_option, tableware_count, remark, refund_status, refund_amount, refund_reason,
               refunded_at, refund_initiator_type, refund_initiator_id, paid_at, completed_at, created_at, updated_at
        from order_main
        """ + where, this::mapOrder, args);
  }

  private void touchCart(long cartId) {
    jdbcTemplate.update("update cart set updated_at = current_timestamp where id = ?", cartId);
  }

  private void insertDeliveryNode(Long taskId, int order, String code, String text, boolean reached) {
    jdbcTemplate.update(
        "insert into delivery_track_node(delivery_task_id, node_order, node_code, node_text, reached_at) values (?, ?, ?, ?, ?)",
        taskId, order, code, text, reached ? Timestamp.valueOf(LocalDateTime.now()) : null);
  }

  private OrderRow mapOrder(ResultSet rs, int rowNum) throws SQLException {
    return new OrderRow(
        rs.getLong("id"), rs.getString("order_no"), rs.getLong("user_id"), rs.getLong("store_id"), rs.getString("store_name"),
        rs.getString("order_type"), rs.getString("title"), rs.getString("display_status"), rs.getString("payment_status"),
        rs.getString("fulfillment_status"), rs.getString("payment_method"), rs.getBigDecimal("amount"), rs.getBigDecimal("delivery_fee"),
        rs.getBigDecimal("package_fee"), rs.getBigDecimal("discount_amount"), rs.getBigDecimal("payable_amount"), rs.getString("address_snapshot"),
        rs.getBigDecimal("delivery_distance_km"), toLocalDateTime(rs.getTimestamp("estimated_arrival_at")), rs.getString("voucher_summary"),
        rs.getString("tableware_option"), (Integer) rs.getObject("tableware_count"), rs.getString("remark"), rs.getString("refund_status"),
        rs.getBigDecimal("refund_amount"), rs.getString("refund_reason"), toLocalDateTime(rs.getTimestamp("refunded_at")), rs.getString("refund_initiator_type"),
        nullableLong(rs, "refund_initiator_id"), toLocalDateTime(rs.getTimestamp("paid_at")), toLocalDateTime(rs.getTimestamp("completed_at")),
        toLocalDateTime(rs.getTimestamp("created_at")), toLocalDateTime(rs.getTimestamp("updated_at")));
  }

  private OrderItemRow mapOrderItem(ResultSet rs, int rowNum) throws SQLException {
    return new OrderItemRow(rs.getLong("id"), rs.getLong("order_id"), rs.getLong("item_id"), rs.getString("item_name"),
        rs.getString("item_subtitle"), rs.getString("business_type"), rs.getLong("category_id"), rs.getInt("quantity"),
        rs.getBigDecimal("unit_price"), rs.getBigDecimal("total_price"), rs.getString("cover_url"), rs.getBoolean("is_reviewed"));
  }

  private VoucherRow mapVoucher(ResultSet rs, int rowNum) throws SQLException {
    long verifiedBy = rs.getLong("verified_by");
    boolean verifiedByNull = rs.wasNull();
    return new VoucherRow(rs.getLong("id"), rs.getLong("order_id"), rs.getString("voucher_code"), rs.getString("qr_payload"), rs.getString("status"),
        toLocalDateTime(rs.getTimestamp("effective_from")), toLocalDateTime(rs.getTimestamp("effective_to")), toLocalDateTime(rs.getTimestamp("verified_at")),
        verifiedByNull ? null : verifiedBy);
  }

  private DeliveryTaskRow mapDeliveryTask(ResultSet rs, int rowNum) throws SQLException {
    return new DeliveryTaskRow(rs.getLong("id"), rs.getLong("order_id"), rs.getString("current_stage"), rs.getString("current_stage_text"),
        rs.getInt("eta_minutes"), toLocalDateTime(rs.getTimestamp("next_tick_at")), toLocalDateTime(rs.getTimestamp("completed_at")),
        rs.getBoolean("auto_advance_enabled"), toLocalDateTime(rs.getTimestamp("paused_at")), rs.getString("abnormal_reason"), toLocalDateTime(rs.getTimestamp("updated_at")));
  }

  private OpsVoucherRow mapOpsVoucher(ResultSet rs, int rowNum) throws SQLException {
    long verifiedBy = rs.getLong("verified_by");
    boolean verifiedByNull = rs.wasNull();
    return new OpsVoucherRow(rs.getLong("id"), rs.getLong("order_id"), rs.getString("voucher_code"), rs.getString("qr_payload"), rs.getString("status"),
        toLocalDateTime(rs.getTimestamp("effective_from")), toLocalDateTime(rs.getTimestamp("effective_to")), toLocalDateTime(rs.getTimestamp("verified_at")),
        verifiedByNull ? null : verifiedBy, rs.getString("order_no"), rs.getString("title"), rs.getString("store_name"), rs.getString("order_type"),
        rs.getBigDecimal("payable_amount"), rs.getString("display_status"), rs.getString("refund_status"), toLocalDateTime(rs.getTimestamp("created_at")));
  }

  private OpsBookingRow mapOpsBooking(ResultSet rs, int rowNum) throws SQLException {
    long confirmedBy = rs.getLong("confirmed_by");
    boolean confirmedByNull = rs.wasNull();
    Date bookingDate = rs.getDate("booking_date");
    return new OpsBookingRow(rs.getLong("id"), rs.getLong("order_id"), rs.getString("business_type"), rs.getString("contact_name"),
        rs.getString("contact_phone"), bookingDate == null ? null : bookingDate.toString(), rs.getString("booking_time_slot"), rs.getInt("guest_count"),
        rs.getString("store_confirm_status"), rs.getString("store_confirm_remark"), toLocalDateTime(rs.getTimestamp("confirmed_at")),
        confirmedByNull ? null : confirmedBy, toLocalDateTime(rs.getTimestamp("created_at")), rs.getString("order_no"), rs.getString("title"),
        rs.getString("store_name"), rs.getString("display_status"), rs.getString("payment_status"), rs.getString("refund_status"), rs.getBigDecimal("payable_amount"));
  }

  private BookingRow mapBooking(ResultSet rs, int rowNum) throws SQLException {
    long confirmedBy = rs.getLong("confirmed_by");
    boolean confirmedByNull = rs.wasNull();
    Date bookingDate = rs.getDate("booking_date");
    return new BookingRow(rs.getLong("id"), rs.getLong("order_id"), rs.getString("business_type"), rs.getString("contact_name"),
        rs.getString("contact_phone"), bookingDate == null ? null : bookingDate.toString(), rs.getString("booking_time_slot"), rs.getInt("guest_count"),
        rs.getString("store_confirm_status"), rs.getString("store_confirm_remark"), toLocalDateTime(rs.getTimestamp("confirmed_at")),
        confirmedByNull ? null : confirmedBy, toLocalDateTime(rs.getTimestamp("created_at")));
  }

  private Timestamp toTimestamp(LocalDateTime value) {
    return value == null ? null : Timestamp.valueOf(value);
  }

  private LocalDateTime toLocalDateTime(Timestamp value) {
    return value == null ? null : value.toLocalDateTime();
  }

  private Date toDate(String value) {
    return value == null || value.isBlank() ? null : Date.valueOf(value);
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  public record CartItemRow(Long itemId, int quantity) {}

  public record OrderInsertRow(Long userId, Long storeId, String storeName, String orderType, String title, String displayStatus,
                               String paymentStatus, String fulfillmentStatus, String paymentMethod, BigDecimal amount, BigDecimal deliveryFee,
                               BigDecimal packageFee, BigDecimal discountAmount, BigDecimal payableAmount, String addressSnapshot,
                               BigDecimal deliveryDistanceKm, LocalDateTime estimatedArrivalAt, String voucherSummary, String tablewareOption,
                               Integer tablewareCount, String remark, String idempotencyKey, String orderNo) {}

  public record OrderItemInsertRow(Long itemId, String itemName, String subtitle, String businessType, Long categoryId, int quantity,
                                   BigDecimal unitPrice, BigDecimal totalPrice, String coverUrl) {}

  public record OrderRow(Long id, String orderNo, Long userId, Long storeId, String storeName, String orderType, String title,
                         String displayStatus, String paymentStatus, String fulfillmentStatus, String paymentMethod, BigDecimal amount,
                         BigDecimal deliveryFee, BigDecimal packageFee, BigDecimal discountAmount, BigDecimal payableAmount,
                         String addressSnapshot, BigDecimal deliveryDistanceKm, LocalDateTime estimatedArrivalAt, String voucherSummary,
                         String tablewareOption, Integer tablewareCount, String remark, String refundStatus, BigDecimal refundAmount,
                         String refundReason, LocalDateTime refundedAt, String refundInitiatorType, Long refundInitiatorId,
                         LocalDateTime paidAt, LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {}

  public record OrderItemRow(Long id, Long orderId, Long itemId, String itemName, String itemSubtitle, String businessType, Long categoryId,
                             int quantity, BigDecimal unitPrice, BigDecimal totalPrice, String coverUrl, boolean isReviewed) {}

  public record VoucherRow(Long id, Long orderId, String voucherCode, String qrPayload, String status, LocalDateTime effectiveFrom,
                           LocalDateTime effectiveTo, LocalDateTime verifiedAt, Long verifiedBy) {}

  public record DeliveryTaskRow(Long id, Long orderId, String currentStage, String currentStageText, int etaMinutes,
                                LocalDateTime nextTickAt, LocalDateTime completedAt, boolean autoAdvanceEnabled,
                                LocalDateTime pausedAt, String abnormalReason, LocalDateTime updatedAt) {}

  public record TimelineRow(String code, String text, LocalDateTime reachedAt) {}

  public record StatusCountRow(String status, long count) {}

  public record OpsVoucherRow(Long id, Long orderId, String voucherCode, String qrPayload, String status, LocalDateTime effectiveFrom,
                              LocalDateTime effectiveTo, LocalDateTime verifiedAt, Long verifiedBy, String orderNo, String orderTitle,
                              String storeName, String businessType, BigDecimal payableAmount, String displayStatus, String refundStatus,
                              LocalDateTime orderCreatedAt) {}

  public record BookingRow(Long id, Long orderId, String businessType, String contactName, String contactPhone, String bookingDate,
                           String bookingTimeSlot, int guestCount, String storeConfirmStatus, String storeConfirmRemark,
                           LocalDateTime confirmedAt, Long confirmedBy, LocalDateTime createdAt) {}

  public record OpsBookingRow(Long id, Long orderId, String businessType, String contactName, String contactPhone, String bookingDate,
                              String bookingTimeSlot, int guestCount, String storeConfirmStatus, String storeConfirmRemark,
                              LocalDateTime confirmedAt, Long confirmedBy, LocalDateTime createdAt, String orderNo, String orderTitle,
                              String storeName, String displayStatus, String paymentStatus, String refundStatus, BigDecimal payableAmount) {}
}
