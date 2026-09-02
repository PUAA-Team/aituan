package com.aituan.identity.client;

import com.aituan.common.api.RequestIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class IdentityAuditClient {
  private static final Logger log = LoggerFactory.getLogger(IdentityAuditClient.class);

  private final RestClient restClient;
  private final boolean enabled;
  private final String serviceToken;

  public IdentityAuditClient(
      RestClient.Builder builder,
      @Value("${aituan.internal.platform-base-url:http://engagement-platform-service:8084}") String platformBaseUrl,
      @Value("${aituan.internal.service-token:}") String serviceToken,
      @Value("${aituan.internal.audit.enabled:false}") boolean enabled) {
    this.restClient = builder.baseUrl(platformBaseUrl).build();
    this.serviceToken = serviceToken;
    this.enabled = enabled;
  }

  public void publish(Long actorId, String actionType, String targetType, Long targetId, String detail) {
    if (!enabled) {
      return;
    }
    try {
      restClient.post()
          .uri("/internal/audit-logs")
          .header("X-Request-Id", RequestIds.current())
          .header("X-Caller-Service", "identity-asset-service")
          .header("X-Service-Token", serviceToken)
          .header("Idempotency-Key", idempotencyKey(actionType, targetType, targetId))
          .body(new AuditLogRequest("admin", actorId, actionType, targetType, targetId, detail))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException exception) {
      log.warn("Failed to publish identity audit log actionType={} targetType={} targetId={}", actionType, targetType, targetId, exception);
    }
  }

  private String idempotencyKey(String actionType, String targetType, Long targetId) {
    return "identity-asset-service:audit:" + actionType + ":" + targetType + ":" + targetId;
  }

  record AuditLogRequest(String actorType, Long actorId, String actionType, String targetType, Long targetId, String detail) {}
}
