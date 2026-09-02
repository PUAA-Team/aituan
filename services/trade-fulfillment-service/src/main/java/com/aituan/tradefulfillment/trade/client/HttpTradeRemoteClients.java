package com.aituan.tradefulfillment.trade.client;

import com.aituan.common.api.RequestIds;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("!demo & !test")
public class HttpTradeRemoteClients implements CatalogClient, IdentityClient, CouponClient,
    InventoryClient, MemberGrowthClient, MerchantAuthClient, MessageClient {
  private static final String CALLER = "trade-fulfillment-service";

  private final RestClient identityClient;
  private final RestClient merchantClient;
  private final RestClient platformClient;
  private final String serviceToken;
  private final ObjectMapper objectMapper;

  public HttpTradeRemoteClients(
      @Value("${aituan.clients.identity-base-url}") String identityBaseUrl,
      @Value("${aituan.clients.merchant-base-url}") String merchantBaseUrl,
      @Value("${aituan.clients.platform-base-url}") String platformBaseUrl,
      @Value("${aituan.clients.connect-timeout-ms:1000}") int connectTimeoutMs,
      @Value("${aituan.clients.read-timeout-ms:2000}") int readTimeoutMs,
      @Value("${aituan.internal.service-token}") String serviceToken,
      ObjectMapper objectMapper) {
    this.identityClient = client(identityBaseUrl, connectTimeoutMs, readTimeoutMs);
    this.merchantClient = client(merchantBaseUrl, connectTimeoutMs, readTimeoutMs);
    this.platformClient = client(platformBaseUrl, connectTimeoutMs, readTimeoutMs);
    this.serviceToken = serviceToken;
    this.objectMapper = objectMapper;
  }

  @Override
  public Optional<StoreSnapshot> findStore(long storeId) {
    JsonNode data = getOptional(merchantClient, "/internal/stores/{storeId}/snapshot", storeId);
    if (data == null) return Optional.empty();
    return Optional.of(new StoreSnapshot(
        longValue(data, "storeId", storeId),
        nullableLong(data, "merchantId"),
        text(data, "storeName", "门店"),
        text(data, "businessType", "takeaway"),
        nullableDecimal(data, "longitude"),
        nullableDecimal(data, "latitude")));
  }

  @Override
  public Optional<ItemSnapshot> findItem(long itemId) {
    JsonNode snapshot = getOptional(merchantClient, "/internal/catalog/items/{itemId}/snapshot", itemId);
    if (snapshot == null) return Optional.empty();
    long storeId = longValue(snapshot, "storeId", 0);
    JsonNode quote = post(merchantClient, "/internal/catalog/checkout-quote",
        Map.of("storeId", storeId, "items", List.of(Map.of("itemId", itemId, "quantity", 1))),
        null, "商品报价服务暂不可用");
    JsonNode line = quote.path("items").isArray() && !quote.path("items").isEmpty()
        ? quote.path("items").get(0) : null;
    if (line == null) return Optional.empty();
    return Optional.of(new ItemSnapshot(
        longValue(line, "itemId", itemId),
        nullableLong(line, "skuId"),
        storeId,
        text(line, "itemName", text(snapshot, "itemName", "商品")),
        text(line, "subtitle", text(snapshot, "subtitle", null)),
        text(line, "businessType", text(snapshot, "businessType", "takeaway")),
        nullableLong(line, "categoryId"),
        text(line, "categoryName", null),
        decimal(line, "unitPrice"),
        text(line, "status", text(snapshot, "status", "off_sale")),
        line.path("stock").asInt(0),
        text(line, "coverUrl", text(snapshot, "coverUrl", null))));
  }

  @Override
  public DeliveryRuleSnapshot deliveryRule(long storeId) {
    JsonNode data = get(merchantClient, "/internal/stores/{storeId}/fulfillment-rules", "履约规则服务暂不可用", storeId);
    return new DeliveryRuleSnapshot(
        decimal(data, "deliveryFee"), decimal(data, "startPrice"), data.path("estimatedMinutes").asInt(35),
        decimal(data, "maxDeliveryDistanceKm"), text(data, "packageFeeMode", "none"),
        decimal(data, "packageFeeFixed"), decimal(data, "packageFeePerItem"),
        decimal(data, "distanceExtraThresholdKm"), decimal(data, "distanceExtraFee"),
        decimal(data, "distanceExtraStepKm"));
  }

  @Override
  public Optional<AddressSnapshot> findAddress(long userId, Long addressId) {
    if (addressId == null) return Optional.empty();
    JsonNode data = getOptional(identityClient, "/internal/users/{userId}/addresses/{addressId}/snapshot", userId, addressId);
    if (data == null) return Optional.empty();
    return Optional.of(new AddressSnapshot(
        longValue(data, "id", longValue(data, "addressId", addressId)), userId,
        text(data, "contactName", null), text(data, "contactPhone", null),
        text(data, "province", null), text(data, "city", null), text(data, "district", null),
        text(data, "detailAddress", null), nullableDecimal(data, "longitude"), nullableDecimal(data, "latitude")));
  }

  @Override
  public CouponDiscount calcDiscount(Long userId, Long couponId, BigDecimal amount) {
    if (couponId == null) return new CouponDiscount(true, BigDecimal.ZERO, null);
    JsonNode data = post(identityClient, "/internal/coupons/quote",
        Map.of("userId", userId, "couponId", couponId, "orderAmount", amount),
        "trade:coupon:quote:" + userId + ":" + couponId + ":" + amount,
        "优惠券报价服务暂不可用");
    return new CouponDiscount(data.path("usable").asBoolean(false), decimal(data, "discountAmount"), text(data, "reason", null));
  }

  @Override
  public void use(long couponId, long userId, long orderId, BigDecimal orderAmount) {
    post(identityClient, "/internal/coupons/{couponId}/use",
        Map.of("userId", userId, "orderId", orderId, "orderAmount", orderAmount),
        "trade:order:" + orderId + ":coupon:use", "优惠券核销失败", couponId);
  }

  @Override
  public void release(long couponId, long orderId) {
    post(identityClient, "/internal/coupons/{couponId}/release", Map.of(),
        "trade:order:" + orderId + ":coupon:release", "优惠券释放失败", couponId);
  }

  @Override
  public void deduct(long orderId, List<InventoryLine> items) {
    post(merchantClient, "/internal/inventory/deduct", inventoryBody(orderId, items),
        "trade:order:" + orderId + ":inventory:deduct", "库存扣减失败");
  }

  @Override
  public void restore(long orderId, List<InventoryLine> items) {
    post(merchantClient, "/internal/inventory/restore", inventoryBody(orderId, items),
        "trade:order:" + orderId + ":inventory:restore", "库存恢复失败");
  }

  @Override
  public void addOrderCompletionGrowth(long userId, long orderId, BigDecimal amount) {
    int delta = Math.max(1, amount == null ? 1 : amount.intValue());
    growth(userId, orderId, "order_paid", delta, "订单支付成长值");
  }

  @Override
  public void refundOrderGrowth(long userId, long orderId, BigDecimal amount) {
    int delta = -Math.max(1, amount == null ? 1 : amount.intValue());
    growth(userId, orderId, "order_refund", delta, "订单退款成长值冲正");
  }

  @Override
  public boolean canManageStore(long accountId, long storeId) {
    JsonNode merchant = getOptional(merchantClient, "/internal/merchants/by-account/{accountId}", accountId);
    JsonNode store = getOptional(merchantClient, "/internal/stores/{storeId}/snapshot", storeId);
    if (merchant == null || store == null) return false;
    String status = text(merchant, "status", "");
    if (!("active".equalsIgnoreCase(status) || "normal".equalsIgnoreCase(status))) return false;
    long merchantId = longValue(merchant, "merchantId", 0);
    return merchantId > 0 && merchantId == longValue(store, "merchantId", -1);
  }

  @Override
  public void order(long userId, String title, String content, String badge, long orderId) {
    post(identityClient, "/internal/messages",
        new MessageCommand(userId, "order", title, content, badge, orderId, "order", orderId),
        "trade:order:" + orderId + ":message:" + title.hashCode(), "订单消息发送失败");
  }

  @Override
  public void remindMerchant(long storeId, long orderId, String orderNo, String remark) {
    post(platformClient, "/internal/audit-logs",
        Map.of("actorType", "system", "actorId", 0, "actionType", "order_reminder",
            "targetType", "order", "targetId", orderId,
            "detail", "storeId=" + storeId + ", orderNo=" + orderNo + ", remark=" + (remark == null ? "" : remark)),
        "trade:order:" + orderId + ":merchant-reminder", "商家催单记录失败");
  }

  private void growth(long userId, long orderId, String sourceType, int delta, String reason) {
    post(identityClient, "/internal/members/{userId}/growth",
        Map.of("sourceType", sourceType, "sourceId", orderId, "delta", delta, "reason", reason),
        "trade:order:" + orderId + ":growth:" + sourceType, "成长值服务暂不可用", userId);
  }

  private Map<String, Object> inventoryBody(long orderId, List<InventoryLine> items) {
    List<Map<String, Object>> lines = items.stream()
        .map(item -> Map.<String, Object>of("skuId", item.skuId(), "itemId", item.itemId(), "quantity", item.quantity()))
        .toList();
    return Map.of("orderId", orderId, "items", lines);
  }

  private RestClient client(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeoutMs);
    requestFactory.setReadTimeout(readTimeoutMs);
    return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  private JsonNode getOptional(RestClient client, String uri, Object... variables) {
    try {
      return get(client, uri, "依赖服务暂不可用", variables);
    } catch (BusinessException exception) {
      if (exception.getErrorCode() == ErrorCode.NOT_FOUND) return null;
      throw exception;
    }
  }

  private JsonNode get(RestClient client, String uri, String failureMessage, Object... variables) {
    try {
      JsonNode root = client.get().uri(uri, variables).headers(this::internalHeaders).retrieve().body(JsonNode.class);
      return unwrap(root, failureMessage);
    } catch (HttpClientErrorException.NotFound exception) {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    } catch (HttpClientErrorException exception) {
      throw remoteFailure(exception, failureMessage);
    } catch (RestClientException exception) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, failureMessage);
    }
  }

  private JsonNode post(RestClient client, String uri, Object body, String idempotencyKey,
      String failureMessage, Object... variables) {
    try {
      JsonNode root = client.post().uri(uri, variables).headers(headers -> {
        internalHeaders(headers);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) headers.set("Idempotency-Key", idempotencyKey);
      }).body(body).retrieve().body(JsonNode.class);
      return unwrap(root, failureMessage);
    } catch (HttpClientErrorException exception) {
      throw remoteFailure(exception, failureMessage);
    } catch (RestClientException exception) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, failureMessage);
    }
  }

  private void internalHeaders(HttpHeaders headers) {
    headers.set("X-Caller-Service", CALLER);
    headers.set("X-Request-Id", RequestIds.current());
    headers.set("X-Service-Token", serviceToken);
  }

  private JsonNode unwrap(JsonNode root, String failureMessage) {
    if (root == null || root.path("code").asInt(-1) != 0) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
          root == null ? failureMessage : root.path("message").asText(failureMessage));
    }
    return root.path("data");
  }

  private BusinessException remoteFailure(HttpClientErrorException exception, String fallback) {
    try {
      JsonNode root = objectMapper.readTree(exception.getResponseBodyAsString());
      String message = root.path("message").asText(fallback);
      ErrorCode code = exception.getStatusCode().value() == 404 ? ErrorCode.NOT_FOUND : ErrorCode.BUSINESS_RULE_VIOLATION;
      return new BusinessException(code, message);
    } catch (Exception ignored) {
      return new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, fallback);
    }
  }

  private static long longValue(JsonNode node, String field, long fallback) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? fallback : value.asLong(fallback);
  }

  private static Long nullableLong(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? null : value.asLong();
  }

  private static String text(JsonNode node, String field, String fallback) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
  }

  private static BigDecimal decimal(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? BigDecimal.ZERO : value.decimalValue();
  }

  private static BigDecimal nullableDecimal(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? null : value.decimalValue();
  }

  private record MessageCommand(
      long userId, String type, String title, String content, String badgeText,
      Long relatedOrderId, String relatedTargetType, Long relatedTargetId) {}
}
