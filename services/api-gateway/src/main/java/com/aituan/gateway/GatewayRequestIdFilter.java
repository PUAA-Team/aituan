package com.aituan.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
class GatewayRequestIdFilter implements GlobalFilter, Ordered {
  private static final String HEADER = "X-Request-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
    String requestId = StringUtils.hasText(incoming) ? incoming.trim() : "req_" + UUID.randomUUID().toString().replace("-", "");
    ServerWebExchange mutated = exchange.mutate()
        .request(request -> request.headers(headers -> headers.set(HEADER, requestId)))
        .build();
    mutated.getResponse().getHeaders().set(HEADER, requestId);
    mutated.getResponse().beforeCommit(() -> {
      mutated.getResponse().getHeaders().set(HEADER, requestId);
      return Mono.empty();
    });
    return chain.filter(mutated);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
