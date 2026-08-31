package com.aituan.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.core.env.Environment;

@SpringBootTest(properties = "AITUAN_TRADE_URI=http://trade-fulfillment-service:8083")
class GatewayTradeRouteTest {

  private static final Set<String> TRADE_ROUTE_IDS = Set.of(
      "trade-app",
      "trade-merchant-orders",
      "trade-admin-orders",
      "trade-merchant-vouchers",
      "trade-admin-vouchers",
      "trade-merchant-bookings",
      "trade-admin-bookings",
      "trade-admin-delivery-tasks");

  @Autowired
  private RouteDefinitionLocator routeDefinitionLocator;

  @Autowired
  private Environment environment;

  @Test
  void routesAllTradeApisToTradeFulfillmentService() {
    Map<String, RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
        .collectList()
        .block(Duration.ofSeconds(5))
        .stream()
        .collect(Collectors.toMap(RouteDefinition::getId, Function.identity()));

    assertThat(routes.keySet()).containsAll(TRADE_ROUTE_IDS);
    for (String routeId : TRADE_ROUTE_IDS) {
      assertThat(routes.get(routeId).getUri().toString())
          .as("route %s should point to trade service", routeId)
          .isEqualTo("http://trade-fulfillment-service:8083");
    }
  }

  @Test
  void gatewayDoesNotKeepLegacyFallbackRoute() {
    Map<String, RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
        .collectList()
        .block(Duration.ofSeconds(5))
        .stream()
        .collect(Collectors.toMap(RouteDefinition::getId, Function.identity()));

    assertThat(routes.keySet()).doesNotContain("legacy-api-fallback");
    assertThat(environment.getProperty("aituan.gateway.service-uri.legacy")).isNull();
    assertThat(environment.getProperty("AITUAN_LEGACY_URI")).isNull();
  }
}
