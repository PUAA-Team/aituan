package com.aituan.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRouteTest {

  @Autowired WebTestClient webTestClient;
  @Autowired RouteDefinitionLocator routeDefinitionLocator;

  @Test
  void internalPathsAreDeniedWith403() {
    webTestClient.get().uri("/internal/stores/1/snapshot").exchange().expectStatus().isForbidden();
  }

  @Test
  void merchantCatalogRoutesAreRegistered() {
    List<String> routeIds = routeDefinitionLocator.getRouteDefinitions()
        .map(route -> route.getId())
        .collectList()
        .block();
    assertThat(routeIds).isNotNull();
    assertThat(routeIds)
        .contains("merchant-open", "merchant-discovery", "merchant-location", "merchant-profile",
            "merchant-stores", "merchant-catalog", "merchant-trade-stores",
            "admin-merchants", "admin-stores", "admin-catalog", "admin-trade-stores");
  }

  @Test
  void denyInternalRouteSetsForbiddenStatus() {
    org.springframework.cloud.gateway.route.RouteDefinition denyRoute = routeDefinitionLocator.getRouteDefinitions()
        .filter(route -> "deny-internal-api".equals(route.getId()))
        .blockFirst();
    assertThat(denyRoute).isNotNull();
    assertThat(denyRoute.getFilters()).anyMatch(filter -> "SetStatus".equals(filter.getName()));
  }
}
