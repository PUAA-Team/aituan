package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.money;
import static com.aituan.ai.AiSkillSupport.params;
import static com.aituan.ai.AiSkillSupport.shouldRun;

import java.math.BigDecimal;
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
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM-dd");
  private static final List<String> WORDS = List.of(
      "优惠", "优惠券", "红包", "满减", "活动", "券", "折扣", "领券", "可用券", "过期");

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
    return "读取当前用户真实优惠券、可领券和使用门槛";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<CouponRow> coupons = jdbcTemplate.query(
        """
        select uc.id, ct.name, uc.status, uc.face_value_snapshot, uc.threshold_snapshot, uc.expire_at,
               uc.used_order_id, ct.business_scope
        from user_coupon uc
        join coupon_template ct on ct.id = uc.template_id and ct.is_deleted = 0
        where uc.user_id = ? and uc.is_deleted = 0
        order by case uc.status when 'unused' then 0 when 'used' then 1 else 2 end, uc.expire_at asc, uc.id desc
        limit 6
        """,
        this::mapCoupon,
        context.currentUser().userId());
    List<CouponTemplateRow> available = jdbcTemplate.query(
        """
        select id, name, face_value, threshold_amount, business_scope, valid_kind, valid_end, total_qty, issued_qty
        from coupon_template
        where status = 'enabled' and is_deleted = 0
        order by face_value desc, id desc
        limit 4
        """,
        this::mapTemplate);
    StringBuilder summary = new StringBuilder();
    if (coupons.isEmpty()) {
      summary.append("当前账号暂无已领取优惠券。");
    } else {
      summary.append("当前账号优惠券：");
      for (CouponRow coupon : coupons) {
        summary.append("\n- ").append(coupon.name()).append("，")
            .append(statusLabel(coupon.status())).append("，满 ")
            .append(money(coupon.threshold())).append(" 减 ")
            .append(money(coupon.faceValue())).append("，")
            .append(coupon.expireAt() == null ? "有效期未知" : DATE.format(coupon.expireAt().toLocalDateTime()) + " 到期")
            .append("，范围 ").append(coupon.businessScope());
      }
    }
    if (!available.isEmpty()) {
      summary.append("\n可领取活动：");
      for (CouponTemplateRow row : available) {
        summary.append("\n- ").append(row.name()).append("，满 ")
            .append(money(row.threshold())).append(" 减 ").append(money(row.faceValue()))
            .append("，已发 ").append(row.issuedQty()).append("/").append(row.totalQty());
      }
    }
    return Optional.of(new AiSkillResult(
        name(),
        "优惠券查询",
        summary.toString(),
        List.of(new AiAssistantCard("coupon", "优惠券中心", "已查询我的券和可领取活动。", "查看优惠券", "/coupon/list", params())),
        List.of(
            new AiAssistantAction("我的优惠券", null, "/coupon/list", params()),
            new AiAssistantAction("去领券", null, "/coupon/claim", params()))));
  }

  private String statusLabel(String status) {
    return switch (status == null ? "" : status) {
      case "unused" -> "未使用";
      case "used" -> "已使用";
      case "expired" -> "已过期";
      default -> status == null || status.isBlank() ? "状态未知" : status;
    };
  }

  private CouponRow mapCoupon(ResultSet rs, int rowNum) throws SQLException {
    return new CouponRow(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getString("status"),
        rs.getBigDecimal("face_value_snapshot"),
        rs.getBigDecimal("threshold_snapshot"),
        rs.getTimestamp("expire_at"),
        rs.getLong("used_order_id"),
        rs.getString("business_scope"));
  }

  private CouponTemplateRow mapTemplate(ResultSet rs, int rowNum) throws SQLException {
    return new CouponTemplateRow(
        rs.getLong("id"),
        rs.getString("name"),
        rs.getBigDecimal("face_value"),
        rs.getBigDecimal("threshold_amount"),
        rs.getString("business_scope"),
        rs.getString("valid_kind"),
        rs.getTimestamp("valid_end"),
        rs.getInt("total_qty"),
        rs.getInt("issued_qty"));
  }

  record CouponRow(long id, String name, String status, BigDecimal faceValue, BigDecimal threshold,
                   Timestamp expireAt, Long usedOrderId, String businessScope) {}
  record CouponTemplateRow(long id, String name, BigDecimal faceValue, BigDecimal threshold, String businessScope,
                           String validKind, Timestamp validEnd, int totalQty, int issuedQty) {}
}
