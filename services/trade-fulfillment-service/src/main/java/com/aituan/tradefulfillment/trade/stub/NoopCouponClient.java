package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.CouponClient;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NoopCouponClient implements CouponClient {

  @Override
  public CouponDiscount calcDiscount(Long userId, Long couponId, BigDecimal amount) {
    if (couponId == null) {
      return new CouponDiscount(true, BigDecimal.ZERO, null);
    }
    return new CouponDiscount(false, BigDecimal.ZERO, "优惠券服务尚未接入");
  }
}
