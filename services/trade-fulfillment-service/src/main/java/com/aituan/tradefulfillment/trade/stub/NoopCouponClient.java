package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.CouponClient;
import java.math.BigDecimal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "test"})
public class NoopCouponClient implements CouponClient {

  @Override
  public CouponDiscount calcDiscount(Long userId, Long couponId, BigDecimal amount) {
    if (couponId == null) {
      return new CouponDiscount(true, BigDecimal.ZERO, null);
    }
    return new CouponDiscount(false, BigDecimal.ZERO, "优惠券服务尚未接入");
  }

  @Override
  public void use(long couponId, long userId, long orderId, BigDecimal orderAmount) {
  }

  @Override
  public void release(long couponId, long orderId) {
  }
}
