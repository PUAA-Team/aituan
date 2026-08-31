package com.aituan.tradefulfillment.trade;

import com.aituan.common.api.PageResponse;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
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
import com.aituan.tradefulfillment.trade.dto.TradeDtos.DeliveryTimelineView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderDetailView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderItemView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.OrderSummaryView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.PayOrderRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.PaymentMethodView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.RefundRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.TimelineNodeView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.VoucherView;
import com.aituan.tradefulfillment.trade.repository.TradeRepository;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.BookingRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.DeliveryTaskRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderInsertRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderItemInsertRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderItemRow;
import com.aituan.tradefulfillment.trade.repository.TradeRepository.OrderRow;
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

  public TradeService(
      TradeRepository tradeRepository,
      CatalogClient catalogClient,
      IdentityClient identityClient,
      CouponClient couponClient,
      DistanceClient distanceClient,
      InventoryClient inventoryClient,
      MemberGrowthClient memberGrowthClient,
      MessageClient messageClient) {
    this.tradeRepository = tradeRepository;
    this.catalogClient = catalogClient;
    this.identityClient = identityClient;
    this.couponClient = couponClient;
    this.distanceClient = distanceClient;
    this.inventoryClient = inventoryClient;
    this.memberGrowthClient = memberGrowthClient;
    this.messageClient = messageClient;
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
