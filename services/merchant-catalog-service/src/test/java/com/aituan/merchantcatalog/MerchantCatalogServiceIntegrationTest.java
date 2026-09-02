package com.aituan.merchantcatalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.common.enums.AccountType;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.JwtTokenService;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "management.endpoint.health.probes.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantCatalogServiceIntegrationTest {
  @Autowired MockMvc mockMvc;
  @Autowired JwtTokenService jwtTokenService;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void livenessProbeShouldBePublic() throws Exception {
    mockMvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void discoveryHomeUsesMerchantDatabaseOnlyAndReturnsRecommendations() throws Exception {
    mockMvc.perform(get("/api/app/discovery/home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.modules").isArray())
        .andExpect(jsonPath("$.data.recommendations.list").isArray());
  }

  @Test
  void internalStoreSnapshotAndCheckoutQuoteWork() throws Exception {
    mockMvc.perform(get("/internal/stores/1/snapshot")
            .headers(internalHeaders()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.storeId").value(1))
        .andExpect(jsonPath("$.data.storeName").value("塔斯汀中国汉堡"));

    mockMvc.perform(post("/internal/catalog/checkout-quote")
            .headers(internalHeaders())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"storeId\":1,\"items\":[{\"itemId\":1002,\"quantity\":2}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].itemId").value(1002))
        .andExpect(jsonPath("$.data.items[0].skuId").value(2));
  }

  @Test
  void inventoryDeductRequiresServiceHeadersAndReusesIdempotencyKey() throws Exception {
    String body = "{\"orderId\":10001,\"items\":[{\"skuId\":2,\"itemId\":1002,\"quantity\":1}]}";

    mockMvc.perform(post("/internal/inventory/deduct")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(2002));

    mockMvc.perform(post("/internal/inventory/deduct")
            .headers(internalHeaders())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1000));

    mockMvc.perform(post("/internal/inventory/deduct")
            .headers(internalHeaders())
            .header("Idempotency-Key", "trade-fulfillment-service:inventory-deduct:order-10001:v1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("deducted"));

    mockMvc.perform(post("/internal/inventory/deduct")
            .headers(internalHeaders())
            .header("Idempotency-Key", "trade-fulfillment-service:inventory-deduct:order-10001:v1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("deducted"));

    Integer rows = jdbcTemplate.queryForObject(
        "select count(1) from inventory_idempotency_record where caller_service = ? and api_action = 'deduct' and idempotency_key = ?",
        Integer.class,
        "trade-fulfillment-service",
        "trade-fulfillment-service:inventory-deduct:order-10001:v1");
    assertThat(rows).isEqualTo(1);
  }

  @Test
  void merchantCatalogRequiresMerchantToken() throws Exception {
    String token = jwtTokenService.createToken(new CurrentUser(2L, 0L, AccountType.MERCHANT, "demo_merchant"));
    mockMvc.perform(get("/api/merchant/catalog/items").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void merchantMetricsAreAvailableForPlatformAggregation() throws Exception {
    mockMvc.perform(get("/internal/metrics/platform/merchants")
            .headers(internalHeaders()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.merchantCount").value(14))
        .andExpect(jsonPath("$.data.onSaleItemCount").isNumber());
  }

  @Test
  void merchantDashboardAggregatesRemoteMetricsWithFallbacks() throws Exception {
    String token = jwtTokenService.createToken(new CurrentUser(2L, 0L, AccountType.MERCHANT, "demo_merchant"));
    mockMvc.perform(get("/api/merchant/ops/dashboard").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.todayOrders").isNumber())
        .andExpect(jsonPath("$.data.weeklyOrders").isArray());
  }

  private HttpHeaders internalHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Caller-Service", "trade-fulfillment-service");
    headers.set("X-Request-Id", "test-request-id");
    headers.set("X-Service-Token", "dev-internal-token");
    return headers;
  }
}
