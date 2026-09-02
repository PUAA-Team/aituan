package com.aituan.gateway;

import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
class InternalPathBlockFilter implements WebFilter, Ordered {
  private static final byte[] NOT_FOUND =
      "{\"code\":3001,\"message\":\"资源不存在\",\"data\":null}".getBytes(StandardCharsets.UTF_8);

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    if (!"/internal".equals(path) && !path.startsWith("/internal/")) return chain.filter(exchange);
    exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(NOT_FOUND);
    return exchange.getResponse().writeWith(Mono.just(buffer));
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
