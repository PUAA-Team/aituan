package com.aituan.trade;

import com.aituan.common.api.PageResponse;
import com.aituan.common.enums.DisplayOrderStatus;
import com.aituan.common.enums.PaymentStatus;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
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
    if ("takeaway".equals(order.orderType())) {
      tradeRepository.updateOrderAfterTakeawayPaid(orderId);
      tradeRepository.insertDeliveryTask(orderId, LocalDateTime.now().plusMinutes(3));
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

  OrderDetailView getOrderDetail(long orderId) {
    return buildOrderDetail(requireOrder(orderId));
  }

  OrderDetailView getOrderDetailForStaff(long orderId) {
    return buildOrderDetail(requireOrderById(orderId));
  }

  DeliveryTimelineView deliveryTimeline(long orderId) {
    return buildDeliveryTimeline(requireOrder(orderId));
  }

  DeliveryTimelineView deliveryTimelineForStaff(long orderId) {
    return buildDeliveryTimeline(requireOrderById(orderId));
  }

  @Transactional
  OrderDetailView advanceDelivery(long orderId) {
    TradeRepository.OrderRow order = requireOrderById(orderId);
    TradeRepository.DeliveryTaskRow task = tradeRepository.findDeliveryTask(orderId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    switch (task.currentStage()) {
      case "accepted" -> tradeRepository.advanceDeliveryTask(task.id(), task.currentStage(), "preparing", "商家正在备餐", false);
      case "preparing" -> tradeRepository.advanceDeliveryTask(task.id(), task.currentStage(), "delivering", "骑手正在配送", false);
      case "delivering" -> tradeRepository.advanceDeliveryTask(task.id(), task.currentStage(), "delivered", "订单已送达", true);
      case "delivered" -> {
        return buildOrderDetail(order);
      }
      default -> throw new BusinessException(ErrorCode.ORDER_STATE_INVALID);
    }
    return buildOrderDetail(requireOrderById(orderId));
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
    tradeRepository.setOrderUsed(voucher.orderId(), operatorId);
    return buildOrderDetail(requireOrderById(voucher.orderId()));
  }

  @Transactional
  void advanceDueDeliveryTasks() {
    for (TradeRepository.DeliveryTaskRow task : tradeRepository.listDueDeliveryTasks()) {
      advanceDelivery(task.orderId());
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
}
