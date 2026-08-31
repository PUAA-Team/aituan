package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.InventoryClient;
import org.springframework.stereotype.Component;

@Component
public class NoopInventoryClient implements InventoryClient {

  @Override
  public boolean reserve(long itemId, int quantity) {
    return true;
  }

  @Override
  public void release(long itemId, int quantity) {
  }
}
