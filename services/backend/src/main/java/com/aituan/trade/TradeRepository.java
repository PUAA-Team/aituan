package com.aituan.trade;

import static com.aituan.common.jdbc.JdbcGeneratedKeys.insertAndReturnId;

import com.aituan.common.enums.BusinessType;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
          select id, user_id, contact_name, contact_phone, province, city, district, detail_address, longitude, latitude, tag_name, is_default, delivery_note
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
        select id, user_id, contact_name, contact_phone, province, city, district, detail_address, longitude, latitude, tag_name, is_default, delivery_note
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

  long getOrCreateCart(long userId, long storeId) {
    Optional<Long> existing = findCartId(userId, storeId);
    if (existing.isPresent()) {
      return existing.get();
    }
    jdbcTemplate.update(
        "insert into cart(user_id, store_id) values (?, ?)",
        userId,
        storeId);
    return findCartId(userId, storeId).orElseThrow();
  }

  private Optional<Long> findCartId(long userId, long storeId) {
    List<Long> rows = jdbcTemplate.query(
        "select id from cart where user_id = ? and store_id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> rs.getLong("id"),
        userId,
        storeId);
    return rows.stream().findFirst();
  }

  List<CartItemRow> listCartItems(long cartId) {
    return jdbcTemplate.query(
        """
        select ci.item_id, i.item_name, i.subtitle, c.category_name, i.price,
               coalesce(sku.stock, 0) as stock, i.status, ci.quantity
        from cart_item ci
        join catalog_item i on i.id = ci.item_id and i.is_deleted = 0
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where ci.cart_id = ? and ci.is_deleted = 0
        order by ci.updated_at desc, ci.id desc
        """,
        (rs, rowNum) -> new CartItemRow(
            rs.getLong("item_id"),
            rs.getString("item_name"),
            rs.getString("subtitle"),
            rs.getString("category_name"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getString("status"),
            rs.getInt("quantity")),
        cartId);
  }

  int findCartItemQuantity(long cartId, long itemId) {
    List<Integer> rows = jdbcTemplate.query(
        "select quantity from cart_item where cart_id = ? and item_id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> rs.getInt("quantity"),
        cartId,
        itemId);
    return rows.stream().findFirst().orElse(0);
  }

  void upsertCartItem(long cartId, long itemId, int quantity) {
    int updated = jdbcTemplate.update(
        "update cart_item set quantity = quantity + ?, is_deleted = 0, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        quantity,
        cartId,
        itemId);
    if (updated == 0) {
      jdbcTemplate.update(
          "insert into cart_item(cart_id, item_id, quantity) values (?, ?, ?)",
          cartId,
          itemId,
          quantity);
    }
    touchCart(cartId);
  }

  void setCartItemQuantity(long cartId, long itemId, int quantity) {
    if (quantity <= 0) {
      removeCartItem(cartId, itemId);
      return;
    }
    int updated = jdbcTemplate.update(
        "update cart_item set quantity = ?, is_deleted = 0, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        quantity,
        cartId,
        itemId);
    if (updated == 0) {
      jdbcTemplate.update(
          "insert into cart_item(cart_id, item_id, quantity) values (?, ?, ?)",
          cartId,
          itemId,
          quantity);
    }
    touchCart(cartId);
  }

  void removeCartItem(long cartId, long itemId) {
    jdbcTemplate.update(
        "update cart_item set quantity = 0, is_deleted = 1, updated_at = current_timestamp where cart_id = ? and item_id = ?",
        cartId,
        itemId);
    touchCart(cartId);
  }

  void clearCart(long cartId) {
    jdbcTemplate.update(
        "update cart_item set quantity = 0, is_deleted = 1, updated_at = current_timestamp where cart_id = ? and is_deleted = 0",
        cartId);
    touchCart(cartId);
  }

  private void touchCart(long cartId) {
    jdbcTemplate.update("update cart set updated_at = current_timestamp where id = ?", cartId);
  }

  List<MerchantItemRow> listTakeawayItems(long storeId, String status) {
    StringBuilder sql = new StringBuilder("""
        select i.id, i.store_id, i.item_name, i.subtitle, c.category_name, i.price, i.original_price,
               coalesce(sku.stock, 0) as stock, i.status, i.sales_count
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where i.store_id = ? and i.business_type = 'takeaway' and i.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    params.add(storeId);
    if (status != null && !status.isBlank()) {
      sql.append(" and i.status = ?");
      params.add(status);
    }
    sql.append(" order by c.sort_order, i.sort_order, i.id");
    return jdbcTemplate.query(sql.toString(), this::mapMerchantItem, params.toArray());
  }

  Optional<MerchantItemRow> findMerchantItem(long itemId) {
    List<MerchantItemRow> rows = jdbcTemplate.query(
        """
        select i.id, i.store_id, i.item_name, i.subtitle, c.category_name, i.price, i.original_price,
               coalesce(sku.stock, 0) as stock, i.status, i.sales_count
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where i.id = ? and i.business_type = 'takeaway' and i.is_deleted = 0
        limit 1
        """,
        this::mapMerchantItem,
        itemId);
    return rows.stream().findFirst();
  }

  void updateMerchantItem(long itemId, String title, String subtitle, BigDecimal price, int stock, String status) {
    jdbcTemplate.update(
        """
        update catalog_item
        set item_name = ?, subtitle = ?, price = ?, status = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        title,
        subtitle,
        price,
        status,
        itemId);
    int updatedSku = jdbcTemplate.update(
        """
        update catalog_sku
        set price = ?, stock = ?, status = ?, updated_at = current_timestamp
        where item_id = ? and sku_name = '默认' and is_deleted = 0
        """,
        price,
        stock,
        status,
        itemId);
    if (updatedSku == 0) {
      jdbcTemplate.update(
          """
          insert into catalog_sku(item_id, sku_name, price, stock, status, created_at, updated_at)
          values (?, '默认', ?, ?, ?, current_timestamp, current_timestamp)
          """,
          itemId,
          price,
          stock,
          status);
    }
  }

  void updateMerchantItemStatus(long itemId, String status) {
    jdbcTemplate.update(
        "update catalog_item set status = ?, updated_at = current_timestamp where id = ? and is_deleted = 0",
        status,
        itemId);
    jdbcTemplate.update(
        "update catalog_sku set status = ?, updated_at = current_timestamp where item_id = ? and sku_name = '默认' and is_deleted = 0",
        status,
        itemId);
  }

  void upsertDeliveryRule(long storeId, BigDecimal deliveryFee, BigDecimal startPrice, int estimatedMinutes, BigDecimal maxDeliveryDistanceKm, String deliveryText) {
    int updated = jdbcTemplate.update(
        """
        update merchant_delivery_rule
        set delivery_fee = ?, start_price = ?, estimated_minutes = ?, max_delivery_distance_km = ?, delivery_text = ?, updated_at = current_timestamp
        where store_id = ? and is_deleted = 0
        """,
        deliveryFee,
        startPrice,
        estimatedMinutes,
        maxDeliveryDistanceKm,
        deliveryText,
        storeId);
    if (updated == 0) {
      jdbcTemplate.update(
          """
          insert into merchant_delivery_rule(store_id, delivery_fee, start_price, estimated_minutes, max_delivery_distance_km, delivery_text, created_at, updated_at)
          values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
          """,
          storeId,
          deliveryFee,
          startPrice,
          estimatedMinutes,
          maxDeliveryDistanceKm,
          deliveryText);
    }
  }

  Optional<StoreRow> findStore(long storeId) {
    List<StoreRow> rows = jdbcTemplate.query(
        """
        select id, merchant_id, store_name, business_type, summary, address, distance_text, longitude, latitude, rating,
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
        "select delivery_fee, start_price, estimated_minutes, max_delivery_distance_km, delivery_text from merchant_delivery_rule where store_id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> new DeliveryRuleRow(
            rs.getBigDecimal("delivery_fee"),
            rs.getBigDecimal("start_price"),
            rs.getInt("estimated_minutes"),
            rs.getBigDecimal("max_delivery_distance_km"),
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
    return insertAndReturnId(
        jdbcTemplate,
        """
        insert into order_main(order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status,
                               fulfillment_status, payment_method, amount, delivery_fee, discount_amount, payable_amount,
                               address_snapshot, delivery_distance_km, estimated_arrival_at, voucher_summary, remark, idempotency_key, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
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
        order.deliveryDistanceKm(),
        order.estimatedArrivalAt() == null ? null : Timestamp.valueOf(order.estimatedArrivalAt()),
        order.voucherSummary(),
        order.remark(),
        order.idempotencyKey());
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
               address_snapshot, delivery_distance_km, estimated_arrival_at, voucher_summary, remark, paid_at, completed_at, created_at, updated_at
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
               address_snapshot, delivery_distance_km, estimated_arrival_at, voucher_summary, remark, paid_at, completed_at, created_at, updated_at
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
                 address_snapshot, delivery_distance_km, estimated_arrival_at, voucher_summary, remark, paid_at, completed_at, created_at, updated_at
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
               address_snapshot, delivery_distance_km, estimated_arrival_at, voucher_summary, remark, paid_at, completed_at, created_at, updated_at
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

  void updateOrderAfterTakeawayPaid(long orderId, String fulfillmentStatus) {
    jdbcTemplate.update(
        """
        update order_main
        set display_status = 'pending', fulfillment_status = ?, updated_at = current_timestamp
        where id = ?
        """,
        fulfillmentStatus,
        orderId);
  }

  void updateOrderDeliveryAddress(long orderId, String addressSnapshot, BigDecimal deliveryDistanceKm, LocalDateTime estimatedArrivalAt) {
    jdbcTemplate.update(
        """
        update order_main
        set address_snapshot = ?, delivery_distance_km = ?, estimated_arrival_at = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        addressSnapshot,
        deliveryDistanceKm,
        estimatedArrivalAt == null ? null : Timestamp.valueOf(estimatedArrivalAt),
        orderId);
  }

  void updateDeliveryTaskEta(long orderId, int etaMinutes) {
    jdbcTemplate.update(
        """
        update delivery_task
        set eta_minutes = ?, updated_at = current_timestamp
        where order_id = ? and is_deleted = 0
        """,
        etaMinutes,
        orderId);
  }

  void updateTakeawayFulfillment(long orderId, String displayStatus, String fulfillmentStatus, boolean completed) {
    if (completed) {
      jdbcTemplate.update(
          """
          update order_main
          set display_status = ?, fulfillment_status = ?, completed_at = case when ? = 'cancelled' then completed_at else current_timestamp end, updated_at = current_timestamp
          where id = ?
          """,
          displayStatus,
          fulfillmentStatus,
          fulfillmentStatus,
          orderId);
      return;
    }
    jdbcTemplate.update(
        """
        update order_main
        set display_status = ?, fulfillment_status = ?, updated_at = current_timestamp
        where id = ?
        """,
        displayStatus,
        fulfillmentStatus,
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

  void insertDeliveryTask(long orderId, String currentStage, String currentStageText, LocalDateTime nextTickAt) {
    jdbcTemplate.update(
        """
        insert into delivery_task(order_id, current_stage, current_stage_text, eta_minutes, next_tick_at)
        values (?, ?, ?, coalesce((select timestampdiff(minute, current_timestamp, estimated_arrival_at) from order_main where id = ?), 35), ?)
        """,
        orderId,
        currentStage,
        currentStageText,
        orderId,
        nextTickAt == null ? null : Timestamp.valueOf(nextTickAt));
    Long taskId = jdbcTemplate.queryForObject("select id from delivery_task where order_id = ?", Long.class, orderId);
    insertDeliveryNode(taskId, 1, "merchant_pending", "待商家接单", true);
    insertDeliveryNode(taskId, 2, "accepted", "商家已接单", isReached(currentStage, "accepted"));
    insertDeliveryNode(taskId, 3, "preparing", "商家正在备餐", isReached(currentStage, "preparing"));
    insertDeliveryNode(taskId, 4, "ready_for_delivery", "餐品已出餐，待配送", isReached(currentStage, "ready_for_delivery"));
    insertDeliveryNode(taskId, 5, "delivering", "骑手正在配送", isReached(currentStage, "delivering"));
    insertDeliveryNode(taskId, 6, "delivered", "订单已送达", isReached(currentStage, "delivered"));
    insertDeliveryNode(taskId, 7, "completed", "订单已完成", isReached(currentStage, "completed"));
  }

  int updateDeliveryTaskStage(long taskId, String currentStage, String nextStage, String nextText, LocalDateTime nextTickAt, boolean completed) {
    int updated;
    if (completed) {
      updated = jdbcTemplate.update(
          """
          update delivery_task
          set current_stage = ?, current_stage_text = ?, completed_at = current_timestamp,
              next_tick_at = null, updated_at = current_timestamp
          where id = ? and is_deleted = 0 and current_stage = ?
          """,
          nextStage,
          nextText,
          taskId,
          currentStage);
    } else {
      updated = jdbcTemplate.update(
          """
          update delivery_task
          set current_stage = ?, current_stage_text = ?, next_tick_at = ?, updated_at = current_timestamp
          where id = ? and is_deleted = 0 and current_stage = ?
          """,
          nextStage,
          nextText,
          nextTickAt == null ? null : Timestamp.valueOf(nextTickAt),
          taskId,
          currentStage);
    }
    if (updated > 0) {
      markDeliveryNodeReached(taskId, nextStage);
    }
    return updated;
  }

  void cancelDeliveryTask(long orderId) {
    jdbcTemplate.update(
        """
        update delivery_task
        set current_stage = 'cancelled', current_stage_text = '订单已取消', next_tick_at = null, completed_at = current_timestamp, updated_at = current_timestamp
        where order_id = ? and is_deleted = 0
        """,
        orderId);
  }

  void markDeliveryNodeReached(long taskId, String nodeCode) {
    jdbcTemplate.update(
        """
        update delivery_track_node
        set reached_at = current_timestamp, updated_at = current_timestamp
        where delivery_task_id = ? and node_code = ?
        """,
        taskId,
        nodeCode);
  }

  private void insertDeliveryNode(Long taskId, int order, String code, String text, boolean reached) {
    if (reached) {
      jdbcTemplate.update(
          """
          insert into delivery_track_node(delivery_task_id, node_order, node_code, node_text, reached_at)
          values (?, ?, ?, ?, current_timestamp)
          """,
          taskId,
          order,
          code,
          text);
      return;
    }
    jdbcTemplate.update(
        """
        insert into delivery_track_node(delivery_task_id, node_order, node_code, node_text)
        values (?, ?, ?, ?)
        """,
        taskId,
        order,
        code,
        text);
  }

  private boolean isReached(String currentStage, String nodeCode) {
    List<String> stages = List.of("merchant_pending", "accepted", "preparing", "ready_for_delivery", "delivering", "delivered", "completed");
    return stages.indexOf(nodeCode) <= stages.indexOf(currentStage);
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

  boolean isStoreOwnedByAccount(long storeId, long accountId) {
    Long count = jdbcTemplate.queryForObject(
        """
        select count(1)
        from merchant_store s
        join merchant_profile m on m.id = s.merchant_id
        where s.id = ? and m.account_id = ? and s.is_deleted = 0 and m.is_deleted = 0
        """,
        Long.class,
        storeId,
        accountId);
    return count != null && count > 0;
  }

  List<OpsOrderRow> listOpsOrders(Long merchantAccountId, String displayStatus, String fulfillmentStatus, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select o.id, o.order_no, o.user_id, o.store_id, o.store_name, o.order_type, o.title, o.display_status, o.payment_status,
               o.fulfillment_status, o.payment_method, o.amount, o.delivery_fee, o.discount_amount, o.payable_amount,
               o.address_snapshot, o.delivery_distance_km, o.estimated_arrival_at, o.voucher_summary, o.remark, o.paid_at, o.completed_at, o.created_at, o.updated_at,
               dt.current_stage, dt.current_stage_text
        from order_main o
        join merchant_store s on s.id = o.store_id
        left join merchant_profile m on m.id = s.merchant_id
        left join delivery_task dt on dt.order_id = o.id and dt.is_deleted = 0
        """);
    List<Object> params = new java.util.ArrayList<>();
    appendOpsFilters(sql, params, merchantAccountId, displayStatus, fulfillmentStatus);
    sql.append(" order by o.created_at desc, o.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapOpsOrder, params.toArray());
  }

  long countOpsOrders(Long merchantAccountId, String displayStatus, String fulfillmentStatus) {
    StringBuilder sql = new StringBuilder("""
        select count(1)
        from order_main o
        join merchant_store s on s.id = o.store_id
        left join merchant_profile m on m.id = s.merchant_id
        left join delivery_task dt on dt.order_id = o.id and dt.is_deleted = 0
        """);
    List<Object> params = new java.util.ArrayList<>();
    appendOpsFilters(sql, params, merchantAccountId, displayStatus, fulfillmentStatus);
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return count == null ? 0 : count;
  }

  List<StatusCountRow> countOpsOrdersByStage(Long merchantAccountId) {
    StringBuilder sql = new StringBuilder("""
        select coalesce(dt.current_stage, o.fulfillment_status) as status, count(1) as total
        from order_main o
        join merchant_store s on s.id = o.store_id
        left join merchant_profile m on m.id = s.merchant_id
        left join delivery_task dt on dt.order_id = o.id and dt.is_deleted = 0
        """);
    List<Object> params = new java.util.ArrayList<>();
    appendOpsFilters(sql, params, merchantAccountId, null, null);
    sql.append(" group by coalesce(dt.current_stage, o.fulfillment_status) order by total desc");
    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new StatusCountRow(rs.getString("status"), rs.getLong("total")), params.toArray());
  }

  Optional<TakeawaySettingRow> findTakeawaySetting(long storeId) {
    List<TakeawaySettingRow> rows = jdbcTemplate.query(
        """
        select s.id as store_id, s.store_name, coalesce(t.accept_mode, 'manual') as accept_mode
        from merchant_store s
        left join merchant_takeaway_setting t on t.store_id = s.id and t.is_deleted = 0
        where s.id = ? and s.business_type = 'takeaway' and s.is_deleted = 0
        limit 1
        """,
        (rs, rowNum) -> new TakeawaySettingRow(rs.getLong("store_id"), rs.getString("store_name"), rs.getString("accept_mode")),
        storeId);
    return rows.stream().findFirst();
  }

  void upsertTakeawaySetting(long storeId, String acceptMode, long updatedBy) {
    jdbcTemplate.update(
        """
        insert into merchant_takeaway_setting(store_id, accept_mode, updated_by, created_at, updated_at)
        values (?, ?, ?, current_timestamp, current_timestamp)
        on duplicate key update accept_mode = values(accept_mode), updated_by = values(updated_by), updated_at = current_timestamp, is_deleted = 0
        """,
        storeId,
        acceptMode,
        updatedBy);
  }

  void insertOrderStateLog(long orderId, String fromStatus, String toStatus, String actionType, String operatorType, Long operatorId, String remark) {
    jdbcTemplate.update(
        """
        insert into order_state_log(order_id, from_status, to_status, action_type, operator_type, operator_id, remark)
        values (?, ?, ?, ?, ?, ?, ?)
        """,
        orderId,
        fromStatus,
        toStatus,
        actionType,
        operatorType,
        operatorId,
        remark);
  }

  void insertAuditLog(String actorType, Long actorId, String actionType, String targetType, long targetId, String detail) {
    jdbcTemplate.update(
        """
        insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail)
        values (?, ?, ?, ?, ?, ?)
        """,
        actorType,
        actorId,
        actionType,
        targetType,
        targetId,
        detail);
  }

  private void appendOpsFilters(StringBuilder sql, List<Object> params, Long merchantAccountId, String displayStatus, String fulfillmentStatus) {
    sql.append(" where o.is_deleted = 0 and o.order_type = 'takeaway'");
    if (merchantAccountId != null) {
      sql.append(" and m.account_id = ?");
      params.add(merchantAccountId);
    }
    if (displayStatus != null && !displayStatus.isBlank()) {
      sql.append(" and o.display_status = ?");
      params.add(displayStatus);
    }
    if (fulfillmentStatus != null && !fulfillmentStatus.isBlank()) {
      sql.append(" and coalesce(dt.current_stage, o.fulfillment_status) = ?");
      params.add(fulfillmentStatus);
    }
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
        getNullableBigDecimal(rs, "longitude"),
        getNullableBigDecimal(rs, "latitude"),
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

  private MerchantItemRow mapMerchantItem(ResultSet rs, int rowNum) throws SQLException {
    return new MerchantItemRow(
        rs.getLong("id"),
        rs.getLong("store_id"),
        rs.getString("item_name"),
        rs.getString("subtitle"),
        rs.getString("category_name"),
        rs.getBigDecimal("price"),
        rs.getBigDecimal("original_price"),
        rs.getInt("stock"),
        rs.getString("status"),
        rs.getInt("sales_count"));
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
        getNullableBigDecimal(rs, "longitude"),
        getNullableBigDecimal(rs, "latitude"),
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
        rs.getBigDecimal("delivery_distance_km"),
        rs.getTimestamp("estimated_arrival_at") == null ? null : rs.getTimestamp("estimated_arrival_at").toLocalDateTime(),
        rs.getString("voucher_summary"),
        rs.getString("remark"),
        paidAt == null ? null : paidAt.toLocalDateTime(),
        completedAt == null ? null : completedAt.toLocalDateTime(),
        createdAt == null ? null : createdAt.toLocalDateTime(),
        updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private OpsOrderRow mapOpsOrder(ResultSet rs, int rowNum) throws SQLException {
    return new OpsOrderRow(
        mapOrder(rs, rowNum),
        rs.getString("current_stage"),
        rs.getString("current_stage_text"));
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

  private BigDecimal getNullableBigDecimal(ResultSet rs, String column) throws SQLException {
    BigDecimal value = rs.getBigDecimal(column);
    return rs.wasNull() ? null : value;
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

  record AddressRow(Long id, Long userId, String contactName, String contactPhone, String province, String city, String district, String detailAddress, BigDecimal longitude, BigDecimal latitude, String tagName, boolean isDefault, String deliveryNote) {}

  record ItemRow(Long id, Long storeId, String storeName, String businessType, Long categoryId, String categoryName, String title, String subtitle, BigDecimal price, BigDecimal originalPrice, String coverUrl, String ruleText, int salesCount, String status) {}

  record MerchantItemRow(Long id, Long storeId, String title, String subtitle, String categoryName, BigDecimal price, BigDecimal originalPrice, int stock, String status, int salesCount) {}

  record StoreRow(Long id, Long merchantId, String storeName, String businessType, String summary, String address, String distanceText, BigDecimal longitude, BigDecimal latitude, BigDecimal rating, int monthlySales, BigDecimal avgPrice, String status, String businessHoursText, String tagText, String coverUrl) {}

  record OrderInsertRow(Long userId, Long storeId, String storeName, String orderType, String title, String displayStatus, String paymentStatus, String fulfillmentStatus, String paymentMethod, BigDecimal amount, BigDecimal deliveryFee, BigDecimal discountAmount, BigDecimal payableAmount, String addressSnapshot, BigDecimal deliveryDistanceKm, LocalDateTime estimatedArrivalAt, String voucherSummary, String remark, String idempotencyKey, String orderNo) {}

  record OrderRow(Long id, String orderNo, Long userId, Long storeId, String storeName, String orderType, String title, String displayStatus, String paymentStatus, String fulfillmentStatus, String paymentMethod, BigDecimal amount, BigDecimal deliveryFee, BigDecimal discountAmount, BigDecimal payableAmount, String addressSnapshot, BigDecimal deliveryDistanceKm, LocalDateTime estimatedArrivalAt, String voucherSummary, String remark, LocalDateTime paidAt, LocalDateTime completedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {}

  record OrderItemRow(Long id, Long orderId, Long itemId, String itemName, String itemSubtitle, String businessType, Long categoryId, int quantity, BigDecimal unitPrice, BigDecimal totalPrice, String coverUrl, boolean isReviewed) {}

  record OpsOrderRow(OrderRow order, String currentStage, String currentStageText) {}

  record StatusCountRow(String status, long total) {}

  record TakeawaySettingRow(Long storeId, String storeName, String acceptMode) {}

  record VoucherRow(Long id, Long orderId, String voucherCode, String qrPayload, String status, LocalDateTime effectiveFrom, LocalDateTime effectiveTo, LocalDateTime verifiedAt, Long verifiedBy) {}

  record DeliveryTaskRow(Long id, Long orderId, String currentStage, String currentStageText, int etaMinutes, LocalDateTime nextTickAt, LocalDateTime completedAt) {}

  record TimelineRow(String code, String text, LocalDateTime reachedAt) {}

  record SkuRow(Long id, Long itemId, String skuName, BigDecimal price, int stock, String status) {}

  record CartItemRow(Long itemId, String itemName, String subtitle, String categoryName, BigDecimal unitPrice, int stock, String status, int quantity) {}

  record DeliveryRuleRow(BigDecimal deliveryFee, BigDecimal startPrice, int estimatedMinutes, BigDecimal maxDeliveryDistanceKm, String deliveryText) {}
}
