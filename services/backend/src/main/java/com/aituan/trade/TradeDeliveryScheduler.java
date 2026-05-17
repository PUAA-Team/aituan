package com.aituan.trade;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class TradeDeliveryScheduler {
  private final TradeService tradeService;

  TradeDeliveryScheduler(TradeService tradeService) {
    this.tradeService = tradeService;
  }

  @Scheduled(fixedDelayString = "${aituan.trade.delivery-tick-ms:120000}")
  void tick() {
    tradeService.advanceDueDeliveryTasks();
  }
}
