package com.aituan.statistics;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class StatisticsRepository {
  private final JdbcTemplate jdbcTemplate;

  StatisticsRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  long count(String sql, Object... args) {
    Long v = jdbcTemplate.queryForObject(sql, Long.class, args);
    return v == null ? 0 : v;
  }

  BigDecimal sum(String sql, Object... args) {
    BigDecimal v = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
    return v == null ? BigDecimal.ZERO : v;
  }

  double averageRating(long merchantId) {
    Double v = jdbcTemplate.queryForObject(
        """
        select avg(cast(r.rating as double))
        from review_record r
        join merchant_store ms on ms.id = r.store_id and ms.is_deleted = 0
        where ms.merchant_id = ? and r.status = 'published' and r.is_deleted = 0
        """,
        Double.class, merchantId);
    return v == null ? 0.0 : v;
  }

  List<DailyCountRow> dailyOrdersByMerchant(long merchantId, LocalDate startDate, LocalDate endDate) {
    return jdbcTemplate.query(
        """
        select cast(o.created_at as date) as d, count(1) as c
        from order_main o
        join merchant_store ms on ms.id = o.store_id and ms.is_deleted = 0
        where ms.merchant_id = ?
          and o.is_deleted = 0
          and cast(o.created_at as date) >= ?
          and cast(o.created_at as date) <= ?
        group by cast(o.created_at as date)
        order by d asc
        """,
        (rs, n) -> {
          Date d = rs.getDate("d");
          return new DailyCountRow(d == null ? null : d.toLocalDate(), rs.getLong("c"));
        },
        merchantId, Date.valueOf(startDate), Date.valueOf(endDate));
  }

  Map<String, Long> ratingDistribution() {
    Map<String, Long> result = new HashMap<>();
    result.put("five", 0L);
    result.put("four", 0L);
    result.put("three", 0L);
    result.put("two", 0L);
    result.put("one", 0L);
    jdbcTemplate.query(
        """
        select rating, count(1) as c
        from review_record where status = 'published' and is_deleted = 0
        group by rating
        """,
        rs -> {
          int r = rs.getInt("rating");
          long c = rs.getLong("c");
          String key = switch (r) {
            case 5 -> "five";
            case 4 -> "four";
            case 3 -> "three";
            case 2 -> "two";
            case 1 -> "one";
            default -> null;
          };
          if (key != null) result.put(key, c);
        });
    return result;
  }

  record DailyCountRow(LocalDate date, long count) {}
}
