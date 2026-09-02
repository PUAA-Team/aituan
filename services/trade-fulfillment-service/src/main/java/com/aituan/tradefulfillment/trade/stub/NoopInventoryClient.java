package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.InventoryClient;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "test"})
public class NoopInventoryClient implements InventoryClient {

  @Override
  public void deduct(long orderId, List<InventoryLine> items) {
  }

  @Override
  public void restore(long orderId, List<InventoryLine> items) {
  }
}
