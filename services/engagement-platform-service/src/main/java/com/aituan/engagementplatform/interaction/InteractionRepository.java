package com.aituan.engagementplatform.interaction;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class InteractionRepository {
  private final JdbcTemplate jdbcTemplate;

  InteractionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // ============ 评价查询（共用 SELECT） ============

  private static final String REVIEW_SELECT_CORE = """
      select r.id, r.order_id, r.order_no, r.order_title, r.store_id, r.store_name,
             r.rating, r.content, r.labels, r.image_urls,
             r.helpful_count, r.reported_count,
             r.status, r.replied, rr.reply_content, rr.replied_at, r.created_at,
             r.user_nickname, r.user_id
      from review_record r
      left join review_reply rr on rr.review_id = r.id and rr.is_deleted = 0
      """;

  Optional<ReviewRow> findReviewByOrder(long userId, long orderId) {
    List<ReviewRow> rows = jdbcTemplate.query(
        REVIEW_SELECT_CORE + " where r.user_id = ? and r.order_id = ? and r.is_deleted = 0 limit 1",
        this::mapReview, userId, orderId);
    return rows.stream().findFirst();
  }

  Optional<ReviewRow> findReviewById(long reviewId) {
    List<ReviewRow> rows = jdbcTemplate.query(
        REVIEW_SELECT_CORE + " where r.id = ? and r.is_deleted = 0 limit 1",
        this::mapReview, reviewId);
    return rows.stream().findFirst();
  }

  long countMyReviews(long userId, String statusFilter) {
    if (statusFilter == null) {
      Long count = jdbcTemplate.queryForObject(
          "select count(1) from review_record where user_id = ? and is_deleted = 0",
          Long.class, userId);
      return count == null ? 0 : count;
    }
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from review_record where user_id = ? and status = ? and is_deleted = 0",
        Long.class, userId, statusFilter);
    return count == null ? 0 : count;
  }

  List<ReviewRow> listMyReviews(long userId, String statusFilter, int offset, int limit) {
    if (statusFilter == null) {
      return jdbcTemplate.query(
          REVIEW_SELECT_CORE
              + " where r.user_id = ? and r.is_deleted = 0"
              + " order by r.created_at desc, r.id desc limit ? offset ?",
          this::mapReview, userId, limit, offset);
    }
    return jdbcTemplate.query(
        REVIEW_SELECT_CORE
            + " where r.user_id = ? and r.status = ? and r.is_deleted = 0"
            + " order by r.created_at desc, r.id desc limit ? offset ?",
        this::mapReview, userId, statusFilter, limit, offset);
  }

  long countStoreReviews(long storeId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from review_record where store_id = ? and status = 'published' and is_deleted = 0",
        Long.class,
        storeId);
    return count == null ? 0 : count;
  }

  List<ReviewRow> listStoreReviews(long storeId, int offset, int limit) {
    return jdbcTemplate.query(
        REVIEW_SELECT_CORE
            + " where r.store_id = ? and r.status = 'published' and r.is_deleted = 0"
            + " order by r.created_at desc, r.id desc limit ? offset ?",
        this::mapReview,
        storeId,
        limit,
        offset);
  }

  // ============ 评价提交 ============

  Long insertReview(long userId, OrderReviewRow order, ReviewCreateRequest request, String labels, String imageUrls) {
    jdbcTemplate.update(
        """
        insert into review_record(order_id, order_no, order_title, store_id, merchant_id, store_name,
                                  user_id, user_nickname, rating, content, labels, image_urls,
                                  status, replied, helpful_count, reported_count, order_marked)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'published', 0, 0, 0, 0)
        """,
        order.id(), order.orderNo(), order.orderTitle(), order.storeId(), order.merchantId(), order.storeName(),
        userId, order.userNickname(), request.rating(), request.content().trim(), labels, imageUrls);
    return jdbcTemplate.queryForObject(
        "select max(id) from review_record where user_id = ? and order_id = ?", Long.class, userId, order.id());
  }

  void markOrderSyncSucceeded(long reviewId) {
    jdbcTemplate.update(
        "update review_record set order_marked = 1, order_mark_attempts = order_mark_attempts + 1, order_mark_last_error = null, order_mark_locked_at = null, updated_at = current_timestamp where id = ?",
        reviewId);
  }

  void markOrderSyncFailed(long reviewId, String error) {
    jdbcTemplate.update(
        "update review_record set order_mark_attempts = order_mark_attempts + 1, order_mark_last_error = ?, order_mark_locked_at = null, updated_at = current_timestamp where id = ?",
        error, reviewId);
  }

  List<PendingOrderMark> claimPendingOrderMarks(int maxAttempts, int limit, int lockTimeoutSeconds) {
    LocalDateTime lockExpiredBefore = LocalDateTime.now().minusSeconds(lockTimeoutSeconds);
    List<PendingOrderMark> candidates = jdbcTemplate.query(
        """
        select id, order_id from review_record
        where order_marked = 0 and order_mark_attempts < ? and is_deleted = 0
          and (order_mark_locked_at is null or order_mark_locked_at < ?)
        order by updated_at asc, id asc limit ?
        """,
        (rs, rowNum) -> new PendingOrderMark(rs.getLong("id"), rs.getLong("order_id")),
        maxAttempts, lockExpiredBefore, limit);
    return candidates.stream()
        .filter(candidate -> jdbcTemplate.update(
            """
            update review_record set order_mark_locked_at = current_timestamp
            where id = ? and order_marked = 0 and order_mark_attempts < ? and is_deleted = 0
              and (order_mark_locked_at is null or order_mark_locked_at < ?)
            """,
            candidate.reviewId(), maxAttempts, lockExpiredBefore) == 1)
        .toList();
  }

  // ============ 评价"有用" ============

  boolean isHelpful(long reviewId, long userId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from review_helpful where review_id = ? and user_id = ? and is_deleted = 0",
        Long.class, reviewId, userId);
    return count != null && count > 0;
  }

  void insertHelpful(long reviewId, long userId) {
    jdbcTemplate.update(
        """
        insert into review_helpful(review_id, user_id, is_deleted) values (?, ?, 0)
        on duplicate key update is_deleted = 0, created_at = current_timestamp
        """,
        reviewId, userId);
  }

  void removeHelpful(long reviewId, long userId) {
    jdbcTemplate.update(
        "update review_helpful set is_deleted = 1 where review_id = ? and user_id = ?",
        reviewId, userId);
  }

  int countHelpful(long reviewId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from review_helpful where review_id = ? and is_deleted = 0",
        Long.class, reviewId);
    return count == null ? 0 : count.intValue();
  }

  void updateHelpfulCount(long reviewId, int count) {
    jdbcTemplate.update(
        "update review_record set helpful_count = ?, updated_at = current_timestamp where id = ?",
        count, reviewId);
  }

  // ============ 评价举报 ============

  Long insertReport(long reviewId, long reporterUserId, String reason, String detail, String evidenceUrls) {
    jdbcTemplate.update(
        "insert into review_report(review_id, reporter_user_id, reason, detail, evidence_urls, status) values (?, ?, ?, ?, ?, 'submitted')",
        reviewId, reporterUserId, reason, detail, evidenceUrls);
    return jdbcTemplate.queryForObject(
        "select max(id) from review_report where review_id = ? and reporter_user_id = ?",
        Long.class, reviewId, reporterUserId);
  }

  Optional<ReportRow> findActiveReport(long reviewId, long reporterUserId) {
    List<ReportRow> rows = jdbcTemplate.query(
        """
        select id, review_id, reporter_user_id, reason, detail, evidence_urls, status
        from review_report
        where review_id = ? and reporter_user_id = ? and status = 'submitted' and is_deleted = 0
        order by id desc limit 1
        """,
        (rs, n) -> new ReportRow(
            rs.getLong("id"), rs.getLong("review_id"), rs.getLong("reporter_user_id"),
            rs.getString("reason"), rs.getString("detail"), rs.getString("evidence_urls"),
            rs.getString("status")),
        reviewId, reporterUserId);
    return rows.stream().findFirst();
  }

  int countActiveReports(long reviewId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from review_report where review_id = ? and status = 'submitted' and is_deleted = 0",
        Long.class, reviewId);
    return count == null ? 0 : count.intValue();
  }

  void refreshReportedCount(long reviewId) {
    jdbcTemplate.update(
        "update review_record set reported_count = ?, updated_at = current_timestamp where id = ?",
        countActiveReports(reviewId), reviewId);
  }

  void markReportsHandled(long reviewId, long handlerAccountId) {
    jdbcTemplate.update(
        """
        update review_report set status = 'handled', handled_by = ?, handled_at = current_timestamp, updated_at = current_timestamp
        where review_id = ? and status = 'submitted' and is_deleted = 0
        """,
        handlerAccountId, reviewId);
  }

  List<String> listActiveReportReasons(long reviewId) {
    return jdbcTemplate.queryForList(
        "select concat(reason, coalesce(concat('：', detail), '')) from review_report where review_id = ? and status = 'submitted' and is_deleted = 0 order by id desc",
        String.class, reviewId);
  }

  List<String> listActiveReportEvidenceUrls(long reviewId) {
    return jdbcTemplate.queryForList(
        "select evidence_urls from review_report where review_id = ? and status = 'submitted' and is_deleted = 0 and evidence_urls is not null order by id desc",
        String.class, reviewId);
  }

  // ============ 商家端评价 ============

  long countMerchantReviews(long merchantId, String statusFilter, Boolean replied) {
    StringBuilder sql = new StringBuilder("""
        select count(1) from review_record r
        where r.merchant_id = ? and r.is_deleted = 0
        """);
    List<Object> args = new java.util.ArrayList<>();
    args.add(merchantId);
    if (statusFilter != null) {
      if ("reported".equals(statusFilter)) {
        sql.append(" and ").append(activeReportExistsSql());
      } else {
        sql.append(" and r.status = ? ");
        args.add(statusFilter);
      }
    }
    if (replied != null) {
      sql.append(" and r.replied = ? ");
      args.add(replied ? 1 : 0);
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return count == null ? 0 : count;
  }

  List<ReviewRow> listMerchantReviews(long merchantId, String statusFilter, Boolean replied, int offset, int limit) {
    StringBuilder sql = new StringBuilder(REVIEW_SELECT_CORE);
    sql.append(" where r.merchant_id = ? and r.is_deleted = 0 ");
    List<Object> args = new java.util.ArrayList<>();
    args.add(merchantId);
    if (statusFilter != null) {
      if ("reported".equals(statusFilter)) {
        sql.append(" and ").append(activeReportExistsSql());
      } else {
        sql.append(" and r.status = ? ");
        args.add(statusFilter);
      }
    }
    if (replied != null) {
      sql.append(" and r.replied = ? ");
      args.add(replied ? 1 : 0);
    }
    sql.append(" order by r.created_at desc, r.id desc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapReview, args.toArray());
  }

  Optional<ReviewRow> findReviewForMerchant(long merchantId, long reviewId) {
    List<ReviewRow> rows = jdbcTemplate.query(
        REVIEW_SELECT_CORE
            + " where r.id = ? and r.merchant_id = ? and r.is_deleted = 0 limit 1",
        this::mapReview, reviewId, merchantId);
    return rows.stream().findFirst();
  }

  Optional<ReplyRow> findReplyByReview(long reviewId) {
    List<ReplyRow> rows = jdbcTemplate.query(
        """
        select id, review_id, merchant_id, reply_content, replied_at
        from review_reply
        where review_id = ? and is_deleted = 0
        order by id desc limit 1
        """,
        (rs, n) -> {
          Timestamp t = rs.getTimestamp("replied_at");
          return new ReplyRow(
              rs.getLong("id"), rs.getLong("review_id"), rs.getLong("merchant_id"),
              rs.getString("reply_content"), t == null ? null : t.toLocalDateTime());
        },
        reviewId);
    return rows.stream().findFirst();
  }

  Long insertReply(long reviewId, long merchantId, String content) {
    jdbcTemplate.update(
        "insert into review_reply(review_id, merchant_id, reply_content) values (?, ?, ?)",
        reviewId, merchantId, content);
    return jdbcTemplate.queryForObject(
        "select max(id) from review_reply where review_id = ? and merchant_id = ?",
        Long.class, reviewId, merchantId);
  }

  void markReviewReplied(long reviewId) {
    jdbcTemplate.update(
        "update review_record set replied = 1, updated_at = current_timestamp where id = ?",
        reviewId);
  }

  // ============ 后台端评价审核 ============

  long countAdminReviews(String statusFilter, Boolean reported) {
    StringBuilder sql = new StringBuilder("select count(1) from review_record r where r.is_deleted = 0 ");
    List<Object> args = new java.util.ArrayList<>();
    if (statusFilter != null) {
      sql.append(" and r.status = ? ");
      args.add(statusFilter);
    }
    if (Boolean.TRUE.equals(reported)) {
      sql.append(" and ").append(activeReportExistsSql());
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
    return count == null ? 0 : count;
  }

  List<ReviewRow> listAdminReviews(String statusFilter, Boolean reported, int offset, int limit) {
    StringBuilder sql = new StringBuilder(REVIEW_SELECT_CORE);
    sql.append(" where r.is_deleted = 0 ");
    List<Object> args = new java.util.ArrayList<>();
    if (statusFilter != null) {
      sql.append(" and r.status = ? ");
      args.add(statusFilter);
    }
    if (Boolean.TRUE.equals(reported)) {
      sql.append(" and ").append(activeReportExistsSql());
    }
    sql.append(" order by r.reported_count desc, r.created_at desc, r.id desc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapReview, args.toArray());
  }

  void updateReviewStatus(long reviewId, String status) {
    jdbcTemplate.update(
        "update review_record set status = ?, updated_at = current_timestamp where id = ?",
        status, reviewId);
  }

  void insertReviewAuditLog(long reviewId, String action, String fromStatus, String toStatus, long operatorId, String remark) {
    jdbcTemplate.update(
        """
        insert into review_audit_log(review_id, action, from_status, to_status, operator_id, remark)
        values (?, ?, ?, ?, ?, ?)
        """,
        reviewId, action, fromStatus, toStatus, operatorId, remark);
  }

  void insertSysAuditLog(String actorType, long actorId, String actionType, String targetType, long targetId, String detail) {
    jdbcTemplate.update(
        """
        insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail)
        values (?, ?, ?, ?, ?, ?)
        """,
        actorType, actorId, actionType, targetType, targetId, detail);
  }

  private String activeReportExistsSql() {
    return """
        exists (
          select 1 from review_report rp
          where rp.review_id = r.id and rp.status = 'submitted' and rp.is_deleted = 0
        )
        """;
  }

  // ============ 行映射 ============

  private ReviewRow mapReview(ResultSet rs, int rowNum) throws SQLException {
    Timestamp repliedAt = rs.getTimestamp("replied_at");
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new ReviewRow(
        rs.getLong("id"), rs.getLong("order_id"), rs.getString("order_no"), rs.getString("order_title"),
        rs.getLong("store_id"), rs.getString("store_name"),
        rs.getInt("rating"), rs.getString("content"), rs.getString("labels"), rs.getString("image_urls"),
        rs.getInt("helpful_count"), rs.getInt("reported_count"),
        rs.getString("status"), rs.getBoolean("replied"),
        rs.getString("reply_content"),
        repliedAt == null ? null : repliedAt.toLocalDateTime(),
        createdAt == null ? null : createdAt.toLocalDateTime(),
        rs.getString("user_nickname"), rs.getLong("user_id"));
  }

  record OrderReviewRow(Long id, String orderNo, String orderTitle, Long storeId, Long merchantId,
                        String storeName, String userNickname) {}

  record PendingOrderMark(long reviewId, long orderId) {}

  record ReviewRow(Long id, Long orderId, String orderNo, String orderTitle, Long storeId, String storeName,
                   int rating, String content, String labels, String imageUrls,
                   int helpfulCount, int reportedCount,
                   String status, boolean replied,
                   String replyContent, LocalDateTime repliedAt, LocalDateTime createdAt,
                   String userNickname, Long userId) {}

  record ReplyRow(Long id, Long reviewId, Long merchantId, String content, LocalDateTime repliedAt) {}

  record ReportRow(Long id, Long reviewId, Long reporterUserId, String reason, String detail,
                   String evidenceUrls, String status) {}
}
