package com.aituan.engagementplatform.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class PlatformInternalContractTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;

  @Test void healthIsPublic() throws Exception { mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP")); }

  @Test void auditWriteRequiresServiceIdentityAndIsIdempotent() throws Exception {
    String body="""
        {"actorType":"service","actorId":7,"actionType":"order_created","targetType":"order","targetId":99,"detail":"contract test"}
        """;
    var request=internal(post("/internal/audit-logs")).contentType(MediaType.APPLICATION_JSON).content(body)
        .header("Idempotency-Key","contract-audit-99");
    mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.duplicate").value(false));
    mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.duplicate").value(true));
    Long count=jdbc.queryForObject("select count(*) from sys_audit_log where caller_service=? and idempotency_key=?",Long.class,"trade-fulfillment-service","contract-audit-99");
    assertThat(count).isEqualTo(1);
  }

  @Test void invalidInternalTokenIsRejected() throws Exception {
    mvc.perform(get("/internal/metrics/platform/governance").header("X-Request-Id","req-invalid-token").header("X-Caller-Service","trade-fulfillment-service").header("X-Service-Token","wrong"))
        .andExpect(status().isForbidden());
  }

  @Test void requestIdIsMandatoryForInternalCalls() throws Exception {
    mvc.perform(get("/internal/metrics/platform/governance")
            .header("X-Caller-Service","merchant-catalog-service")
            .header("X-Service-Token","dev-internal-token"))
        .andExpect(status().isForbidden());
  }

  @Test void governanceMetricsExposeStableWireFields() throws Exception {
    mvc.perform(internal(get("/internal/metrics/platform/governance")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.reviews").isNumber())
        .andExpect(jsonPath("$.data.pendingReviews").isNumber())
        .andExpect(jsonPath("$.data.reportedReviews").isNumber())
        .andExpect(jsonPath("$.data.pendingComplaints").isNumber())
        .andExpect(jsonPath("$.data.openSessions").isNumber());
  }

  @Test void reviewSummaryAndEngagementMatchMerchantCatalogWireContract() throws Exception {
    long storeId=99101;
    jdbc.update("insert into review_record(order_id,order_no,order_title,store_id,merchant_id,store_name,user_id,rating,content,labels,replied) values(?,?,?,?,?,?,?,?,?,?,?)",991011,"R-991011","订单一",storeId,881,"测试门店",771,5,"很好","服务好,环境好",0);
    jdbc.update("insert into review_record(order_id,order_no,order_title,store_id,merchant_id,store_name,user_id,rating,content,labels,replied) values(?,?,?,?,?,?,?,?,?,?,?)",991012,"R-991012","订单二",storeId,881,"测试门店",772,3,"一般","服务好",0);
    jdbc.update("insert into support_session(session_no,user_id,store_id,merchant_id,store_name,topic,status) values(?,?,?,?,?,?,?)","SS-"+UUID.randomUUID(),771,storeId,881,"测试门店","咨询","open");

    mvc.perform(internal(get("/internal/reviews/stores/{storeId}/summary",storeId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.rating").value(4.0))
        .andExpect(jsonPath("$.data.count").value(2))
        .andExpect(jsonPath("$.data.highlights[0]").value("服务好"))
        .andExpect(jsonPath("$.data.highlights[1]").value("环境好"))
        .andExpect(jsonPath("$.data.storeId").doesNotExist())
        .andExpect(jsonPath("$.data.averageRating").doesNotExist());

    mvc.perform(internal(get("/internal/metrics/stores/{storeId}/engagement",storeId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.rating").value(4.0))
        .andExpect(jsonPath("$.data.reviewCount").value(2))
        .andExpect(jsonPath("$.data.pendingReplyCount").value(2))
        .andExpect(jsonPath("$.data.activeSessionCount").value(1))
        .andExpect(jsonPath("$.data.pendingReviews").doesNotExist());
  }

  private MockHttpServletRequestBuilder internal(MockHttpServletRequestBuilder request) {
    return request.header("X-Request-Id","req-contract-test")
        .header("X-Caller-Service","trade-fulfillment-service")
        .header("X-Service-Token","dev-internal-token");
  }
}
