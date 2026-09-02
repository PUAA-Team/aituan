package com.aituan.merchantcatalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MerchantCatalogServiceUnitTest {

  @Mock MerchantCatalogRepository repository;
  @Mock FileStorageService fileStorageService;
  @Mock MapDistanceService mapDistanceService;
  @Mock InternalServiceClient internalClient;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MerchantCatalogService service;

  @BeforeEach
  void setUp() {
    service = new MerchantCatalogService(repository, fileStorageService, mapDistanceService, internalClient, objectMapper);
  }

  // ---------- 库存扣减 / 回滚（幂等落库） ----------

  @Test
  void deductInventoryDecreasesStockAndPersistsIdempotency() {
    when(repository.findInventoryIdempotency("trade-fulfillment-service", "deduct", "key-1")).thenReturn(Optional.empty());
    when(repository.findSku(2L)).thenReturn(Optional.of(sku(2L, 1002L, new BigDecimal("19.90"), 500)));
    when(repository.decreaseSkuStock(2L, 2)).thenReturn(1);

    InventoryResultView result = service.deductInventory(
        new InventoryDeductRequest(10001L, List.of(new InventoryItemRequest(2L, 1002L, 2))),
        "trade-fulfillment-service",
        "key-1");

    assertThat(result.idempotencyKey()).isEqualTo("key-1");
    assertThat(result.status()).isEqualTo("deducted");
    assertThat(result.lines()).hasSize(1);
    assertThat(result.lines().get(0).skuId()).isEqualTo(2L);
    verify(repository).decreaseSkuStock(2L, 2);
    verify(repository).insertInventoryIdempotency(eq("trade-fulfillment-service"), eq("deduct"), eq("key-1"), anyString(), anyString(), eq("deducted"));
  }

  @Test
  void deductInventoryReplaysStoredResultForRepeatedKey() {
    String storedLines = "[{\"skuId\":2,\"itemId\":1002,\"quantity\":1,\"status\":\"deducted\"}]";
    when(repository.findInventoryIdempotency("trade-fulfillment-service", "deduct", "key-1"))
        .thenReturn(Optional.of(new MerchantCatalogRepository.InventoryIdempotencyRow(1L, "deducted", storedLines)));

    InventoryResultView result = service.deductInventory(
        new InventoryDeductRequest(10001L, List.of(new InventoryItemRequest(2L, 1002L, 1))),
        "trade-fulfillment-service",
        "key-1");

    assertThat(result.status()).isEqualTo("deducted");
    assertThat(result.lines()).hasSize(1);
    verify(repository, never()).findSku(anyLong());
    verify(repository, never()).decreaseSkuStock(anyLong(), anyInt());
  }

  @Test
  void deductInventoryRejectsMissingIdempotencyKey() {
    assertThatThrownBy(() -> service.deductInventory(
        new InventoryDeductRequest(10001L, List.of(new InventoryItemRequest(2L, 1002L, 1))),
        "trade-fulfillment-service",
        " "))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);
    verify(repository, never()).findSku(anyLong());
  }

  @Test
  void deductInventoryRejectsSkuItemMismatch() {
    when(repository.findInventoryIdempotency(anyString(), eq("deduct"), anyString())).thenReturn(Optional.empty());
    when(repository.findSku(2L)).thenReturn(Optional.of(sku(2L, 9999L, new BigDecimal("19.90"), 500)));

    assertThatThrownBy(() -> service.deductInventory(
        new InventoryDeductRequest(10001L, List.of(new InventoryItemRequest(2L, 1002L, 1))),
        "trade-fulfillment-service",
        "key-1"))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);
    verify(repository, never()).decreaseSkuStock(anyLong(), anyInt());
  }

  @Test
  void deductInventoryRejectsWhenStockInsufficient() {
    when(repository.findInventoryIdempotency(anyString(), eq("deduct"), anyString())).thenReturn(Optional.empty());
    when(repository.findSku(2L)).thenReturn(Optional.of(sku(2L, 1002L, new BigDecimal("19.90"), 1)));
    when(repository.decreaseSkuStock(2L, 2)).thenReturn(0);

    assertThatThrownBy(() -> service.deductInventory(
        new InventoryDeductRequest(10001L, List.of(new InventoryItemRequest(2L, 1002L, 2))),
        "trade-fulfillment-service",
        "key-1"))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_STOCK_NOT_ENOUGH);
    verify(repository, never()).insertInventoryIdempotency(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void restoreInventoryIncreasesStockAndPersistsIdempotency() {
    when(repository.findInventoryIdempotency("trade-fulfillment-service", "restore", "key-2")).thenReturn(Optional.empty());
    when(repository.findSku(2L)).thenReturn(Optional.of(sku(2L, 1002L, new BigDecimal("19.90"), 500)));

    InventoryResultView result = service.restoreInventory(
        new InventoryRestoreRequest(10001L, List.of(new InventoryItemRequest(2L, 1002L, 2))),
        "trade-fulfillment-service",
        "key-2");

    assertThat(result.status()).isEqualTo("restored");
    verify(repository).increaseSkuStock(2L, 2);
    verify(repository).insertInventoryIdempotency(eq("trade-fulfillment-service"), eq("restore"), eq("key-2"), anyString(), anyString(), eq("restored"));
  }

  // ---------- 结算报价 ----------

  @Test
  void checkoutQuoteComputesTotalsFromSkuPrice() {
    when(repository.findStore(1L)).thenReturn(Optional.of(store(1L, 1L, "塔斯汀", "takeaway", "open")));
    when(repository.findCatalogItem(1002L)).thenReturn(Optional.of(item(1002L, 1L, "藤椒鸡腿堡", "takeaway", "on_sale", new BigDecimal("19.90"))));
    when(repository.findDefaultSku(1002L)).thenReturn(Optional.of(sku(2L, 1002L, new BigDecimal("19.90"), 500)));

    CheckoutQuoteView quote = service.checkoutQuote(
        new CheckoutQuoteRequest(1L, List.of(new CheckoutQuoteItemRequest(1002L, 2))));

    assertThat(quote.storeId()).isEqualTo(1L);
    assertThat(quote.items()).hasSize(1);
    assertThat(quote.items().get(0).totalPrice()).isEqualByComparingTo(new BigDecimal("39.80"));
    assertThat(quote.amount()).isEqualByComparingTo(new BigDecimal("39.80"));
  }

  @Test
  void checkoutQuoteRejectsItemNotInStore() {
    when(repository.findStore(1L)).thenReturn(Optional.of(store(1L, 1L, "塔斯汀", "takeaway", "open")));
    when(repository.findCatalogItem(1002L)).thenReturn(Optional.of(item(1002L, 2L, "藤椒鸡腿堡", "takeaway", "on_sale", new BigDecimal("19.90"))));

    assertThatThrownBy(() -> service.checkoutQuote(
        new CheckoutQuoteRequest(1L, List.of(new CheckoutQuoteItemRequest(1002L, 1)))))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);
  }

  @Test
  void checkoutQuoteRejectsOffSaleItem() {
    when(repository.findStore(1L)).thenReturn(Optional.of(store(1L, 1L, "塔斯汀", "takeaway", "open")));
    when(repository.findCatalogItem(1002L)).thenReturn(Optional.of(item(1002L, 1L, "藤椒鸡腿堡", "takeaway", "off_sale", new BigDecimal("19.90"))));

    assertThatThrownBy(() -> service.checkoutQuote(
        new CheckoutQuoteRequest(1L, List.of(new CheckoutQuoteItemRequest(1002L, 1)))))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BUSINESS_RULE_VIOLATION);
  }

  @Test
  void checkoutQuoteRejectsWhenStockInsufficient() {
    when(repository.findStore(1L)).thenReturn(Optional.of(store(1L, 1L, "塔斯汀", "takeaway", "open")));
    when(repository.findCatalogItem(1002L)).thenReturn(Optional.of(item(1002L, 1L, "藤椒鸡腿堡", "takeaway", "on_sale", new BigDecimal("19.90"))));
    when(repository.findDefaultSku(1002L)).thenReturn(Optional.of(sku(2L, 1002L, new BigDecimal("19.90"), 1)));

    assertThatThrownBy(() -> service.checkoutQuote(
        new CheckoutQuoteRequest(1L, List.of(new CheckoutQuoteItemRequest(1002L, 2)))))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_STOCK_NOT_ENOUGH);
  }

  // ---------- 内部快照 ----------

  @Test
  void storeSnapshotBuildsFromStoreAndMerchant() {
    when(repository.findStore(1L)).thenReturn(Optional.of(store(1L, 1L, "塔斯汀", "takeaway", "open")));
    when(repository.findMerchant(1L)).thenReturn(Optional.of(merchant(1L, 2L, "塔斯汀中国汉堡")));

    StoreSnapshotView snapshot = service.storeSnapshot(1L);

    assertThat(snapshot.storeId()).isEqualTo(1L);
    assertThat(snapshot.storeName()).isEqualTo("塔斯汀");
    assertThat(snapshot.merchantName()).isEqualTo("塔斯汀中国汉堡");
    assertThat(snapshot.accountId()).isEqualTo(2L);
  }

  @Test
  void storeSnapshotThrowsWhenStoreMissing() {
    when(repository.findStore(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.storeSnapshot(99L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
  }

  // ---------- 测试数据工厂 ----------

  private MerchantCatalogRepository.SkuRow sku(long skuId, long itemId, BigDecimal price, int stock) {
    return new MerchantCatalogRepository.SkuRow(skuId, itemId, "默认", price, stock, "on_sale");
  }

  private MerchantCatalogRepository.StoreRow store(long id, long merchantId, String name, String businessType, String status) {
    return new MerchantCatalogRepository.StoreRow(id, merchantId, name, businessType, "摘要", "地址", "1km", null, null, new BigDecimal("4.8"), 100, new BigDecimal("30.00"), status, "09:00-22:00", "标签", null, "13800000000", "", null);
  }

  private MerchantCatalogRepository.MerchantRow merchant(long id, long accountId, String name) {
    return new MerchantCatalogRepository.MerchantRow(id, accountId, name, "联系人", "13800000000", "L001", "normal", "approved", 1, 5, null);
  }

  private MerchantCatalogRepository.CatalogItemRow item(long id, long storeId, String title, String businessType, String status, BigDecimal price) {
    return new MerchantCatalogRepository.CatalogItemRow(id, storeId, "塔斯汀", businessType, 101L, "汉堡", title, "副标题", price, new BigDecimal("24.00"), 500, status, null, null, 100, "", "", "", "", 90, null);
  }
}
