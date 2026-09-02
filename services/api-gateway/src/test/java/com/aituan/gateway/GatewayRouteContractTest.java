package com.aituan.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

@SpringBootTest
class GatewayRouteContractTest {
  private static final Pattern OPERATION = Pattern.compile(
      "^\\|\\s*\\d+\\s*\\|\\s*`([A-Z]+)`\\s*\\|\\s*`([^`]+)`\\s*\\|.*$");
  private static final Map<String, Integer> EXPECTED_COUNTS = Map.of(
      "identity-public", 47,
      "merchant-public", 65,
      "trade-public", 56,
      "engagement-public", 59);

  @Autowired private RouteLocator routeLocator;

  @Test
  void everyDocumentedExternalOperationRoutesToExactlyItsOwner() throws IOException {
    List<Operation> operations = readExternalOperations();
    assertThat(operations).hasSize(227);
    Map<String, Integer> actualCounts = new LinkedHashMap<>();
    operations.forEach(operation -> actualCounts.merge(operation.owner(), 1, Integer::sum));
    assertThat(actualCounts).containsExactlyInAnyOrderEntriesOf(EXPECTED_COUNTS);

    List<Route> publicRoutes = routeLocator.getRoutes()
        .filter(route -> route.getId().endsWith("-public"))
        .collectList()
        .block();
    assertThat(publicRoutes).extracting(Route::getId)
        .containsExactlyInAnyOrderElementsOf(EXPECTED_COUNTS.keySet());

    for (Operation operation : operations) {
      String concretePath = operation.path().replaceAll("\\{[^/}]+}", "1");
      MockServerWebExchange exchange = MockServerWebExchange.from(
          MockServerHttpRequest.method(HttpMethod.valueOf(operation.method()), concretePath).build());
      List<String> matches = publicRoutes.stream()
          .filter(route -> Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block()))
          .map(Route::getId)
          .toList();
      assertThat(matches)
          .as("%s %s must route only to %s", operation.method(), operation.path(), operation.owner())
          .containsExactly(operation.owner());
    }
  }

  private List<Operation> readExternalOperations() throws IOException {
    Path contract = findRepositoryRoot().resolve("docs/stage-new-3/微服务接口清单.md");
    List<Operation> operations = new ArrayList<>();
    boolean externalSection = false;
    String owner = null;
    for (String line : Files.readAllLines(contract)) {
      if (line.startsWith("## 2. 外部接口清单")) {
        externalSection = true;
        continue;
      }
      if (line.startsWith("## 3. 内部接口清单")) break;
      if (!externalSection) continue;
      if (line.startsWith("### 2.1 ")) owner = "identity-public";
      if (line.startsWith("### 2.2 ")) owner = "merchant-public";
      if (line.startsWith("### 2.3 ")) owner = "trade-public";
      if (line.startsWith("### 2.4 ")) owner = "engagement-public";
      Matcher matcher = OPERATION.matcher(line);
      if (matcher.matches()) {
        assertThat(owner).as("owner heading before %s", line).isNotNull();
        operations.add(new Operation(owner, matcher.group(1), matcher.group(2)));
      }
    }
    return operations;
  }

  private Path findRepositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null) {
      if (Files.isRegularFile(current.resolve("docs/stage-new-3/微服务接口清单.md"))) return current;
      current = current.getParent();
    }
    throw new IllegalStateException("cannot locate repository root from " + Path.of("").toAbsolutePath());
  }

  private record Operation(String owner, String method, String path) {}
}
