package com.aituan.tradefulfillment.trade;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.tradefulfillment.trade.client.CatalogClient;
import com.aituan.tradefulfillment.trade.client.CatalogClient.DeliveryRuleSnapshot;
import com.aituan.tradefulfillment.trade.client.CatalogClient.ItemSnapshot;
import com.aituan.tradefulfillment.trade.client.CatalogClient.StoreSnapshot;
import com.aituan.tradefulfillment.trade.client.CouponClient;
import com.aituan.tradefulfillment.trade.client.DistanceClient;
import com.aituan.tradefulfillment.trade.client.IdentityClient;
import com.aituan.tradefulfillment.trade.client.IdentityClient.AddressSnapshot;
import com.aituan.tradefulfillment.trade.client.InventoryClient;
import com.aituan.tradefulfillment.trade.client.MemberGrowthClient;
import com.aituan.tradefulfillment.trade.client.MessageClient;
import com.aituan.tradefulfillment.trade.client.MerchantAuthClient;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.AdminDeliveryTaskView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.BookingConfirmRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.BookingRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.BookingView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartItemQuantityRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartItemRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartLineView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutItemRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutItemView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutPreviewRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutPreviewView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CreateOrderRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.DeliveryActionRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.DeliveryTimelineView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OpsBookingView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OpsOrderSummaryView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OpsVoucherView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderDetailView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderItemView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderStatusCountView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderSummaryView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.PayOrderRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.PaymentMethodView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.RefundRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.TakeawayOrderActionRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.TimelineNodeView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.VoucherLookupView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.VoucherView;
import com.aituan.tradefulfillment.trade.repository.TradeRepository;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.BookingRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.DeliveryTaskRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderInsertRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderItemInsertRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderItemRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OpsBookingRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OpsVoucherRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.StatusCountRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.TimelineRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.VoucherRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeService {
  private static final String TAKEAWAY = "takeaway";
  private static final BigDecimal DEFAULT_MAX_DELIVERY_DISTANCE_KM = BigDecimal.valueOf(5).setScale(2);
  private static final DateTimeFormatter ARRIVAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final TradeRepository tradeRepository;
  private final CatalogClient catalogClient;
  private final IdentityClient identityClient;
  private final CouponClient couponClient;
  private final DistanceClient distanceClient;
  private final InventoryClient inventoryClient;
  private final MemberGrowthClient memberGrowthClient;
  private final MessageClient messageClient;
  private final MerchantAuthClient merchantAuthClient;

  public TradeService(
      TradeRepository tradeRepository,
      CatalogClient catalogClient,
      IdentityClient identityClient,
      CouponClient couponClient,
      DistanceClient distanceClient,
      InventoryClient inventoryClient,
      MemberGrowthClient memberGrowthClient,
      MessageClient messageClient,
      MerchantAuthClient merchantAuthClient) {
    this.tradeRepository = tradeRepository;
    this.catalogClient = catalogClient;
    this.identityClient = identityClient;
    this.couponClient = couponClient;
    this.distanceClient = distanceClient;
    this.inventoryClient = inventoryClient;
    this.memberGrowthClient = memberGrowthClient;
    this.messageClient = messageClient;
    this.merchantAuthClient = merchantAuthClient;
  }

  public List<PaymentMethodView> paymentMethods() {
    return List.of(
        new PaymentMethodView("mock", "模拟支付", true),
        new PaymentMethodView("wechat", "微信支付", false),
        new PaymentMethodView("alipay", "支付宝", false));
  }

  public CartView getCart(long storeId) {
    long userId = CurrentUserContext.required().userId();
    StoreSnapshot store = requireTakeawayStore(storeId);
    long cartId = tradeRepository.getOrCreateCart(userId, store.id());
    return buildCartView(store, cartId);
  }

  @Transactional
  public CartView addCartItem(CartItemRequest request) {
    long userId = CurrentUserContext.required().userId();
    StoreSnapshot store = requireTakeawayStore(request.storeId());
    ItemSnapshot item = requireCartItem(store.id(), request.itemId());
    long cartId = tradeRepository.getOrCreateCart(userId, store.id());
    int nextQuantity = tradeRepository.findCartItemQuantity(cartId, item.id()) + request.quantity();
    if (nextQuantity > stock(item)) {
      throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
    }
    tradeRepository.upsertCartItem(cartId, item.id(), request.quantity());
    return buildCartView(store, cartId);
  }

  @Transactional
  public CartView updateCartItem(long itemId, CartItemQuantityRequest request) {
    long userId = CurrentUserContext.required().userId();
    StoreSnapshot store = requireTakeawayStore(request.storeId());
    ItemSnapshot item = requireCartItem(store.id(), itemId);
    if (request.quantity() > stock(item)) {
      throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
    }
    long cartId = tradeRepository.getOrCreateCart(userId, store.id());
    tradeRepository.setCartItemQuantity(cartId, item.id(), request.quantity());
    return buildCartView(store, cartId);
  }

  @Transactional
  public CartView removeCartItem(long storeId, long itemId) {
    long userId = CurrentUserContext.required().userId();
    StoreSnapshot store = requireTakeawayStore(storeId);
    long cartId = tradeRepository.getOrCreateCart(userId, store.id());
    tradeRepository.removeCartItem(cartId, itemId);
    return buildCartView(store, cartId);
  }

  @Transactional
  public CartView clearCart(long storeId) {
    long userId = CurrentUserContext.required().userId();
    StoreSnapshot store = requireTakeawayStore(storeId);
    long cartId = tradeRepository.getOrCreateCart(userId, store.id());
    tradeRepository.clearCart(cartId);
    return buildCartView(store, cartId);
  }

  public CheckoutPreviewView preview(CheckoutPreviewRequest request) {
    long userId = CurrentUserContext.required().userId();
    TradeContext context = loadTradeContext(userId, request.storeId(), request.businessType(), request.addressId(), request.items());
    return buildPreview(context, request.remark(), tablewareSelection(request.tablewareOption(), request.tablewareCount()), request.couponId(), userId);
  }

  @Transactional
  public OrderDetailView createOrder(CreateOrderRequest request) {
    long userId = CurrentUserContext.required().userId();
    String idempotencyKey = normalizeIdempotencyKey(request.idempotencyKey());
    if (idempotencyKey != null) {
      var existing = tradeRepository.findOrderByIdempotency(userId, idempotencyKey);
      if (existing.isPresent()) {
        return toOrderDetail(existing.get());
      }
    }

    TradeContext context = loadTradeContext(userId, request.storeId(), request.businessType(), request.addressId(), request.items());
    CheckoutPreviewView preview = buildPreview(context, request.remark(), tablewareSelection(request.tablewareOption(), request.tablewareCount()), request.couponId(), userId);
    validatePreviewForOrder(preview);
    reserveInventory(context.items());

    Long orderId = tradeRepository.insertOrder(new OrderInsertRow(
        userId,
        context.store().id(),
        context.store().storeName(),
        context.store().businessType(),
        orderTitle(context.items()),
        "unpaid",
        "unpaid",
        "created",
        null,
        preview.amount(),
        preview.deliveryFee(),
        preview.packageFee(),
        preview.discountAmount(),
        preview.payableAmount(),
        preview.addressSnapshot(),
        preview.deliveryDistanceKm(),
        preview.estimatedArrivalAt(),
        null,
        preview.tablewareOption(),
        preview.tablewareCount(),
        request.remark(),
        idempotencyKey,
        newOrderNo()));
    for (TradeItem item : context.items()) {
      tradeRepository.insertOrderItem(orderId, toOrderItemInsert(item));
    }
    if (TAKEAWAY.equals(context.store().businessType())) {
      tradeRepository.clearCart(tradeRepository.getOrCreateCart(userId, context.store().id()));
    }
    return getOrderDetail(orderId);
  }

  @Transactional
  public OrderDetailView pay(long orderId, PayOrderRequest request) {
    OrderRow order = requireOwnOrder(orderId);
    if (!"mock".equalsIgnoreCase(request.paymentMode())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "当前仅支持模拟支付");
    }
    if ("refunded".equals(order.paymentStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "已退款订单不能支付");
    }
    if ("paid".equals(order.paymentStatus())) {
      return toOrderDetail(order);
    }
    tradeRepository.markPaymentSuccess(order.id(), "mock", order.payableAmount());
    if (TAKEAWAY.equals(order.orderType())) {
      tradeRepository.updateTakeawayAfterPaid(order.id(), "merchant_pending");
      tradeRepository.insertDeliveryTask(order.id(), "merchant_pending", "待商家接单", etaMinutes(order));
    } else {
      String voucherCode = voucherCode(order.id());
      tradeRepository.insertVoucher(order.id(), voucherCode, "AITUAN-VOUCHER:" + voucherCode, LocalDateTime.now().plusDays(30));
      tradeRepository.updateServiceAfterPaid(order.id(), "券码已生成，请到店出示核销");
    }
    memberGrowthClient.addOrderCompletionGrowth(order.userId(), order.id(), order.payableAmount());
    messageClient.order(order.userId(), "订单支付成功", order.title(), "trade", order.id());
    return getOrderDetail(order.id());
  }

  public PageResponse<OrderSummaryView> listOrders(String displayStatus, int page, int pageSize) {
    long userId = CurrentUserContext.required().userId();
    int safePage = Math.max(1, page);
    int safePageSize = Math.min(Math.max(1, pageSize), 50);
    String status = displayStatus == null || displayStatus.isBlank() ? null : displayStatus.trim();
    List<OrderSummaryView> rows = tradeRepository.listOrders(userId, status, (safePage - 1) * safePageSize, safePageSize)
        .stream()
        .map(this::toOrderSummary)
        .toList();
    return PageResponse.of(rows, safePage, safePageSize, tradeRepository.countOrders(userId, status));
  }

  public OrderDetailView getOrderDetail(long orderId) {
    return toOrderDetail(requireOwnOrder(orderId));
  }

  public DeliveryTimelineView deliveryTimeline(long orderId) {
    OrderRow order = requireOwnOrder(orderId);
    List<TimelineNodeView> nodes = tradeRepository.listDeliveryTimeline(order.id()).stream()
        .map(row -> new TimelineNodeView(row.code(), row.text(), row.reachedAt()))
        .toList();
    String currentStage = tradeRepository.findDeliveryTask(order.id()).map(DeliveryTaskRow::currentStage).orElse(order.fulfillmentStatus());
    return new DeliveryTimelineView(order.orderNo(), currentStage, nodes);
  }

  public PageResponse<OpsOrderSummaryView> listOpsOrders(String displayStatus, String fulfillmentStatus, int page, int pageSize) {
    requireStaff();
    int safePage = Math.max(1, page);
    int safePageSize = Math.min(Math.max(1, pageSize), 50);
    List<OpsOrderSummaryView> rows = tradeRepository.listOpsOrders(normalizeBlank(displayStatus), normalizeBlank(fulfillmentStatus), (safePage - 1) * safePageSize, safePageSize)
        .stream()
        .filter(this::canCurrentStaffManageOrder)
        .map(this::toOpsOrderSummary)
        .toList();
    return PageResponse.of(rows, safePage, safePageSize, tradeRepository.countOpsOrders(normalizeBlank(displayStatus), normalizeBlank(fulfillmentStatus)));
  }

  public List<OrderStatusCountView> opsOrderStats() {
    requireStaff();
    return tradeRepository.orderStatusCounts().stream()
        .map(row -> new OrderStatusCountView(row.status(), statusLabel(row.status()), row.count()))
        .toList();
  }

  public OrderDetailView getOrderDetailForStaff(long orderId) {
    return toOrderDetail(requireStaffOrder(orderId));
  }

  @Transactional
  public OrderDetailView acceptTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "merchant_pending", "accepted", "商家已接单", "pending", false, request);
  }

  @Transactional
  public OrderDetailView rejectTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    OrderRow order = requireStaffOrder(orderId);
    ensureTakeaway(order);
    refundStaffOrder(order, defaultText(actionRemark(request), "商家拒单，系统自动退款"));
    return getOrderDetailForStaff(order.id());
  }

  @Transactional
  public OrderDetailView prepareTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "accepted", "preparing", "商家正在备餐", "pending", false, request);
  }

  @Transactional
  public OrderDetailView readyTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "preparing", "ready_for_delivery", "餐品已出餐，待配送", "pending", false, request);
  }

  @Transactional
  public OrderDetailView advanceDelivery(long orderId) {
    OrderRow order = requireStaffOrder(orderId);
    ensureTakeaway(order);
    DeliveryTaskRow task = tradeRepository.findDeliveryTask(order.id()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return moveTakeawayOrder(order.id(), task.currentStage(), nextStage(task.currentStage()), nextStageText(task.currentStage()), nextStageDisplayStatus(task.currentStage()), "delivered".equals(task.currentStage()), null);
  }

  @Transactional
  public OrderDetailView completeTakeawayOrder(long orderId, TakeawayOrderActionRequest request) {
    return moveTakeawayOrder(orderId, "delivered", "completed", "订单已完成", "used", true, request);
  }

  @Transactional
  public OrderDetailView markTakeawayAbnormal(long orderId, TakeawayOrderActionRequest request) {
    OrderRow order = requireStaffOrder(orderId);
    ensureTakeaway(order);
    tradeRepository.updateTakeawayStage(order.id(), "pending", "abnormal", false);
    tradeRepository.findDeliveryTask(order.id()).ifPresent(task -> tradeRepository.markDeliveryTaskAbnormal(task.id(), defaultText(actionRemark(request), "订单异常，待处理")));
    tradeRepository.insertOrderStateLog(order.id(), order.fulfillmentStatus(), "abnormal", "staff_abnormal", operatorType(), operatorId(), actionRemark(request));
    return getOrderDetailForStaff(order.id());
  }

  @Transactional
  public OrderDetailView refundOrderForStaff(long orderId, RefundRequest request) {
    OrderRow order = requireStaffOrder(orderId);
    refundStaffOrder(order, request == null ? null : request.reason());
    return getOrderDetailForStaff(order.id());
  }

  public VoucherLookupView lookupVoucher(String voucherCode) {
    requireStaff();
    VoucherRow voucher = tradeRepository.findVoucherByCode(voucherCode).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    OrderRow order = tradeRepository.findOrderById(voucher.orderId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    requireStaffCanManage(order);
    return new VoucherLookupView(voucher.voucherCode(), voucher.qrPayload(), voucher.status(), voucher.effectiveFrom(), voucher.effectiveTo(),
        order.id(), order.orderNo(), order.title(), order.storeName(), order.orderType(), order.payableAmount(), "到店出示券码核销");
  }

  @Transactional
  public OrderDetailView redeemVoucher(String voucherCode) {
    requireStaff();
    VoucherRow voucher = tradeRepository.findVoucherByCode(voucherCode).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!"unused".equals(voucher.status())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "refunded".equals(voucher.status()) ? "券码已退款失效" : "券码已核销");
    }
    OrderRow order = tradeRepository.findOrderById(voucher.orderId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    requireStaffCanManage(order);
    tradeRepository.setVoucherUsed(order.id(), operatorId());
    memberGrowthClient.addOrderCompletionGrowth(order.userId(), order.id(), order.payableAmount());
    messageClient.order(order.userId(), "券码已核销", order.storeName() + " 的订单已完成核销。", "核销", order.id());
    return getOrderDetailForStaff(order.id());
  }

  public PageResponse<OpsVoucherView> listOpsVouchers(String status, String keyword, int page, int pageSize) {
    requireStaff();
    int safePage = Math.max(1, page);
    int safePageSize = Math.min(Math.max(1, pageSize), 50);
    List<OpsVoucherView> rows = tradeRepository.listOpsVouchers(normalizeBlank(status), normalizeBlank(keyword), (safePage - 1) * safePageSize, safePageSize)
        .stream()
        .filter(row -> canCurrentStaffManageStore(row.storeName(), row.orderId()))
        .map(this::toOpsVoucherView)
        .toList();
    return PageResponse.of(rows, safePage, safePageSize, tradeRepository.countOpsVouchers(normalizeBlank(status), normalizeBlank(keyword)));
  }

  public PageResponse<OpsBookingView> listOpsBookings(String status, String businessType, int page, int pageSize) {
    requireStaff();
    int safePage = Math.max(1, page);
    int safePageSize = Math.min(Math.max(1, pageSize), 50);
    List<OpsBookingView> rows = tradeRepository.listOpsBookings(normalizeBlank(status), normalizeBlank(businessType), (safePage - 1) * safePageSize, safePageSize)
        .stream()
        .map(this::toOpsBookingView)
        .toList();
    return PageResponse.of(rows, safePage, safePageSize, tradeRepository.countOpsBookings(normalizeBlank(status), normalizeBlank(businessType)));
  }

  @Transactional
  public BookingView confirmBookingForStaff(long orderId, BookingConfirmRequest request) {
    OrderRow order = requireStaffOrder(orderId);
    tradeRepository.confirmBooking(order.id(), operatorId(), request == null ? null : request.remark());
    messageClient.order(order.userId(), "预约已确认", order.storeName() + " 已确认您的预约。", "预约", order.id());
    return getBookingForStaff(order.id());
  }

  public BookingView getBookingForStaff(long orderId) {
    OrderRow order = requireStaffOrder(orderId);
    return tradeRepository.findBookingByOrder(order.id()).map(row -> toBookingView(order, row)).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  public PageResponse<AdminDeliveryTaskView> deliveryTasks(String stage, int page, int pageSize) {
    requireAdmin();
    int safePage = Math.max(1, page);
    int safePageSize = Math.min(Math.max(1, pageSize), 50);
    List<AdminDeliveryTaskView> rows = tradeRepository.listDeliveryTasks(normalizeBlank(stage), (safePage - 1) * safePageSize, safePageSize).stream()
        .map(this::toAdminDeliveryTaskView)
        .toList();
    return PageResponse.of(rows, safePage, safePageSize, tradeRepository.countDeliveryTasks(normalizeBlank(stage)));
  }

  public AdminDeliveryTaskView deliveryTask(long taskId) {
    requireAdmin();
    return toAdminDeliveryTaskView(tradeRepository.findDeliveryTaskById(taskId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)));
  }

  @Transactional
  public AdminDeliveryTaskView advanceDeliveryTask(long taskId, DeliveryActionRequest request) {
    requireAdmin();
    DeliveryTaskRow task = tradeRepository.findDeliveryTaskById(taskId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    advanceDelivery(task.orderId());
    return deliveryTask(taskId);
  }

  @Transactional
  public AdminDeliveryTaskView pauseDeliveryTask(long taskId) {
    requireAdmin();
    tradeRepository.pauseDeliveryTask(taskId);
    return deliveryTask(taskId);
  }

  @Transactional
  public AdminDeliveryTaskView resumeDeliveryTask(long taskId) {
    requireAdmin();
    tradeRepository.resumeDeliveryTask(taskId);
    return deliveryTask(taskId);
  }

  @Transactional
  public AdminDeliveryTaskView markDeliveryAbnormal(long taskId, DeliveryActionRequest request) {
    requireAdmin();
    tradeRepository.markDeliveryTaskAbnormal(taskId, request == null ? null : defaultText(request.reason(), request.remark()));
    return deliveryTask(taskId);
  }

  @Transactional
  public OrderDetailView refundOrderForUser(long orderId, RefundRequest request) {
    OrderRow order = requireOwnOrder(orderId);
    if (!"paid".equals(order.paymentStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "仅已支付订单可申请退款");
    }
    if (!"none".equals(order.refundStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单已退款或正在退款中");
    }
    String reason = request == null || request.reason() == null || request.reason().isBlank() ? "用户申请退款" : request.reason().trim();
    tradeRepository.insertRefundRecord(order.id(), refundNo(order.id()), order.userId(), order.storeId(), order.payableAmount(), "user", order.userId(), reason);
    tradeRepository.markOrderRefunded(order.id(), order.payableAmount(), reason, "user", order.userId());
    tradeRepository.markVoucherRefunded(order.id());
    tradeRepository.markDeliveryTaskRefunded(order.id());
    for (OrderItemRow item : tradeRepository.listOrderItems(order.id())) {
      inventoryClient.release(item.itemId(), item.quantity());
    }
    memberGrowthClient.refundOrderGrowth(order.userId(), order.id());
    messageClient.order(order.userId(), "订单退款成功", reason, "refund", order.id());
    return getOrderDetail(order.id());
  }

  public BookingView getBookingForUser(long orderId) {
    OrderRow order = requireOwnOrder(orderId);
    return tradeRepository.findBookingByOrder(order.id()).map(row -> toBookingView(order, row)).orElse(null);
  }

  @Transactional
  public BookingView upsertBooking(long orderId, BookingRequest request) {
    OrderRow order = requireOwnOrder(orderId);
    int guestCount = request.guestCount() == null || request.guestCount() < 1 ? 1 : request.guestCount();
    tradeRepository.upsertBooking(
        order.id(),
        order.orderType(),
        defaultText(request.contactName(), "爱团用户"),
        defaultText(request.contactPhone(), "18800001111"),
        defaultText(request.bookingDate(), LocalDate.now().plusDays(1).toString()),
        defaultText(request.bookingTimeSlot(), "18:00-20:00"),
        guestCount,
        request.remark());
    return getBookingForUser(order.id());
  }

  private OrderDetailView moveTakeawayOrder(long orderId, String expectedStage, String nextStage, String stageText, String displayStatus, boolean completed, TakeawayOrderActionRequest request) {
    OrderRow order = requireStaffOrder(orderId);
    ensureTakeaway(order);
    if (!"paid".equals(order.paymentStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "仅已支付订单可履约");
    }
    int updated = tradeRepository.updateDeliveryTaskStage(order.id(), expectedStage, nextStage, stageText, completed);
    if (updated == 0) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
    }
    tradeRepository.updateTakeawayStage(order.id(), displayStatus, nextStage, completed);
    tradeRepository.insertOrderStateLog(order.id(), order.fulfillmentStatus(), nextStage, "staff_delivery", operatorType(), operatorId(), actionRemark(request));
    if (completed) {
      memberGrowthClient.addOrderCompletionGrowth(order.userId(), order.id(), order.payableAmount());
      messageClient.order(order.userId(), "订单已完成", order.storeName() + " 的订单已完成。", "完成", order.id());
    }
    return getOrderDetailForStaff(order.id());
  }

  private void refundStaffOrder(OrderRow order, String reasonText) {
    if (!"paid".equals(order.paymentStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "只有已支付订单可以退款");
    }
    if (!"none".equals(order.refundStatus())) {
      throw new BusinessException(ErrorCode.ORDER_STATE_INVALID, "订单已退款或正在退款中");
    }
    String reason = defaultText(reasonText, "工作人员发起退款");
    tradeRepository.insertRefundRecord(order.id(), refundNo(order.id()), order.userId(), order.storeId(), order.payableAmount(), operatorType(), operatorId(), reason);
    tradeRepository.markOrderRefunded(order.id(), order.payableAmount(), reason, operatorType(), operatorId());
    tradeRepository.markVoucherRefunded(order.id());
    tradeRepository.markDeliveryTaskRefunded(order.id());
    for (OrderItemRow item : tradeRepository.listOrderItems(order.id())) {
      inventoryClient.release(item.itemId(), item.quantity());
    }
    memberGrowthClient.refundOrderGrowth(order.userId(), order.id());
    messageClient.order(order.userId(), "订单退款成功", reason, "退款", order.id());
    tradeRepository.insertOrderStateLog(order.id(), order.fulfillmentStatus(), "refunded", "staff_refund", operatorType(), operatorId(), reason);
  }

  private OpsOrderSummaryView toOpsOrderSummary(OrderRow order) {
    DeliveryTaskRow task = tradeRepository.findDeliveryTask(order.id()).orElse(null);
    return new OpsOrderSummaryView(order.id(), order.orderNo(), order.orderType(), order.displayStatus(), order.paymentStatus(), order.fulfillmentStatus(),
        order.refundStatus(), task == null ? null : task.currentStage(), task == null ? null : task.currentStageText(), order.storeName(), order.title(), order.payableAmount(), order.createdAt());
  }

  private OpsVoucherView toOpsVoucherView(OpsVoucherRow row) {
    return new OpsVoucherView(row.voucherCode(), row.qrPayload(), row.status(), row.effectiveFrom(), row.effectiveTo(), row.verifiedAt(), row.verifiedBy(),
        row.orderId(), row.orderNo(), row.orderTitle(), row.storeName(), row.businessType(), row.payableAmount(), row.displayStatus(), row.refundStatus(),
        "none".equals(row.refundStatus()), row.orderCreatedAt());
  }

  private OpsBookingView toOpsBookingView(OpsBookingRow row) {
    BookingView booking = new BookingView(row.orderId(), row.orderNo(), row.storeName(), row.businessType(), row.contactName(), row.contactPhone(),
        row.bookingDate(), row.bookingTimeSlot(), row.guestCount(), row.storeConfirmStatus(), row.storeConfirmRemark(), row.confirmedAt(), row.createdAt());
    return new OpsBookingView(booking, row.orderTitle(), row.displayStatus(), row.paymentStatus(), row.refundStatus(), row.payableAmount(), "none".equals(row.refundStatus()));
  }

  private AdminDeliveryTaskView toAdminDeliveryTaskView(DeliveryTaskRow task) {
    OrderRow order = tradeRepository.findOrderById(task.orderId()).orElse(null);
    return new AdminDeliveryTaskView(task.id(), task.orderId(), order == null ? null : order.orderNo(), order == null ? null : order.storeName(),
        task.currentStage(), task.currentStageText(), task.autoAdvanceEnabled(), task.pausedAt(), task.abnormalReason(), task.nextTickAt(), task.completedAt(), task.updatedAt());
  }

  private CurrentUser requireStaff() {
    CurrentUser current = CurrentUserContext.required();
    if (current.accountType() != AccountType.MERCHANT && current.accountType() != AccountType.ADMIN) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    return current;
  }

  private void requireAdmin() {
    if (CurrentUserContext.required().accountType() != AccountType.ADMIN) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
  }

  private OrderRow requireStaffOrder(long orderId) {
    OrderRow order = tradeRepository.findOrderById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    requireStaffCanManage(order);
    return order;
  }

  private void requireStaffCanManage(OrderRow order) {
    CurrentUser current = requireStaff();
    if (current.accountType() == AccountType.ADMIN) {
      return;
    }
    if (!merchantAuthClient.canManageStore(current.accountId(), order.storeId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
  }

  private boolean canCurrentStaffManageOrder(OrderRow order) {
    try {
      requireStaffCanManage(order);
      return true;
    } catch (BusinessException ex) {
      return false;
    }
  }

  private boolean canCurrentStaffManageStore(String ignoredStoreName, long orderId) {
    return tradeRepository.findOrderById(orderId).map(this::canCurrentStaffManageOrder).orElse(false);
  }

  private void ensureTakeaway(OrderRow order) {
    if (!TAKEAWAY.equals(order.orderType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "仅支持外卖订单");
    }
  }

  private String nextStage(String currentStage) {
    return switch (currentStage) {
      case "merchant_pending" -> "accepted";
      case "accepted" -> "preparing";
      case "preparing" -> "ready_for_delivery";
      case "ready_for_delivery" -> "delivering";
      case "delivering" -> "delivered";
      case "delivered" -> "completed";
      default -> throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
    };
  }

  private String nextStageText(String currentStage) {
    return switch (nextStage(currentStage)) {
      case "accepted" -> "商家已接单";
      case "preparing" -> "商家正在备餐";
      case "ready_for_delivery" -> "餐品已出餐，待配送";
      case "delivering" -> "骑手正在配送";
      case "delivered" -> "订单已送达";
      case "completed" -> "订单已完成";
      default -> "订单处理中";
    };
  }

  private String nextStageDisplayStatus(String currentStage) {
    return "delivered".equals(currentStage) ? "used" : "pending";
  }

  private String statusLabel(String status) {
    return switch (status) {
      case "unpaid" -> "待支付";
      case "pending" -> "进行中";
      case "unused" -> "待使用";
      case "used" -> "已完成";
      case "refunded" -> "已退款";
      default -> status;
    };
  }

  private String actionRemark(TakeawayOrderActionRequest request) {
    return request == null ? null : request.remark();
  }

  private String operatorType() {
    return CurrentUserContext.required().accountType().name().toLowerCase();
  }

  private Long operatorId() {
    return CurrentUserContext.required().accountId();
  }

  private String normalizeBlank(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private CartView buildCartView(StoreSnapshot store, long cartId) {
    List<CartLineView> items = tradeRepository.listCartItems(cartId).stream()
        .map(row -> toCartLineView(row.itemId(), row.quantity()))
        .toList();
    BigDecimal amount = items.stream().map(CartLineView::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CartView(store.id(), store.storeName(), amount, items);
  }

  private CartLineView toCartLineView(long itemId, int quantity) {
    ItemSnapshot item = catalogClient.findItem(itemId)
        .orElse(new ItemSnapshot(itemId, null, "已失效商品", null, TAKEAWAY, null, null, BigDecimal.ZERO, "off_sale", 0));
    BigDecimal totalPrice = valueOrZero(item.price()).multiply(BigDecimal.valueOf(quantity));
    boolean soldOut = stock(item) <= 0 || !"on_sale".equals(item.status());
    return new CartLineView(item.id(), item.itemName(), item.subtitle(), item.categoryName(), valueOrZero(item.price()), quantity, totalPrice, stock(item), item.status(), soldOut);
  }

  private TradeContext loadTradeContext(long userId, long storeId, String businessType, Long addressId, List<CheckoutItemRequest> requests) {
    StoreSnapshot store = catalogClient.findStore(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!normalizeBusinessType(businessType).equalsIgnoreCase(store.businessType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "商品不属于当前业务类型");
    }
    List<TradeItem> items = new ArrayList<>();
    for (CheckoutItemRequest request : requests) {
      ItemSnapshot item = requireCartItem(storeId, request.itemId());
      if (stock(item) < request.quantity()) {
        throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
      }
      items.add(new TradeItem(item, request.quantity()));
    }
    AddressSnapshot address = identityClient.findAddress(userId, addressId).orElse(null);
    return new TradeContext(store, address, catalogClient.deliveryRule(storeId), items);
  }

  private CheckoutPreviewView buildPreview(TradeContext context, String remark, TablewareSelection tableware, Long couponId, Long userId) {
    BigDecimal amount = BigDecimal.ZERO;
    List<CheckoutItemView> itemViews = new ArrayList<>();
    for (TradeItem tradeItem : context.items()) {
      BigDecimal total = money(valueOrZero(tradeItem.item().price()).multiply(BigDecimal.valueOf(tradeItem.quantity())));
      amount = amount.add(total);
      itemViews.add(new CheckoutItemView(
          tradeItem.item().id(),
          tradeItem.item().itemName(),
          tradeItem.item().subtitle(),
          tradeItem.quantity(),
          money(valueOrZero(tradeItem.item().price())),
          total,
          tradeItem.item().categoryId(),
          tradeItem.item().categoryName()));
    }
    DeliveryQuote quote = deliveryQuote(context);
    BigDecimal distanceExtraFee = valueOrZero(quote.distanceExtraFee());
    BigDecimal deliveryFee = TAKEAWAY.equals(context.store().businessType()) ? valueOrZero(context.deliveryRule().deliveryFee()).add(distanceExtraFee) : BigDecimal.ZERO;
    BigDecimal packageFee = packageFee(context);
    BigDecimal discountAmount = couponDiscount(userId, couponId, amount);
    BigDecimal payableAmount = money(amount.add(deliveryFee).add(packageFee).subtract(discountAmount).max(BigDecimal.ZERO));
    BigDecimal startPrice = startPrice(context);
    BigDecimal startPriceMissing = startPriceMissing(amount, startPrice);
    boolean minimumOrderMet = startPriceMissing.compareTo(BigDecimal.ZERO) <= 0;
    return new CheckoutPreviewView(
        context.store().id(), context.store().storeName(), context.store().businessType(), context.address() == null ? null : formatAddress(context.address()),
        money(deliveryFee), packageFee, distanceExtraFee, money(amount), payableAmount, money(discountAmount), startPrice, startPriceMissing,
        minimumOrderMet, quote.distanceKm(), quote.maxDistanceKm(), quote.estimatedMinutes(), quote.estimatedArrivalAt(), arrivalText(quote.estimatedArrivalAt()),
        quote.deliverable(), quote.unavailableReason(), tableware.option(), tableware.count(), tableware.text(), itemViews, remark);
  }

  private void validatePreviewForOrder(CheckoutPreviewView preview) {
    if (!Boolean.TRUE.equals(preview.deliverable())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, preview.unavailableReason());
    }
    if (!Boolean.TRUE.equals(preview.minimumOrderMet())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "未达到起送价");
    }
  }

  private void reserveInventory(List<TradeItem> items) {
    List<TradeItem> reserved = new ArrayList<>();
    for (TradeItem item : items) {
      if (!inventoryClient.reserve(item.item().id(), item.quantity())) {
        reserved.forEach(row -> inventoryClient.release(row.item().id(), row.quantity()));
        throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
      }
      reserved.add(item);
    }
  }

  private OrderItemInsertRow toOrderItemInsert(TradeItem item) {
    BigDecimal total = money(valueOrZero(item.item().price()).multiply(BigDecimal.valueOf(item.quantity())));
    return new OrderItemInsertRow(item.item().id(), item.item().itemName(), item.item().subtitle(), item.item().businessType(), item.item().categoryId(),
        item.quantity(), money(valueOrZero(item.item().price())), total, null);
  }

  private OrderDetailView toOrderDetail(OrderRow order) {
    List<OrderItemView> items = tradeRepository.listOrderItems(order.id()).stream().map(this::toOrderItemView).toList();
    DeliveryTimelineView timeline = TAKEAWAY.equals(order.orderType()) ? deliveryTimelineForOrder(order) : null;
    VoucherView voucher = tradeRepository.findVoucher(order.id()).map(this::toVoucherView).orElse(null);
    BookingView booking = tradeRepository.findBookingByOrder(order.id()).map(row -> toBookingView(order, row)).orElse(null);
    boolean refundable = "paid".equals(order.paymentStatus()) && "none".equals(order.refundStatus());
    return new OrderDetailView(
        order.id(), order.orderNo(), order.orderType(), order.displayStatus(), order.paymentStatus(), order.fulfillmentStatus(), order.paymentMethod(),
        order.storeId(), order.storeName(), order.title(), order.amount(), order.deliveryFee(), order.packageFee(), order.discountAmount(), order.payableAmount(),
        order.addressSnapshot(), order.deliveryDistanceKm(), order.estimatedArrivalAt(), arrivalText(order.estimatedArrivalAt()), deliveryCompletionText(order),
        order.voucherSummary(), order.tablewareOption(), order.tablewareCount(), tablewareSelection(order.tablewareOption(), order.tablewareCount()).text(), order.remark(),
        order.refundStatus(), order.refundAmount(), order.refundReason(), order.refundedAt(), refundable, false, refundable ? "可申请退款" : "当前状态不可退款",
        order.createdAt(), order.paidAt(), order.completedAt(), items, timeline, voucher, booking);
  }

  private OrderSummaryView toOrderSummary(OrderRow order) {
    return new OrderSummaryView(order.id(), order.orderNo(), order.orderType(), order.displayStatus(), order.fulfillmentStatus(), order.refundStatus(),
        order.storeName(), order.title(), order.payableAmount(), order.createdAt());
  }

  private OrderItemView toOrderItemView(OrderItemRow row) {
    String categoryName = catalogClient.findItem(row.itemId()).map(ItemSnapshot::categoryName).orElse(null);
    return new OrderItemView(row.itemId(), row.itemName(), row.itemSubtitle(), row.businessType(), row.categoryId(), categoryName,
        row.quantity(), row.unitPrice(), row.totalPrice(), row.coverUrl());
  }

  private DeliveryTimelineView deliveryTimelineForOrder(OrderRow order) {
    List<TimelineNodeView> nodes = tradeRepository.listDeliveryTimeline(order.id()).stream().map(this::toTimelineNode).toList();
    String currentStage = tradeRepository.findDeliveryTask(order.id()).map(DeliveryTaskRow::currentStage).orElse(order.fulfillmentStatus());
    return new DeliveryTimelineView(order.orderNo(), currentStage, nodes);
  }

  private TimelineNodeView toTimelineNode(TimelineRow row) {
    return new TimelineNodeView(row.code(), row.text(), row.reachedAt());
  }

  private VoucherView toVoucherView(VoucherRow row) {
    return new VoucherView(row.voucherCode(), row.qrPayload(), row.status(), row.effectiveFrom(), row.effectiveTo());
  }

  private BookingView toBookingView(OrderRow order, BookingRow row) {
    return new BookingView(order.id(), order.orderNo(), order.storeName(), row.businessType(), row.contactName(), row.contactPhone(), row.bookingDate(),
        row.bookingTimeSlot(), row.guestCount(), row.storeConfirmStatus(), row.storeConfirmRemark(), row.confirmedAt(), row.createdAt());
  }

  private OrderRow requireOwnOrder(long orderId) {
    long userId = CurrentUserContext.required().userId();
    OrderRow order = tradeRepository.findOrderById(orderId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (order.userId() != userId) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    return order;
  }

  private StoreSnapshot requireTakeawayStore(long storeId) {
    StoreSnapshot store = catalogClient.findStore(storeId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!TAKEAWAY.equals(store.businessType())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "购物车仅支持外卖门店");
    }
    return store;
  }

  private ItemSnapshot requireCartItem(long storeId, long itemId) {
    ItemSnapshot item = catalogClient.findItem(itemId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!storeIdEquals(item.storeId(), storeId)) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "商品不属于当前门店");
    }
    if (!"on_sale".equals(item.status())) {
      throw new BusinessException(ErrorCode.ITEM_STOCK_NOT_ENOUGH);
    }
    return item;
  }

  private boolean storeIdEquals(Long itemStoreId, long storeId) {
    return itemStoreId != null && itemStoreId == storeId;
  }

  private String normalizeBusinessType(String businessType) {
    return switch (businessType.trim().toLowerCase()) {
      case "group", "groupbuy" -> "group_buy";
      case "fun" -> "entertainment";
      default -> businessType.trim().toLowerCase();
    };
  }

  private BigDecimal couponDiscount(Long userId, Long couponId, BigDecimal amount) {
    CouponClient.CouponDiscount result = couponClient.calcDiscount(userId, couponId, amount);
    if (!result.usable()) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, result.reason() == null ? "优惠券不可用" : result.reason());
    }
    return valueOrZero(result.discountAmount());
  }

  private DeliveryQuote deliveryQuote(TradeContext context) {
    if (!TAKEAWAY.equals(context.store().businessType())) {
      return new DeliveryQuote(null, null, BigDecimal.ZERO, null, null, true, null);
    }
    DeliveryRuleSnapshot rule = context.deliveryRule();
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
    BigDecimal distanceKm = BigDecimal.valueOf(distanceClient.distanceKm(userLatitude, userLongitude, storeLatitude, storeLongitude)).setScale(2, RoundingMode.HALF_UP);
    if (distanceKm.compareTo(maxDistance) > 0) {
      return new DeliveryQuote(distanceKm, maxDistance, BigDecimal.ZERO, null, null, false,
          "超出商家可配送范围，当前距离 " + distanceKm.stripTrailingZeros().toPlainString() + "km，商家最多配送 " + maxDistance.stripTrailingZeros().toPlainString() + "km");
    }
    DistanceClient.DistanceEstimate estimate = distanceClient.estimate(userLatitude, userLongitude, storeLatitude, storeLongitude);
    int estimatedMinutes = Math.max(rule.estimatedMinutes(), parseEstimatedMinutes(estimate.estimatedTimeText(), rule.estimatedMinutes()));
    return new DeliveryQuote(distanceKm, maxDistance, distanceExtraFee(rule, distanceKm), estimatedMinutes, LocalDateTime.now().plusMinutes(estimatedMinutes), true, null);
  }

  private BigDecimal distanceExtraFee(DeliveryRuleSnapshot rule, BigDecimal distanceKm) {
    if (distanceKm == null || rule.distanceExtraFee() == null || rule.distanceExtraFee().compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal threshold = valueOrZero(rule.distanceExtraThresholdKm());
    if (threshold.compareTo(BigDecimal.ZERO) <= 0 || distanceKm.compareTo(threshold) <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal step = rule.distanceExtraStepKm() == null || rule.distanceExtraStepKm().compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ONE : rule.distanceExtraStepKm();
    BigDecimal steps = distanceKm.subtract(threshold).divide(step, 0, RoundingMode.CEILING);
    return money(rule.distanceExtraFee().multiply(steps));
  }

  private BigDecimal packageFee(TradeContext context) {
    if (!TAKEAWAY.equals(context.store().businessType()) || context.deliveryRule() == null) {
      return BigDecimal.ZERO;
    }
    DeliveryRuleSnapshot rule = context.deliveryRule();
    String mode = rule.packageFeeMode() == null ? "none" : rule.packageFeeMode();
    return switch (mode) {
      case "fixed" -> money(valueOrZero(rule.packageFeeFixed()));
      case "per_item" -> money(valueOrZero(rule.packageFeePerItem()).multiply(BigDecimal.valueOf(context.items().stream().mapToInt(TradeItem::quantity).sum())));
      default -> BigDecimal.ZERO;
    };
  }

  private BigDecimal startPrice(TradeContext context) {
    if (!TAKEAWAY.equals(context.store().businessType()) || context.deliveryRule() == null) {
      return BigDecimal.ZERO;
    }
    return money(valueOrZero(context.deliveryRule().startPrice()));
  }

  private BigDecimal startPriceMissing(BigDecimal amount, BigDecimal startPrice) {
    BigDecimal missing = startPrice.subtract(amount == null ? BigDecimal.ZERO : amount);
    return missing.compareTo(BigDecimal.ZERO) <= 0 ? BigDecimal.ZERO : money(missing);
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

  private String formatAddress(AddressSnapshot address) {
    return address.province() + address.city() + address.district() + address.detailAddress();
  }

  private int parseEstimatedMinutes(String text, int fallback) {
    if (text == null || text.isBlank()) {
      return fallback;
    }
    String digits = text.replaceAll("\\D+", "");
    return digits.isBlank() ? fallback : Math.max(1, Integer.parseInt(digits));
  }

  private String arrivalText(LocalDateTime estimatedArrivalAt) {
    return estimatedArrivalAt == null ? null : estimatedArrivalAt.format(ARRIVAL_TIME_FORMATTER);
  }

  private String deliveryCompletionText(OrderRow order) {
    if (order.completedAt() != null) return "订单已完成";
    if ("refunded".equals(order.fulfillmentStatus())) return "订单已退款";
    return null;
  }

  private String orderTitle(List<TradeItem> items) {
    if (items.isEmpty()) return "爱团订单";
    String firstName = items.get(0).item().itemName();
    return items.size() == 1 ? firstName : firstName + "等" + items.size() + "件商品";
  }

  private String newOrderNo() {
    return "T" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + String.format("%06d", Math.floorMod(System.nanoTime(), 1_000_000));
  }

  private String voucherCode(long orderId) {
    return String.format("88%08d", Math.floorMod(orderId, 100_000_000));
  }

  private String refundNo(long orderId) {
    return "R" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + String.format("%06d", Math.floorMod(orderId, 1_000_000));
  }

  private int etaMinutes(OrderRow order) {
    if (order.estimatedArrivalAt() == null) return 35;
    return Math.max(1, (int) java.time.Duration.between(LocalDateTime.now(), order.estimatedArrivalAt()).toMinutes());
  }

  private String normalizeIdempotencyKey(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private BigDecimal valueOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal money(BigDecimal value) {
    return valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
  }

  private int stock(ItemSnapshot item) {
    return item.stock() == null ? 0 : item.stock();
  }

  private record TradeContext(StoreSnapshot store, AddressSnapshot address, DeliveryRuleSnapshot deliveryRule, List<TradeItem> items) {}

  private record TradeItem(ItemSnapshot item, int quantity) {}

  private record TablewareSelection(String option, Integer count, String text) {}

  private record DeliveryQuote(
      BigDecimal distanceKm,
      BigDecimal maxDistanceKm,
      BigDecimal distanceExtraFee,
      Integer estimatedMinutes,
      LocalDateTime estimatedArrivalAt,
      boolean deliverable,
      String unavailableReason) {}
}
