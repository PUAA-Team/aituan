package com.aituan.merchant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
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
               status, business_hours_text, tag_text, cover_url, contact_phone, announcement, longitude, latitude, updated_at
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
               status, business_hours_text, tag_text, cover_url, contact_phone, announcement, longitude, latitude, updated_at
        from merchant_store
        where merchant_id = ? and id = ? and is_deleted = 0
        limit 1
        """,
        this::mapStore,
        merchantId,
        storeId);
    return rows.stream().findFirst();
  }

  long insertApplication(String applicationNo, MerchantApplicationSubmitRequest request, String businessType) {
    jdbcTemplate.update(
        """
        insert into merchant_application(application_no, merchant_name, contact_name, contact_phone, business_type, store_name, address, status, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, 'pending', current_timestamp, current_timestamp)
        """,
        applicationNo,
        request.merchantName().trim(),
        request.contactName().trim(),
        request.contactPhone().trim(),
        businessType,
        request.storeName().trim(),
        request.address().trim());
    return jdbcTemplate.queryForObject("select max(id) from merchant_application where application_no = ?", Long.class, applicationNo);
  }

  Optional<ApplicationRow> findApplication(long applicationId) {
    List<ApplicationRow> rows = jdbcTemplate.query(
        """
        select id, application_no, account_id, merchant_name, contact_name, contact_phone, business_type, store_name, address,
               status, audit_remark, submitted_at, audited_at
        from merchant_application
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapApplication,
        applicationId);
    return rows.stream().findFirst();
  }

  List<MaterialRow> listMaterialsByMerchant(long merchantId) {
    return jdbcTemplate.query(
        """
        select id, merchant_id, application_id, material_type, material_name, file_url, status, reject_reason, submitted_at, audited_at
        from merchant_certification_material
        where merchant_id = ? and is_deleted = 0
        order by submitted_at desc, id desc
        """,
        this::mapMaterial,
        merchantId);
  }

  Optional<MaterialRow> findMaterial(long id) {
    List<MaterialRow> rows = jdbcTemplate.query(
        """
        select id, merchant_id, application_id, material_type, material_name, file_url, status, reject_reason, submitted_at, audited_at
        from merchant_certification_material
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapMaterial,
        id);
    return rows.stream().findFirst();
  }

  long insertMaterial(long merchantId, String materialType, String materialName, String fileUrl) {
    jdbcTemplate.update(
        """
        insert into merchant_certification_material(merchant_id, material_type, material_name, file_url, status, created_at, updated_at)
        values (?, ?, ?, ?, 'pending', current_timestamp, current_timestamp)
        """,
        merchantId,
        materialType,
        materialName,
        fileUrl);
    return jdbcTemplate.queryForObject("select max(id) from merchant_certification_material where merchant_id = ?", Long.class, merchantId);
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

  void updateStore(long storeId, MerchantStoreUpdateRequest request, BigDecimal longitude, BigDecimal latitude) {
    jdbcTemplate.update(
        """
        update merchant_store
        set store_name = ?, summary = ?, address = ?, business_hours_text = ?, tag_text = ?, contact_phone = ?, announcement = ?, status = ?, longitude = ?, latitude = ?, updated_at = current_timestamp
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
        longitude,
        latitude,
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
        rs.getBigDecimal("longitude"),
        rs.getBigDecimal("latitude"),
        updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private ApplicationRow mapApplication(ResultSet rs, int rowNum) throws SQLException {
    Timestamp submittedAt = rs.getTimestamp("submitted_at");
    Timestamp auditedAt = rs.getTimestamp("audited_at");
    return new ApplicationRow(
        rs.getLong("id"),
        rs.getString("application_no"),
        rs.getObject("account_id", Long.class),
        rs.getString("merchant_name"),
        rs.getString("contact_name"),
        rs.getString("contact_phone"),
        rs.getString("business_type"),
        rs.getString("store_name"),
        rs.getString("address"),
        rs.getString("status"),
        rs.getString("audit_remark"),
        submittedAt == null ? null : submittedAt.toLocalDateTime(),
        auditedAt == null ? null : auditedAt.toLocalDateTime());
  }

  private MaterialRow mapMaterial(ResultSet rs, int rowNum) throws SQLException {
    Timestamp submittedAt = rs.getTimestamp("submitted_at");
    Timestamp auditedAt = rs.getTimestamp("audited_at");
    return new MaterialRow(
        rs.getLong("id"),
        rs.getObject("merchant_id", Long.class),
        rs.getObject("application_id", Long.class),
        rs.getString("material_type"),
        rs.getString("material_name"),
        rs.getString("file_url"),
        rs.getString("status"),
        rs.getString("reject_reason"),
        submittedAt == null ? null : submittedAt.toLocalDateTime(),
        auditedAt == null ? null : auditedAt.toLocalDateTime());
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

  record StoreRow(Long id, Long merchantId, String storeName, String businessType, String summary, String address, BigDecimal rating, int monthlySales, BigDecimal avgPrice, String status, String businessHoursText, String tagText, String coverUrl, String contactPhone, String announcement, BigDecimal longitude, BigDecimal latitude, LocalDateTime updatedAt) {}

  record ApplicationRow(Long id, String applicationNo, Long accountId, String merchantName, String contactName, String contactPhone, String businessType, String storeName, String address, String status, String auditRemark, LocalDateTime submittedAt, LocalDateTime auditedAt) {}

  record MaterialRow(Long id, Long merchantId, Long applicationId, String materialType, String materialName, String fileUrl, String status, String rejectReason, LocalDateTime submittedAt, LocalDateTime auditedAt) {}
}
