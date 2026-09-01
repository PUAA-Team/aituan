package com.aituan.engagementplatform.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

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
    var request=post("/internal/audit-logs").contentType(MediaType.APPLICATION_JSON).content(body)
        .header("X-Caller-Service","trade-fulfillment-service").header("X-Internal-Token","aituan-internal-demo-token")
        .header("Idempotency-Key","contract-audit-99");
    mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.duplicate").value(false));
    mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.data.duplicate").value(true));
    Long count=jdbc.queryForObject("select count(*) from sys_audit_log where caller_service=? and idempotency_key=?",Long.class,"trade-fulfillment-service","contract-audit-99");
    assertThat(count).isEqualTo(1);
  }

  @Test void invalidInternalTokenIsRejected() throws Exception {
    mvc.perform(get("/internal/metrics/platform/governance").header("X-Caller-Service","trade-fulfillment-service").header("X-Internal-Token","wrong"))
        .andExpect(status().isForbidden());
  }
}
