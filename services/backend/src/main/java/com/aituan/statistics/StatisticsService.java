package com.aituan.statistics;

import com.aituan.common.enums.AccountType;
import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUser;
import com.aituan.common.security.CurrentUserContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
class StatisticsService {
  private final StatisticsRepository statisticsRepository;
  private final JdbcTemplate jdbcTemplate;

  StatisticsService(StatisticsRepository statisticsRepository, JdbcTemplate jdbcTemplate) {
    this.statisticsRepository = statisticsRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  // ============ 商家驾驶舱 ============

  MerchantDashboardView merchantDashboard() {
    long merchantId = currentMerchantId();
    long todayOrders = statisticsRepository.count(
        """
        select count(1) from order_main o
        join merchant_store ms on ms.id = o.store_id and ms.is_deleted = 0
        where ms.merchant_id = ? and o.is_deleted = 0
          and cast(o.created_at as date) = current_date
        """,
        merchantId);
    BigDecimal todayRevenue = statisticsRepository.sum(
        """
        select coalesce(sum(o.payable_amount), 0) from order_main o
        join merchant_store ms on ms.id = o.store_id and ms.is_deleted = 0
        where ms.merchant_id = ? and o.payment_status = 'paid' and o.is_deleted = 0
          and cast(o.created_at as date) = current_date
        """,
        merchantId);
    long pendingReviews = statisticsRepository.count(
        """
        select count(1) from review_record r
        join merchant_store ms on ms.id = r.store_id and ms.is_deleted = 0
        where ms.merchant_id = ? and r.status = 'published' and r.replied = 0 and r.is_deleted = 0
        """,
        merchantId);
    long openSessions = statisticsRepository.count(
        "select count(1) from support_session where merchant_id = ? and status = 'open' and is_deleted = 0",
        merchantId);
    double averageRating = round1(statisticsRepository.averageRating(merchantId));
    LocalDate today = LocalDate.now();
    LocalDate start = today.minusDays(6);
    Map<LocalDate, Long> bucket = new HashMap<>();
    for (StatisticsRepository.DailyCountRow row : statisticsRepository.dailyOrdersByMerchant(merchantId, start, today)) {
      bucket.put(row.date(), row.count());
    }
    List<DailyCountView> weekly = new ArrayList<>();
    for (int i = 0; i <= 6; i++) {
      LocalDate d = start.plusDays(i);
      weekly.add(new DailyCountView(d, bucket.getOrDefault(d, 0L)));
    }
    return new MerchantDashboardView(todayOrders, todayRevenue, pendingReviews, openSessions, averageRating, weekly);
  }

  // ============ 后台驾驶舱 ============

  AdminGovernanceDashboardView adminGovernanceDashboard() {
    requireAdmin();
    long todayOrders = statisticsRepository.count(
        "select count(1) from order_main where cast(created_at as date) = current_date and is_deleted = 0");
    BigDecimal todayRevenue = statisticsRepository.sum(
        "select coalesce(sum(payable_amount), 0) from order_main where cast(created_at as date) = current_date and payment_status = 'paid' and is_deleted = 0");
    long abnormal = statisticsRepository.count(
        "select count(1) from order_main where fulfillment_status = 'abnormal' and is_deleted = 0");
    long merchants = statisticsRepository.count("select count(1) from merchant_profile where is_deleted = 0");
    long users = statisticsRepository.count("select count(1) from user_profile where is_deleted = 0");
    long pendingReviews = statisticsRepository.count(
        "select count(1) from review_record where status = 'published' and reported_count > 0 and is_deleted = 0");
    long reportedReviews = statisticsRepository.count(
        "select count(1) from review_record where reported_count > 0 and is_deleted = 0");
    long pendingComplaints = statisticsRepository.count(
        "select count(1) from complaint_ticket where status in ('pending', 'processing') and is_deleted = 0");
    long openSessions = statisticsRepository.count(
        "select count(1) from support_session where status = 'open' and is_deleted = 0");
    return new AdminGovernanceDashboardView(
        todayOrders, todayRevenue, abnormal, merchants, users,
        pendingReviews, reportedReviews, pendingComplaints, openSessions,
        statisticsRepository.ratingDistribution());
  }

  // ============ 鉴权工具 ============

  private CurrentUser requireMerchant() {
    CurrentUser c = CurrentUserContext.required();
    if (c.accountType() != AccountType.MERCHANT) throw new BusinessException(ErrorCode.FORBIDDEN);
    return c;
  }

  private void requireAdmin() {
    CurrentUser c = CurrentUserContext.required();
    if (c.accountType() != AccountType.ADMIN) throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  private long currentMerchantId() {
    long accountId = requireMerchant().accountId();
    Long merchantId = jdbcTemplate.query(
        "select id from merchant_profile where account_id = ? and is_deleted = 0 limit 1",
        rs -> rs.next() ? rs.getLong("id") : null,
        accountId);
    if (merchantId == null) throw new BusinessException(ErrorCode.NOT_FOUND, "商家资料不存在");
    return merchantId;
  }

  private double round1(double v) {
    return Math.round(v * 10.0) / 10.0;
  }
}
