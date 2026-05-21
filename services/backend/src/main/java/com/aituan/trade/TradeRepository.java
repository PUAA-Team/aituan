package com.aituan.trade;

import com.aituan.common.enums.BusinessType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class TradeRepository {
  private final JdbcTemplate jdbcTemplate;

  TradeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  Optional<AddressRow> findAddress(long userId, Long addressId) {
    if (addressId == null) {
      List<AddressRow> defaults = jdbcTemplate.query(
          """
          select id, user_id, contact_name, contact_phone, province, city, district, detail_address, tag_name, is_default, delivery_note
          from user_address
          where user_id = ? and is_deleted = 0 and is_default = 1
          limit 1
          """,
          this::mapAddress,
          userId);
      return defaults.stream().findFirst();
    }
    List<AddressRow> rows = jdbcTemplate.query(
        """
        select id, user_id, contact_name, contact_phone, province, city, district, detail_address, tag_name, is_default, delivery_note
        from user_address
        where user_id = ? and id = ? and is_deleted = 0
        limit 1
        """,
        this::mapAddress,
        userId,
        addressId);
    return rows.stream().findFirst();
  }

  Optional<ItemRow> findItem(long itemId) {
    List<ItemRow> rows = jdbcTemplate.query(
        """
        select i.id, i.store_id, s.store_name, i.business_type, i.category_id, c.category_name,
               i.item_name, i.subtitle, i.price, i.original_price, i.cover_url, i.rule_text, i.sales_count, i.status
        from catalog_item i
        join merchant_store s on s.id = i.store_id
        join catalog_category c on c.id = i.category_id
        where i.id = ? and i.is_deleted = 0 and i.status = 'on_sale'
        limit 1
        """,
        this::mapItem,
        itemId);
    return rows.stream().findFirst();
  }

  Optional<SkuRow> findSkuByItem(long itemId) {
    List<SkuRow> rows = jdbcTemplate.query(
        """
        select id, item_id, sku_name, price, stock, status
        from catalog_sku
        where item_id = ? and is_deleted = 0 and status = 'on_sale'
        order by id
        limit 1
        """,
        (rs, rowNum) -> new SkuRow(
            rs.getLong("id"),
            rs.getLong("item_id"),
            rs.getString("sku_name"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getString("status")),
        itemId);
    return rows.stream().findFirst();
  }

  Optional<StoreRow> findStore(long storeId) {
    List<StoreRow> rows = jdbcTemplate.query(
        """
        select id, merchant_id, store_name, business_type, summary, address, distance_text, rating,
               monthly_sales, avg_price, status, business_hours_text, tag_text, cover_url
        from merchant_store
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapStore,
        storeId);
    return rows.stream().findFirst();
  }

  Optional<DeliveryRuleRow> findDeliveryRule(long storeId) {
    List<DeliveryRuleRow> rows = jdbcTemplate.query(
        "select delivery_fee, start_price, estimated_minutes, delivery_text from merchant_delivery_rule where store_id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> new DeliveryRuleRow(
            rs.getBigDecimal("delivery_fee"),
            rs.getBigDecimal("start_price"),
            rs.getInt("estimated_minutes"),
            rs.getString("delivery_text")),
        storeId);
    return rows.stream().findFirst();
  }

  int decreaseSkuStock(long skuId, int quantity) {
    return jdbcTemplate.update(
        """
        update catalog_sku
        set stock = stock - ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0 and status = 'on_sale' and stock >= ?
        """,
        quantity,
        skuId,
        quantity);
  }

  List<DeliveryTaskRow> listDueDeliveryTasks() {
    return jdbcTemplate.query(
        """
        select id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at
        from delivery_task
        where next_tick_at is not null and next_tick_at <= current_timestamp and completed_at is null and is_deleted = 0
        order by next_tick_at, id
        """,
        this::mapDeliveryTask);
  }

  Long insertOrder(OrderInsertRow order) {
    jdbcTemplate.update(
        """
        insert into order_main(order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status,
                               fulfillment_status, payment_method, amount, delivery_fee, discount_amount, payable_amount,
                               address_snapshot, voucher_summary, remark, idempotency_key, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        order.orderNo(),
        order.userId(),
        order.storeId(),
        order.storeName(),
        order.orderType(),
        order.title(),
        order.displayStatus(),
        order.paymentStatus(),
        order.fulfillmentStatus(),
        order.paymentMethod(),
        order.amount(),
        order.deliveryFee(),
        order.discountAmount(),
        order.payableAmount(),
        order.addressSnapshot(),
        order.voucherSummary(),
        order.remark(),
        order.idempotencyKey());
    return jdbcTemplate.queryForObject("select max(id) from order_main", Long.class);
  }

  void insertOrderItem(long orderId, ItemRow item, int quantity) {
    jdbcTemplate.update(
        """
        insert into order_item(order_id, item_id, item_name, item_subtitle, business_type, category_id, quantity, unit_price, total_price, cover_url)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        orderId,
        item.id(),
        item.title(),
        item.subtitle(),
        item.businessType(),
        item.categoryId(),
        quantity,
        item.price(),
        item.price().multiply(BigDecimal.valueOf(quantity)),
        item.coverUrl());
  }

  Optional<OrderRow> findOrderById(long orderId) {
    List<OrderRow> rows = jdbcTemplate.query(
        """
        select id, order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status,
               fulfillment_status, payment_method, amount, delivery_fee, discount_amount, payable_amount,
               address_snapshot, voucher_summary, remark, paid_at, completed_at, created_at, updated_at
        from order_main
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapOrder,
        orderId);
    return rows.stream().findFirst();
  }

  Optional<OrderRow> findOrderByIdempotency(long userId, String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return Optional.empty();
    }
    List<OrderRow> rows = jdbcTemplate.query(
        """
        select id, order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status,
               fulfillment_status, payment_method, amount, delivery_fee, discount_amount, payable_amount,
               address_snapshot, voucher_summary, remark, paid_at, completed_at, created_at, updated_at
        from order_main
        where user_id = ? and idempotency_key = ? and is_deleted = 0
        limit 1
        """,
        this::mapOrder,
        userId,
        idempotencyKey);
    return rows.stream().findFirst();
  }

  List<OrderRow> listOrders(long userId, String displayStatus, int offset, int limit) {
    if (displayStatus == null || displayStatus.isBlank()) {
      return jdbcTemplate.query(
          """
          select id, order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status,
                 fulfillment_status, payment_method, amount, delivery_fee, discount_amount, payable_amount,
                 address_snapshot, voucher_summary, remark, paid_at, completed_at, created_at, updated_at
          from order_main
          where user_id = ? and is_deleted = 0
          order by created_at desc, id desc
          limit ? offset ?
          """,
          this::mapOrder,
          userId,
          limit,
          offset);
    }
    return jdbcTemplate.query(
        """
        select id, order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status,
               fulfillment_status, payment_method, amount, delivery_fee, discount_amount, payable_amount,
               address_snapshot, voucher_summary, remark, paid_at, completed_at, created_at, updated_at
        from order_main
        where user_id = ? and display_status = ? and is_deleted = 0
        order by created_at desc, id desc
        limit ? offset ?
        """,
        this::mapOrder,
        userId,
        displayStatus,
        limit,
        offset);
  }

  long countOrders(long userId, String displayStatus) {
    Long count;
    if (displayStatus == null || displayStatus.isBlank()) {
      count = jdbcTemplate.queryForObject("select count(1) from order_main where user_id = ? and is_deleted = 0", Long.class, userId);
    } else {
      count = jdbcTemplate.queryForObject(
          "select count(1) from order_main where user_id = ? and display_status = ? and is_deleted = 0",
          Long.class,
          userId,
          displayStatus);
    }
    return count == null ? 0 : count;
  }

  List<OrderItemRow> listOrderItems(long orderId) {
    return jdbcTemplate.query(
        """
        select id, order_id, item_id, item_name, item_subtitle, business_type, category_id, quantity, unit_price,
               total_price, cover_url, is_reviewed
        from order_item
        where order_id = ? and is_deleted = 0
        order by id
        """,
        this::mapOrderItem,
        orderId);
  }

  Optional<VoucherRow> findVoucher(long orderId) {
    List<VoucherRow> rows = jdbcTemplate.query(
        """
        select id, order_id, voucher_code, qr_payload, status, effective_from, effective_to, verified_at, verified_by
        from order_voucher
        where order_id = ? and is_deleted = 0
        limit 1
        """,
        this::mapVoucher,
        orderId);
    return rows.stream().findFirst();
  }

  Optional<VoucherRow> findVoucherByCode(String voucherCode) {
    List<VoucherRow> rows = jdbcTemplate.query(
        """
        select id, order_id, voucher_code, qr_payload, status, effective_from, effective_to, verified_at, verified_by
        from order_voucher
        where voucher_code = ? and is_deleted = 0
        limit 1
        """,
        this::mapVoucher,
        voucherCode);
    return rows.stream().findFirst();
  }

  Optional<DeliveryTaskRow> findDeliveryTask(long orderId) {
    List<DeliveryTaskRow> rows = jdbcTemplate.query(
        """
        select id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at
        from delivery_task
        where order_id = ? and is_deleted = 0
        limit 1
        """,
        this::mapDeliveryTask,
        orderId);
    return rows.stream().findFirst();
  }

  List<TimelineRow> listDeliveryTimeline(long orderId) {
    List<DeliveryTaskRow> taskRows = jdbcTemplate.query(
        "select id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at from delivery_task where order_id = ? and is_deleted = 0 limit 1",
        this::mapDeliveryTask,
        orderId);
    if (taskRows.isEmpty()) {
      return List.of();
    }
    long taskId = taskRows.get(0).id();
    return jdbcTemplate.query(
        """
        select node_code, node_text, reached_at
        from delivery_track_node
        where delivery_task_id = ? and is_deleted = 0
        order by node_order
        """,
        (rs, rowNum) -> new TimelineRow(
            rs.getString("node_code"),
            rs.getString("node_text"),
            rs.getTimestamp("reached_at") == null ? null : rs.getTimestamp("reached_at").toLocalDateTime()),
        taskId);
  }

  void markPaymentSuccess(long orderId, String paymentMethod, BigDecimal amount) {
    jdbcTemplate.update(
        """
        update order_main
        set payment_status = 'paid', payment_method = ?, paid_at = current_timestamp, updated_at = current_timestamp
        where id = ?
        """,
        paymentMethod,
        orderId);
    jdbcTemplate.update(
        """
        insert into order_payment_record(order_id, payment_no, payment_method, amount, status, provider_trade_no, paid_at)
        values (?, ?, ?, ?, 'paid', ?, current_timestamp)
        """,
        orderId,
        "PAY" + orderId + System.currentTimeMillis(),
        paymentMethod,
        amount,
        "MOCK" + orderId + System.currentTimeMillis());
  }

  void updateOrderAfterTakeawayPaid(long orderId) {
    jdbcTemplate.update(
        """
        update order_main
        set display_status = 'pending', fulfillment_status = 'delivering', updated_at = current_timestamp
        where id = ?
        """,
        orderId);
  }

  void updateOrderAfterServicePaid(long orderId, String voucherSummary) {
    jdbcTemplate.update(
        """
        update order_main
        set display_status = 'unused', fulfillment_status = 'voucher_unused', voucher_summary = ?, updated_at = current_timestamp
        where id = ?
        """,
        voucherSummary,
        orderId);
  }

  void insertVoucher(long orderId, String voucherCode, String qrPayload, LocalDateTime effectiveTo) {
    jdbcTemplate.update(
        """
        insert into order_voucher(order_id, voucher_code, qr_payload, status, effective_from, effective_to)
        values (?, ?, ?, 'unused', current_timestamp, ?)
        """,
        orderId,
        voucherCode,
        qrPayload,
        Timestamp.valueOf(effectiveTo));
  }

  void insertDeliveryTask(long orderId, LocalDateTime nextTickAt) {
    jdbcTemplate.update(
        """
        insert into delivery_task(order_id, current_stage, current_stage_text, eta_minutes, next_tick_at)
        values (?, 'accepted', '商家已接单', 35, ?)
        """,
        orderId,
        Timestamp.valueOf(nextTickAt));
    Long taskId = jdbcTemplate.queryForObject("select max(id) from delivery_task", Long.class);
    jdbcTemplate.update(
        "insert into delivery_track_node(delivery_task_id, node_order, node_code, node_text, reached_at) values (?, 1, 'accepted', '商家已接单', current_timestamp)",
        taskId);
    jdbcTemplate.update(
        "insert into delivery_track_node(delivery_task_id, node_order, node_code, node_text) values (?, 2, 'preparing', '商家正在备餐')",
        taskId);
    jdbcTemplate.update(
        "insert into delivery_track_node(delivery_task_id, node_order, node_code, node_text) values (?, 3, 'delivering', '骑手正在配送')",
        taskId);
    jdbcTemplate.update(
        "insert into delivery_track_node(delivery_task_id, node_order, node_code, node_text) values (?, 4, 'delivered', '订单已送达')",
        taskId);
  }

  void advanceDeliveryTask(long taskId, String currentStage, String nextStage, String nextText, boolean delivered) {
    if (delivered) {
      jdbcTemplate.update(
          """
          update delivery_task
          set current_stage = ?, current_stage_text = ?, completed_at = current_timestamp, next_tick_at = null, updated_at = current_timestamp
          where id = ? and is_deleted = 0 and current_stage = ?
          """,
          nextStage,
          nextText,
          taskId,
          currentStage);
      Long orderId = jdbcTemplate.queryForObject("select order_id from delivery_task where id = ?", Long.class, taskId);
      jdbcTemplate.update(
          """
          update order_main
          set display_status = 'used', fulfillment_status = 'delivered', completed_at = current_timestamp, updated_at = current_timestamp
          where id = ?
          """,
          orderId);
      jdbcTemplate.update(
          """
          update delivery_track_node
          set reached_at = current_timestamp, updated_at = current_timestamp
          where delivery_task_id = ? and node_code = 'delivered'
          """,
          taskId);
      return;
    }
    jdbcTemplate.update(
        """
        update delivery_task
        set current_stage = ?, current_stage_text = ?, next_tick_at = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0 and current_stage = ?
        """,
        nextStage,
        nextText,
        Timestamp.valueOf(LocalDateTime.now().plusMinutes(3)),
        taskId,
        currentStage);
    jdbcTemplate.update(
        """
        update delivery_track_node
        set reached_at = current_timestamp, updated_at = current_timestamp
        where delivery_task_id = ? and node_code = ?
        """,
        taskId,
        nextStage);
  }

  void setOrderUsed(long orderId) {
    setOrderUsed(orderId, null);
  }

  void setOrderUsed(long orderId, Long verifiedBy) {
    jdbcTemplate.update(
        "update order_main set display_status = 'used', fulfillment_status = 'voucher_used', completed_at = current_timestamp, updated_at = current_timestamp where id = ?",
        orderId);
    jdbcTemplate.update(
        "update order_voucher set status = 'used', verified_at = current_timestamp, verified_by = ?, updated_at = current_timestamp where order_id = ?",
        verifiedBy,
        orderId);
  }

  private AddressRow mapAddress(ResultSet rs, int rowNum) throws SQLException {
    return new AddressRow(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getString("contact_name"),
        rs.getString("contact_phone"),
        rs.getString("province"),
        rs.getString("city"),
        rs.getString("district"),
        rs.getString("detail_address"),
        rs.getString("tag_name"),
        rs.getBoolean("is_default"),
        rs.getString("delivery_note"));
  }

  private ItemRow mapItem(ResultSet rs, int rowNum) throws SQLException {
    return new ItemRow(
        rs.getLong("id"),
        rs.getLong("store_id"),
        rs.getString("store_name"),
        rs.getString("business_type"),
        rs.getLong("category_id"),
        rs.getString("category_name"),
        rs.getString("item_name"),
        rs.getString("subtitle"),
        rs.getBigDecimal("price"),
        rs.getBigDecimal("original_price"),
        rs.getString("cover_url"),
        rs.getString("rule_text"),
        rs.getInt("sales_count"),
        rs.getString("status"));
  }

  private StoreRow mapStore(ResultSet rs, int rowNum) throws SQLException {
    return new StoreRow(
        rs.getLong("id"),
        rs.getLong("merchant_id"),
        rs.getString("store_name"),
        rs.getString("business_type"),
        rs.getString("summary"),
        rs.getString("address"),
        rs.getString("distance_text"),
        rs.getBigDecimal("rating"),
        rs.getInt("monthly_sales"),
        rs.getBigDecimal("avg_price"),
        rs.getString("status"),
        rs.getString("business_hours_text"),
        rs.getString("tag_text"),
        rs.getString("cover_url"));
  }

  private OrderRow mapOrder(ResultSet rs, int rowNum) throws SQLException {
    Timestamp paidAt = rs.getTimestamp("paid_at");
    Timestamp completedAt = rs.getTimestamp("completed_at");
    Timestamp createdAt = rs.getTimestamp("created_at");
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new OrderRow(
        rs.getLong("id"),
        rs.getString("order_no"),
        rs.getLong("user_id"),
        rs.getLong("store_id"),
        rs.getString("store_name"),
        rs.getString("order_type"),
        rs.getString("title"),
        rs.getString("display_status"),
        rs.getString("payment_status"),
        rs.getString("fulfillment_status"),
        rs.getString("payment_method"),
        rs.getBigDecimal("amount"),
        rs.getBigDecimal("delivery_fee"),
        rs.getBigDecimal("discount_amount"),
        rs.getBigDecimal("payable_amount"),
        rs.getString("address_snapshot"),
        rs.getString("voucher_summary"),
        rs.getString("remark"),
        paidAt == null ? null : paidAt.toLocalDateTime(),
        completedAt == null ? null : completedAt.toLocalDateTime(),
        createdAt == null ? null : createdAt.toLocalDateTime(),
        updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private OrderItemRow mapOrderItem(ResultSet rs, int rowNum) throws SQLException {
    return new OrderItemRow(
        rs.getLong("id"),
        rs.getLong("order_id"),
        rs.getLong("item_id"),
        rs.getString("item_name"),
        rs.getString("item_subtitle"),
        rs.getString("business_type"),
        rs.getLong("category_id"),
        rs.getInt("quantity"),
        rs.getBigDecimal("unit_price"),
        rs.getBigDecimal("total_price"),
        rs.getString("cover_url"),
        rs.getBoolean("is_reviewed"));
  }

  private VoucherRow mapVoucher(ResultSet rs, int rowNum) throws SQLException {
    Timestamp effectiveFrom = rs.getTimestamp("effective_from");
    Timestamp effectiveTo = rs.getTimestamp("effective_to");
    Timestamp verifiedAt = rs.getTimestamp("verified_at");
    long verifiedBy = rs.getLong("verified_by");
    return new VoucherRow(
        rs.getLong("id"),
        rs.getLong("order_id"),
        rs.getString("voucher_code"),
        rs.getString("qr_payload"),
        rs.getString("status"),
        effectiveFrom == null ? null : effectiveFrom.toLocalDateTime(),
        effectiveTo == null ? null : effectiveTo.toLocalDateTime(),
        verifiedAt == null ? null : verifiedAt.toLocalDateTime(),
        rs.wasNull() ? null : verifiedBy);
  }

  private DeliveryTaskRow mapDeliveryTask(ResultSet rs, int rowNum) throws SQLException {
    Timestamp nextTick = rs.getTimestamp("next_tick_at");
    Timestamp completedAt = rs.getTimestamp("completed_at");
    return new DeliveryTaskRow(
        rs.getLong("id"),
        rs.getLong("order_id"),
        rs.getString("current_stage"),
        rs.getString("current_stage_text"),
        rs.getInt("eta_minutes"),
        nextTick == null ? null : nextTick.toLocalDateTime(),
        completedAt == null ? null : completedAt.toLocalDateTime());
  }

  record AddressRow(Long id, Long userId, String contactName, String contactPhone, String province, String city, String district, String detailAddress, String tagName, boolean isDefault, String deliveryNote) {}

  record ItemRow(Long id, Long storeId, String storeName, String businessType, Long categoryId, String categoryName, String title, String subtitle, BigDecimal price, BigDecimal originalPrice, String coverUrl, String ruleText, int salesCount, String status) {}

  record StoreRow(Long id, Long merchantId, String storeName, String businessType, String summary, String address, String distanceText, BigDecimal rating, int monthlySales, BigDecimal avgPrice, String status, String businessHoursText, String tagText, String coverUrl) {}

  record OrderInsertRow(Long userId, Long storeId, String storeName, String orderType, String title, String displayStatus, String paymentStatus, String fulfillmentStatus, String paymentMethod, BigDecimal amount, BigDecimal deliveryFee, BigDecimal discountAmount, BigDecimal payableAmount, String addressSnapshot, String voucherSummary, String remark, String idempotencyKey, String orderNo) {}

  record OrderRow(Long id, String orderNo, Long userId, Long storeId, String storeName, String orderType, String title, String displayStatus, String paymentStatus, String fulfillmentStatus, String paymentMethod, BigDecimal amount, BigDecimal deliveryFee, BigDecimal discountAmount, BigDecimal payableAmount, String addressSnapshot, String voucherSummary, String remark, LocalDateTime paidAt, LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {}

  record OrderItemRow(Long id, Long orderId, Long itemId, String itemName, String itemSubtitle, String businessType, Long categoryId, int quantity, BigDecimal unitPrice, BigDecimal totalPrice, String coverUrl, boolean isReviewed) {}

  record VoucherRow(Long id, Long orderId, String voucherCode, String qrPayload, String status, LocalDateTime effectiveFrom, LocalDateTime effectiveTo, LocalDateTime verifiedAt, Long verifiedBy) {}

  record DeliveryTaskRow(Long id, Long orderId, String currentStage, String currentStageText, int etaMinutes, LocalDateTime nextTickAt, LocalDateTime completedAt) {}

  record TimelineRow(String code, String text, LocalDateTime reachedAt) {}

  record SkuRow(Long id, Long itemId, String skuName, BigDecimal price, int stock, String status) {}

  record DeliveryRuleRow(BigDecimal deliveryFee, BigDecimal startPrice, int estimatedMinutes, String deliveryText) {}
}
