package com.aituan.account;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AccountRepository {
  private final JdbcTemplate jdbcTemplate;

  AccountRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  Optional<AccountProfileRow> findProfile(long accountId) {
    List<AccountProfileRow> rows = jdbcTemplate.query(
        """
        select a.id as account_id, p.id as user_id, p.nickname, p.avatar_url, a.phone, a.email, p.member_level_name, p.growth_value
        from iam_account a
        join user_profile p on p.account_id = a.id
        where a.id = ? and a.is_deleted = 0 and p.is_deleted = 0
        limit 1
        """,
        this::mapProfile,
        accountId);
    return rows.stream().findFirst();
  }

  void updateProfile(long userId, String nickname, String avatarUrl) {
    jdbcTemplate.update(
        """
        update user_profile
        set nickname = ?, avatar_url = coalesce(?, avatar_url), updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        nickname,
        avatarUrl,
        userId);
  }

  void updateAvatar(long userId, String avatarUrl) {
    jdbcTemplate.update(
        "update user_profile set avatar_url = ?, updated_at = current_timestamp where id = ? and is_deleted = 0",
        avatarUrl,
        userId);
  }

  Optional<AccountPasswordRow> findPasswordByAccountId(long accountId) {
    List<AccountPasswordRow> rows = jdbcTemplate.query(
        "select id, password_hash from iam_account where id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> new AccountPasswordRow(rs.getLong("id"), rs.getString("password_hash")),
        accountId);
    return rows.stream().findFirst();
  }

  void updatePassword(long accountId, String passwordHash) {
    jdbcTemplate.update(
        "update iam_account set password_hash = ?, updated_at = current_timestamp where id = ? and is_deleted = 0",
        passwordHash,
        accountId);
  }

  long countOrders(long userId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from order_main where user_id = ? and is_deleted = 0",
        Long.class,
        userId);
    return count == null ? 0 : count;
  }

  long countAddresses(long userId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from user_address where user_id = ? and is_deleted = 0",
        Long.class,
        userId);
    return count == null ? 0 : count;
  }

  long countFavorites(long userId, String favoriteType) {
    Long count;
    if (favoriteType == null || favoriteType.isBlank()) {
      count = jdbcTemplate.queryForObject(
          "select count(1) from user_favorite where user_id = ? and is_deleted = 0",
          Long.class,
          userId);
    } else {
      count = jdbcTemplate.queryForObject(
          "select count(1) from user_favorite where user_id = ? and favorite_type = ? and is_deleted = 0",
          Long.class,
          userId,
          favoriteType);
    }
    return count == null ? 0 : count;
  }

  long countUnreadMessages(long userId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from support_station_message where user_id = ? and read_status = 'unread' and is_deleted = 0",
        Long.class,
        userId);
    return count == null ? 0 : count;
  }

  List<AddressRow> listAddresses(long userId) {
    return jdbcTemplate.query(
        """
        select id, user_id, contact_name, contact_phone, province, city, district, detail_address, longitude, latitude,
               tag_name, is_default, delivery_note, created_at
        from user_address
        where user_id = ? and is_deleted = 0
        order by is_default desc, id desc
        """,
        this::mapAddress,
        userId);
  }

  Optional<AddressRow> findAddress(long userId, long addressId) {
    List<AddressRow> rows = jdbcTemplate.query(
        """
        select id, user_id, contact_name, contact_phone, province, city, district, detail_address, longitude, latitude,
               tag_name, is_default, delivery_note, created_at
        from user_address
        where user_id = ? and id = ? and is_deleted = 0
        limit 1
        """,
        this::mapAddress,
        userId,
        addressId);
    return rows.stream().findFirst();
  }

  Long insertAddress(long userId, AddressUpsertRequest request, boolean isDefault) {
    jdbcTemplate.update(
        """
        insert into user_address(user_id, contact_name, contact_phone, province, city, district, detail_address, longitude, latitude, tag_name, is_default, delivery_note)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        userId,
        request.contactName(),
        request.contactPhone(),
        request.province(),
        request.city(),
        request.district(),
        request.detailAddress(),
        request.longitude(),
        request.latitude(),
        request.tagName(),
        isDefault ? 1 : 0,
        request.deliveryNote());
    return jdbcTemplate.queryForObject("select max(id) from user_address", Long.class);
  }

  void updateAddress(long userId, long addressId, AddressUpsertRequest request, boolean isDefault) {
    jdbcTemplate.update(
        """
        update user_address
        set contact_name = ?, contact_phone = ?, province = ?, city = ?, district = ?, detail_address = ?, longitude = ?, latitude = ?, tag_name = ?, is_default = ?, delivery_note = ?, updated_at = current_timestamp
        where user_id = ? and id = ? and is_deleted = 0
        """,
        request.contactName(),
        request.contactPhone(),
        request.province(),
        request.city(),
        request.district(),
        request.detailAddress(),
        request.longitude(),
        request.latitude(),
        request.tagName(),
        isDefault ? 1 : 0,
        request.deliveryNote(),
        userId,
        addressId);
  }

  void clearDefaultAddresses(long userId) {
    jdbcTemplate.update(
        "update user_address set is_default = 0, updated_at = current_timestamp where user_id = ? and is_deleted = 0",
        userId);
  }

  void markAddressDefault(long userId, long addressId) {
    jdbcTemplate.update(
        "update user_address set is_default = 1, updated_at = current_timestamp where user_id = ? and id = ? and is_deleted = 0",
        userId,
        addressId);
  }

  void deleteAddress(long userId, long addressId) {
    jdbcTemplate.update(
        "update user_address set is_deleted = 1, updated_at = current_timestamp where user_id = ? and id = ? and is_deleted = 0",
        userId,
        addressId);
  }

  Long insertFavorite(long userId, FavoriteUpsertRequest request) {
    jdbcTemplate.update(
        """
        insert into user_favorite(user_id, favorite_type, target_id, target_name, cover_url, subtitle)
        values (?, ?, ?, ?, ?, ?)
        on duplicate key update target_name = values(target_name), cover_url = values(cover_url), subtitle = values(subtitle), is_deleted = 0, created_at = current_timestamp
        """,
        userId,
        request.favoriteType(),
        request.targetId(),
        request.targetName(),
        request.coverUrl(),
        request.subtitle());
    return jdbcTemplate.queryForObject("select max(id) from user_favorite", Long.class);
  }

  void deleteFavorite(long userId, String favoriteType, long targetId) {
    jdbcTemplate.update(
        "update user_favorite set is_deleted = 1 where user_id = ? and favorite_type = ? and target_id = ?",
        userId,
        favoriteType,
        targetId);
  }

  List<FavoriteRow> listFavorites(long userId, String favoriteType, int offset, int limit) {
    if (favoriteType == null || favoriteType.isBlank()) {
      return jdbcTemplate.query(
          """
          select id, user_id, favorite_type, target_id, target_name, cover_url, subtitle, created_at
          from user_favorite
          where user_id = ? and is_deleted = 0
          order by created_at desc, id desc
          limit ? offset ?
          """,
          this::mapFavorite,
          userId,
          limit,
          offset);
    }
    return jdbcTemplate.query(
        """
        select id, user_id, favorite_type, target_id, target_name, cover_url, subtitle, created_at
        from user_favorite
        where user_id = ? and favorite_type = ? and is_deleted = 0
        order by created_at desc, id desc
        limit ? offset ?
        """,
        this::mapFavorite,
        userId,
        favoriteType,
        limit,
        offset);
  }

  Optional<FavoriteRow> findFavorite(long userId, String favoriteType, long targetId) {
    List<FavoriteRow> rows = jdbcTemplate.query(
        """
        select id, user_id, favorite_type, target_id, target_name, cover_url, subtitle, created_at
        from user_favorite
        where user_id = ? and favorite_type = ? and target_id = ? and is_deleted = 0
        limit 1
        """,
        this::mapFavorite,
        userId,
        favoriteType,
        targetId);
    return rows.stream().findFirst();
  }

  private AccountProfileRow mapProfile(ResultSet rs, int rowNum) throws SQLException {
    return new AccountProfileRow(
        rs.getLong("account_id"),
        rs.getLong("user_id"),
        rs.getString("nickname"),
        rs.getString("avatar_url"),
        rs.getString("phone"),
        rs.getString("email"),
        rs.getString("member_level_name"),
        rs.getInt("growth_value"));
  }

  private AddressRow mapAddress(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new AddressRow(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getString("contact_name"),
        rs.getString("contact_phone"),
        rs.getString("province"),
        rs.getString("city"),
        rs.getString("district"),
        rs.getString("detail_address"),
        rs.getBigDecimal("longitude") == null ? null : rs.getBigDecimal("longitude").doubleValue(),
        rs.getBigDecimal("latitude") == null ? null : rs.getBigDecimal("latitude").doubleValue(),
        rs.getString("tag_name"),
        rs.getBoolean("is_default"),
        rs.getString("delivery_note"),
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  private FavoriteRow mapFavorite(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new FavoriteRow(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getString("favorite_type"),
        rs.getLong("target_id"),
        rs.getString("target_name"),
        rs.getString("cover_url"),
        rs.getString("subtitle"),
        createdAt == null ? null : createdAt.toLocalDateTime());
  }

  record AccountProfileRow(Long accountId, Long userId, String nickname, String avatarUrl, String phone, String email, String memberLevelName, int growthValue) {}

  record AccountPasswordRow(Long accountId, String passwordHash) {}

  record AddressRow(Long id, Long userId, String contactName, String contactPhone, String province, String city, String district, String detailAddress, Double longitude, Double latitude, String tagName, boolean isDefault, String deliveryNote, LocalDateTime createdAt) {}

  record FavoriteRow(Long id, Long userId, String favoriteType, Long targetId, String targetName, String coverUrl, String subtitle, LocalDateTime createdAt) {}
}
