package com.aituan.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

@SpringBootTest
class IdentityRouteDefinitionTest {
  @Autowired private RouteDefinitionLocator routeDefinitionLocator;

  @Test
  void identityAdminRouteShouldCoverBaseAndChildPaths() {
    RouteDefinition route = requireRoute("identity-admin-users");
    List<String> pathPatterns = pathPatterns(route);

    assertThat(pathPatterns).contains(
        "/api/admin/users",
        "/api/admin/users/**",
        "/api/admin/account/profile",
        "/api/admin/operation/member-levels",
        "/api/admin/operation/member-levels/**",
        "/api/admin/operation/coupon-templates",
        "/api/admin/operation/coupon-templates/**");
  }

  @Test
  void internalApiShouldRemainForbiddenAtGateway() {
    RouteDefinition route = requireRoute("forbid-internal-api");

    assertThat(pathPatterns(route)).contains("/internal/**");
    assertThat(route.getFilters()).anySatisfy(filter -> assertThat(filter.getName()).isEqualTo("SetStatus"));
  }

  private RouteDefinition requireRoute(String routeId) {
    return routeDefinitions().stream()
        .filter(route -> routeId.equals(route.getId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing gateway route: " + routeId));
  }

  private List<RouteDefinition> routeDefinitions() {
    List<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
        .collectList()
        .block(Duration.ofSeconds(5));
    assertThat(routes).isNotNull();
    return routes;
  }

  private List<String> pathPatterns(RouteDefinition route) {
    return route.getPredicates().stream()
        .filter(predicate -> "Path".equals(predicate.getName()))
        .flatMap(predicate -> predicate.getArgs().values().stream())
        .flatMap(value -> List.of(value.split(",")).stream())
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }
}
