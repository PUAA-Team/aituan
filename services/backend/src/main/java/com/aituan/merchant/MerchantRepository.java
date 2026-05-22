package com.aituan.merchant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class MerchantRepository {
  private final JdbcTemplate jdbcTemplate;

  MerchantRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  Optional<MerchantRow> findByAccount(long accountId) {
    List<MerchantRow> rows = jdbcTemplate.query(
        """
        select id, account_id, merchant_name, contact_name, contact_phone, license_no, status, audit_status, settled_at
        from merchant_profile
        where account_id = ? and is_deleted = 0
        order by id
        limit 1
        """,
        this::mapMerchant,
        accountId);
    return rows.stream().findFirst();
  }

  List<StoreRow> listStores(long merchantId) {
    return jdbcTemplate.query(
        """
        select id, merchant_id, store_name, business_type, summary, address, rating, monthly_sales, avg_price,
               status, business_hours_text, tag_text, cover_url, contact_phone, announcement, updated_at
        from merchant_store
        where merchant_id = ? and is_deleted = 0
        order by case when business_type = 'takeaway' then 0 else 1 end, id
        """,
        this::mapStore,
        merchantId);
  }

  Optional<StoreRow> findStore(long merchantId, long storeId) {
    List<StoreRow> rows = jdbcTemplate.query(
        """
        select id, merchant_id, store_name, business_type, summary, address, rating, monthly_sales, avg_price,
               status, business_hours_text, tag_text, cover_url, contact_phone, announcement, updated_at
        from merchant_store
        where merchant_id = ? and id = ? and is_deleted = 0
        limit 1
        """,
        this::mapStore,
        merchantId,
        storeId);
    return rows.stream().findFirst();
  }

  void updateProfile(long merchantId, MerchantProfileUpdateRequest request) {
    jdbcTemplate.update(
        """
        update merchant_profile
        set merchant_name = ?, contact_name = ?, contact_phone = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.merchantName().trim(),
        request.contactName().trim(),
        request.contactPhone().trim(),
        merchantId);
  }

  void updateStore(long storeId, MerchantStoreUpdateRequest request) {
    jdbcTemplate.update(
        """
        update merchant_store
        set store_name = ?, summary = ?, address = ?, business_hours_text = ?, tag_text = ?, contact_phone = ?, announcement = ?, status = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.storeName().trim(),
        request.summary().trim(),
        request.address().trim(),
        clean(request.businessHoursText()),
        clean(request.tagText()),
        clean(request.contactPhone()),
        clean(request.announcement()),
        normalizeStatus(request.status()),
        storeId);
  }

  void updateStoreCover(long storeId, String coverUrl) {
    jdbcTemplate.update(
        "update merchant_store set cover_url = ?, updated_at = current_timestamp where id = ? and is_deleted = 0",
        coverUrl,
        storeId);
  }

  private MerchantRow mapMerchant(ResultSet rs, int rowNum) throws SQLException {
    Timestamp settledAt = rs.getTimestamp("settled_at");
    return new MerchantRow(
        rs.getLong("id"),
        rs.getLong("account_id"),
        rs.getString("merchant_name"),
        rs.getString("contact_name"),
        rs.getString("contact_phone"),
        rs.getString("license_no"),
        rs.getString("status"),
        rs.getString("audit_status"),
        settledAt == null ? null : settledAt.toLocalDateTime());
  }

  private StoreRow mapStore(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new StoreRow(
        rs.getLong("id"),
        rs.getLong("merchant_id"),
        rs.getString("store_name"),
        rs.getString("business_type"),
        rs.getString("summary"),
        rs.getString("address"),
        rs.getBigDecimal("rating"),
        rs.getInt("monthly_sales"),
        rs.getBigDecimal("avg_price"),
        rs.getString("status"),
        rs.getString("business_hours_text"),
        rs.getString("tag_text"),
        rs.getString("cover_url"),
        rs.getString("contact_phone"),
        rs.getString("announcement"),
        updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
  }

  private String normalizeStatus(String status) {
    String value = status == null || status.isBlank() ? "open" : status.trim().toLowerCase();
    return switch (value) {
      case "open", "closed", "休息", "营业" -> value.equals("休息") ? "closed" : value.equals("营业") ? "open" : value;
      default -> "open";
    };
  }

  record MerchantRow(Long id, Long accountId, String merchantName, String contactName, String contactPhone, String licenseNo, String status, String auditStatus, LocalDateTime settledAt) {}

  record StoreRow(Long id, Long merchantId, String storeName, String businessType, String summary, String address, java.math.BigDecimal rating, int monthlySales, java.math.BigDecimal avgPrice, String status, String businessHoursText, String tagText, String coverUrl, String contactPhone, String announcement, LocalDateTime updatedAt) {}
}
