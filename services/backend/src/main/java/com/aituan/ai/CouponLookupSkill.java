package com.aituan.ai;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class CouponLookupSkill implements AiSkill {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
  private final JdbcTemplate jdbcTemplate;

  CouponLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "coupon_lookup";
  }

  @Override
  public String description() {
    return "读取用户未使用优惠券，辅助回答优惠券、红包、满减和活动问题";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    String text = context.normalizedMessage();
    if (!containsAny(text, "优惠券", "红包", "满减", "活动", "券", "折扣")) {
      return Optional.empty();
    }
    List<CouponRow> coupons = jdbcTemplate.query(
        """
        select uc.id, ct.name, uc.face_value_snapshot, uc.threshold_snapshot, uc.expire_at
        from user_coupon uc
        join coupon_template ct on ct.id = uc.template_id and ct.is_deleted = 0
        where uc.user_id = ? and uc.status = 'unused' and uc.is_deleted = 0
        order by uc.expire_at asc, uc.id desc
        limit 3
        """,
        this::mapCoupon,
        context.currentUser().userId());
    if (coupons.isEmpty()) {
      return Optional.of(new AiSkillResult(
          name(),
          "优惠券查询",
          "当前账号没有未使用优惠券。可引导用户去领券中心查看可领取活动。",
          List.of(),
          List.of(new AiAssistantAction("去领券", null, "/coupon/claim", java.util.Map.of()))));
    }
    StringBuilder summary = new StringBuilder("用户可用优惠券：");
    for (CouponRow coupon : coupons) {
      String expire = coupon.expireAt() == null ? "有效期未知" : DATE_FORMATTER.format(coupon.expireAt().toLocalDateTime());
      summary.append("\n- ")
          .append(coupon.name())
          .append("，满 ")
          .append(coupon.threshold())
          .append(" 减 ")
          .append(coupon.faceValue())
          .append("，")
          .append(expire)
          .append(" 到期");
    }
    return Optional.of(new AiSkillResult(
        name(),
        "优惠券查询",
        summary.toString(),
        List.of(new AiAssistantCard("coupon", "我的优惠券", "已查询到可用券，可进入列表查看适用门店和订单门槛。", "查看优惠券", "/coupon/list", java.util.Map.of())),
        List.of(new AiAssistantAction("查看优惠券", null, "/coupon/list", java.util.Map.of()))));
  }

  private CouponRow mapCoupon(ResultSet rs, int rowNum) throws SQLException {
    return new CouponRow(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getBigDecimal("face_value_snapshot"),
        rs.getBigDecimal("threshold_snapshot"),
        rs.getTimestamp("expire_at"));
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  record CouponRow(long id, String name, java.math.BigDecimal faceValue, java.math.BigDecimal threshold, Timestamp expireAt) {}
}
