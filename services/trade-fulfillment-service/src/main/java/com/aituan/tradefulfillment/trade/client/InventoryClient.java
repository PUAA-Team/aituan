package com.aituan.tradefulfillment.trade.client;

public interface InventoryClient {

  boolean reserve(long itemId, int quantity);

  void release(long itemId, int quantity);
}
