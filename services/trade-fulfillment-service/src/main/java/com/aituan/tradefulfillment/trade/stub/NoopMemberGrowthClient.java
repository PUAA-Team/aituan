package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.MemberGrowthClient;
import java.math.BigDecimal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"demo", "test"})
public class NoopMemberGrowthClient implements MemberGrowthClient {

  @Override
  public void addOrderCompletionGrowth(long userId, long orderId, BigDecimal amount) {
  }

  @Override
  public void refundOrderGrowth(long userId, long orderId, BigDecimal amount) {
  }
}
