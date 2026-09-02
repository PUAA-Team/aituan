package com.aituan.identity.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalIdentityControllerTest {
  private static final String SERVICE_TOKEN = "test-internal-token";

  @Autowired private MockMvc mockMvc;

  @Test
  void internalApiShouldRejectMissingServiceToken() throws Exception {
    mockMvc.perform(get("/internal/users/5001/summary")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "trade-fulfillment-service"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(2002));
  }

  @Test
  void internalApiShouldRejectWrongServiceToken() throws Exception {
    mockMvc.perform(get("/internal/users/5001/summary")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "trade-fulfillment-service")
            .header("X-Service-Token", "wrong-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(2002));
  }

  @Test
  void addressSnapshotShouldExposeCanonicalAndTradeCompatibleIds() throws Exception {
    mockMvc.perform(get("/internal/users/5001/addresses/7001/snapshot")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "trade-fulfillment-service")
            .header("X-Service-Token", SERVICE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").value(7001))
        .andExpect(jsonPath("$.data.addressId").value(7001))
        .andExpect(jsonPath("$.data.longitude").isNumber());
  }

  @Test
  void preferenceSignalsShouldMatchMerchantCallerContract() throws Exception {
    mockMvc.perform(get("/internal/users/5001/preference-signals")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "merchant-catalog-service")
            .header("X-Service-Token", SERVICE_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[0].weight").isNumber())
        .andExpect(jsonPath("$.data[0].source").value("favorite"))
        .andExpect(jsonPath("$.data[0].itemId").value(1002))
        .andExpect(jsonPath("$.data[1].storeId").value(1));
  }

  @Test
  void merchantProvisionShouldMatchMerchantCallerContract() throws Exception {
    mockMvc.perform(post("/internal/merchant-accounts/provision")
            .header("Idempotency-Key", "merchant-catalog-service:merchant-account-provision:contract-9001:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "merchant-catalog-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"loginName":"APP-CONTRACT-9001","merchantName":"契约商家","contactName":"测试联系人","contactPhone":"13800009001"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.success").value(true))
        .andExpect(jsonPath("$.data.accountId").isNumber())
        .andExpect(jsonPath("$.data.message").isNotEmpty());

    mockMvc.perform(post("/internal/merchant-accounts/provision")
            .header("Idempotency-Key", "merchant-catalog-service:merchant-account-provision:contract-9001:v1")
            .header("X-Request-Id", "test-request-retry")
            .header("X-Caller-Service", "merchant-catalog-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"loginName":"APP-CONTRACT-9001","merchantName":"契约商家","contactName":"测试联系人","contactPhone":"13800009001"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.success").value(true))
        .andExpect(jsonPath("$.data.message").value("商家账号已存在，按原结果返回"));
  }

  @Test
  void legacyCallerFieldAliasesShouldMapToCanonicalContract() throws Exception {
    mockMvc.perform(post("/internal/members/5001/growth")
            .header("Idempotency-Key", "engagement-platform-service:review-growth:alias-10003:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "engagement-platform-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"sourceType":"review","sourceId":10003,"growth":3,"reason":"字段兼容测试"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("changed"));

    mockMvc.perform(post("/internal/messages")
            .header("Idempotency-Key", "engagement-platform-service:message:alias-10003:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "engagement-platform-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"userId":5001,"messageType":"review","title":"评价通知","content":"字段兼容测试"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("created"));
  }

  @Test
  void internalWriteApiShouldRequireIdempotencyKey() throws Exception {
    mockMvc.perform(post("/internal/members/5001/growth")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "trade-fulfillment-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"sourceType":"test_growth","sourceId":10001,"delta":5,"reason":"测试成长值"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(1000));
  }

  @Test
  void growthCommandShouldBeIdempotentBySource() throws Exception {
    String body = """
        {"sourceType":"test_growth","sourceId":10002,"delta":5,"reason":"测试成长值"}
        """;
    mockMvc.perform(post("/internal/members/5001/growth")
            .header("Idempotency-Key", "identity-test:growth:10002:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "trade-fulfillment-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("changed"));

    mockMvc.perform(post("/internal/members/5001/growth")
            .header("Idempotency-Key", "identity-test:growth:10002:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "trade-fulfillment-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("unchanged"));
  }

  @Test
  void couponUseShouldReturnSameResultForSameOrder() throws Exception {
    String body = """
        {"userId":5001,"orderId":99001,"orderAmount":60.00}
        """;
    mockMvc.perform(post("/internal/coupons/8001/use")
            .header("Idempotency-Key", "identity-test:coupon-use:99001:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "trade-fulfillment-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("used"));

    mockMvc.perform(post("/internal/coupons/8001/use")
            .header("Idempotency-Key", "identity-test:coupon-use:99001:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "trade-fulfillment-service")
            .header("X-Service-Token", SERVICE_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("used"));
  }
}
