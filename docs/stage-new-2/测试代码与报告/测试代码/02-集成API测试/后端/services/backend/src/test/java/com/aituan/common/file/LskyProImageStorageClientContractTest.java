package com.aituan.common.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** LskyPro 图床外部接口 mock contract：校验请求格式并使用模拟响应解析结果。 */
class LskyProImageStorageClientContractTest {
  private HttpServer server;
  private final AtomicReference<RecordedRequest> recorded = new AtomicReference<>();

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/v1/upload", this::handleUpload);
    server.start();
  }

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void uploadShouldSendLskyProMultipartContractAndParsePublicUrl() {
    LskyProImageStorageClient client = new LskyProImageStorageClient(properties(), new ObjectMapper());

    StoredImage image = client.save(
        new MockMultipartFile("file", "review.png", "image/png", tinyPng()),
        "review",
        "review-1.png");

    RecordedRequest request = recorded.get();
    assertThat(request).isNotNull();
    assertThat(request.method()).isEqualTo("POST");
    assertThat(request.path()).isEqualTo("/api/v1/upload");
    assertThat(request.authorization()).isEqualTo("Bearer test-token");
    assertThat(request.contentType()).startsWith("multipart/form-data;");
    assertThat(request.contentType()).contains("boundary=");
    assertThat(request.body()).contains(
        "name=\"file\"",
        "filename=\"review-1.png\"",
        "name=\"strategy_id\"",
        "strategy-test",
        "name=\"album_id\"",
        "album-test",
        "name=\"permission\"",
        "1");

    assertThat(image.storageType()).isEqualTo("lskypro");
    assertThat(image.objectKey()).isEqualTo("review/uploads/review-1.png");
    assertThat(image.publicUrl()).isEqualTo("https://cdn.example.com/review-1.png");
  }

  private ImageStorageProperties properties() {
    ImageStorageProperties properties = new ImageStorageProperties();
    ImageStorageProperties.Lskypro lskypro = properties.getLskypro();
    lskypro.setApiUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1");
    lskypro.setToken("test-token");
    lskypro.setStrategyId("strategy-test");
    lskypro.setAlbumId("album-test");
    lskypro.setPermission(1);
    return properties;
  }

  private void handleUpload(HttpExchange exchange) throws IOException {
    byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
    recorded.set(new RecordedRequest(
        exchange.getRequestMethod(),
        exchange.getRequestURI().getPath(),
        exchange.getRequestHeaders().getFirst("Authorization"),
        exchange.getRequestHeaders().getFirst("Content-Type"),
        new String(bodyBytes, StandardCharsets.ISO_8859_1)));

    byte[] response = """
        {"status":true,"message":"success","data":{"key":"uploads/review-1.png","pathname":"uploads/review-1.png","links":{"url":"https://cdn.example.com/review-1.png"}}}
        """.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, response.length);
    exchange.getResponseBody().write(response);
    exchange.close();
  }

  private byte[] tinyPng() {
    return java.util.Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=");
  }

  private record RecordedRequest(
      String method,
      String path,
      String authorization,
      String contentType,
      String body) {}
}
