package com.aituan.tradefulfillment.trade.client;

import java.math.BigDecimal;

public interface CouponClient {

  CouponDiscount calcDiscount(Long userId, Long couponId, BigDecimal amount);

  void use(long couponId, long userId, long orderId, BigDecimal orderAmount);

  void release(long couponId, long orderId);

  record CouponDiscount(boolean usable, BigDecimal discountAmount, String reason) {}
}
