package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.CatalogClient;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class StubCatalogClient implements CatalogClient {
  private static final StoreSnapshot TAKEAWAY_STORE = new StoreSnapshot(
      1L,
      "爱团炸鸡中关村店",
      "takeaway",
      new BigDecimal("116.316000"),
      new BigDecimal("39.983000"));
  private static final StoreSnapshot GROUP_BUY_STORE = new StoreSnapshot(
      2L,
      "爱团桌游体验馆",
      "group_buy",
      new BigDecimal("116.320000"),
      new BigDecimal("39.980000"));

  private static final Map<Long, ItemSnapshot> ITEMS = Map.of(
      1001L, new ItemSnapshot(1001L, 1L, "香辣鸡腿堡", "经典微辣", "takeaway", 10L, "汉堡", new BigDecimal("18.80"), "on_sale", 30),
      1002L, new ItemSnapshot(1002L, 1L, "吮指原味鸡", "两块装", "takeaway", 11L, "炸鸡", new BigDecimal("19.90"), "on_sale", 50),
      1003L, new ItemSnapshot(1003L, 1L, "停售测试商品", "不可购买", "takeaway", 11L, "炸鸡", new BigDecimal("9.90"), "off_sale", 0),
      2001L, new ItemSnapshot(2001L, 2L, "双人桌游体验券", "周末通用", "group_buy", 20L, "休闲娱乐", new BigDecimal("68.00"), "on_sale", 20));

  @Override
  public Optional<StoreSnapshot> findStore(long storeId) {
    if (storeId == TAKEAWAY_STORE.id()) {
      return Optional.of(TAKEAWAY_STORE);
    }
    if (storeId == GROUP_BUY_STORE.id()) {
      return Optional.of(GROUP_BUY_STORE);
    }
    return Optional.empty();
  }

  @Override
  public Optional<ItemSnapshot> findItem(long itemId) {
    return Optional.ofNullable(ITEMS.get(itemId));
  }

  @Override
  public DeliveryRuleSnapshot deliveryRule(long storeId) {
    return new DeliveryRuleSnapshot(
        new BigDecimal("4.00"),
        new BigDecimal("20.00"),
        35,
        new BigDecimal("5.00"),
        "none",
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ONE);
  }
}
