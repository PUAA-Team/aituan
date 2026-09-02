package com.aituan.tradefulfillment.trade.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpTradeRemoteClientsContractTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final List<RequestCapture> requests = new CopyOnWriteArrayList<>();
  private HttpServer server;
  private HttpTradeRemoteClients client;

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", this::respond);
    server.start();
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    client = new HttpTradeRemoteClients(baseUrl, baseUrl, baseUrl, 1000, 1000, "contract-token", objectMapper);
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void usesDocumentedPathsHeadersAndPayloads() throws Exception {
    CatalogClient.ItemSnapshot item = client.findItem(1002L).orElseThrow();
    assertThat(item.skuId()).isEqualTo(2L);
    assertThat(item.stock()).isEqualTo(8);

    IdentityClient.AddressSnapshot address = client.findAddress(7L, 3L).orElseThrow();
    assertThat(address.id()).isEqualTo(3L);

    CouponClient.CouponDiscount discount = client.calcDiscount(7L, 9L, new BigDecimal("20.00"));
    assertThat(discount.discountAmount()).isEqualByComparingTo("5.00");

    List<InventoryClient.InventoryLine> lines = List.of(new InventoryClient.InventoryLine(2L, 1002L, 2));
    client.deduct(88L, lines);
    client.restore(88L, lines);
    client.use(9L, 7L, 88L, new BigDecimal("20.00"));
    client.release(9L, 88L);
    assertThat(client.canManageStore(77L, 1L)).isTrue();

    assertThat(requests).allSatisfy(request -> {
      assertThat(request.caller()).isEqualTo("trade-fulfillment-service");
      assertThat(request.token()).isEqualTo("contract-token");
      assertThat(request.requestId()).isNotBlank();
    });
    RequestCapture deduct = request("/internal/inventory/deduct");
    JsonNode deductBody = objectMapper.readTree(deduct.body());
    assertThat(deductBody.path("orderId").asLong()).isEqualTo(88L);
    assertThat(deductBody.path("items").get(0).path("skuId").asLong()).isEqualTo(2L);
    assertThat(deduct.idempotencyKey()).isEqualTo("trade:order:88:inventory:deduct");
    assertThat(requests).extracting(RequestCapture::path).contains(
        "/internal/catalog/items/1002/snapshot",
        "/internal/catalog/checkout-quote",
        "/internal/users/7/addresses/3/snapshot",
        "/internal/coupons/9/use",
        "/internal/coupons/9/release");
  }

  private RequestCapture request(String path) {
    return requests.stream().filter(request -> request.path().equals(path)).findFirst().orElseThrow();
  }

  private void respond(HttpExchange exchange) throws java.io.IOException {
    String path = exchange.getRequestURI().getPath();
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    requests.add(new RequestCapture(
        path, body,
        exchange.getRequestHeaders().getFirst("X-Caller-Service"),
        exchange.getRequestHeaders().getFirst("X-Request-Id"),
        exchange.getRequestHeaders().getFirst("X-Service-Token"),
        exchange.getRequestHeaders().getFirst("Idempotency-Key")));
    String data = switch (path) {
      case "/internal/catalog/items/1002/snapshot" ->
          "{\"itemId\":1002,\"storeId\":1,\"itemName\":\"汉堡\",\"businessType\":\"takeaway\",\"price\":18.8,\"status\":\"on_sale\"}";
      case "/internal/catalog/checkout-quote" ->
          "{\"storeId\":1,\"items\":[{\"itemId\":1002,\"skuId\":2,\"itemName\":\"汉堡\",\"businessType\":\"takeaway\",\"quantity\":1,\"unitPrice\":18.8,\"stock\":8,\"status\":\"on_sale\"}]}";
      case "/internal/users/7/addresses/3/snapshot" ->
          "{\"id\":3,\"addressId\":3,\"userId\":7,\"contactName\":\"测试用户\",\"longitude\":116.3,\"latitude\":39.9}";
      case "/internal/coupons/quote" -> "{\"usable\":true,\"discountAmount\":5.00}";
      case "/internal/merchants/by-account/77" -> "{\"merchantId\":99,\"accountId\":77,\"status\":\"normal\",\"currentStoreId\":1}";
      case "/internal/stores/1/snapshot" -> "{\"storeId\":1,\"merchantId\":99,\"storeName\":\"测试门店\",\"businessType\":\"takeaway\"}";
      default -> "{\"success\":true,\"status\":\"completed\"}";
    };
    byte[] response = ("{\"code\":0,\"message\":\"success\",\"data\":" + data + "}")
        .getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
    exchange.sendResponseHeaders(200, response.length);
    exchange.getResponseBody().write(response);
    exchange.close();
  }

  private record RequestCapture(
      String path, String body, String caller, String requestId, String token, String idempotencyKey) {}
}
