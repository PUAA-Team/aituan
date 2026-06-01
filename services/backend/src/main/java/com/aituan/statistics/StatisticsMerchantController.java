package com.aituan.statistics;

import com.aituan.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant/ops/dashboard")
class StatisticsMerchantController {
  private final StatisticsService statisticsService;

  StatisticsMerchantController(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  @GetMapping
  ApiResponse<MerchantDashboardView> dashboard() {
    return ApiResponse.ok(statisticsService.merchantDashboard());
  }
}
