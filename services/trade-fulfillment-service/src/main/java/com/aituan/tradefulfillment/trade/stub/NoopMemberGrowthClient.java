package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.MemberGrowthClient;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class NoopMemberGrowthClient implements MemberGrowthClient {

  @Override
  public void addOrderCompletionGrowth(long userId, long orderId, BigDecimal amount) {
  }

  @Override
  public void refundOrderGrowth(long userId, long orderId) {
  }
}
