package com.aituan.statistics;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
record MerchantDashboardView(
    long todayOrders,
    BigDecimal todayRevenue,
    long pendingReviews,
    long openSessions,
    double averageRating,
    List<DailyCountView> weeklyOrders) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record AdminGovernanceDashboardView(
    long todayOrders,
    BigDecimal todayRevenue,
    long abnormalOrders,
    long totalMerchants,
    long totalUsers,
    long pendingReviews,
    long reportedReviews,
    long pendingComplaints,
    long openSessions,
    Map<String, Long> ratingDistribution) {}

@JsonInclude(JsonInclude.Include.NON_NULL)
record DailyCountView(LocalDate date, long count) {}
