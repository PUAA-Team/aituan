package com.aituan.trade;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.enums.DisplayOrderStatus;
import com.aituan.common.enums.PaymentStatus;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TradeService {
  private static final String TAKEAWAY = "takeaway";
  private static final String ACCEPT_MODE_AUTO = "auto";
  private static final String ACCEPT_MODE_MANUAL = "manual";
  private static final int DELIVERY_TICK_MINUTES = 3;

  private final TradeRepository tradeRepository;

  TradeService(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  List<PaymentMethodView> paymentMethods() {
    return List.of(
        new PaymentMethodView("mock", "模拟支付", true),
        new PaymentMethodView("wechat", "微信支付", false),
        new PaymentMethodView("alipay", "支付宝", false));
  }

  CartView getCart(long storeId) {
    long cartId = tradeRepository.getOrCreateCart(CurrentUserContext.required().userId(), requireTakeawayStore(storeId).id());
    return buildCartView(storeId, cartId);
  }

  @Transactional
  CartView addCartItem(CartItemRequest request) {
    long userId = CurrentUserContext.required().userId();
    TradeRepository.ItemRow item = requireCartItem(request.storeId(), request.itemId());
    TradeRepository.SkuRow sku = tradeRepository.findSkuByItem(item.id())
        .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH));
    long cartId = tradeRepository.getOrCreateCart(userId, request.storeId());
    int nextQuantity = tradeRepository.findCartItemQuantity(cartId, item.id()) + request.quantity();
    if (nextQuantity > sku.stock()) {
      throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
    }
    tradeRepository.upsertCartItem(cartId, item.id(), request.quantity());
    return buildCartView(request.storeId(), cartId);
  }

  @Transactional
  CartView updateCartItem(long itemId, CartItemQuantityRequest request) {
    long userId = CurrentUserContext.required().userId();
    TradeRepository.ItemRow item = requireCartItem(request.storeId(), itemId);
    long cartId = tradeRepository.getOrCreateCart(userId, request.storeId());
    if (request.quantity() > 0) {
      TradeRepository.SkuRow sku = tradeRepository.findSkuByItem(item.id())
          .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH));
      if (request.quantity() > sku.stock()) {
        throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
      }
    }
    tradeRepository.setCartItemQuantity(cartId, item.id(), request.quantity());
    return buildCartView(request.storeId(), cartId);
  }

  @Transactional
  CartView removeCartItem(long storeId, long itemId) {
    long cartId = tradeRepository.getOrCreateCart(CurrentUserContext.required().userId(), requireTakeawayStore(storeId).id());
    tradeRepository.removeCartItem(cartId, itemId);
    return buildCartView(storeId, cartId);
  }

  @Transactional
  CartView clearCart(long storeId) {
    long cartId = tradeRepository.getOrCreateCart(CurrentUserContext.required().userId(), requireTakeawayStore(storeId).id());
    tradeRepository.clearCart(cartId);
    return buildCartView(storeId, cartId);
  }

  CheckoutPreviewView preview(CheckoutPreviewRequest request) {
    TradeContext context = loadTradeContext(request.storeId(), request.businessType(), request.addressId(), request.items());
    return buildPreview(context, request.remark());
  }

  @Transactional
  OrderDetailView createOrder(CreateOrderRequest request) {
    long userId = CurrentUserContext.required().userId();
    if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
      TradeRepository.OrderRow existing = tradeRepository.findOrderByIdempotency(userId, request.idempotencyKey())
          .orElse(null);
      if (existing != null) {
        return getOrderDetail(existing.id());
      }
    }
    TradeContext context = loadTradeContext(request.storeId(), request.businessType(), request.addressId(), request.items());
    reserveStock(context.items());
    OrderInsert orderInsert = buildOrderInsert(context, request.remark(), request.idempotencyKey());
    Long orderId = tradeRepository.insertOrder(orderInsert.row());
    for (TradeItem tradeItem : context.items()) {
      tradeRepository.insertOrderItem(orderId, tradeItem.row(), tradeItem.quantity());
    }
    return getOrderDetail(orderId);
  }

  @Transactional
  OrderDetailView pay(long orderId, PayOrderRequest request) {
    if (!"mock".equalsIgnoreCase(request.paymentMode())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "首包只开放模拟支付");
    }
    TradeRepository.OrderRow order = requireOrder(orderId);
    if (!PaymentStatus.UNPAID.code().equals(order.paymentStatus())) {
      return getOrderDetail(orderId);
    }
    tradeRepository.markPaymentSuccess(orderId, request.paymentMode(), order.payableAmount());
    if (TAKEAWAY.equals(order.orderType())) {
      createTakeawayDeliveryAfterPaid(order);
    } else {
      String voucherCode = String.format("%08d", Math.abs((int) (System.nanoTime() % 100_000_000L)));
      tradeRepository.insertVoucher(orderId, voucherCode, "AITUAN:VOUCHER:" + voucherCode, LocalDateTime.now().plusMonths(6));
      tradeRepository.updateOrderAfterServicePaid(orderId, "券码 " + voucherCode);
    }
    return getOrderDetail(orderId);
  }

  PageResponse<OrderSummaryView> listOrders(String displayStatus, int page, int pageSize) {
    long userId = CurrentUserContext.required().userId();
    long total = tradeRepository.countOrders(userId, displayStatus);
    List<OrderSummaryView> list = tradeRepository.listOrders(userId, displayStatus, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toSummaryView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  PageResponse<OpsOrderSummaryView> listOpsOrders(String displayStatus, String fulfillmentStatus, int page, int pageSize) {
    Long merchantAccountId = merchantAccountScope(CurrentUserContext.required());
    long total = tradeRepository.countOpsOrders(merchantAccountId, displayStatus, fulfillmentStatus);
    List<OpsOrderSummaryView> list = tradeRepository.listOpsOrders(merchantAccountId, displayStatus, fulfillmentStatus, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toOpsSummaryView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  List<OrderStatusCountView> opsOrderStats() {
    Long merchantAccountId = merchantAccountScope(CurrentUserContext.required());
    return tradeRepository.countOpsOrdersByStage(merchantAccountId).stream()
        .map(row -> new OrderStatusCountView(row.status(), takeawayStageLabel(row.status()), row.total()))
        .toList();
  }

  TakeawaySettingView getTakeawaySetting(long storeId) {
    TradeRepository.TakeawaySettingRow setting = requireTakeawaySettingStore(storeId);
    return new TakeawaySettingView(setting.storeId(), setting.storeName(), setting.acceptMode());
  }

  @Transactional
  TakeawaySettingView updateTakeawaySetting(long storeId, TakeawaySettingRequest request) {
    TradeRepository.TakeawaySettingRow setting = requireTakeawaySettingStore(storeId);
    String acceptMode = normalizeAcceptMode(request.acceptMode());
    CurrentUser current = CurrentUserContext.required();
    tradeRepository.upsertTakeawaySetting(storeId, acceptMode, current.accountId());
    tradeRepository.insertAuditLog(current.accountType().name().toLowerCase(), current.accountId(), "takeaway_setting_update", "store", storeId, "接单模式：" + acceptMode);
    return new TakeawaySettingView(setting.storeId(), setting.storeName(), acceptMode);
  }

  List<MerchantItemView> listTakeawayItems(long storeId, String status) {
    requireTakeawaySettingStore(storeId);
    String normalizedStatus = normalizeItemStatusFilter(status);
    return tradeRepository.listTakeawayItems(storeId, normalizedStatus).stream()
        .map(this::toMerchantItemView)
        .toList();
  }

  @Transactional
  MerchantItemView updateTakeawayItem(long storeId, long itemId, MerchantItemUpdateRequest request) {
    requireTakeawaySettingStore(storeId);
    TradeRepository.MerchantItemRow item = requireMerchantItem(storeId, itemId);
    if (request.price().compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "价格必须大于 0");
    }
    String status = normalizeItemStatus(request.status());
    tradeRepository.updateMerchantItem(
        item.id(),
        request.title().trim(),
        request.subtitle() == null ? "" : request.subtitle().trim(),
        request.price(),
        request.stock(),
        status);
    CurrentUser current = CurrentUserContext.required();
    tradeRepository.insertAuditLog(current.accountType().name().toLowerCase(), current.accountId(), "takeaway_item_update", "item", itemId, request.title());
    return toMerchantItemView(requireMerchantItem(storeId, itemId));
  }

  @Transactional
  MerchantItemView updateTakeawayItemStatus(long storeId, long itemId, MerchantItemStatusRequest request) {
    requireTakeawaySettingStore(storeId);
    TradeRepository.MerchantItemRow item = requireMerchantItem(storeId, itemId);
    String status = normalizeItemStatus(request.status());
    tradeRepository.updateMerchantItemStatus(item.id(), status);
    CurrentUser current = CurrentUserContext.required();
    tradeRepository.insertAuditLog(current.accountType().name().toLowerCase(), current.accountId(), "takeaway_item_status", "item", itemId, status);
    return toMerchantItemView(requireMerchantItem(storeId, itemId));
  }

  DeliveryRuleOpsView getDeliveryRule(long storeId) {
    requireTakeawaySettingStore(storeId);
    TradeRepository.DeliveryRuleRow rule = tradeRepository.findDeliveryRule(storeId)
        .orElse(new TradeRepository.DeliveryRuleRow(BigDecimal.ZERO, BigDecimal.ZERO, 35, ""));
    return toDeliveryRuleOpsView(storeId, rule);
  }

  @Transactional
  DeliveryRuleOpsView updateDeliveryRule(long storeId, DeliveryRuleUpdateRequest request) {
    requireTakeawaySettingStore(storeId);
    if (request.deliveryFee().compareTo(BigDecimal.ZERO) < 0 || request.startPrice().compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "配送费和起送价不能为负数");
    }
    String deliveryText = request.deliveryText() == null ? "" : request.deliveryText().trim();
    tradeRepository.upsertDeliveryRule(storeId, request.deliveryFee(), request.startPrice(), request.estimatedMinutes(), deliveryText);
    CurrentUser current = CurrentUserContext.required();
    tradeRepository.insertAuditLog(current.accountType().name().toLowerCase(), current.accountId(), "delivery_rule_update", "store", storeId, deliveryText);
    return getDeliveryRule(storeId);
  }

  @Transactional
  OrderDetailView acceptTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "merchant_pending", nextStage("accepted", "商家已接单", DisplayOrderStatus.PENDING.code(), false, true), "merchant_accept", request);
  }

  @Transactional
  OrderDetailView rejectTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "merchant_pending", nextStage("merchant_rejected", "商家已拒单", DisplayOrderStatus.USED.code(), true, false), "merchant_reject", request);
  }

  @Transactional
  OrderDetailView prepareTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "accepted", nextStage("preparing", "商家正在备餐", DisplayOrderStatus.PENDING.code(), false, true), "merchant_prepare", request);
  }

  @Transactional
  OrderDetailView readyTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "preparing", nextStage("ready_for_delivery", "餐品已出餐，待配送", DisplayOrderStatus.PENDING.code(), false, true), "merchant_ready", request);
  }

  @Transactional
  OrderDetailView completeTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "delivered", nextStage("completed", "订单已完成", DisplayOrderStatus.USED.code(), true, false), "merchant_complete", request);
  }

  @Transactional
  OrderDetailView markTakeawayAbnormal(long orderId, TakeawayOrderActionRequest request) {
    TradeRepository.OrderRow order = requireTakeawayOrderForStaff(orderId);
    TradeRepository.DeliveryTaskRow task = tradeRepository.findDeliveryTask(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    CurrentUser current = CurrentUserContext.required();
    tradeRepository.updateDeliveryTaskStage(task.id(), task.currentStage(), "abnormal", "订单异常，待处理", null, false);
    tradeRepository.updateTakeawayFulfillment(orderId, DisplayOrderStatus.PENDING.code(), "abnormal", false);
    writeOrderLogs(order, "abnormal", "takeaway_abnormal", current.accountType().name().toLowerCase(), current.accountId(), actionRemark(request));
    return buildOrderDetail(requireOrderById(orderId));
  }

  OrderDetailView getOrderDetail(long orderId) {
    return buildOrderDetail(requireOrder(orderId));
  }

  OrderDetailView getOrderDetailForStaff(long orderId) {
    return buildOrderDetail(requireOrderForStaff(orderId));
  }

  DeliveryTimelineView deliveryTimeline(long orderId) {
    return buildDeliveryTimeline(requireOrder(orderId));
  }

  DeliveryTimelineView deliveryTimelineForStaff(long orderId) {
    return buildDeliveryTimeline(requireOrderForStaff(orderId));
  }

  @Transactional
  OrderDetailView advanceDelivery(long orderId) {
    TradeRepository.OrderRow order = requireTakeawayOrderForStaff(orderId);
    CurrentUser current = CurrentUserContext.required();
    return advanceDeliveryInternal(order, "staff_advance", current.accountType().name().toLowerCase(), current.accountId(), null);
  }

  @Transactional
  OrderDetailView redeemVoucher(String voucherCode) {
    long operatorId = CurrentUserContext.required().accountId();
    TradeRepository.VoucherRow voucher = tradeRepository.findVoucherByCode(voucherCode)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"unused".equals(voucher.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "券码已核销");
    }
    if (voucher.effectiveTo() != null && voucher.effectiveTo().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "券码已过期");
    }
    requireOrderForStaff(voucher.orderId());
    tradeRepository.setOrderUsed(voucher.orderId(), operatorId);
    return buildOrderDetail(requireOrderById(voucher.orderId()));
  }

  @Transactional
  void advanceDueDeliveryTasks() {
    for (TradeRepository.DeliveryTaskRow task : tradeRepository.listDueDeliveryTasks()) {
      TradeRepository.OrderRow order = tradeRepository.findOrderById(task.orderId()).orElse(null);
      if (order != null && TAKEAWAY.equals(order.orderType())) {
        advanceDeliveryInternal(order, "system_advance", "system", null, "系统自动推进");
      }
    }
  }

  CheckoutContext previewForOrder(CreateOrderRequest request) {
    TradeContext context = loadTradeContext(request.storeId(), request.businessType(), request.addressId(), request.items());
    return new CheckoutContext(context, buildPreview(context, request.remark()));
  }

  private OrderDetailView buildOrderDetail(TradeRepository.OrderRow order) {
    List<OrderItemView> items = tradeRepository.listOrderItems(order.id()).stream().map(this::toOrderItemView).toList();
    DeliveryTimelineView deliveryTimeline = null;
    VoucherView voucher = null;
    if ("takeaway".equals(order.orderType())) {
      deliveryTimeline = buildDeliveryTimeline(order);
    } else {
      voucher = tradeRepository.findVoucher(order.id())
          .map(row -> new VoucherView(row.voucherCode(), row.qrPayload(), row.status(), row.effectiveFrom(), row.effectiveTo()))
          .orElse(null);
    }
    return new OrderDetailView(
        order.id(),
        order.orderNo(),
        order.orderType(),
        order.displayStatus(),
        order.paymentStatus(),
        order.fulfillmentStatus(),
        order.paymentMethod(),
        order.storeName(),
        order.title(),
        order.amount(),
        order.deliveryFee(),
        order.discountAmount(),
        order.payableAmount(),
        order.addressSnapshot(),
        order.voucherSummary(),
        order.remark(),
        order.createdAt(),
        order.paidAt(),
        order.completedAt(),
        items,
        deliveryTimeline,
        voucher);
  }

  private void createTakeawayDeliveryAfterPaid(TradeRepository.OrderRow order) {
    CurrentUser current = CurrentUserContext.required();
    String acceptMode = tradeRepository.findTakeawaySetting(order.storeId())
        .map(TradeRepository.TakeawaySettingRow::acceptMode)
        .orElse(ACCEPT_MODE_MANUAL);
    if (ACCEPT_MODE_AUTO.equalsIgnoreCase(acceptMode)) {
      tradeRepository.updateOrderAfterTakeawayPaid(order.id(), "accepted");
      tradeRepository.insertDeliveryTask(order.id(), "accepted", "商家已接单", LocalDateTime.now().plusMinutes(DELIVERY_TICK_MINUTES));
      writeOrderLogs(order, "accepted", "auto_accept", current.accountType().name().toLowerCase(), current.accountId(), "门店自动接单");
      return;
    }
    tradeRepository.updateOrderAfterTakeawayPaid(order.id(), "merchant_pending");
    tradeRepository.insertDeliveryTask(order.id(), "merchant_pending", "待商家接单", null);
    writeOrderLogs(order, "merchant_pending", "payment_takeaway", current.accountType().name().toLowerCase(), current.accountId(), "等待商家接单");
  }

  private OrderDetailView moveTakeawayOrder(long orderId, String expectedStage, TakeawayStage nextStage, String actionType, TakeawayOrderActionRequest request) {
    TradeRepository.OrderRow order = requireTakeawayOrderForStaff(orderId);
    TradeRepository.DeliveryTaskRow task = tradeRepository.findDeliveryTask(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!expectedStage.equals(task.currentStage())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
    }
    CurrentUser current = CurrentUserContext.required();
    applyTakeawayStage(order, task, nextStage, actionType, current.accountType().name().toLowerCase(), current.accountId(), actionRemark(request));
    return buildOrderDetail(requireOrderById(orderId));
  }

  private OrderDetailView advanceDeliveryInternal(TradeRepository.OrderRow order, String actionType, String operatorType, Long operatorId, String remark) {
    TradeRepository.DeliveryTaskRow task = tradeRepository.findDeliveryTask(order.id())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    TakeawayStage next = switch (task.currentStage()) {
      case "merchant_pending" -> nextStage("accepted", "商家已接单", DisplayOrderStatus.PENDING.code(), false, true);
      case "accepted" -> nextStage("preparing", "商家正在备餐", DisplayOrderStatus.PENDING.code(), false, true);
      case "preparing" -> nextStage("ready_for_delivery", "餐品已出餐，待配送", DisplayOrderStatus.PENDING.code(), false, true);
      case "ready_for_delivery" -> nextStage("delivering", "骑手正在配送", DisplayOrderStatus.PENDING.code(), false, true);
      case "delivering" -> nextStage("delivered", "订单已送达", DisplayOrderStatus.PENDING.code(), false, true);
      case "delivered" -> nextStage("completed", "订单已完成", DisplayOrderStatus.USED.code(), true, false);
      case "completed" -> null;
      default -> throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
    };
    if (next == null) {
      return buildOrderDetail(order);
    }
    applyTakeawayStage(order, task, next, actionType, operatorType, operatorId, remark);
    return buildOrderDetail(requireOrderById(order.id()));
  }

  private void applyTakeawayStage(TradeRepository.OrderRow order, TradeRepository.DeliveryTaskRow task, TakeawayStage nextStage, String actionType, String operatorType, Long operatorId, String remark) {
    LocalDateTime nextTickAt = nextStage.scheduleNext() ? LocalDateTime.now().plusMinutes(DELIVERY_TICK_MINUTES) : null;
    int updated = tradeRepository.updateDeliveryTaskStage(task.id(), task.currentStage(), nextStage.stage(), nextStage.text(), nextTickAt, nextStage.completed());
    if (updated == 0) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
    }
    tradeRepository.updateTakeawayFulfillment(order.id(), nextStage.displayStatus(), nextStage.stage(), nextStage.completed());
    writeOrderLogs(order, nextStage.stage(), actionType, operatorType, operatorId, remark);
  }

  private void writeOrderLogs(TradeRepository.OrderRow order, String toStatus, String actionType, String operatorType, Long operatorId, String remark) {
    tradeRepository.insertOrderStateLog(order.id(), order.fulfillmentStatus(), toStatus, actionType, operatorType, operatorId, remark);
    tradeRepository.insertAuditLog(operatorType, operatorId, actionType, "order", order.id(), remark == null ? order.orderNo() + " -> " + toStatus : remark);
  }

  private TakeawayStage nextStage(String stage, String text, String displayStatus, boolean completed, boolean scheduleNext) {
    return new TakeawayStage(stage, text, displayStatus, completed, scheduleNext);
  }

  private String actionRemark(TakeawayOrderActionRequest request) {
    return request == null ? null : request.remark();
  }

  private DeliveryTimelineView buildDeliveryTimeline(TradeRepository.OrderRow order) {
    List<TimelineNodeView> nodes = tradeRepository.listDeliveryTimeline(order.id()).stream()
        .map(row -> new TimelineNodeView(row.code(), row.text(), row.reachedAt()))
        .toList();
    TradeRepository.DeliveryTaskRow task = tradeRepository.findDeliveryTask(order.id()).orElse(null);
    return new DeliveryTimelineView(order.orderNo(), task == null ? order.fulfillmentStatus() : task.currentStage(), nodes);
  }

  private TradeRepository.OrderRow requireOrderById(long orderId) {
    return tradeRepository.findOrderById(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  private TradeRepository.OrderRow requireOrderForStaff(long orderId) {
    TradeRepository.OrderRow order = requireOrderById(orderId);
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() == AccountType.ADMIN) {
      return order;
    }
    if (current.accountType() == AccountType.MERCHANT && tradeRepository.isStoreOwnedByAccount(order.storeId(), current.accountId())) {
      return order;
    }
    throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  private TradeRepository.OrderRow requireTakeawayOrderForStaff(long orderId) {
    TradeRepository.OrderRow order = requireOrderForStaff(orderId);
    if (!TAKEAWAY.equals(order.orderType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅支持外卖订单");
    }
    return order;
  }

  private Long merchantAccountScope(CurrentUser current) {
    if (current.accountType() == AccountType.ADMIN) {
      return null;
    }
    if (current.accountType() == AccountType.MERCHANT) {
      return current.accountId();
    }
    throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  private TradeRepository.TakeawaySettingRow requireTakeawaySettingStore(long storeId) {
    TradeRepository.TakeawaySettingRow setting = tradeRepository.findTakeawaySetting(storeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() == AccountType.ADMIN) {
      return setting;
    }
    if (current.accountType() == AccountType.MERCHANT && tradeRepository.isStoreOwnedByAccount(storeId, current.accountId())) {
      return setting;
    }
    throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  private TradeRepository.StoreRow requireTakeawayStore(long storeId) {
    TradeRepository.StoreRow store = tradeRepository.findStore(storeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!TAKEAWAY.equals(store.businessType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "购物车仅支持外卖门店");
    }
    return store;
  }

  private TradeRepository.ItemRow requireCartItem(long storeId, long itemId) {
    requireTakeawayStore(storeId);
    TradeRepository.ItemRow item = tradeRepository.findItem(itemId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!item.storeId().equals(storeId)) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "商品不属于当前门店");
    }
    return item;
  }

  private CartView buildCartView(long storeId, long cartId) {
    TradeRepository.StoreRow store = requireTakeawayStore(storeId);
    List<CartLineView> items = tradeRepository.listCartItems(cartId).stream()
        .map(this::toCartLineView)
        .toList();
    BigDecimal amount = items.stream()
        .map(CartLineView::totalPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CartView(store.id(), store.storeName(), amount, items);
  }

  private CartLineView toCartLineView(TradeRepository.CartItemRow row) {
    BigDecimal totalPrice = row.unitPrice().multiply(BigDecimal.valueOf(row.quantity()));
    boolean soldOut = row.stock() <= 0 || !"on_sale".equals(row.status());
    return new CartLineView(
        row.itemId(),
        row.itemName(),
        row.subtitle(),
        row.categoryName(),
        row.unitPrice(),
        row.quantity(),
        totalPrice,
        row.stock(),
        row.status(),
        soldOut);
  }

  private TradeRepository.MerchantItemRow requireMerchantItem(long storeId, long itemId) {
    TradeRepository.MerchantItemRow item = tradeRepository.findMerchantItem(itemId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!item.storeId().equals(storeId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return item;
  }

  private MerchantItemView toMerchantItemView(TradeRepository.MerchantItemRow row) {
    return new MerchantItemView(
        row.id(),
        row.storeId(),
        row.title(),
        row.subtitle(),
        row.categoryName(),
        row.price(),
        row.originalPrice(),
        row.stock(),
        row.status(),
        row.salesCount());
  }

  private DeliveryRuleOpsView toDeliveryRuleOpsView(long storeId, TradeRepository.DeliveryRuleRow row) {
    return new DeliveryRuleOpsView(storeId, row.deliveryFee(), row.startPrice(), row.estimatedMinutes(), row.deliveryText());
  }

  private String normalizeItemStatusFilter(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    return normalizeItemStatus(status);
  }

  private String normalizeItemStatus(String status) {
    String value = status == null ? "" : status.trim().toLowerCase();
    return switch (value) {
      case "on_sale", "online", "上架", "售卖" -> "on_sale";
      case "off_sale", "offline", "下架", "停售" -> "off_sale";
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "商品状态只能是 on_sale 或 off_sale");
    };
  }

  private String normalizeAcceptMode(String acceptMode) {
    String value = acceptMode == null ? "" : acceptMode.trim().toLowerCase();
    return switch (value) {
      case "auto", "automatic", "自动", "自动接单" -> ACCEPT_MODE_AUTO;
      case "manual", "手动", "手动接单" -> ACCEPT_MODE_MANUAL;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "接单模式只能是 auto 或 manual");
    };
  }

  private TradeContext loadTradeContext(long storeId, String businessType, Long addressId, List<CheckoutItemRequest> requests) {
    long userId = CurrentUserContext.required().userId();
    TradeRepository.StoreRow store = tradeRepository.findStore(storeId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!normalizeBusinessType(businessType).equalsIgnoreCase(store.businessType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "商品不属于当前业务类型");
    }
    List<TradeRepository.ItemRow> items = new ArrayList<>();
    List<TradeItem> tradeItems = new ArrayList<>();
    for (CheckoutItemRequest request : requests) {
      TradeRepository.ItemRow item = tradeRepository.findItem(request.itemId())
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
      if (item.storeId() != storeId) {
        throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "商品不属于当前门店");
      }
      TradeRepository.SkuRow sku = tradeRepository.findSkuByItem(item.id())
          .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH));
      if (sku.stock() < request.quantity()) {
        throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
      }
      items.add(item);
      tradeItems.add(new TradeItem(item, request.quantity()));
    }
    TradeRepository.AddressRow address = tradeRepository.findAddress(userId, addressId)
        .orElse(null);
    TradeRepository.DeliveryRuleRow deliveryRule = tradeRepository.findDeliveryRule(storeId).orElse(null);
    return new TradeContext(store, address, deliveryRule, tradeItems);
  }

  private void reserveStock(List<TradeItem> items) {
    for (TradeItem tradeItem : items) {
      TradeRepository.SkuRow sku = tradeRepository.findSkuByItem(tradeItem.row().id())
          .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH));
      if (tradeRepository.decreaseSkuStock(sku.id(), tradeItem.quantity()) == 0) {
        throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
      }
    }
  }

  private String normalizeBusinessType(String businessType) {
    return switch (businessType.trim().toLowerCase()) {
      case "group", "groupbuy" -> "group_buy";
      case "fun" -> "entertainment";
      default -> businessType.trim().toLowerCase();
    };
  }

  private CheckoutPreviewView buildPreview(TradeContext context, String remark) {
    BigDecimal amount = BigDecimal.ZERO;
    List<CheckoutItemView> itemViews = new ArrayList<>();
    for (TradeItem tradeItem : context.items()) {
      BigDecimal total = tradeItem.row().price().multiply(BigDecimal.valueOf(tradeItem.quantity()));
      amount = amount.add(total);
      itemViews.add(new CheckoutItemView(
          tradeItem.row().id(),
          tradeItem.row().title(),
          tradeItem.row().subtitle(),
          tradeItem.quantity(),
          tradeItem.row().price(),
          total,
          tradeItem.row().categoryId(),
          tradeItem.row().categoryName()));
    }
    BigDecimal deliveryFee = BigDecimal.ZERO;
    if ("takeaway".equals(context.store().businessType()) && context.deliveryRule() != null) {
      deliveryFee = context.deliveryRule().deliveryFee();
    }
    BigDecimal discountAmount = BigDecimal.ZERO;
    BigDecimal payableAmount = amount.add(deliveryFee).subtract(discountAmount);
    return new CheckoutPreviewView(
        context.store().id(),
        context.store().storeName(),
        context.store().businessType(),
        context.address() == null ? null : formatAddress(context.address()),
        deliveryFee,
        amount,
        payableAmount,
        discountAmount,
        itemViews,
        remark);
  }

  private OrderInsert buildOrderInsert(TradeContext context, String remark, String idempotencyKey) {
    CheckoutPreviewView preview = buildPreview(context, remark);
    String orderType = context.store().businessType();
    String title = context.items().size() == 1 ? context.items().get(0).row().title() : context.items().get(0).row().title() + "等" + context.items().size() + "件";
    String orderNo = "AT" + System.currentTimeMillis();
    String addressSnapshot = preview.addressSnapshot();
    return new OrderInsert(new TradeRepository.OrderInsertRow(
        CurrentUserContext.required().userId(),
        context.store().id(),
        context.store().storeName(),
        orderType,
        title,
        DisplayOrderStatus.UNPAID.code(),
        PaymentStatus.UNPAID.code(),
        "created",
        null,
        preview.amount(),
        preview.deliveryFee(),
        preview.discountAmount(),
        preview.payableAmount(),
        addressSnapshot,
        null,
        remark,
        idempotencyKey,
        orderNo));
  }

  private TradeRepository.OrderRow requireOrder(long orderId) {
    long userId = CurrentUserContext.required().userId();
    TradeRepository.OrderRow order = tradeRepository.findOrderById(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!order.userId().equals(userId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return order;
  }

  private OpsOrderSummaryView toOpsSummaryView(TradeRepository.OpsOrderRow row) {
    TradeRepository.OrderRow order = row.order();
    return new OpsOrderSummaryView(
        order.id(),
        order.orderNo(),
        order.orderType(),
        order.displayStatus(),
        order.paymentStatus(),
        order.fulfillmentStatus(),
        row.currentStage() == null ? order.fulfillmentStatus() : row.currentStage(),
        row.currentStageText(),
        order.storeName(),
        order.title(),
        order.payableAmount(),
        order.createdAt());
  }

  private String takeawayStageLabel(String status) {
    return switch (status) {
      case "created" -> "待付款";
      case "merchant_pending" -> "待商家接单";
      case "accepted" -> "商家已接单";
      case "preparing" -> "备餐中";
      case "ready_for_delivery" -> "待配送";
      case "delivering" -> "配送中";
      case "delivered" -> "已送达";
      case "completed" -> "已完成";
      case "merchant_rejected" -> "商家已拒单";
      case "cancelled" -> "已取消";
      case "abnormal" -> "异常处理中";
      default -> status;
    };
  }

  private OrderSummaryView toSummaryView(TradeRepository.OrderRow row) {
    return new OrderSummaryView(
        row.id(),
        row.orderNo(),
        row.orderType(),
        row.displayStatus(),
        row.storeName(),
        row.title(),
        row.payableAmount(),
        row.createdAt());
  }

  private OrderItemView toOrderItemView(TradeRepository.OrderItemRow row) {
    return new OrderItemView(
        row.itemId(),
        row.itemName(),
        row.itemSubtitle(),
        row.businessType(),
        row.categoryId(),
        row.categoryId() == null ? null : String.valueOf(row.categoryId()),
        row.quantity(),
        row.unitPrice(),
        row.totalPrice(),
        row.coverUrl());
  }

  private String formatAddress(TradeRepository.AddressRow address) {
    return address.province() + address.city() + address.district() + address.detailAddress();
  }

  private record TradeContext(TradeRepository.StoreRow store, TradeRepository.AddressRow address, TradeRepository.DeliveryRuleRow deliveryRule, List<TradeItem> items) {}

  private record TradeItem(TradeRepository.ItemRow row, int quantity) {}

  private record OrderInsert(TradeRepository.OrderInsertRow row) {}

  private record CheckoutContext(TradeContext tradeContext, CheckoutPreviewView preview) {}

  private record TakeawayStage(String stage, String text, String displayStatus, boolean completed, boolean scheduleNext) {}
}
