package com.aituan.catalog;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
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
class MerchantCatalogApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void merchantCanListCatalogAndDeliveryRule() throws Exception {
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/catalog/items")
                .param("businessType", "takeaway")
                .param("status", "on_sale"),
            merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data[0].storeId").value(1))
        .andExpect(jsonPath("$.data[0].businessType").value("takeaway"));

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/catalog/categories").param("businessType", "takeaway"),
            merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data[0].businessType").value("takeaway"));

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/trade/stores/1/delivery-rule"),
            merchantToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.storeId").value(1))
        .andExpect(jsonPath("$.data.deliveryFee").exists());
  }

  @Test
  @Transactional
  void merchantCanCreateItemAndToggleStatus() throws Exception {
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);

    MvcResult created = mockMvc.perform(ApiTestSupport.bearer(
            post("/api/merchant/catalog/items"),
            merchantToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {
                  "businessType":"takeaway",
                  "categoryId":101,
                  "title":"接口测试汉堡",
                  "subtitle":"用于目录维护 API 测试",
                  "price":19.90,
                  "originalPrice":24.00,
                  "stock":30,
                  "status":"on_sale",
                  "coverUrl":"/uploads/test-burger.png",
                  "tagText":"测试"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id", greaterThan(0)))
        .andExpect(jsonPath("$.data.storeId").value(1))
        .andExpect(jsonPath("$.data.title").value("接口测试汉堡"))
        .andReturn();

    long itemId = dataId(created);
    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/merchant/catalog/items/{itemId}/status", itemId),
            merchantToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"status":"off_sale"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.id").value(itemId))
        .andExpect(jsonPath("$.data.status").value("off_sale"));
  }

  @Test
  @Transactional
  void merchantCanUpdateDeliveryRuleButUserCannotMaintainCatalog() throws Exception {
    String merchantToken = ApiTestSupport.merchantToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/merchant/trade/stores/1/delivery-rule"),
            merchantToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {
                  "deliveryFee":4.00,
                  "startPrice":20.00,
                  "estimatedMinutes":35,
                  "maxDeliveryDistanceKm":5.00,
                  "packageFeeMode":"fixed",
                  "packageFeeFixed":1.00,
                  "packageFeePerItem":0.00,
                  "distanceExtraThresholdKm":3.00,
                  "distanceExtraFee":2.00,
                  "distanceExtraStepKm":1.00,
                  "deliveryText":"35分钟送达"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.storeId").value(1))
        .andExpect(jsonPath("$.data.estimatedMinutes").value(35))
        .andExpect(jsonPath("$.data.deliveryText").value("35分钟送达"));

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/merchant/catalog/items"),
            ApiTestSupport.userToken(jwtTokenService)))
        .andExpect(ApiTestSupport.forbidden());
  }

  private long dataId(MvcResult result) throws Exception {
    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    return root.path("data").path("id").asLong();
  }
}
