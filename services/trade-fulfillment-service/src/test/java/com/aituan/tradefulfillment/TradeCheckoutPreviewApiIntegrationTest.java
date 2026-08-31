package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TradeCheckoutPreviewApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Test
  void userCanPreviewCheckoutWithStubbedCrossServiceData() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/checkout/preview"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {
                  "storeId":1,
                  "businessType":"takeaway",
                  "addressId":1,
                  "items":[{"itemId":1002,"quantity":2}],
                  "remark":"少放辣",
                  "tablewareOption":"merchant_decide"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.storeId").value(1))
        .andExpect(jsonPath("$.data.storeName").value("爱团炸鸡中关村店"))
        .andExpect(jsonPath("$.data.amount").value(39.80))
        .andExpect(jsonPath("$.data.deliveryFee").value(4.00))
        .andExpect(jsonPath("$.data.packageFee").value(0))
        .andExpect(jsonPath("$.data.payableAmount").value(43.80))
        .andExpect(jsonPath("$.data.startPrice").value(20.00))
        .andExpect(jsonPath("$.data.minimumOrderMet").value(true))
        .andExpect(jsonPath("$.data.deliverable").value(true))
        .andExpect(jsonPath("$.data.estimatedDeliveryMinutes", greaterThan(0)))
        .andExpect(jsonPath("$.data.items[0].itemId").value(1002))
        .andExpect(jsonPath("$.data.items[0].quantity").value(2));
  }

  @Test
  void reportsMinimumOrderMissingInPreview() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/checkout/preview"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {
                  "storeId":1,
                  "businessType":"takeaway",
                  "addressId":1,
                  "items":[{"itemId":1002,"quantity":1}]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.amount").value(19.90))
        .andExpect(jsonPath("$.data.startPriceMissing").value(0.10))
        .andExpect(jsonPath("$.data.minimumOrderMet").value(false));
  }
}
