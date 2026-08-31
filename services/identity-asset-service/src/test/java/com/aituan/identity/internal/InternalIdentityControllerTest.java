package com.aituan.identity.internal;

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
  @Autowired private MockMvc mockMvc;

  @Test
  void internalWriteApiShouldRequireIdempotencyKey() throws Exception {
    mockMvc.perform(post("/internal/members/5001/growth")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "identity-test")
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
            .header("X-Caller-Service", "identity-test")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("changed"));

    mockMvc.perform(post("/internal/members/5001/growth")
            .header("Idempotency-Key", "identity-test:growth:10002:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "identity-test")
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
            .header("X-Caller-Service", "identity-test")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("used"));

    mockMvc.perform(post("/internal/coupons/8001/use")
            .header("Idempotency-Key", "identity-test:coupon-use:99001:v1")
            .header("X-Request-Id", "test-request")
            .header("X-Caller-Service", "identity-test")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.status").value("used"));
  }
}
