package com.aituan.tradefulfillment.trade.client;

import java.util.List;

public interface InventoryClient {

  void deduct(long orderId, List<InventoryLine> items);

  void restore(long orderId, List<InventoryLine> items);

  record InventoryLine(long skuId, long itemId, int quantity) {}
}
