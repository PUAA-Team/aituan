package com.aituan.tradefulfillment.trade.client;

public interface MerchantAuthClient {

  boolean canManageStore(long accountId, long storeId);
}
