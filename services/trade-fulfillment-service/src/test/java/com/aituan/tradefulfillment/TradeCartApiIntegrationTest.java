package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class TradeCartApiIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Test
  void userCanManageCart() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/payment-methods"), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data[0].code").value("mock"))
        .andExpect(jsonPath("$.data[0].enabled").value(true));

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/cart").param("storeId", "1"), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.storeId").value(1))
        .andExpect(jsonPath("$.data.amount").value(0))
        .andExpect(jsonPath("$.data.items", hasSize(0)));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/cart/items"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"itemId":1002,"quantity":2}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.items[0].itemId").value(1002))
        .andExpect(jsonPath("$.data.items[0].quantity").value(2))
        .andExpect(jsonPath("$.data.items[0].totalPrice").value(39.80));

    mockMvc.perform(ApiTestSupport.bearer(put("/api/app/trade/cart/items/{itemId}", 1002), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"quantity":1}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.items[0].quantity").value(1))
        .andExpect(jsonPath("$.data.items[0].soldOut").value(false));

    mockMvc.perform(ApiTestSupport.bearer(delete("/api/app/trade/cart/items/{itemId}", 1002).param("storeId", "1"), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.items", hasSize(0)));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/cart/items"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"itemId":1001,"quantity":1}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.items", hasSize(1)));

    mockMvc.perform(ApiTestSupport.bearer(delete("/api/app/trade/cart").param("storeId", "1"), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.items", hasSize(0)));
  }

  @Test
  void rejectsCartItemBeyondStock() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/cart/items"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"itemId":1002,"quantity":99}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code", is(5003)));
  }

  @Test
  void requiresLoginForAppTradeApi() throws Exception {
    mockMvc.perform(get("/api/app/trade/cart").param("storeId", "1"))
        .andExpect(ApiTestSupport.unauthorized());
  }
}
