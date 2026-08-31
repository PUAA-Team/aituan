package com.aituan.trade;

import static org.hamcrest.Matchers.greaterThan;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeLifecycleApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @Transactional
  void userCanPreviewCreatePayAndRefundTakeawayOrder() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);
    long addressId = insertAddressWithLocation();
    String orderBody = """
        {
          "storeId":1,
          "businessType":"takeaway",
          "addressId":%d,
          "items":[{"itemId":1002,"quantity":2}],
          "remark":"少放辣",
          "idempotencyKey":"api-test-takeaway-lifecycle"
        }
        """.formatted(addressId);

    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/app/trade/checkout/preview"),
            userToken)
            .contentType(ApiTestSupport.JSON)
            .content(orderBody))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.storeId").value(1))
        .andExpect(jsonPath("$.data.items[0].itemId").value(1002))
        .andExpect(jsonPath("$.data.payableAmount", greaterThan(0.0)))
        .andExpect(jsonPath("$.data.minimumOrderMet").isBoolean());

    MvcResult created = mockMvc.perform(ApiTestSupport.bearer(
            post("/api/app/trade/orders"),
            userToken)
            .contentType(ApiTestSupport.JSON)
            .content(orderBody))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id", greaterThan(0)))
        .andExpect(jsonPath("$.data.paymentStatus").value("unpaid"))
        .andReturn();

    long orderId = dataId(created);
    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/app/trade/orders/{orderId}/pay", orderId),
            userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"paymentMode":"mock"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.paymentStatus").value("paid"))
        .andExpect(jsonPath("$.data.fulfillmentStatus").value("merchant_pending"));

    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/app/trade/orders/{orderId}/refund", orderId),
            userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"reason":"用户取消测试订单"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(orderId))
        .andExpect(jsonPath("$.data.displayStatus").value("refunded"))
        .andExpect(jsonPath("$.data.paymentStatus").value("refunded"))
        .andExpect(jsonPath("$.data.refundStatus").value("succeeded"))
        .andExpect(jsonPath("$.data.refundReason").value("用户取消测试订单"));
  }

  private long dataId(MvcResult result) throws Exception {
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    return root.path("data").path("id").asLong();
  }

  private long insertAddressWithLocation() {
    jdbcTemplate.update(
        """
        insert into user_address(user_id, contact_name, contact_phone, province, city, district,
                                 detail_address, longitude, latitude, tag_name, is_default, delivery_note)
        values (1, '李同学', '18800001111', '北京市', '北京市', '海淀区',
                '城市广场测试地址 1 号', 116.313600, 39.982300, '测试', 0, '测试地址')
        """);
    return jdbcTemplate.queryForObject(
        "select id from user_address where detail_address = ? order by id desc limit 1",
        Long.class,
        "城市广场测试地址 1 号");
  }
}
