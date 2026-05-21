package com.aituan.auth;

import com.aituan.common.enums.AccountType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AuthRepository {
  private final JdbcTemplate jdbcTemplate;

  AuthRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  Optional<AccountRow> findAccountByLogin(String account) {
    List<AccountRow> rows = jdbcTemplate.query(
        """
        select id, account_no, account_type, login_name, phone, email, password_hash, status
        from iam_account
        where is_deleted = 0 and (phone = ? or email = ? or login_name = ?)
        order by id desc
        limit 1
        """,
        this::mapAccount,
        account, account, account);
    return rows.stream().findFirst();
  }

  Optional<AccountRow> findAccountByPhone(String phone) {
    List<AccountRow> rows = jdbcTemplate.query(
        "select id, account_no, account_type, login_name, phone, email, password_hash, status from iam_account where is_deleted = 0 and phone = ? limit 1",
        this::mapAccount,
        phone);
    return rows.stream().findFirst();
  }

  Optional<AccountRow> findAccountByEmail(String email) {
    List<AccountRow> rows = jdbcTemplate.query(
        "select id, account_no, account_type, login_name, phone, email, password_hash, status from iam_account where is_deleted = 0 and email = ? limit 1",
        this::mapAccount,
        email);
    return rows.stream().findFirst();
  }

  Optional<AccountRow> findAccountById(Long id) {
    List<AccountRow> rows = jdbcTemplate.query(
        "select id, account_no, account_type, login_name, phone, email, password_hash, status from iam_account where id = ? and is_deleted = 0 limit 1",
        this::mapAccount,
        id);
    return rows.stream().findFirst();
  }

  Optional<UserProfileRow> findProfileByAccountId(Long accountId) {
    List<UserProfileRow> rows = jdbcTemplate.query(
        """
        select p.id, p.account_id, p.nickname, p.avatar_url, p.member_level_name, p.growth_value, a.phone, a.email
        from user_profile p
        join iam_account a on a.id = p.account_id
        where p.account_id = ? and p.is_deleted = 0 and a.is_deleted = 0
        limit 1
        """,
        this::mapProfile,
        accountId);
    return rows.stream().findFirst();
  }

  boolean existsPhone(String phone) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(1) from iam_account where phone = ? and is_deleted = 0",
        Integer.class,
        phone);
    return count != null && count > 0;
  }

  boolean existsEmail(String email) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(1) from iam_account where email = ? and is_deleted = 0",
        Integer.class,
        email);
    return count != null && count > 0;
  }

  boolean existsCode(String target, String scene, String code) {
    Integer count = jdbcTemplate.queryForObject(
        """
        select count(1) from iam_verification_code
        where target = ? and scene = ? and code = ? and used_at is null and expire_at > current_timestamp
        """,
        Integer.class,
        target,
        scene,
        code);
    return count != null && count > 0;
  }

  VerificationCodeRow insertCode(String target, String scene, String code, LocalDateTime expireAt) {
    jdbcTemplate.update(
        "insert into iam_verification_code(target, scene, code, expire_at, status) values (?, ?, ?, ?, 'unused')",
        target,
        scene,
        code,
        Timestamp.valueOf(expireAt));
    Long id = jdbcTemplate.queryForObject("select max(id) from iam_verification_code", Long.class);
    return new VerificationCodeRow(id, target, scene, code, expireAt, null, "unused");
  }

  Optional<VerificationCodeRow> findValidCode(String target, String scene, String code) {
    List<VerificationCodeRow> rows = jdbcTemplate.query(
        """
        select id, target, scene, code, expire_at, used_at, status
        from iam_verification_code
        where target = ? and scene = ? and code = ? and used_at is null and expire_at > current_timestamp
        order by id desc
        limit 1
        """,
        this::mapCode,
        target,
        scene,
        code);
    return rows.stream().findFirst();
  }

  void markCodeUsed(Long id) {
    jdbcTemplate.update(
        "update iam_verification_code set used_at = current_timestamp, status = 'used' where id = ?",
        id);
  }

  Long insertAccount(String accountNo, String accountType, String loginName, String phone, String email, String passwordHash) {
    jdbcTemplate.update(
        """
        insert into iam_account(account_no, account_type, login_name, phone, email, password_hash, status)
        values (?, ?, ?, ?, ?, ?, 'normal')
        """,
        accountNo,
        accountType,
        loginName,
        phone,
        email,
        passwordHash);
    return jdbcTemplate.queryForObject("select max(id) from iam_account", Long.class);
  }

  void insertAccountRole(Long accountId, Long roleId) {
    jdbcTemplate.update(
        "insert into iam_account_role(account_id, role_id) values (?, ?) on duplicate key update role_id = values(role_id)",
        accountId,
        roleId);
  }

  Long insertProfile(Long accountId, String nickname) {
    jdbcTemplate.update(
        """
        insert into user_profile(account_id, nickname, avatar_url, register_source, member_level_name, growth_value, status)
        values (?, ?, '', 'app', '普通会员', 0, 'normal')
        """,
        accountId,
        nickname);
    return jdbcTemplate.queryForObject("select max(id) from user_profile", Long.class);
  }

  void updatePassword(Long accountId, String passwordHash) {
    jdbcTemplate.update(
        "update iam_account set password_hash = ?, updated_at = current_timestamp where id = ?",
        passwordHash,
        accountId);
  }

  void updateLastLogin(Long accountId, String loginIp) {
    jdbcTemplate.update(
        "update iam_account set last_login_at = current_timestamp, last_login_ip = ?, updated_at = current_timestamp where id = ?",
        loginIp,
        accountId);
  }

  private AccountRow mapAccount(ResultSet rs, int rowNum) throws SQLException {
    return new AccountRow(
        rs.getLong("id"),
        rs.getString("account_no"),
        AccountType.valueOf(rs.getString("account_type")),
        rs.getString("login_name"),
        rs.getString("phone"),
        rs.getString("email"),
        rs.getString("password_hash"),
        rs.getString("status"));
  }

  private UserProfileRow mapProfile(ResultSet rs, int rowNum) throws SQLException {
    return new UserProfileRow(
        rs.getLong("id"),
        rs.getLong("account_id"),
        rs.getString("nickname"),
        rs.getString("avatar_url"),
        rs.getString("member_level_name"),
        rs.getInt("growth_value"),
        rs.getString("phone"),
        rs.getString("email"));
  }

  private VerificationCodeRow mapCode(ResultSet rs, int rowNum) throws SQLException {
    Timestamp expireAt = rs.getTimestamp("expire_at");
    Timestamp usedAt = rs.getTimestamp("used_at");
    return new VerificationCodeRow(
        rs.getLong("id"),
        rs.getString("target"),
        rs.getString("scene"),
        rs.getString("code"),
        expireAt == null ? null : expireAt.toLocalDateTime(),
        usedAt == null ? null : usedAt.toLocalDateTime(),
        rs.getString("status"));
  }

  record AccountRow(Long id, String accountNo, AccountType accountType, String loginName, String phone, String email, String passwordHash, String status) {}

  record UserProfileRow(Long id, Long accountId, String nickname, String avatarUrl, String memberLevelName, int growthValue, String phone, String email) {}

  record VerificationCodeRow(Long id, String target, String scene, String code, LocalDateTime expireAt, LocalDateTime usedAt, String status) {}
}
