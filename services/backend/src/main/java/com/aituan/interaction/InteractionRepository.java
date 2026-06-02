package com.aituan.interaction;

import static com.aituan.common.jdbc.JdbcGeneratedKeys.insertAndReturnId;

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

  Optional<OrderReviewRow> findOrderForReview(long userId, long orderId) {
    List<OrderReviewRow> rows = jdbcTemplate.query(
        """
        select id, order_no, title, store_id, store_name, display_status, payment_status, fulfillment_status, order_type
        from order_main
        where id = ? and user_id = ? and is_deleted = 0
        limit 1
        """,
        this::mapOrder,
        orderId,
        userId);
    return rows.stream().findFirst();
  }

  Optional<ReviewRow> findReviewByOrder(long userId, long orderId) {
    List<ReviewRow> rows = jdbcTemplate.query(
        """
        select r.id, r.order_id, o.order_no, o.title, r.store_id, o.store_name, r.rating, r.content, r.labels,
               r.status, r.replied, rr.reply_content, rr.replied_at, r.created_at
        from review_record r
        join order_main o on o.id = r.order_id
        left join review_reply rr on rr.review_id = r.id and rr.is_deleted = 0
        where r.user_id = ? and r.order_id = ? and r.is_deleted = 0
        limit 1
        """,
        this::mapReview,
        userId,
        orderId);
    return rows.stream().findFirst();
  }

  Optional<ReviewRow> findReviewById(long userId, long reviewId) {
    List<ReviewRow> rows = jdbcTemplate.query(
        """
        select r.id, r.order_id, o.order_no, o.title, r.store_id, o.store_name, r.rating, r.content, r.labels,
               r.status, r.replied, rr.reply_content, rr.replied_at, r.created_at
        from review_record r
        join order_main o on o.id = r.order_id
        left join review_reply rr on rr.review_id = r.id and rr.is_deleted = 0
        where r.user_id = ? and r.id = ? and r.is_deleted = 0
        limit 1
        """,
        this::mapReview,
        userId,
        reviewId);
    return rows.stream().findFirst();
  }

  long countReviews(long userId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from review_record where user_id = ? and is_deleted = 0",
        Long.class,
        userId);
    return count == null ? 0 : count;
  }

  List<ReviewRow> listReviews(long userId, int offset, int limit) {
    return jdbcTemplate.query(
        """
        select r.id, r.order_id, o.order_no, o.title, r.store_id, o.store_name, r.rating, r.content, r.labels,
               r.status, r.replied, rr.reply_content, rr.replied_at, r.created_at
        from review_record r
        join order_main o on o.id = r.order_id
        left join review_reply rr on rr.review_id = r.id and rr.is_deleted = 0
        where r.user_id = ? and r.is_deleted = 0
        order by r.created_at desc, r.id desc
        limit ? offset ?
        """,
        this::mapReview,
        userId,
        limit,
        offset);
  }

  Long insertReview(long userId, OrderReviewRow order, ReviewCreateRequest request, String labels) {
    return insertAndReturnId(
        jdbcTemplate,
        """
        insert into review_record(order_id, store_id, user_id, rating, content, labels, status, replied)
        values (?, ?, ?, ?, ?, ?, 'published', 0)
        """,
        order.id(),
        order.storeId(),
        userId,
        request.rating(),
        request.content().trim(),
        labels);
  }

  void markOrderReviewed(long orderId) {
    jdbcTemplate.update(
        "update order_item set is_reviewed = 1, updated_at = current_timestamp where order_id = ? and is_deleted = 0",
        orderId);
  }

  private OrderReviewRow mapOrder(ResultSet rs, int rowNum) throws SQLException {
    return new OrderReviewRow(
        rs.getLong("id"),
        rs.getString("order_no"),
        rs.getString("title"),
        rs.getLong("store_id"),
        rs.getString("store_name"),
        rs.getString("display_status"),
        rs.getString("payment_status"),
        rs.getString("fulfillment_status"),
        rs.getString("order_type"));
  }

  private ReviewRow mapReview(ResultSet rs, int rowNum) throws SQLException {
    Timestamp repliedAt = rs.getTimestamp("replied_at");
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new ReviewRow(
        rs.getLong("id"),
        rs.getLong("order_id"),
        rs.getString("order_no"),
        rs.getString("title"),
        rs.getLong("store_id"),
        rs.getString("store_name"),
        rs.getInt("rating"),
        rs.getString("content"),
        rs.getString("labels"),
        rs.getString("status"),
        rs.getBoolean("replied"),
        rs.getString("reply_content"),
        repliedAt == null ? null : repliedAt.toLocalDateTime(),
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  record OrderReviewRow(Long id, String orderNo, String orderTitle, Long storeId, String storeName, String displayStatus, String paymentStatus, String fulfillmentStatus, String orderType) {}

  record ReviewRow(Long id, Long orderId, String orderNo, String orderTitle, Long storeId, String storeName, int rating, String content, String labels, String status, boolean replied, String replyContent, LocalDateTime repliedAt, LocalDateTime createdAt) {}
}
