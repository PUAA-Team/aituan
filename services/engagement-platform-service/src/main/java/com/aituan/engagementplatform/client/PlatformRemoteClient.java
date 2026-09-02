package com.aituan.engagementplatform.client;

import com.aituan.common.api.RequestIds;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;

@Component
public class PlatformRemoteClient {
  private static final String CALLER = "engagement-platform-service";

  private final RestClient identityClient;
  private final RestClient merchantClient;
  private final RestClient tradeClient;
  private final String serviceToken;

  PlatformRemoteClient(
      @Value("${aituan.clients.identity-base-url}") String identityBaseUrl,
      @Value("${aituan.clients.merchant-base-url}") String merchantBaseUrl,
      @Value("${aituan.clients.trade-base-url}") String tradeBaseUrl,
      @Value("${aituan.clients.connect-timeout-ms}") int connectTimeoutMs,
      @Value("${aituan.clients.read-timeout-ms}") int readTimeoutMs,
      @Value("${aituan.internal.service-token}") String serviceToken) {
    this.serviceToken = serviceToken;
    this.identityClient = client(identityBaseUrl, connectTimeoutMs, readTimeoutMs);
    this.merchantClient = client(merchantBaseUrl, connectTimeoutMs, readTimeoutMs);
    this.tradeClient = client(tradeBaseUrl, connectTimeoutMs, readTimeoutMs);
  }

  public UserSnapshot userSummary(long userId) {
    JsonNode data = get(identityClient, "/internal/users/{userId}/summary", "用户资料服务暂不可用", userId);
    return new UserSnapshot(longValue(data, "userId", userId), text(data, "nickname", text(data, "displayName", "用户")));
  }

  public StoreSnapshot storeSnapshot(long storeId) {
    JsonNode data = get(merchantClient, "/internal/stores/{storeId}/snapshot", "门店信息暂不可用", storeId);
    return new StoreSnapshot(
        longValue(data, "storeId", longValue(data, "id", storeId)),
        longValue(data, "merchantId", 0),
        text(data, "storeName", "门店"));
  }

  public ItemSnapshot itemSnapshot(long itemId) {
    JsonNode data = get(merchantClient, "/internal/catalog/items/{itemId}/snapshot", "商品信息暂不可用", itemId);
    return new ItemSnapshot(
        longValue(data, "itemId", longValue(data, "id", itemId)),
        longValue(data, "storeId", 0),
        text(data, "itemName", text(data, "title", "商品")));
  }

  public long merchantIdByAccount(long accountId) {
    JsonNode data = get(merchantClient, "/internal/merchants/by-account/{accountId}", "商家信息暂不可用", accountId);
    long merchantId = longValue(data, "merchantId", longValue(data, "id", 0));
    if (merchantId <= 0) throw new BusinessException(ErrorCode.NOT_FOUND, "商家资料不存在");
    return merchantId;
  }

  public OrderSnapshot orderSnapshot(long orderId) {
    JsonNode data = get(tradeClient, "/internal/orders/{orderId}/snapshot", "订单信息暂不可用", orderId);
    return new OrderSnapshot(
        longValue(data, "orderId", longValue(data, "id", orderId)),
        text(data, "orderNo", ""),
        text(data, "title", text(data, "orderTitle", "订单")),
        longValue(data, "userId", 0),
        longValue(data, "storeId", 0),
        longValue(data, "merchantId", 0),
        text(data, "storeName", "门店"),
        text(data, "displayStatus", ""),
        text(data, "paymentStatus", ""),
        text(data, "fulfillmentStatus", ""),
        text(data, "orderType", ""));
  }

  public ReviewEligibility reviewEligibility(long orderId) {
    JsonNode data = get(tradeClient, "/internal/orders/{orderId}/review-eligibility", "评价资格暂时无法校验", orderId);
    boolean eligible = booleanValue(data, "eligible", booleanValue(data, "reviewable", false));
    return new ReviewEligibility(eligible, text(data, "reason", null), orderSnapshotFrom(data, orderId));
  }

  public void markOrderReviewed(long orderId, long reviewId) {
    post(tradeClient, "/internal/orders/{orderId}/reviewed", Map.of("reviewId", reviewId),
        "review-" + reviewId, "订单评价标记暂时失败", orderId);
  }

  public void addReviewGrowth(long userId, long reviewId) {
    post(identityClient, "/internal/members/{userId}/growth",
        Map.of("sourceType", "review", "sourceId", reviewId, "delta", 5, "reason", "发布评价"),
        "review-growth-" + reviewId, "评价成长值暂时无法入账", userId);
  }

  public void publishMessage(MessageCommand command, String idempotencyKey) {
    post(identityClient, "/internal/messages", command, idempotencyKey, "站内消息暂时无法发送");
  }

  public JsonNode identityMetrics() {
    return get(identityClient, "/internal/metrics/platform/users", "用户指标暂不可用");
  }

  public JsonNode merchantMetrics() {
    return get(merchantClient, "/internal/metrics/platform/merchants", "商家指标暂不可用");
  }

  public JsonNode orderMetrics() {
    return get(tradeClient, "/internal/metrics/platform/orders", "订单指标暂不可用");
  }

  private RestClient client(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeoutMs);
    requestFactory.setReadTimeout(readTimeoutMs);
    return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  private JsonNode get(RestClient client, String uri, String failureMessage, Object... variables) {
    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        JsonNode root = client.get().uri(uri, variables)
            .headers(this::internalHeaders)
            .retrieve().body(JsonNode.class);
        return unwrap(root, failureMessage);
      } catch (HttpClientErrorException.NotFound exception) {
        throw new BusinessException(ErrorCode.NOT_FOUND);
      } catch (HttpServerErrorException | ResourceAccessException exception) {
        if (attempt == 1) {
          throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, failureMessage);
        }
      } catch (RestClientException exception) {
        throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, failureMessage);
      }
    }
    throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, failureMessage);
  }

  private void post(RestClient client, String uri, Object body, String idempotencyKey, String failureMessage, Object... variables) {
    try {
      JsonNode root = client.post().uri(uri, variables)
          .headers(headers -> {
            internalHeaders(headers);
            headers.set("Idempotency-Key", idempotencyKey);
          })
          .body(body)
          .retrieve().body(JsonNode.class);
      unwrap(root, failureMessage);
    } catch (RestClientException exception) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, failureMessage);
    }
  }

  private void internalHeaders(org.springframework.http.HttpHeaders headers) {
    headers.set("X-Caller-Service", CALLER);
    headers.set("X-Request-Id", RequestIds.current());
    headers.set("X-Service-Token", serviceToken);
  }

  private JsonNode unwrap(JsonNode root, String failureMessage) {
    if (root == null || root.path("code").asInt(-1) != 0) {
      String message = root == null ? failureMessage : root.path("message").asText(failureMessage);
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
    return root.path("data");
  }

  private OrderSnapshot orderSnapshotFrom(JsonNode data, long orderId) {
    JsonNode source = data.has("order") ? data.path("order") : data;
    return new OrderSnapshot(
        longValue(source, "orderId", longValue(source, "id", orderId)),
        text(source, "orderNo", ""), text(source, "title", text(source, "orderTitle", "订单")),
        longValue(source, "userId", 0), longValue(source, "storeId", 0), longValue(source, "merchantId", 0),
        text(source, "storeName", "门店"), text(source, "displayStatus", ""),
        text(source, "paymentStatus", ""), text(source, "fulfillmentStatus", ""), text(source, "orderType", ""));
  }

  public static long longValue(JsonNode node, String field, long fallback) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? fallback : value.asLong(fallback);
  }

  public static String text(JsonNode node, String field, String fallback) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() || value.asText().isBlank() ? fallback : value.asText();
  }

  private boolean booleanValue(JsonNode node, String field, boolean fallback) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null || value.isNull() ? fallback : value.asBoolean(fallback);
  }

  public record UserSnapshot(long userId, String nickname) {}
  public record StoreSnapshot(long storeId, long merchantId, String storeName) {}
  public record ItemSnapshot(long itemId, long storeId, String itemName) {}
  public record OrderSnapshot(
      long orderId, String orderNo, String title, long userId, long storeId, long merchantId,
      String storeName, String displayStatus, String paymentStatus, String fulfillmentStatus, String orderType) {}
  public record ReviewEligibility(boolean eligible, String reason, OrderSnapshot order) {}
  public record MessageCommand(
      long userId, String type, String title, String content, String badgeText,
      Long relatedOrderId, String relatedTargetType, Long relatedTargetId) {}
}
