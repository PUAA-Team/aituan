package com.aituan.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class GatewaySecurityFilterTest {
  @Test
  void internalPathsAreNeverForwarded() {
    InternalPathBlockFilter filter = new InternalPathBlockFilter();
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/internal/orders/1").build());
    AtomicBoolean forwarded = new AtomicBoolean();

    filter.filter(exchange, ignored -> {
      forwarded.set(true);
      return Mono.empty();
    }).block();

    assertThat(forwarded).isFalse();
    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    String body = DataBufferUtils.join(exchange.getResponse().getBody())
        .map(buffer -> StandardCharsets.UTF_8.decode(buffer.asByteBuffer()).toString())
        .block();
    assertThat(body).contains("资源不存在");
  }

  @Test
  void requestIdIsGeneratedAndForwarded() {
    GatewayRequestIdFilter filter = new GatewayRequestIdFilter();
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/app/discovery/home").build());
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

    filter.filter(exchange, filtered -> {
      forwarded.set(filtered);
      return Mono.empty();
    }).block();

    String requestId = forwarded.get().getRequest().getHeaders().getFirst("X-Request-Id");
    assertThat(requestId).startsWith("req_");
    assertThat(forwarded.get().getResponse().getHeaders().getFirst("X-Request-Id"))
        .isEqualTo(requestId);
  }

  @Test
  void incomingRequestIdIsPreserved() {
    GatewayRequestIdFilter filter = new GatewayRequestIdFilter();
    MockServerWebExchange exchange = MockServerWebExchange.from(
        MockServerHttpRequest.get("/api/app/discovery/home")
            .header("X-Request-Id", "request-from-client")
            .build());
    AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

    filter.filter(exchange, filtered -> {
      forwarded.set(filtered);
      return Mono.empty();
    }).block();

    assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-Request-Id"))
        .isEqualTo("request-from-client");
  }
}
