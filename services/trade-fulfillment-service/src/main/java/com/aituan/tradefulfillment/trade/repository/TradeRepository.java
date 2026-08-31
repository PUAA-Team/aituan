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
        "select id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at from delivery_task where order_id = ? and is_deleted = 0 limit 1",
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
        rs.getInt("eta_minutes"), toLocalDateTime(rs.getTimestamp("next_tick_at")), toLocalDateTime(rs.getTimestamp("completed_at")));
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
                                LocalDateTime nextTickAt, LocalDateTime completedAt) {}

  public record TimelineRow(String code, String text, LocalDateTime reachedAt) {}

  public record BookingRow(Long id, Long orderId, String businessType, String contactName, String contactPhone, String bookingDate,
                           String bookingTimeSlot, int guestCount, String storeConfirmStatus, String storeConfirmRemark,
                           LocalDateTime confirmedAt, Long confirmedBy, LocalDateTime createdAt) {}
}
