package com.aituan.statistics;

import com.aituan.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/governance/dashboard")
class StatisticsAdminController {
  private final StatisticsService statisticsService;

  StatisticsAdminController(StatisticsService statisticsService) {
    this.statisticsService = statisticsService;
  }

  @GetMapping
  ApiResponse<AdminGovernanceDashboardView> dashboard() {
    return ApiResponse.ok(statisticsService.adminGovernanceDashboard());
  }
}
