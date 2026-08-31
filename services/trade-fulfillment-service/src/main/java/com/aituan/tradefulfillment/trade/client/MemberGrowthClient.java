package com.aituan.tradefulfillment.trade.client;

import java.math.BigDecimal;

public interface MemberGrowthClient {

  void addOrderCompletionGrowth(long userId, long orderId, BigDecimal amount);

  void refundOrderGrowth(long userId, long orderId);
}
