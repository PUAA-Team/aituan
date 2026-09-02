package com.aituan.engagementplatform.ai;

import static com.aituan.engagementplatform.ai.AiSkillSupport.limit;
import static com.aituan.engagementplatform.ai.AiSkillSupport.params;
import static com.aituan.engagementplatform.ai.AiSkillSupport.shouldRun;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class ReviewLookupSkill implements AiSkill {
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm");
  private static final List<String> WORDS = List.of("评价", "评分", "评论", "点赞", "举报", "商家回复", "我的评价", "图片", "证据");

  private final JdbcTemplate jdbcTemplate;

  ReviewLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "review_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户真实评价、图片、商家回复、点赞和举报状态";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<ReviewRow> reviews = jdbcTemplate.query(
        """
        select r.id, r.order_id, r.store_name, r.rating, r.content, r.image_urls, r.helpful_count, r.reported_count,
               r.status, r.replied, rr.reply_content, r.created_at,
               (select count(1) from review_report rp where rp.review_id = r.id and rp.reporter_user_id = ? and rp.is_deleted = 0) as reported_by_me,
               (select count(1) from review_helpful h where h.review_id = r.id and h.user_id = ? and h.is_deleted = 0) as helpful_by_me
        from review_record r
        left join review_reply rr on rr.review_id = r.id and rr.is_deleted = 0
        where r.user_id = ? and r.is_deleted = 0
        order by r.created_at desc, r.id desc
        limit 5
        """,
        this::mapReview,
        context.currentUser().userId(),
        context.currentUser().userId(),
        context.currentUser().userId());
    if (reviews.isEmpty()) {
      return Optional.of(new AiSkillResult(
          name(),
          "评价查询",
          "当前账号还没有评价记录。完成订单后可进入订单详情发布评价。",
          List.of(),
          List.of(new AiAssistantAction("查看订单", null, "/orders", params()))));
    }
    StringBuilder summary = new StringBuilder("当前用户最近评价：");
    for (ReviewRow review : reviews) {
      summary.append("\n- ").append(review.storeName()).append("，")
          .append(review.rating()).append("星，").append(statusLabel(review.status()))
          .append("，图片 ").append(imageCount(review.imageUrls())).append(" 张")
          .append("，有用 ").append(review.helpfulCount())
          .append(review.helpfulByMe() ? "（已点赞）" : "")
          .append("，举报 ").append(review.reportedCount())
          .append(review.reportedByMe() ? "（我已举报）" : "")
          .append(review.replied() ? "，商家回复：" + limit(review.replyContent(), 40) : "，商家未回复")
          .append(review.createdAt() == null ? "" : "，" + review.createdAt());
    }
    List<AiAssistantCard> cards = reviews.stream()
        .map(review -> new AiAssistantCard(
            "review",
            review.storeName() + " · " + review.rating() + "星",
            limit(review.content(), 60),
            "查看评价",
            "/review/detail",
            params("reviewId", review.id())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "评价查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("我的评价", null, "/review/my", params()))));
  }

  private int imageCount(String urls) {
    if (urls == null || urls.isBlank()) return 0;
    return (int) java.util.Arrays.stream(urls.split(",")).filter(s -> !s.isBlank()).count();
  }

  private String statusLabel(String status) {
    return switch (status == null ? "" : status) {
      case "published" -> "已发布";
      case "hidden" -> "已隐藏";
      case "pending" -> "待审核";
      default -> status == null || status.isBlank() ? "状态未知" : status;
    };
  }

  private ReviewRow mapReview(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new ReviewRow(
        rs.getLong("id"),
        rs.getLong("order_id"),
        rs.getString("store_name"),
        rs.getInt("rating"),
        rs.getString("content"),
        rs.getString("image_urls"),
        rs.getInt("helpful_count"),
        rs.getInt("reported_count"),
        rs.getString("status"),
        rs.getInt("replied") == 1,
        rs.getString("reply_content"),
        createdAt == null ? null : TIME.format(createdAt.toLocalDateTime()),
        rs.getInt("reported_by_me") > 0,
        rs.getInt("helpful_by_me") > 0);
  }

  record ReviewRow(long id, long orderId, String storeName, int rating, String content, String imageUrls,
                   int helpfulCount, int reportedCount, String status, boolean replied, String replyContent,
                   String createdAt, boolean reportedByMe, boolean helpfulByMe) {}
}
