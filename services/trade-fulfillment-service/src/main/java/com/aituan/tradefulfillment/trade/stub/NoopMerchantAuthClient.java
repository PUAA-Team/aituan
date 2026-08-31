package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.MerchantAuthClient;
import org.springframework.stereotype.Component;

@Component
public class NoopMerchantAuthClient implements MerchantAuthClient {

  @Override
  public boolean canManageStore(long accountId, long storeId) {
    return true;
  }
}
