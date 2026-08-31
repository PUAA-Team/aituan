package com.aituan.tradefulfillment.trade.client;

public interface MessageClient {

  void order(long userId, String title, String content, String badge, long orderId);
}
