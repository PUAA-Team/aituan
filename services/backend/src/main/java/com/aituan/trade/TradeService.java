package com.aituan.trade;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.enums.DisplayOrderStatus;
import com.aituan.common.enums.PaymentStatus;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.discovery.MapDistanceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
  private static final BigDecimal DEFAULT_MAX_DELIVERY_DISTANCE_KM = BigDecimal.valueOf(5).setScale(2);
  private static final DateTimeFormatter ARRIVAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final TradeRepository tradeRepository;
  private final MapDistanceService mapDistanceService;

  TradeService(TradeRepository tradeRepository, MapDistanceService mapDistanceService) {
    this.tradeRepository = tradeRepository;
    this.mapDistanceService = mapDistanceService;
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
    TablewareSelection tableware = tablewareSelection(request.tablewareOption(), request.tablewareCount());
    return buildPreview(context, request.remark(), tableware);
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
    TablewareSelection tableware = tablewareSelection(request.tablewareOption(), request.tablewareCount());
    DeliveryQuote quote = deliveryQuote(context);
    requireDeliverable(quote);
    requireMinimumOrder(context);
    reserveStock(context.items());
    OrderInsert orderInsert = buildOrderInsert(context, request.remark(), request.idempotencyKey(), quote, tableware);
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

  @Transactional
  OrderDetailView cancelTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    TradeRepository.OrderRow order = requireOrder(orderId);
    if (!TAKEAWAY.equals(order.orderType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅支持取消外卖订单");
    }
    if (DisplayOrderStatus.CANCELLED.code().equals(order.displayStatus())) {
      return getOrderDetail(orderId);
    }
    if (DisplayOrderStatus.USED.code().equals(order.displayStatus()) || "completed".equals(order.fulfillmentStatus()) || "delivering".equals(order.fulfillmentStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前状态不能取消");
    }
    CurrentUser current = CurrentUserContext.required();
    tradeRepository.updateTakeawayFulfillment(orderId, DisplayOrderStatus.CANCELLED.code(), "cancelled", true);
    tradeRepository.cancelDeliveryTask(orderId);
    writeOrderLogs(order, "cancelled", "user_cancel", "user", current.accountId(), actionRemark(request) == null ? "用户取消订单" : actionRemark(request));
    return getOrderDetail(orderId);
  }

  @Transactional
  OrderDetailView remindTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    TradeRepository.OrderRow order = requireOrder(orderId);
    if (!TAKEAWAY.equals(order.orderType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅支持提醒外卖订单");
    }
    if (!DisplayOrderStatus.PENDING.code().equals(order.displayStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前状态不能催单");
    }
    CurrentUser current = CurrentUserContext.required();
    String remark = actionRemark(request) == null ? "用户催单" : actionRemark(request);
    tradeRepository.insertOrderStateLog(order.id(), order.fulfillmentStatus(), order.fulfillmentStatus(), "user_remind", "user", current.accountId(), remark);
    tradeRepository.insertAuditLog("user", current.accountId(), "user_remind", "order", order.id(), remark);
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
        .orElse(defaultDeliveryRule());
    return toDeliveryRuleOpsView(storeId, rule);
  }

  @Transactional
  DeliveryRuleOpsView updateDeliveryRule(long storeId, DeliveryRuleUpdateRequest request) {
    requireTakeawaySettingStore(storeId);
    if (request.deliveryFee().compareTo(BigDecimal.ZERO) < 0 || request.startPrice().compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "配送费和起送价不能为负数");
    }
    if (request.maxDeliveryDistanceKm().compareTo(BigDecimal.ZERO) <= 0 || request.maxDeliveryDistanceKm().compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "可配送范围需大于 0 且不超过 100km");
    }
    String deliveryText = request.deliveryText() == null ? "" : request.deliveryText().trim();
    String packageFeeMode = normalizePackageFeeMode(request.packageFeeMode());
    BigDecimal packageFeeFixed = nonNegative(request.packageFeeFixed());
    BigDecimal packageFeePerItem = nonNegative(request.packageFeePerItem());
    BigDecimal distanceExtraThresholdKm = nonNegative(request.distanceExtraThresholdKm());
    BigDecimal distanceExtraFee = nonNegative(request.distanceExtraFee());
    BigDecimal distanceExtraStepKm = request.distanceExtraStepKm() == null || request.distanceExtraStepKm().compareTo(BigDecimal.ZERO) <= 0
        ? BigDecimal.ONE
        : request.distanceExtraStepKm();
    tradeRepository.upsertDeliveryRule(
        storeId,
        request.deliveryFee(),
        request.startPrice(),
        request.estimatedMinutes(),
        request.maxDeliveryDistanceKm(),
        packageFeeMode,
        packageFeeFixed,
        packageFeePerItem,
        distanceExtraThresholdKm,
        distanceExtraFee,
        distanceExtraStepKm,
        deliveryText);
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
    return moveTakeawayOrder(orderId, "merchant_pending", nextStage("merchant_rejected", "商家已拒单", DisplayOrderStatus.CANCELLED.code(), true, false), "merchant_reject", request);
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

  @Transactional
  OrderDetailView updateDeliveryAddress(long orderId, OrderAddressUpdateRequest request) {
    TradeRepository.OrderRow order = requireOrder(orderId);
    if (!TAKEAWAY.equals(order.orderType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅支持修改外卖订单地址");
    }
    if (!canChangeDeliveryAddress(order)) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "当前状态不能修改配送地址");
    }
    TradeRepository.AddressRow address = tradeRepository.findAddress(order.userId(), request.addressId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    TradeRepository.StoreRow store = tradeRepository.findStore(order.storeId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    TradeRepository.DeliveryRuleRow rule = tradeRepository.findDeliveryRule(order.storeId()).orElse(defaultDeliveryRule());
    DeliveryQuote quote = deliveryQuote(new TradeContext(store, address, rule, List.of()));
    requireDeliverable(quote);
    tradeRepository.updateOrderDeliveryAddress(order.id(), formatAddress(address), quote.distanceKm(), quote.estimatedArrivalAt());
    tradeRepository.updateDeliveryTaskEta(order.id(), quote.estimatedMinutes() == null ? rule.estimatedMinutes() : quote.estimatedMinutes());
    writeOrderLogs(order, order.fulfillmentStatus(), "user_change_address", "user", CurrentUserContext.required().accountId(), formatAddress(address));
    return getOrderDetail(orderId);
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
    return new CheckoutContext(context, buildPreview(context, request.remark(), tablewareSelection(request.tablewareOption(), request.tablewareCount())));
  }

  private OrderDetailView buildOrderDetail(TradeRepository.OrderRow order) {
    List<OrderItemView> items = tradeRepository.listOrderItems(order.id()).stream().map(this::toOrderItemView).toList();
    DeliveryTimelineView deliveryTimeline = null;
    VoucherView voucher = null;
    BookingView booking = null;
    if ("takeaway".equals(order.orderType())) {
      deliveryTimeline = buildDeliveryTimeline(order);
    } else {
      voucher = tradeRepository.findVoucher(order.id())
          .map(row -> new VoucherView(row.voucherCode(), row.qrPayload(), row.status(), row.effectiveFrom(), row.effectiveTo()))
          .orElse(null);
      booking = tradeRepository.findBookingByOrder(order.id())
          .map(row -> toBookingView(order, row))
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
        order.packageFee(),
        order.discountAmount(),
        order.payableAmount(),
        order.addressSnapshot(),
        order.deliveryDistanceKm(),
        order.estimatedArrivalAt(),
        shouldShowArrivalText(order) ? arrivalText(order.estimatedArrivalAt()) : null,
        deliveryCompletionText(order),
        order.voucherSummary(),
        order.tablewareOption(),
        order.tablewareCount(),
        tablewareText(order.tablewareOption(), order.tablewareCount()),
        order.remark(),
        order.createdAt(),
        order.paidAt(),
        order.completedAt(),
        items,
        deliveryTimeline,
        voucher,
        booking);
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
    return new DeliveryRuleOpsView(
        storeId,
        row.deliveryFee(),
        row.startPrice(),
        row.estimatedMinutes(),
        row.maxDeliveryDistanceKm(),
        row.packageFeeMode(),
        row.packageFeeFixed(),
        row.packageFeePerItem(),
        row.distanceExtraThresholdKm(),
        row.distanceExtraFee(),
        row.distanceExtraStepKm(),
        row.deliveryText());
  }

  private TradeRepository.DeliveryRuleRow defaultDeliveryRule() {
    return new TradeRepository.DeliveryRuleRow(
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        35,
        DEFAULT_MAX_DELIVERY_DISTANCE_KM,
        "none",
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ONE,
        "");
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

  private String normalizePackageFeeMode(String mode) {
    String value = mode == null ? "none" : mode.trim().toLowerCase();
    return switch (value) {
      case "none", "fixed", "per_item" -> value;
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "打包费模式不支持");
    };
  }

  private BigDecimal nonNegative(BigDecimal value) {
    if (value == null) return BigDecimal.ZERO;
    if (value.compareTo(BigDecimal.ZERO) < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "费用不能为负数");
    }
    return value;
  }

  private BigDecimal valueOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private TablewareSelection tablewareSelection(String option, Integer count) {
    String value = option == null || option.isBlank() ? "merchant_decide" : option.trim().toLowerCase();
    return switch (value) {
      case "none" -> new TablewareSelection("none", null, "无需餐具");
      case "by_people" -> {
        int people = count == null || count < 1 ? 1 : count;
        yield new TablewareSelection("by_people", people, "按 " + people + " 人提供餐具");
      }
      case "merchant_decide" -> new TablewareSelection("merchant_decide", null, "商家按餐量提供餐具");
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "餐具选项不支持");
    };
  }

  private String tablewareText(String option, Integer count) {
    return tablewareSelection(option, count).text();
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
    TradeRepository.DeliveryRuleRow deliveryRule = tradeRepository.findDeliveryRule(storeId).orElse(defaultDeliveryRule());
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

  private CheckoutPreviewView buildPreview(TradeContext context, String remark, TablewareSelection tableware) {
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
    DeliveryQuote quote = deliveryQuote(context);
    BigDecimal distanceExtraFee = quote.distanceExtraFee() == null ? BigDecimal.ZERO : quote.distanceExtraFee();
    BigDecimal deliveryFee = BigDecimal.ZERO;
    if (TAKEAWAY.equals(context.store().businessType()) && context.deliveryRule() != null) {
      deliveryFee = context.deliveryRule().deliveryFee().add(distanceExtraFee);
    }
    BigDecimal packageFee = packageFee(context);
    BigDecimal discountAmount = BigDecimal.ZERO;
    BigDecimal payableAmount = amount.add(deliveryFee).add(packageFee).subtract(discountAmount);
    BigDecimal startPrice = startPrice(context);
    BigDecimal startPriceMissing = startPriceMissing(amount, startPrice);
    boolean minimumOrderMet = startPriceMissing.compareTo(BigDecimal.ZERO) <= 0;
    return new CheckoutPreviewView(
        context.store().id(),
        context.store().storeName(),
        context.store().businessType(),
        context.address() == null ? null : formatAddress(context.address()),
        deliveryFee,
        packageFee,
        distanceExtraFee,
        amount,
        payableAmount,
        discountAmount,
        startPrice,
        startPriceMissing,
        minimumOrderMet,
        quote.distanceKm(),
        quote.maxDistanceKm(),
        quote.estimatedMinutes(),
        quote.estimatedArrivalAt(),
        arrivalText(quote.estimatedArrivalAt()),
        quote.deliverable(),
        quote.unavailableReason(),
        tableware.option(),
        tableware.count(),
        tableware.text(),
        itemViews,
        remark);
  }

  private OrderInsert buildOrderInsert(TradeContext context, String remark, String idempotencyKey, DeliveryQuote quote, TablewareSelection tableware) {
    CheckoutPreviewView preview = buildPreview(context, remark, tableware);
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
        preview.packageFee(),
        preview.discountAmount(),
        preview.payableAmount(),
        addressSnapshot,
        quote.distanceKm(),
        quote.estimatedArrivalAt(),
        null,
        tableware.option(),
        tableware.count(),
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
        row.fulfillmentStatus(),
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

  private DeliveryQuote deliveryQuote(TradeContext context) {
    if (!TAKEAWAY.equals(context.store().businessType())) {
      return new DeliveryQuote(null, null, BigDecimal.ZERO, null, null, true, null);
    }
    TradeRepository.DeliveryRuleRow rule = context.deliveryRule() == null ? defaultDeliveryRule() : context.deliveryRule();
    BigDecimal maxDistance = rule.maxDeliveryDistanceKm() == null ? DEFAULT_MAX_DELIVERY_DISTANCE_KM : rule.maxDeliveryDistanceKm();
    if (context.address() == null) {
      return new DeliveryQuote(null, maxDistance, BigDecimal.ZERO, null, null, false, "请先新增或选择收货地址");
    }
    if (context.address().longitude() == null || context.address().latitude() == null) {
      return new DeliveryQuote(null, maxDistance, BigDecimal.ZERO, null, null, false, "该地址缺少定位坐标，请编辑地址并使用当前位置重新定位");
    }
    if (context.store().longitude() == null || context.store().latitude() == null) {
      return new DeliveryQuote(null, maxDistance, BigDecimal.ZERO, null, null, false, "商家暂未配置可配送位置，请稍后再试");
    }
    double userLatitude = context.address().latitude().doubleValue();
    double userLongitude = context.address().longitude().doubleValue();
    double storeLatitude = context.store().latitude().doubleValue();
    double storeLongitude = context.store().longitude().doubleValue();
    double distance = mapDistanceService.distanceKm(userLatitude, userLongitude, storeLatitude, storeLongitude);
    BigDecimal distanceKm = BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    if (distanceKm.compareTo(maxDistance) > 0) {
      return new DeliveryQuote(distanceKm, maxDistance, BigDecimal.ZERO, null, null, false,
          "超出商家可配送范围，当前距离 " + distanceKm.stripTrailingZeros().toPlainString() + "km，商家最多配送 " + maxDistance.stripTrailingZeros().toPlainString() + "km");
    }
    MapDistanceService.DistanceEstimate estimate = mapDistanceService.estimate(userLatitude, userLongitude, storeLatitude, storeLongitude);
    int estimatedMinutes = Math.max(rule.estimatedMinutes(), parseEstimatedMinutes(estimate.estimatedTimeText(), rule.estimatedMinutes()));
    LocalDateTime arrivalAt = LocalDateTime.now().plusMinutes(estimatedMinutes);
    return new DeliveryQuote(distanceKm, maxDistance, distanceExtraFee(rule, distanceKm), estimatedMinutes, arrivalAt, true, null);
  }

  private BigDecimal distanceExtraFee(TradeRepository.DeliveryRuleRow rule, BigDecimal distanceKm) {
    if (distanceKm == null || rule.distanceExtraFee() == null || rule.distanceExtraFee().compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal threshold = rule.distanceExtraThresholdKm() == null ? BigDecimal.ZERO : rule.distanceExtraThresholdKm();
    if (threshold.compareTo(BigDecimal.ZERO) <= 0 || distanceKm.compareTo(threshold) <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal step = rule.distanceExtraStepKm() == null || rule.distanceExtraStepKm().compareTo(BigDecimal.ZERO) <= 0
        ? BigDecimal.ONE
        : rule.distanceExtraStepKm();
    BigDecimal steps = distanceKm.subtract(threshold).divide(step, 0, RoundingMode.CEILING);
    return rule.distanceExtraFee().multiply(steps).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal packageFee(TradeContext context) {
    if (!TAKEAWAY.equals(context.store().businessType()) || context.deliveryRule() == null) {
      return BigDecimal.ZERO;
    }
    TradeRepository.DeliveryRuleRow rule = context.deliveryRule();
    String mode = rule.packageFeeMode() == null ? "none" : rule.packageFeeMode();
    return switch (mode) {
      case "fixed" -> valueOrZero(rule.packageFeeFixed());
      case "per_item" -> valueOrZero(rule.packageFeePerItem()).multiply(BigDecimal.valueOf(totalQuantity(context.items()))).setScale(2, RoundingMode.HALF_UP);
      default -> BigDecimal.ZERO;
    };
  }

  private int totalQuantity(List<TradeItem> items) {
    return items.stream().mapToInt(TradeItem::quantity).sum();
  }

  private BigDecimal startPrice(TradeContext context) {
    if (!TAKEAWAY.equals(context.store().businessType()) || context.deliveryRule() == null) {
      return BigDecimal.ZERO;
    }
    return valueOrZero(context.deliveryRule().startPrice()).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal startPriceMissing(BigDecimal amount, BigDecimal startPrice) {
    BigDecimal missing = startPrice.subtract(amount == null ? BigDecimal.ZERO : amount);
    return missing.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : missing.setScale(2, RoundingMode.HALF_UP);
  }

  private void requireMinimumOrder(TradeContext context) {
    BigDecimal startPrice = startPrice(context);
    if (startPrice.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }
    BigDecimal amount = context.items().stream()
        .map(item -> item.row().price().multiply(BigDecimal.valueOf(item.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal missing = startPriceMissing(amount, startPrice);
    if (missing.compareTo(BigDecimal.ZERO) > 0) {
      throw new BusinessException(
          ErrorCode.BUSINESS_RULE_VIOLATION,
          "商品金额还差￥" + moneyText(missing) + "起送，配送费和打包费不计入起送金额");
    }
  }

  private String moneyText(BigDecimal value) {
    BigDecimal normalized = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
    return normalized.toPlainString();
  }

  private int parseEstimatedMinutes(String text, int fallback) {
    if (text == null || text.isBlank()) {
      return fallback;
    }
    String digits = text.replaceAll("\\D+", "");
    if (digits.isBlank()) {
      return fallback;
    }
    return Math.max(1, Integer.parseInt(digits));
  }

  private void requireDeliverable(DeliveryQuote quote) {
    if (!quote.deliverable()) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, quote.unavailableReason());
    }
  }

  private boolean canChangeDeliveryAddress(TradeRepository.OrderRow order) {
    if (PaymentStatus.UNPAID.code().equals(order.paymentStatus())) {
      return true;
    }
    return DisplayOrderStatus.PENDING.code().equals(order.displayStatus()) && "merchant_pending".equals(order.fulfillmentStatus());
  }

  private boolean shouldShowArrivalText(TradeRepository.OrderRow order) {
    return !("delivered".equals(order.fulfillmentStatus()) || "completed".equals(order.fulfillmentStatus()));
  }

  private String deliveryCompletionText(TradeRepository.OrderRow order) {
    if (!("delivered".equals(order.fulfillmentStatus()) || "completed".equals(order.fulfillmentStatus()))) {
      return null;
    }
    if (order.completedAt() == null) {
      return "已送达";
    }
    String text = "已送达 " + order.completedAt().format(ARRIVAL_TIME_FORMATTER);
    if (order.estimatedArrivalAt() != null) {
      long minutes = java.time.Duration.between(order.completedAt(), order.estimatedArrivalAt()).toMinutes();
      if (minutes > 0) {
        return text + "，提前 " + minutes + " 分钟";
      }
      if (minutes < 0) {
        return text + "，晚于预计 " + Math.abs(minutes) + " 分钟";
      }
    }
    return text;
  }

  private String arrivalText(LocalDateTime estimatedArrivalAt) {
    return estimatedArrivalAt == null ? null : estimatedArrivalAt.format(ARRIVAL_TIME_FORMATTER);
  }

  // ---------- Stage5-D：预约与券码运营 ----------

  @Transactional
  BookingView upsertBooking(long orderId, BookingRequest request) {
    TradeRepository.OrderRow order = requireOrder(orderId);
    if ("takeaway".equals(order.orderType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "外卖订单不支持预约");
    }
    if (request == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "预约信息不能为空");
    }
    int guestCount = request.guestCount() == null || request.guestCount() < 1 ? 1 : request.guestCount();
    tradeRepository.upsertBooking(
        orderId,
        order.orderType(),
        nullIfBlank(request.contactName()),
        nullIfBlank(request.contactPhone()),
        nullIfBlank(request.bookingDate()),
        nullIfBlank(request.bookingTimeSlot()),
        guestCount,
        nullIfBlank(request.remark()));
    return tradeRepository.findBookingByOrder(orderId)
        .map(row -> toBookingView(order, row))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  BookingView getBookingForUser(long orderId) {
    TradeRepository.OrderRow order = requireOrder(orderId);
    return tradeRepository.findBookingByOrder(orderId)
        .map(row -> toBookingView(order, row))
        .orElse(null);
  }

  @Transactional
  BookingView confirmBookingForStaff(long orderId, BookingConfirmRequest request) {
    TradeRepository.OrderRow order = requireOrderForStaff(orderId);
    String remark = request == null ? null : nullIfBlank(request.remark());
    long operatorId = CurrentUserContext.required().accountId();
    int updated = tradeRepository.confirmBooking(orderId, remark, operatorId);
    if (updated == 0 && tradeRepository.findBookingByOrder(orderId).isEmpty()) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    if (updated > 0) {
      tradeRepository.insertAuditLog(
          CurrentUserContext.required().accountType().name().toLowerCase(),
          operatorId,
          "booking_confirm",
          "order",
          orderId,
          remark);
    }
    return tradeRepository.findBookingByOrder(orderId)
        .map(row -> toBookingView(order, row))
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  PageResponse<OpsBookingView> listOpsBookings(String status, String businessType, int page, int pageSize) {
    Long merchantAccountId = merchantAccountScope(CurrentUserContext.required());
    long total = tradeRepository.countOpsBookings(merchantAccountId, status, businessType);
    List<OpsBookingView> list = tradeRepository
        .listOpsBookings(merchantAccountId, status, businessType, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toOpsBookingView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  PageResponse<OpsVoucherView> listOpsVouchers(String status, String keyword, int page, int pageSize) {
    Long merchantAccountId = merchantAccountScope(CurrentUserContext.required());
    long total = tradeRepository.countOpsVouchers(merchantAccountId, status, keyword);
    List<OpsVoucherView> list = tradeRepository
        .listOpsVouchers(merchantAccountId, status, keyword, (page - 1) * pageSize, pageSize)
        .stream()
        .map(this::toOpsVoucherView)
        .toList();
    return PageResponse.of(list, page, pageSize, total);
  }

  VoucherLookupView lookupVoucher(String voucherCode) {
    TradeRepository.VoucherRow voucher = tradeRepository.findVoucherByCode(voucherCode)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    // 校验商家身份，确保只能查到自己门店的券码
    TradeRepository.OrderRow order = tradeRepository.findOrderById(voucher.orderId())
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    requireStoreAccess(order.storeId());
    return new VoucherLookupView(
        voucher.voucherCode(),
        voucher.qrPayload(),
        voucher.status(),
        voucher.effectiveFrom(),
        voucher.effectiveTo(),
        order.orderNo(),
        order.title(),
        order.storeName(),
        order.orderType(),
        order.payableAmount(),
        firstNonBlank(order.voucherSummary(), order.remark()));
  }

  private BookingView toBookingView(TradeRepository.OrderRow order, TradeRepository.BookingRow row) {
    return new BookingView(
        order.id(),
        order.orderNo(),
        order.storeName(),
        row.businessType(),
        row.contactName(),
        row.contactPhone(),
        row.bookingDate(),
        row.bookingTimeSlot(),
        row.guestCount(),
        row.storeConfirmStatus(),
        row.storeConfirmRemark(),
        row.confirmedAt(),
        row.createdAt());
  }

  private OpsBookingView toOpsBookingView(TradeRepository.OpsBookingRow row) {
    TradeRepository.OrderRow order = row.order();
    BookingView view = toBookingView(order, row.booking());
    return new OpsBookingView(
        view,
        order.title(),
        order.displayStatus(),
        order.paymentStatus(),
        order.payableAmount());
  }

  private OpsVoucherView toOpsVoucherView(TradeRepository.OpsVoucherRow row) {
    TradeRepository.VoucherRow voucher = row.voucher();
    TradeRepository.OrderRow order = row.order();
    return new OpsVoucherView(
        voucher.voucherCode(),
        voucher.qrPayload(),
        voucher.status(),
        voucher.effectiveFrom(),
        voucher.effectiveTo(),
        voucher.verifiedAt(),
        voucher.verifiedBy(),
        order.id(),
        order.orderNo(),
        order.title(),
        order.storeName(),
        row.storeBusinessType() != null ? row.storeBusinessType() : order.orderType(),
        order.payableAmount(),
        order.displayStatus(),
        order.createdAt());
  }

  private void requireStoreAccess(long storeId) {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() == AccountType.ADMIN) {
      return;
    }
    if (current.accountType() == AccountType.MERCHANT && tradeRepository.isStoreOwnedByAccount(storeId, current.accountId())) {
      return;
    }
    throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  private String nullIfBlank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second == null || second.isBlank() ? null : second;
  }

  private record TradeContext(TradeRepository.StoreRow store, TradeRepository.AddressRow address, TradeRepository.DeliveryRuleRow deliveryRule, List<TradeItem> items) {}

  private record TradeItem(TradeRepository.ItemRow row, int quantity) {}

  private record OrderInsert(TradeRepository.OrderInsertRow row) {}

  private record TablewareSelection(String option, Integer count, String text) {}

  private record DeliveryQuote(
      BigDecimal distanceKm,
      BigDecimal maxDistanceKm,
      BigDecimal distanceExtraFee,
      Integer estimatedMinutes,
      LocalDateTime estimatedArrivalAt,
      boolean deliverable,
      String unavailableReason) {}

  private record CheckoutContext(TradeContext tradeContext, CheckoutPreviewView preview) {}

  private record TakeawayStage(String stage, String text, String displayStatus, boolean completed, boolean scheduleNext) {}
}
