package com.aituan.tradefulfillment.trade.stub;

import com.aituan.tradefulfillment.trade.client.MessageClient;
import org.springframework.stereotype.Component;

@Component
public class NoopMessageClient implements MessageClient {

  @Override
  public void order(long userId, String title, String content, String badge, long orderId) {
  }
}
