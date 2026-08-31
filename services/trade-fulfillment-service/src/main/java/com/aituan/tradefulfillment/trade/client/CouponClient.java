package com.aituan.tradefulfillment.trade.client;

import java.math.BigDecimal;

public interface CouponClient {

  CouponDiscount calcDiscount(Long userId, Long couponId, BigDecimal amount);

  void releaseByOrder(long orderId);

  record CouponDiscount(boolean usable, BigDecimal discountAmount, String reason) {}
}
