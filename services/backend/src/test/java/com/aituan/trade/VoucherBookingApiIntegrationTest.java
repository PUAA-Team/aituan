package com.aituan.trade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class VoucherBookingApiIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtTokenService jwtTokenService;

  @Test
  void adminCanListVoucherAndBookingWorkbenches() throws Exception {
    String adminToken = ApiTestSupport.adminToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/admin/trade/vouchers")
                .param("status", "unused")
                .param("keyword", "88009006"),
            adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list[0].voucherCode").value("88009006"))
        .andExpect(jsonPath("$.data.list[0].orderId").value(9006));

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/admin/trade/bookings")
                .param("status", "pending")
                .param("businessType", "hotel"),
            adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.list[0].booking.orderId").value(9006))
        .andExpect(jsonPath("$.data.list[0].booking.storeConfirmStatus").value("pending"));
  }

  @Test
  @Transactional
  void adminCanLookupRedeemVoucherAndRejectDuplicateRedeem() throws Exception {
    String adminToken = ApiTestSupport.adminToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(
            get("/api/admin/trade/vouchers/88009008"),
            adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.voucherCode").value("88009008"))
        .andExpect(jsonPath("$.data.status").value("unused"));

    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/admin/trade/vouchers/88009008/redeem"),
            adminToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.voucher.voucherCode").value("88009008"))
        .andExpect(jsonPath("$.data.voucher.status").value("used"));

    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/admin/trade/vouchers/88009008/redeem"),
            adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(5002));
  }

  @Test
  @Transactional
  void adminCanConfirmPendingBooking() throws Exception {
    mockMvc.perform(ApiTestSupport.bearer(
            post("/api/admin/trade/orders/9006/booking/confirm"),
            ApiTestSupport.adminToken(jwtTokenService))
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"remark":"已电话确认入住时间"}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.orderId").value(9006))
        .andExpect(jsonPath("$.data.storeConfirmStatus").value("confirmed"))
        .andExpect(jsonPath("$.data.storeConfirmRemark").value("已电话确认入住时间"));
  }
}
