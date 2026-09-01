package com.aituan.engagementplatform.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PlatformRemoteClientTest {
  private HttpServer server;

  @AfterEach
  void stopServer() {
    if (server != null) server.stop(0);
  }

  @Test
  void parsesSuccessfulInternalResponseAndSendsServiceHeaders() throws Exception {
    AtomicInteger headerChecks = new AtomicInteger();
    start(exchange -> {
      if ("engagement-platform-service".equals(exchange.getRequestHeaders().getFirst("X-Caller-Service"))
          && "test-token".equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
        headerChecks.incrementAndGet();
      }
      respond(exchange, 200, "{\"code\":0,\"data\":{\"userId\":7,\"nickname\":\"Alice\"}}");
    });

    PlatformRemoteClient.UserSnapshot result = client(300).userSummary(7);

    assertThat(result).isEqualTo(new PlatformRemoteClient.UserSnapshot(7, "Alice"));
    assertThat(headerChecks).hasValue(1);
  }

  @Test
  void mapsNotFoundWithoutRetry() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    start(exchange -> {
      requests.incrementAndGet();
      respond(exchange, 404, "{\"code\":3001,\"message\":\"not found\"}");
    });

    assertThatThrownBy(() -> client(300).userSummary(404))
        .isInstanceOfSatisfying(BusinessException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    assertThat(requests).hasValue(1);
  }

  @Test
  void retriesGetOnceAfterServerError() throws Exception {
    AtomicInteger requests = new AtomicInteger();
    start(exchange -> {
      if (requests.incrementAndGet() == 1) {
        respond(exchange, 503, "{\"code\":5001,\"message\":\"unavailable\"}");
      } else {
        respond(exchange, 200, "{\"code\":0,\"data\":{\"userId\":8,\"nickname\":\"Bob\"}}");
      }
    });

    assertThat(client(300).userSummary(8).nickname()).isEqualTo("Bob");
    assertThat(requests).hasValue(2);
  }

  @Test
  void timesOutAfterOneGetRetryAndNeverRetriesPostAutomatically() throws Exception {
    AtomicInteger getRequests = new AtomicInteger();
    AtomicInteger postRequests = new AtomicInteger();
    start(exchange -> {
      if ("POST".equals(exchange.getRequestMethod())) {
        postRequests.incrementAndGet();
        respond(exchange, 503, "{\"code\":5001,\"message\":\"unavailable\"}");
        return;
      }
      getRequests.incrementAndGet();
      try {
        Thread.sleep(120);
      } catch (InterruptedException interrupted) {
        Thread.currentThread().interrupt();
      }
      respond(exchange, 200, "{\"code\":0,\"data\":{}}");
    });
    PlatformRemoteClient client = client(30);

    assertThatThrownBy(() -> client.userSummary(9)).isInstanceOf(BusinessException.class);
    assertThat(getRequests).hasValue(2);
    assertThatThrownBy(() -> client.markOrderReviewed(10, 20)).isInstanceOf(BusinessException.class);
    assertThat(postRequests).hasValue(1);
  }

  private void start(com.sun.net.httpserver.HttpHandler handler) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newCachedThreadPool(task -> {
      Thread thread = new Thread(task, "platform-client-stub");
      thread.setDaemon(true);
      return thread;
    }));
    server.createContext("/", handler);
    server.start();
  }

  private PlatformRemoteClient client(int readTimeoutMs) {
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    return new PlatformRemoteClient(baseUrl, baseUrl, baseUrl, 100, readTimeoutMs, "test-token");
  }

  private static void respond(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    try {
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
    } finally {
      exchange.close();
    }
  }
}
