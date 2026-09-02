package com.aituan.tradefulfillment;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aituan.ApiTestSupport;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.JwtTokenService;
import com.aituan.tradefulfillment.trade.client.CatalogClient;
import com.aituan.tradefulfillment.trade.stub.StubCatalogClient;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TradeCatalogFailureIsolationIntegrationTest.FaultInjectionConfig.class)
@Transactional
class TradeCatalogFailureIsolationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenService jwtTokenService;

  @Autowired
  private FaultInjectableCatalogClient catalogClient;

  @BeforeEach
  void restoreCatalog() {
    catalogClient.setAvailable(true);
  }

  @Test
  void cartUsesDurableSnapshotAndFailsWritesFastWhileCatalogIsDownThenRecovers() throws Exception {
    String userToken = ApiTestSupport.userToken(jwtTokenService);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/cart/items"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"itemId":1002,"quantity":2}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.catalogAvailable").value(true))
        .andExpect(jsonPath("$.data.items[0].itemName").value("吮指原味鸡"))
        .andExpect(jsonPath("$.data.items[0].totalPrice").value(39.80));

    catalogClient.setAvailable(false);

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/cart").param("storeId", "1"), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.catalogAvailable").value(false))
        .andExpect(jsonPath("$.data.notice").value(
            "商品服务暂不可用，已显示最近一次购物车快照；暂不可新增商品或修改数量，仍可移除或清空。"))
        .andExpect(jsonPath("$.data.storeName").value("爱团炸鸡中关村店"))
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.items[0].itemName").value("吮指原味鸡"))
        .andExpect(jsonPath("$.data.items[0].quantity").value(2))
        .andExpect(jsonPath("$.data.items[0].totalPrice").value(39.80));

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/cart/items"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"itemId":1001,"quantity":1}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.businessError(ErrorCode.INTERNAL_ERROR))
        .andExpect(jsonPath("$.message").value("商品服务暂不可用"));

    mockMvc.perform(ApiTestSupport.bearer(get("/api/app/trade/cart").param("storeId", "1"), userToken))
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.items", hasSize(1)));

    mockMvc.perform(ApiTestSupport.bearer(
            delete("/api/app/trade/cart/items/{itemId}", 1002).param("storeId", "1"), userToken))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.catalogAvailable").value(false))
        .andExpect(jsonPath("$.data.items", hasSize(0)));

    catalogClient.setAvailable(true);

    mockMvc.perform(ApiTestSupport.bearer(post("/api/app/trade/cart/items"), userToken)
            .contentType(ApiTestSupport.JSON)
            .content("""
                {"storeId":1,"itemId":1001,"quantity":1}
                """))
        .andExpect(status().isOk())
        .andExpect(ApiTestSupport.okResponse())
        .andExpect(jsonPath("$.data.catalogAvailable").value(true))
        .andExpect(jsonPath("$.data.notice").doesNotExist())
        .andExpect(jsonPath("$.data.items", hasSize(1)))
        .andExpect(jsonPath("$.data.items[0].itemName").value("香辣鸡腿堡"));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FaultInjectionConfig {
    @Bean
    @Primary
    FaultInjectableCatalogClient faultInjectableCatalogClient() {
      return new FaultInjectableCatalogClient();
    }
  }

  static final class FaultInjectableCatalogClient implements CatalogClient {
    private final StubCatalogClient delegate = new StubCatalogClient();
    private volatile boolean available = true;

    void setAvailable(boolean available) {
      this.available = available;
    }

    @Override
    public Optional<StoreSnapshot> findStore(long storeId) {
      requireAvailable();
      return delegate.findStore(storeId);
    }

    @Override
    public Optional<ItemSnapshot> findItem(long itemId) {
      requireAvailable();
      return delegate.findItem(itemId);
    }

    @Override
    public DeliveryRuleSnapshot deliveryRule(long storeId) {
      requireAvailable();
      return delegate.deliveryRule(storeId);
    }

    private void requireAvailable() {
      if (!available) {
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "商品服务暂不可用");
      }
    }
  }
}
