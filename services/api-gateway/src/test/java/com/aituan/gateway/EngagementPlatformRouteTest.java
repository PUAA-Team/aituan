package com.aituan.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EngagementPlatformRouteTest {
  @Autowired RouteDefinitionLocator routeDefinitionLocator;
  @LocalServerPort int port;

  @Test
  void exposesMemberDRoutesWithoutPublishingInternalEndpoints() {
    List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();
    RouteDefinition route = routes.stream()
        .filter(candidate -> candidate.getId().equals("engagement-platform-service"))
        .findFirst()
        .orElseThrow();
    String predicates = route.getPredicates().toString();

    assertThat(predicates)
        .contains("/api/app/interaction/**")
        .contains("/api/app/complaints/**")
        .contains("/api/app/support/**")
        .contains("/api/app/ai/**")
        .contains("/api/merchant/ops/**")
        .contains("/api/admin/governance/**")
        .contains("/api/common/files/**")
        .doesNotContain("/internal/**");
  }

  @Test
  void externalInternalPathIsRejectedByGateway() {
    WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + port).build()
        .get().uri("/internal/metrics/platform/governance")
        .exchange()
        .expectStatus().isNotFound();
  }
}
