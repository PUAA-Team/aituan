package com.aituan.identity.client;

import com.aituan.common.api.RequestIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TradeMetricsClient {
  private static final Logger log = LoggerFactory.getLogger(TradeMetricsClient.class);

  private final RestClient restClient;
  private final String serviceToken;

  public TradeMetricsClient(
      RestClient.Builder builder,
      @Value("${aituan.internal.trade-base-url:http://trade-fulfillment-service:8083}") String tradeBaseUrl,
      @Value("${aituan.internal.service-token:}") String serviceToken) {
    this.restClient = builder.baseUrl(tradeBaseUrl).build();
    this.serviceToken = serviceToken;
  }

  public long countUserOrders(long userId) {
    try {
      UserPurchaseSignalsResponse response = restClient.get()
          .uri("/internal/users/{userId}/purchase-signals", userId)
          .header("X-Request-Id", RequestIds.current())
          .header("X-Caller-Service", "identity-asset-service")
          .header("X-Service-Token", serviceToken)
          .retrieve()
          .body(UserPurchaseSignalsResponse.class);
      if (response == null || response.data() == null || response.data().orderCount() == null) {
        return 0;
      }
      return response.data().orderCount();
    } catch (RestClientException exception) {
      log.warn("Failed to load user order count from trade service userId={}", userId, exception);
      return 0;
    }
  }

  record UserPurchaseSignalsResponse(Integer code, String message, UserPurchaseSignals data) {}

  record UserPurchaseSignals(Long orderCount) {}
}
