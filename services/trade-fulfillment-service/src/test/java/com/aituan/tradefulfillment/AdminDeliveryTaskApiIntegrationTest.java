package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminDeliveryTaskApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void adminCanManageDeliveryTask() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    String adminToken = ApiTestSupport.adminToken(jwtTokenService);
    createPaidTakeawayOrder(userToken, "admin-delivery-task");

    MvcResult list = mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/delivery/tasks"), adminToken)
            .param("stage", "merchant_pending"))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list[0].taskId", greaterThan(0)))
        .andExpect(jsonPath("$.data.list[0].currentStage").value("merchant_pending"))
        .andReturn();
    long taskId = dataAt(list, "data", "list", 0, "taskId");

    mockMvc.perform(ApiTestSupport.bearer(get("/api/admin/delivery/tasks/{taskId}", taskId), adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.currentStage").value("merchant_pending"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/delivery/tasks/{taskId}/pause", taskId), adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.autoAdvanceEnabled").value(false));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/delivery/tasks/{taskId}/resume", taskId), adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.autoAdvanceEnabled").value(true));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/delivery/tasks/{taskId}/advance", taskId), adminToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"remark":"后台推进"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.currentStage").value("accepted"));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/admin/delivery/tasks/{taskId}/abnormal", taskId), adminToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"reason":"骑手异常"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.currentStage").value("abnormal"))
        .andExpect(jsonPath("$.data.abnormalReason").value("骑手异常"));
  }

  private void createPaidTakeawayOrder(String userToken, String idempotencyKey) throws Exception {
    MvcResult created = mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"businessType":"takeaway","addressId":1,"items":[{"itemId":1002,"quantity":2}],"idempotencyKey":"%s"}
                """.formatted(idempotencyKey)))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andReturn();
    long orderId = dataAt(created, "data", "id");
    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/orders/{orderId}/pay", orderId), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"paymentMode":"mock"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse());
  }

  private long dataAt(MvcResult result, Object... path) throws Exception {
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    for (Object segment : path) {
      node = segment instanceof Integer index ? node.get(index) : node.path(segment.toString());
    }
    return node.asLong();
  }
}
