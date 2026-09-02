package com.aituan.merchantcatalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class InternalServiceClientContractTest {

  @Test
  void deserializesTypedRecordAndGenericListFromApiResponseData() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    InternalServiceClient client = new InternalServiceClient(
        restTemplate, new ObjectMapper(), "http://identity", "http://trade", "http://platform", "contract-token");

    server.expect(requestTo("http://trade/internal/metrics/stores/7/orders"))
        .andExpect(header("X-Caller-Service", "merchant-catalog-service"))
        .andExpect(header("X-Service-Token", "contract-token"))
        .andRespond(withSuccess(
            "{\"code\":0,\"message\":\"success\",\"data\":{\"orderCount\":12,\"amount\":88.50,\"pendingCount\":3}}",
            MediaType.APPLICATION_JSON));
    server.expect(requestTo("http://identity/internal/users/9/preference-signals"))
        .andRespond(withSuccess(
            "{\"code\":0,\"message\":\"success\",\"data\":[{\"businessType\":\"takeaway\",\"categoryId\":5,\"storeId\":7,\"itemId\":11,\"weight\":2,\"source\":\"favorite\"}]}",
            MediaType.APPLICATION_JSON));

    StoreOrderMetricsView metrics = client.orderMetrics(7);
    assertThat(metrics.orderCount()).isEqualTo(12);
    assertThat(metrics.amount()).isEqualByComparingTo(new BigDecimal("88.50"));
    List<PreferenceSignalView> signals = client.preferenceSignals(9L);
    assertThat(signals).hasSize(1);
    assertThat(signals.get(0).itemId()).isEqualTo(11L);
    server.verify();
  }
}
