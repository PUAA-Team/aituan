package com.aituan.identity.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AdminRepository {
  private final JdbcTemplate jdbcTemplate;

  AdminRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  Optional<AdminProfileRow> findAdminProfile(long accountId) {
    List<AdminProfileRow> rows = jdbcTemplate.query(
        """
        select id, account_no, account_type, login_name, phone, email, status, created_at, last_login_at
        from iam_account
        where id = ? and account_type = 'ADMIN' and is_deleted = 0
        limit 1
        """,
        this::mapAdminProfile,
        accountId);
    return rows.stream().findFirst();
  }

  long countUsers(String keyword) {
    StringBuilder sql = new StringBuilder("""
        select count(1)
        from iam_account a
        join user_profile p on p.account_id = a.id
        where a.account_type = 'USER' and a.is_deleted = 0 and p.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and (p.nickname like ? or a.phone like ? or a.email like ?)");
      String like = "%" + keyword.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return count == null ? 0 : count;
  }

  List<UserRow> listUsers(String keyword, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select a.id as account_id, p.id as user_id, p.nickname, p.avatar_url, a.phone, a.email, a.status, a.created_at,
               (select count(1) from user_address ua where ua.user_id = p.id and ua.is_deleted = 0) as address_count
        from iam_account a
        join user_profile p on p.account_id = a.id
        where a.account_type = 'USER' and a.is_deleted = 0 and p.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and (p.nickname like ? or a.phone like ? or a.email like ?)");
      String like = "%" + keyword.trim() + "%";
      params.add(like);
      params.add(like);
      params.add(like);
    }
    sql.append(" order by a.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapUser, params.toArray());
  }

  Optional<UserRow> findUser(long accountId) {
    List<UserRow> rows = jdbcTemplate.query(
        """
        select a.id as account_id, p.id as user_id, p.nickname, p.avatar_url, a.phone, a.email, a.status, a.created_at,
               (select count(1) from user_address ua where ua.user_id = p.id and ua.is_deleted = 0) as address_count
        from iam_account a
        join user_profile p on p.account_id = a.id
        where a.id = ? and a.account_type = 'USER' and a.is_deleted = 0 and p.is_deleted = 0
        limit 1
        """,
        this::mapUser,
        accountId);
    return rows.stream().findFirst();
  }

  void updateUser(long accountId, AdminUserUpdateRequest request, String status) {
    jdbcTemplate.update(
        """
        update iam_account
        set phone = ?, email = ?, status = ?, updated_at = current_timestamp
        where id = ? and account_type = 'USER' and is_deleted = 0
        """,
        clean(request.phone()),
        clean(request.email()),
        status,
        accountId);
    jdbcTemplate.update(
        """
        update user_profile
        set nickname = ?, avatar_url = coalesce(?, avatar_url), status = ?, updated_at = current_timestamp
        where account_id = ? and is_deleted = 0
        """,
        request.nickname().trim(),
        clean(request.avatarUrl()),
        status,
        accountId);
  }

  void updateUserStatus(long accountId, String status) {
    jdbcTemplate.update("update iam_account set status = ?, updated_at = current_timestamp where id = ? and account_type = 'USER' and is_deleted = 0", status, accountId);
    jdbcTemplate.update("update user_profile set status = ?, updated_at = current_timestamp where account_id = ? and is_deleted = 0", status, accountId);
  }

  private AdminProfileRow mapAdminProfile(ResultSet rs, int rowNum) throws SQLException {
    return new AdminProfileRow(
        rs.getLong("id"),
        rs.getString("account_no"),
        rs.getString("account_type"),
        rs.getString("login_name"),
        rs.getString("phone"),
        rs.getString("email"),
        rs.getString("status"),
        toLdt(rs.getTimestamp("created_at")),
        toLdt(rs.getTimestamp("last_login_at")));
  }

  private UserRow mapUser(ResultSet rs, int rowNum) throws SQLException {
    return new UserRow(
        rs.getLong("account_id"),
        rs.getLong("user_id"),
        rs.getString("nickname"),
        rs.getString("avatar_url"),
        rs.getString("phone"),
        rs.getString("email"),
        rs.getString("status"),
        rs.getLong("address_count"),
        toLdt(rs.getTimestamp("created_at")));
  }

  private java.time.LocalDateTime toLdt(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toLocalDateTime();
  }

  private String clean(String value) {
    return value == null ? null : value.trim();
  }

  record AdminProfileRow(Long accountId, String accountNo, String accountType, String loginName, String phone, String email,
                         String status, java.time.LocalDateTime createdAt, java.time.LocalDateTime lastLoginAt) {}

  record UserRow(Long accountId, Long userId, String nickname, String avatarUrl, String phone, String email,
                 String status, Long addressCount, java.time.LocalDateTime createdAt) {}
}
