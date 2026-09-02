package com.aituan.identity.internal;

import static com.aituan.identity.common.JdbcGeneratedKeys.insertAndReturnId;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class InternalIdentityRepository {
  private final JdbcTemplate jdbcTemplate;

  InternalIdentityRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  Optional<UserSummaryRow> findUserSummary(long userId) {
    List<UserSummaryRow> rows = jdbcTemplate.query(
        """
        select p.id as user_id, a.id as account_id, p.nickname, p.avatar_url, a.phone, a.email, a.status, p.member_level_name, p.growth_value
        from user_profile p
        join iam_account a on a.id = p.account_id
        where p.id = ? and p.is_deleted = 0 and a.is_deleted = 0
        limit 1
        """,
        this::mapUserSummary,
        userId);
    return rows.stream().findFirst();
  }

  Optional<AddressSnapshotView> findAddressSnapshot(long userId, long addressId) {
    List<AddressSnapshotView> rows = jdbcTemplate.query(
        """
        select id, user_id, contact_name, contact_phone, province, city, district, detail_address, longitude, latitude, delivery_note
        from user_address
        where user_id = ? and id = ? and is_deleted = 0
        limit 1
        """,
        this::mapAddressSnapshot,
        userId,
        addressId);
    return rows.stream().findFirst();
  }

  long countUnreadMessages(long userId) {
    Long count = jdbcTemplate.queryForObject("select count(1) from support_station_message where user_id = ? and read_status = 'unread' and is_deleted = 0", Long.class, userId);
    return count == null ? 0 : count;
  }

  long countUsableCoupons(long userId) {
    Long count = jdbcTemplate.queryForObject("select count(1) from user_coupon where user_id = ? and status = 'unused' and expire_at >= current_timestamp and is_deleted = 0", Long.class, userId);
    return count == null ? 0 : count;
  }

  List<PreferenceSignalView> listPreferenceSignals(long userId) {
    return jdbcTemplate.query(
        """
        select favorite_type, target_id, target_name
        from user_favorite
        where user_id = ? and is_deleted = 0
        order by created_at desc, id desc
        limit 50
        """,
        (rs, rowNum) -> {
          String favoriteType = rs.getString("favorite_type");
          long targetId = rs.getLong("target_id");
          return new PreferenceSignalView(
              favoriteType,
              targetId,
              rs.getString("target_name"),
              null,
              null,
              "store".equalsIgnoreCase(favoriteType) ? targetId : null,
              "item".equalsIgnoreCase(favoriteType) ? targetId : null,
              1,
              "favorite");
        },
        userId);
  }

  Optional<UserCouponRow> findUserCoupon(long userId, long couponId) {
    List<UserCouponRow> rows = jdbcTemplate.query(
        """
        select id, user_id, status, expire_at, type_snapshot, face_value_snapshot, threshold_snapshot, used_order_id
        from user_coupon
        where id = ? and user_id = ? and is_deleted = 0
        limit 1
        """,
        this::mapUserCoupon,
        couponId,
        userId);
    return rows.stream().findFirst();
  }

  int markCouponUsed(long userId, long couponId, long orderId) {
    return jdbcTemplate.update(
        """
        update user_coupon
        set status = 'used', used_at = current_timestamp, used_order_id = ?, updated_at = current_timestamp
        where id = ? and user_id = ? and status = 'unused' and is_deleted = 0
        """,
        orderId,
        couponId,
        userId);
  }

  int releaseCoupon(long couponId) {
    return jdbcTemplate.update(
        """
        update user_coupon
        set status = 'unused', used_at = null, used_order_id = null, updated_at = current_timestamp
        where id = ? and status = 'used' and is_deleted = 0
        """,
        couponId);
  }

  boolean insertGrowthLog(long userId, String sourceType, long sourceId, int delta, String reason) {
    try {
      jdbcTemplate.update(
          """
          insert into member_growth_log(user_id, order_id, source_type, source_id, delta, reason)
          values (?, null, ?, ?, ?, ?)
          """,
          userId,
          sourceType,
          sourceId,
          delta,
          reason == null || reason.isBlank() ? "内部成长值变更" : reason.trim());
      return true;
    } catch (DuplicateKeyException ignored) {
      return false;
    }
  }

  int changeGrowth(long userId, int delta) {
    jdbcTemplate.update(
        """
        update user_profile
        set growth_value = case when growth_value + ? < 0 then 0 else growth_value + ? end,
            updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        delta,
        delta,
        userId);
    Integer growth = jdbcTemplate.queryForObject("select growth_value from user_profile where id = ? and is_deleted = 0", Integer.class, userId);
    return growth == null ? 0 : growth;
  }

  Optional<MemberLevelRow> currentLevel(int growth) {
    List<MemberLevelRow> rows = jdbcTemplate.query(
        """
        select level_code, level_name
        from member_level
        where min_growth_value <= ? and status = 'enabled' and is_deleted = 0
        order by min_growth_value desc, id desc
        limit 1
        """,
        (rs, rowNum) -> new MemberLevelRow(rs.getString("level_code"), rs.getString("level_name")),
        growth);
    return rows.stream().findFirst();
  }

  void updateMemberLevelName(long userId, String levelName) {
    jdbcTemplate.update("update user_profile set member_level_name = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", levelName, userId);
  }

  void insertStationMessage(MessagePublishRequest request, String idempotencyKey) {
    jdbcTemplate.update(
        """
        insert into support_station_message(user_id, message_type, title, content, badge_text, read_status,
                                            related_order_id, related_target_type, related_target_id, idempotency_key)
        values (?, ?, ?, ?, ?, 'unread', ?, ?, ?, ?)
        on duplicate key update idempotency_key = values(idempotency_key)
        """,
        request.userId(),
        request.type().trim(),
        request.title().trim(),
        request.content().trim(),
        clean(request.badgeText()),
        request.relatedOrderId(),
        clean(request.relatedTargetType()),
        request.relatedTargetId(),
        idempotencyKey);
  }

  Optional<AccountRow> findAccountByLogin(String loginName) {
    List<AccountRow> rows = jdbcTemplate.query(
        """
        select id, account_no, login_name, status
        from iam_account
        where login_name = ? and is_deleted = 0
        limit 1
        """,
        this::mapAccount,
        loginName);
    return rows.stream().findFirst();
  }

  long insertMerchantAccount(String loginName, String phone, String email, String passwordHash) {
    long id = insertAndReturnId(
        jdbcTemplate,
        """
        insert into iam_account(account_no, account_type, login_name, phone, email, password_hash, status)
        values (?, 'MERCHANT', ?, ?, ?, ?, 'normal')
        """,
        "M" + System.currentTimeMillis(),
        loginName,
        clean(phone),
        clean(email),
        passwordHash);
    jdbcTemplate.update("insert into iam_account_role(account_id, role_id) values (?, ?) on duplicate key update role_id = values(role_id)", id, 2L);
    return id;
  }

  int deactivateMerchantAccount(long accountId) {
    return jdbcTemplate.update("update iam_account set status = 'disabled', updated_at = current_timestamp where id = ? and account_type = 'MERCHANT' and is_deleted = 0", accountId);
  }

  PlatformUserMetricsView userMetrics() {
    Long userCount = jdbcTemplate.queryForObject("select count(1) from user_profile where is_deleted = 0", Long.class);
    Long activeUserCount = jdbcTemplate.queryForObject("select count(1) from user_profile where status = 'normal' and is_deleted = 0", Long.class);
    Long memberUserCount = jdbcTemplate.queryForObject("select count(1) from user_profile where growth_value > 0 and is_deleted = 0", Long.class);
    return new PlatformUserMetricsView(nvl(userCount), nvl(activeUserCount), nvl(memberUserCount));
  }

  private UserSummaryRow mapUserSummary(ResultSet rs, int rowNum) throws SQLException {
    return new UserSummaryRow(rs.getLong("user_id"), rs.getLong("account_id"), rs.getString("nickname"), rs.getString("avatar_url"),
        rs.getString("phone"), rs.getString("email"), rs.getString("status"), rs.getString("member_level_name"), rs.getInt("growth_value"));
  }

  private AddressSnapshotView mapAddressSnapshot(ResultSet rs, int rowNum) throws SQLException {
    long addressId = rs.getLong("id");
    return new AddressSnapshotView(addressId, addressId, rs.getLong("user_id"), rs.getString("contact_name"), rs.getString("contact_phone"),
        rs.getString("province"), rs.getString("city"), rs.getString("district"), rs.getString("detail_address"),
        rs.getBigDecimal("longitude"), rs.getBigDecimal("latitude"), rs.getString("delivery_note"));
  }

  private UserCouponRow mapUserCoupon(ResultSet rs, int rowNum) throws SQLException {
    Timestamp expireAt = rs.getTimestamp("expire_at");
    return new UserCouponRow(rs.getLong("id"), rs.getLong("user_id"), rs.getString("status"),
        expireAt == null ? null : expireAt.toLocalDateTime(), rs.getString("type_snapshot"), rs.getBigDecimal("face_value_snapshot"),
        rs.getBigDecimal("threshold_snapshot"), rs.getObject("used_order_id", Long.class));
  }

  private AccountRow mapAccount(ResultSet rs, int rowNum) throws SQLException {
    return new AccountRow(rs.getLong("id"), rs.getString("account_no"), rs.getString("login_name"), rs.getString("status"));
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private long nvl(Long value) {
    return value == null ? 0 : value;
  }

  record UserSummaryRow(Long userId, Long accountId, String nickname, String avatarUrl, String phone, String email,
                        String status, String memberLevelName, int growthValue) {}

  record UserCouponRow(Long id, Long userId, String status, LocalDateTime expireAt, String type, BigDecimal faceValue,
                       BigDecimal thresholdAmount, Long usedOrderId) {}

  record MemberLevelRow(String levelCode, String levelName) {}

  record AccountRow(Long accountId, String accountNo, String loginName, String status) {}
}
