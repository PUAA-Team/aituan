package com.aituan.tradefulfillment.trade;

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
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartItemQuantityRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartItemRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartLineView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CartView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutItemRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutItemView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutPreviewRequest;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.CheckoutPreviewView;
import com.aituan.tradefulfillment.trade.dto.TradeDtos.PaymentMethodView;
import com.aituan.tradefulfillment.trade.repository.TradeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  private final TradeRepository tradeRepository;
  private final CatalogClient catalogClient;
  private final IdentityClient identityClient;
  private final CouponClient couponClient;
  private final DistanceClient distanceClient;

  public TradeService(
      TradeRepository tradeRepository,
      CatalogClient catalogClient,
      IdentityClient identityClient,
      CouponClient couponClient,
      DistanceClient distanceClient) {
    this.tradeRepository = tradeRepository;
    this.catalogClient = catalogClient;
    this.identityClient = identityClient;
    this.couponClient = couponClient;
    this.distanceClient = distanceClient;
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
    return new CartLineView(
        item.id(),
        item.itemName(),
        item.subtitle(),
        item.categoryName(),
        valueOrZero(item.price()),
        quantity,
        totalPrice,
        stock(item),
        item.status(),
        soldOut);
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
      BigDecimal total = valueOrZero(tradeItem.item().price()).multiply(BigDecimal.valueOf(tradeItem.quantity()));
      amount = amount.add(total);
      itemViews.add(new CheckoutItemView(
          tradeItem.item().id(),
          tradeItem.item().itemName(),
          tradeItem.item().subtitle(),
          tradeItem.quantity(),
          valueOrZero(tradeItem.item().price()),
          total,
          tradeItem.item().categoryId(),
          tradeItem.item().categoryName()));
    }
    DeliveryQuote quote = deliveryQuote(context);
    BigDecimal distanceExtraFee = valueOrZero(quote.distanceExtraFee());
    BigDecimal deliveryFee = TAKEAWAY.equals(context.store().businessType()) ? valueOrZero(context.deliveryRule().deliveryFee()).add(distanceExtraFee) : BigDecimal.ZERO;
    BigDecimal packageFee = packageFee(context);
    BigDecimal discountAmount = couponDiscount(userId, couponId, amount);
    BigDecimal payableAmount = amount.add(deliveryFee).add(packageFee).subtract(discountAmount).max(BigDecimal.ZERO);
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
    return rule.distanceExtraFee().multiply(steps).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal packageFee(TradeContext context) {
    if (!TAKEAWAY.equals(context.store().businessType()) || context.deliveryRule() == null) {
      return BigDecimal.ZERO;
    }
    DeliveryRuleSnapshot rule = context.deliveryRule();
    String mode = rule.packageFeeMode() == null ? "none" : rule.packageFeeMode();
    return switch (mode) {
      case "fixed" -> valueOrZero(rule.packageFeeFixed());
      case "per_item" -> valueOrZero(rule.packageFeePerItem()).multiply(BigDecimal.valueOf(context.items().stream().mapToInt(TradeItem::quantity).sum())).setScale(2, RoundingMode.HALF_UP);
      default -> BigDecimal.ZERO;
    };
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

  private BigDecimal valueOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
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
