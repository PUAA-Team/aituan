package com.aituan.coupon;

import static com.aituan.common.jdbc.JdbcGeneratedKeys.insertAndReturnId;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CouponRepository {
  private final JdbcTemplate jdbcTemplate;

  CouponRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  Optional<CouponTemplateRow> findTemplate(long id) {
    List<CouponTemplateRow> rows = jdbcTemplate.query(templateSelect() + " where id = ? and is_deleted = 0 limit 1",
        this::mapTemplate, id);
    return rows.stream().findFirst();
  }

  Optional<CouponTemplateRow> findTemplateForUpdate(long id) {
    List<CouponTemplateRow> rows = jdbcTemplate.query(templateSelect() + " where id = ? and is_deleted = 0 limit 1 for update",
        this::mapTemplate, id);
    return rows.stream().findFirst();
  }

  List<CouponTemplateRow> listAllTemplates() {
    return jdbcTemplate.query(templateSelect() + " where is_deleted = 0 order by id desc", this::mapTemplate);
  }

  // 可领取：启用、未结束、仍有库存；会员周券只自动发放，不出现在手动领取列表。
  List<CouponTemplateRow> listClaimableTemplates() {
    return jdbcTemplate.query(
        templateSelect() + """
         where status = 'enabled' and is_deleted = 0
           and business_scope <> 'member_weekly'
           and (valid_kind = 'relative' or valid_end is null or valid_end >= current_timestamp)
           and (total_qty = 0 or issued_qty < total_qty)
         order by id asc
        """,
        this::mapTemplate);
  }

  Long insertTemplate(CouponTemplateUpsertRequest r, int totalQty, int perUserLimit, String scope, String status) {
    return insertAndReturnId(
        jdbcTemplate,
        """
        insert into coupon_template(name, type, face_value, threshold_amount, business_scope, valid_kind, valid_start, valid_end, valid_days, total_qty, issued_qty, per_user_limit, status)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
        """,
        r.name().trim(), r.type(), r.faceValue(), r.thresholdAmount() == null ? BigDecimal.ZERO : r.thresholdAmount(),
        scope, r.validKind(), toTs(r.validStart()), toTs(r.validEnd()), r.validDays(), totalQty, perUserLimit, status);
  }

  void updateTemplate(long id, CouponTemplateUpsertRequest r, int totalQty, int perUserLimit, String scope, String status) {
    jdbcTemplate.update(
        """
        update coupon_template
        set name = ?, type = ?, face_value = ?, threshold_amount = ?, business_scope = ?, valid_kind = ?, valid_start = ?, valid_end = ?, valid_days = ?, total_qty = ?, per_user_limit = ?, status = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        r.name().trim(), r.type(), r.faceValue(), r.thresholdAmount() == null ? BigDecimal.ZERO : r.thresholdAmount(),
        scope, r.validKind(), toTs(r.validStart()), toTs(r.validEnd()), r.validDays(), totalQty, perUserLimit, status, id);
  }

  // 把已过期但仍为 unused 的券改为 expired
  void expireOverdue(long userId) {
    jdbcTemplate.update(
        """
        update user_coupon set status = 'expired', updated_at = current_timestamp
        where user_id = ? and status = 'unused' and expire_at < current_timestamp and is_deleted = 0
        """,
        userId);
  }

  long countUserCouponsByTemplate(long userId, long templateId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from user_coupon where user_id = ? and template_id = ? and is_deleted = 0",
        Long.class, userId, templateId);
    return count == null ? 0 : count;
  }

  Long insertUserCoupon(long userId, CouponTemplateRow t, LocalDateTime expireAt) {
    return insertAndReturnId(
        jdbcTemplate,
        """
        insert into user_coupon(template_id, user_id, expire_at, type_snapshot, face_value_snapshot, threshold_snapshot)
        values (?, ?, ?, ?, ?, ?)
        """,
        t.id(), userId, Timestamp.valueOf(expireAt), t.type(), t.faceValue(),
        t.thresholdAmount() == null ? BigDecimal.ZERO : t.thresholdAmount());
  }

  int incrementIssuedIfAvailable(long templateId) {
    return jdbcTemplate.update(
        """
        update coupon_template
        set issued_qty = issued_qty + 1, updated_at = current_timestamp
        where id = ? and is_deleted = 0 and (total_qty = 0 or issued_qty < total_qty)
        """,
        templateId);
  }

  Optional<String> findCurrentLevelCode(long userId) {
    List<String> rows = jdbcTemplate.query(
        """
        select ml.level_code
        from user_profile p
        join member_level ml on ml.min_growth_value <= p.growth_value and ml.status = 'enabled' and ml.is_deleted = 0
        where p.id = ? and p.is_deleted = 0
        order by ml.min_growth_value desc, ml.id desc
        limit 1
        """,
        (rs, rowNum) -> rs.getString("level_code"),
        userId);
    return rows.stream().findFirst();
  }

  List<WeeklyCouponRuleRow> listWeeklyRules(String levelCode) {
    return jdbcTemplate.query(
        """
        select id, level_code, template_id, issue_quantity, sort_order, status
        from member_weekly_coupon_rule
        where level_code = ? and status = 'enabled' and is_deleted = 0
        order by sort_order asc, id asc
        """,
        this::mapWeeklyRule,
        levelCode);
  }

  Optional<Long> findWeeklyBatch(long userId, LocalDate weekStart) {
    List<Long> rows = jdbcTemplate.query(
        """
        select id from member_weekly_coupon_batch
        where user_id = ? and week_start_date = ? and is_deleted = 0
        limit 1
        """,
        (rs, rowNum) -> rs.getLong("id"),
        userId,
        Date.valueOf(weekStart));
    return rows.stream().findFirst();
  }

  Long insertWeeklyBatch(long userId, LocalDate weekStart, String levelCode) {
    return insertAndReturnId(
        jdbcTemplate,
        """
        insert into member_weekly_coupon_batch(user_id, week_start_date, level_code)
        values (?, ?, ?)
        """,
        userId,
        Date.valueOf(weekStart),
        levelCode);
  }

  boolean insertWeeklyIssue(long batchId, long ruleId, int seqNo) {
    try {
      jdbcTemplate.update(
          """
          insert into member_weekly_coupon_issue(batch_id, rule_id, seq_no)
          values (?, ?, ?)
          """,
          batchId,
          ruleId,
          seqNo);
      return true;
    } catch (DuplicateKeyException ignored) {
      return false;
    }
  }

  void attachWeeklyIssueCoupon(long batchId, long ruleId, int seqNo, long userCouponId) {
    jdbcTemplate.update(
        """
        update member_weekly_coupon_issue
        set user_coupon_id = ?
        where batch_id = ? and rule_id = ? and seq_no = ? and is_deleted = 0
        """,
        userCouponId,
        batchId,
        ruleId,
        seqNo);
  }

  void deleteWeeklyIssue(long batchId, long ruleId, int seqNo) {
    jdbcTemplate.update(
        """
        delete from member_weekly_coupon_issue
        where batch_id = ? and rule_id = ? and seq_no = ? and user_coupon_id is null
        """,
        batchId,
        ruleId,
        seqNo);
  }

  List<UserCouponRow> listUserCoupons(long userId, String status) {
    return jdbcTemplate.query(
        userCouponSelect() + " where uc.user_id = ? and uc.status = ? and uc.is_deleted = 0 order by uc.id desc",
        this::mapUserCoupon, userId, status);
  }

  Optional<UserCouponRow> findUserCoupon(long userId, long userCouponId) {
    List<UserCouponRow> rows = jdbcTemplate.query(
        userCouponSelect() + " where uc.user_id = ? and uc.id = ? and uc.is_deleted = 0 limit 1",
        this::mapUserCoupon, userId, userCouponId);
    return rows.stream().findFirst();
  }

  // 标记已用，返回受影响行数用于幂等校验
  int markUsed(long userCouponId, Long orderId) {
    return jdbcTemplate.update(
        """
        update user_coupon set status = 'used', used_at = current_timestamp, used_order_id = ?, updated_at = current_timestamp
        where id = ? and status = 'unused' and is_deleted = 0
        """,
        orderId, userCouponId);
  }

  void markUnused(long userCouponId) {
    jdbcTemplate.update(
        """
        update user_coupon set status = 'unused', used_at = null, used_order_id = null, updated_at = current_timestamp
        where id = ? and status = 'used' and is_deleted = 0
        """,
        userCouponId);
  }

  void markUnusedByOrder(long orderId) {
    jdbcTemplate.update(
        """
        update user_coupon set status = 'unused', used_at = null, used_order_id = null, updated_at = current_timestamp
        where used_order_id = ? and status = 'used' and is_deleted = 0
        """,
        orderId);
  }

  void insertAudit(Long actorId, String actionType, String targetType, Long targetId, String detail) {
    jdbcTemplate.update(
        """
        insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail)
        values ('admin', ?, ?, ?, ?, ?)
        """,
        actorId, actionType, targetType, targetId, detail);
  }

  private String templateSelect() {
    return """
        select id, name, type, face_value, threshold_amount, business_scope, valid_kind, valid_start, valid_end,
               valid_days, total_qty, issued_qty, per_user_limit, status
        from coupon_template
        """;
  }

  private String userCouponSelect() {
    return """
        select uc.id, uc.template_id, uc.user_id, uc.status, uc.claimed_at, uc.expire_at, uc.used_at, uc.used_order_id,
               uc.type_snapshot, uc.face_value_snapshot, uc.threshold_snapshot, ct.name as template_name
        from user_coupon uc
        left join coupon_template ct on ct.id = uc.template_id
        """;
  }

  private Timestamp toTs(LocalDateTime value) {
    return value == null ? null : Timestamp.valueOf(value);
  }

  private CouponTemplateRow mapTemplate(ResultSet rs, int rowNum) throws SQLException {
    return new CouponTemplateRow(
        rs.getLong("id"), rs.getString("name"), rs.getString("type"), rs.getBigDecimal("face_value"),
        rs.getBigDecimal("threshold_amount"), rs.getString("business_scope"), rs.getString("valid_kind"),
        toLdt(rs.getTimestamp("valid_start")), toLdt(rs.getTimestamp("valid_end")),
        rs.getObject("valid_days", Integer.class), rs.getInt("total_qty"), rs.getInt("issued_qty"),
        rs.getInt("per_user_limit"), rs.getString("status"));
  }

  private WeeklyCouponRuleRow mapWeeklyRule(ResultSet rs, int rowNum) throws SQLException {
    return new WeeklyCouponRuleRow(
        rs.getLong("id"),
        rs.getString("level_code"),
        rs.getLong("template_id"),
        rs.getInt("issue_quantity"),
        rs.getInt("sort_order"),
        rs.getString("status"));
  }

  private UserCouponRow mapUserCoupon(ResultSet rs, int rowNum) throws SQLException {
    return new UserCouponRow(
        rs.getLong("id"), rs.getLong("template_id"), rs.getLong("user_id"), rs.getString("status"),
        toLdt(rs.getTimestamp("claimed_at")), toLdt(rs.getTimestamp("expire_at")), toLdt(rs.getTimestamp("used_at")),
        rs.getObject("used_order_id", Long.class), rs.getString("type_snapshot"),
        rs.getBigDecimal("face_value_snapshot"), rs.getBigDecimal("threshold_snapshot"), rs.getString("template_name"));
  }

  private LocalDateTime toLdt(Timestamp ts) {
    return ts == null ? null : ts.toLocalDateTime();
  }

  record CouponTemplateRow(Long id, String name, String type, BigDecimal faceValue, BigDecimal thresholdAmount,
                           String businessScope, String validKind, LocalDateTime validStart, LocalDateTime validEnd,
                           Integer validDays, int totalQty, int issuedQty, int perUserLimit, String status) {}

  record WeeklyCouponRuleRow(Long id, String levelCode, Long templateId, int issueQuantity, int sortOrder, String status) {}

  record UserCouponRow(Long id, Long templateId, Long userId, String status, LocalDateTime claimedAt,
                       LocalDateTime expireAt, LocalDateTime usedAt, Long usedOrderId, String typeSnapshot,
                       BigDecimal faceValueSnapshot, BigDecimal thresholdSnapshot, String templateName) {}
}
