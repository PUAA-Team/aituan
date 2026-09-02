package com.aituan.merchantcatalog;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class MerchantCatalogRepository {
  private final JdbcTemplate jdbcTemplate;

  MerchantCatalogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  long count(String sql, Object... params) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class, params);
    return value == null ? 0 : value;
  }

  Optional<MerchantRow> findMerchant(long merchantId) {
    List<MerchantRow> rows = jdbcTemplate.query(
        merchantSelectSql() + " where m.id = ? and m.is_deleted = 0 limit 1",
        this::mapMerchant,
        merchantId);
    return rows.stream().findFirst();
  }

  Optional<MerchantRow> findMerchantByAccount(long accountId) {
    List<MerchantRow> rows = jdbcTemplate.query(
        merchantSelectSql() + " where m.account_id = ? and m.is_deleted = 0 order by m.id limit 1",
        this::mapMerchant,
        accountId);
    return rows.stream().findFirst();
  }

  List<MerchantRow> listMerchants(String keyword, int offset, int limit) {
    StringBuilder sql = new StringBuilder(merchantSelectSql() + " where m.is_deleted = 0");
    List<Object> params = new ArrayList<>();
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and m.merchant_name like ?");
      params.add("%" + keyword.trim() + "%");
    }
    sql.append(" order by m.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapMerchant, params.toArray());
  }

  long countMerchants(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return count("select count(1) from merchant_profile where is_deleted = 0");
    }
    return count("select count(1) from merchant_profile where is_deleted = 0 and merchant_name like ?", "%" + keyword.trim() + "%");
  }

  long insertMerchant(String merchantNo, AdminMerchantUpsertRequest request, String status, String auditStatus) {
    jdbcTemplate.update(
        """
        insert into merchant_profile(merchant_no, account_id, merchant_name, contact_name, contact_phone, license_no, status, audit_status, settled_at, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, case when ? = 'approved' then current_timestamp else null end, current_timestamp, current_timestamp)
        """,
        merchantNo,
        request.accountId(),
        request.merchantName().trim(),
        clean(request.contactName()),
        clean(request.contactPhone()),
        clean(request.licenseNo()),
        status,
        auditStatus,
        auditStatus);
    return jdbcTemplate.queryForObject("select max(id) from merchant_profile", Long.class);
  }

  void updateMerchant(long merchantId, AdminMerchantUpsertRequest request, String status, String auditStatus) {
    jdbcTemplate.update(
        """
        update merchant_profile
        set account_id = ?, merchant_name = ?, contact_name = ?, contact_phone = ?, license_no = ?, status = ?, audit_status = ?,
            settled_at = case when ? = 'approved' and settled_at is null then current_timestamp else settled_at end,
            updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.accountId(),
        request.merchantName().trim(),
        clean(request.contactName()),
        clean(request.contactPhone()),
        clean(request.licenseNo()),
        status,
        auditStatus,
        auditStatus,
        merchantId);
  }

  void updateMerchantStatus(long merchantId, String status) {
    jdbcTemplate.update("update merchant_profile set status = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", status, merchantId);
  }

  List<StoreRow> listMerchantStores(long merchantId) {
    return jdbcTemplate.query(
        storeSelectSql() + " where s.merchant_id = ? and s.is_deleted = 0 order by case when s.business_type = 'takeaway' then 0 else 1 end, s.id",
        this::mapStore,
        merchantId);
  }

  Optional<StoreRow> findStore(long storeId) {
    List<StoreRow> rows = jdbcTemplate.query(
        storeSelectSql() + " where s.id = ? and s.is_deleted = 0 limit 1",
        this::mapStore,
        storeId);
    return rows.stream().findFirst();
  }

  Optional<StoreRow> findStore(long merchantId, long storeId) {
    List<StoreRow> rows = jdbcTemplate.query(
        storeSelectSql() + " where s.merchant_id = ? and s.id = ? and s.is_deleted = 0 limit 1",
        this::mapStore,
        merchantId,
        storeId);
    return rows.stream().findFirst();
  }

  List<StoreAdminRow> listStores(Long merchantId, String businessType, String status, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select s.id, s.merchant_id, m.merchant_name, s.store_name, s.business_type, s.summary, s.address,
               s.status, s.business_hours_text, s.tag_text, s.cover_url, s.contact_phone, s.announcement, s.updated_at
        from merchant_store s
        join merchant_profile m on m.id = s.merchant_id and m.is_deleted = 0
        where s.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (merchantId != null) {
      sql.append(" and s.merchant_id = ?");
      params.add(merchantId);
    }
    if (businessType != null && !businessType.isBlank()) {
      sql.append(" and s.business_type = ?");
      params.add(businessType.trim().toLowerCase());
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and s.status = ?");
      params.add(status.trim().toLowerCase());
    }
    sql.append(" order by s.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapStoreAdmin, params.toArray());
  }

  long countStores(Long merchantId, String businessType, String status) {
    StringBuilder sql = new StringBuilder("select count(1) from merchant_store where is_deleted = 0");
    List<Object> params = new ArrayList<>();
    if (merchantId != null) {
      sql.append(" and merchant_id = ?");
      params.add(merchantId);
    }
    if (businessType != null && !businessType.isBlank()) {
      sql.append(" and business_type = ?");
      params.add(businessType.trim().toLowerCase());
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status = ?");
      params.add(status.trim().toLowerCase());
    }
    return count(sql.toString(), params.toArray());
  }

  Optional<StoreAdminRow> findStoreAdmin(long storeId) {
    List<StoreAdminRow> rows = jdbcTemplate.query(
        """
        select s.id, s.merchant_id, m.merchant_name, s.store_name, s.business_type, s.summary, s.address,
               s.status, s.business_hours_text, s.tag_text, s.cover_url, s.contact_phone, s.announcement, s.updated_at
        from merchant_store s
        join merchant_profile m on m.id = s.merchant_id and m.is_deleted = 0
        where s.id = ? and s.is_deleted = 0
        limit 1
        """,
        this::mapStoreAdmin,
        storeId);
    return rows.stream().findFirst();
  }

  long insertStore(AdminStoreUpsertRequest request, String businessType, String status) {
    jdbcTemplate.update(
        """
        insert into merchant_store(merchant_id, store_name, business_type, summary, address, status, business_hours_text, tag_text, cover_url, contact_phone, announcement, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        request.merchantId(),
        request.storeName().trim(),
        businessType,
        request.summary().trim(),
        request.address().trim(),
        status,
        clean(request.businessHoursText()),
        clean(request.tagText()),
        clean(request.coverUrl()),
        clean(request.contactPhone()),
        clean(request.announcement()));
    return jdbcTemplate.queryForObject("select max(id) from merchant_store", Long.class);
  }

  void updateStore(long storeId, AdminStoreUpsertRequest request, String businessType, String status) {
    jdbcTemplate.update(
        """
        update merchant_store
        set merchant_id = ?, store_name = ?, business_type = ?, summary = ?, address = ?, status = ?, business_hours_text = ?,
            tag_text = ?, cover_url = ?, contact_phone = ?, announcement = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.merchantId(),
        request.storeName().trim(),
        businessType,
        request.summary().trim(),
        request.address().trim(),
        status,
        clean(request.businessHoursText()),
        clean(request.tagText()),
        clean(request.coverUrl()),
        clean(request.contactPhone()),
        clean(request.announcement()),
        storeId);
  }

  void updateMerchantStore(long storeId, MerchantStoreUpdateRequest request, BigDecimal longitude, BigDecimal latitude) {
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
        normalizeStoreStatusLenient(request.status()),
        longitude,
        latitude,
        storeId);
  }

  void updateStoreStatus(long storeId, String status) {
    jdbcTemplate.update("update merchant_store set status = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", status, storeId);
  }

  void updateStoreCover(long storeId, String coverUrl) {
    jdbcTemplate.update("update merchant_store set cover_url = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", coverUrl, storeId);
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

  List<ApplicationRow> listApplications(String status, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select id, application_no, account_id, merchant_name, contact_name, contact_phone, business_type, store_name, address,
               status, audit_remark, submitted_at, audited_by, audited_at
        from merchant_application
        where is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status = ?");
      params.add(status.trim().toLowerCase());
    }
    sql.append(" order by submitted_at desc, id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapApplication, params.toArray());
  }

  long countApplications(String status) {
    if (status == null || status.isBlank()) return count("select count(1) from merchant_application where is_deleted = 0");
    return count("select count(1) from merchant_application where is_deleted = 0 and status = ?", status.trim().toLowerCase());
  }

  Optional<ApplicationRow> findApplication(long id) {
    List<ApplicationRow> rows = jdbcTemplate.query(
        """
        select id, application_no, account_id, merchant_name, contact_name, contact_phone, business_type, store_name, address,
               status, audit_remark, submitted_at, audited_by, audited_at
        from merchant_application
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapApplication,
        id);
    return rows.stream().findFirst();
  }

  long insertMerchantFromApplication(String merchantNo, long accountId, ApplicationRow application) {
    jdbcTemplate.update(
        """
        insert into merchant_profile(merchant_no, account_id, merchant_name, contact_name, contact_phone, license_no, status, audit_status, settled_at, created_at, updated_at)
        values (?, ?, ?, ?, ?, '', 'normal', 'approved', current_timestamp, current_timestamp, current_timestamp)
        """,
        merchantNo,
        accountId,
        application.merchantName(),
        application.contactName(),
        application.contactPhone());
    return jdbcTemplate.queryForObject("select max(id) from merchant_profile where merchant_no = ?", Long.class, merchantNo);
  }

  long insertStoreFromApplication(long merchantId, ApplicationRow application) {
    jdbcTemplate.update(
        """
        insert into merchant_store(merchant_id, store_name, business_type, summary, address, status, business_hours_text, tag_text, contact_phone, announcement, created_at, updated_at)
        values (?, ?, ?, ?, ?, 'open', '09:00-22:00', '', ?, '', current_timestamp, current_timestamp)
        """,
        merchantId,
        application.storeName(),
        application.businessType(),
        application.merchantName() + " 已通过入驻审核",
        application.address(),
        application.contactPhone());
    return jdbcTemplate.queryForObject("select max(id) from merchant_store where merchant_id = ?", Long.class, merchantId);
  }

  void updateApplicationAudit(long id, String status, Long accountId, Long auditorId, String remark) {
    jdbcTemplate.update(
        """
        update merchant_application
        set status = ?, account_id = ?, audit_remark = ?, audited_by = ?, audited_at = current_timestamp, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        status,
        accountId,
        remark,
        auditorId,
        id);
  }

  void insertMerchantAuditLog(String targetType, long targetId, String action, String result, String remark, Long operatedBy) {
    jdbcTemplate.update(
        "insert into merchant_audit_log(target_type, target_id, action, result, remark, operated_by) values (?, ?, ?, ?, ?, ?)",
        targetType,
        targetId,
        action,
        result,
        remark,
        operatedBy);
  }

  List<CertificationMaterialRow> listMaterialsByMerchant(long merchantId) {
    return jdbcTemplate.query(
        """
        select id, merchant_id, application_id, material_type, material_name, file_url, status, reject_reason, submitted_at, audited_by, audited_at
        from merchant_certification_material
        where merchant_id = ? and is_deleted = 0
        order by submitted_at desc, id desc
        """,
        this::mapMaterial,
        merchantId);
  }

  List<CertificationMaterialRow> listCertificationMaterials(String status, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select cm.id, cm.merchant_id, cm.application_id, cm.material_type, cm.material_name, cm.file_url, cm.status, cm.reject_reason,
               cm.submitted_at, cm.audited_by, cm.audited_at, coalesce(m.merchant_name, '') as merchant_name
        from merchant_certification_material cm
        left join merchant_profile m on m.id = cm.merchant_id and m.is_deleted = 0
        where cm.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and cm.status = ?");
      params.add(status.trim().toLowerCase());
    }
    sql.append(" order by cm.submitted_at desc, cm.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapMaterial, params.toArray());
  }

  long countCertificationMaterials(String status) {
    if (status == null || status.isBlank()) return count("select count(1) from merchant_certification_material where is_deleted = 0");
    return count("select count(1) from merchant_certification_material where is_deleted = 0 and status = ?", status.trim().toLowerCase());
  }

  Optional<CertificationMaterialRow> findMaterial(long id) {
    List<CertificationMaterialRow> rows = jdbcTemplate.query(
        """
        select cm.id, cm.merchant_id, cm.application_id, cm.material_type, cm.material_name, cm.file_url, cm.status, cm.reject_reason,
               cm.submitted_at, cm.audited_by, cm.audited_at, coalesce(m.merchant_name, '') as merchant_name
        from merchant_certification_material cm
        left join merchant_profile m on m.id = cm.merchant_id and m.is_deleted = 0
        where cm.id = ? and cm.is_deleted = 0
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

  void updateCertificationMaterialStatus(long id, String status, String rejectReason, Long auditorId) {
    jdbcTemplate.update(
        """
        update merchant_certification_material
        set status = ?, reject_reason = ?, audited_by = ?, audited_at = current_timestamp, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        status,
        rejectReason,
        auditorId,
        id);
  }

  Optional<Long> firstStoreByAccount(long accountId) {
    List<Long> rows = jdbcTemplate.query(
        """
        select s.id
        from merchant_store s
        join merchant_profile m on m.id = s.merchant_id
        where m.account_id = ? and s.is_deleted = 0 and m.is_deleted = 0
        order by case when s.business_type = 'takeaway' then 0 else 1 end, s.id
        limit 1
        """,
        (rs, rowNum) -> rs.getLong("id"),
        accountId);
    return rows.stream().findFirst();
  }

  boolean isStoreOwnedByAccount(long storeId, long accountId) {
    return count(
        """
        select count(1)
        from merchant_store s
        join merchant_profile m on m.id = s.merchant_id
        where s.id = ? and m.account_id = ? and s.is_deleted = 0 and m.is_deleted = 0
        """,
        storeId,
        accountId) > 0;
  }

  List<CatalogItemRow> listMerchantItems(long accountId, String businessType, String status, String keyword) {
    StringBuilder sql = new StringBuilder(itemSelectSql() + """
        from catalog_item i
        join merchant_store s on s.id = i.store_id
        join merchant_profile m on m.id = s.merchant_id
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where m.account_id = ? and i.is_deleted = 0 and s.is_deleted = 0 and m.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    params.add(accountId);
    appendItemFilters(sql, params, businessType, status, keyword, null);
    sql.append(" order by i.updated_at desc, i.id desc");
    return jdbcTemplate.query(sql.toString(), this::mapCatalogItem, params.toArray());
  }

  List<CatalogItemRow> listAdminItems(Long storeId, String businessType, String status, String keyword, int offset, int limit) {
    StringBuilder sql = new StringBuilder(itemSelectSql() + """
        from catalog_item i
        join merchant_store s on s.id = i.store_id
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where i.is_deleted = 0 and s.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    appendItemFilters(sql, params, businessType, status, keyword, storeId);
    sql.append(" order by i.updated_at desc, i.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapCatalogItem, params.toArray());
  }

  long countAdminItems(Long storeId, String businessType, String status, String keyword) {
    StringBuilder sql = new StringBuilder("""
        select count(1)
        from catalog_item i
        join merchant_store s on s.id = i.store_id
        where i.is_deleted = 0 and s.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    appendItemFilters(sql, params, businessType, status, keyword, storeId);
    return count(sql.toString(), params.toArray());
  }

  Optional<CatalogItemRow> findCatalogItem(long itemId) {
    List<CatalogItemRow> rows = jdbcTemplate.query(
        itemSelectSql() + """
        from catalog_item i
        join merchant_store s on s.id = i.store_id
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where i.id = ? and i.is_deleted = 0
        limit 1
        """,
        this::mapCatalogItem,
        itemId);
    return rows.stream().findFirst();
  }

  Optional<SkuRow> findDefaultSku(long itemId) {
    List<SkuRow> rows = jdbcTemplate.query(
        """
        select id, item_id, sku_name, price, stock, status
        from catalog_sku
        where item_id = ? and sku_name = '默认' and is_deleted = 0
        order by id
        limit 1
        """,
        this::mapSku,
        itemId);
    return rows.stream().findFirst();
  }

  Optional<SkuRow> findSku(long skuId) {
    List<SkuRow> rows = jdbcTemplate.query(
        """
        select id, item_id, sku_name, price, stock, status
        from catalog_sku
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapSku,
        skuId);
    return rows.stream().findFirst();
  }

  long insertItem(long storeId, CatalogItemUpsertRequest request, long categoryId, String status) {
    jdbcTemplate.update(
        """
        insert into catalog_item(store_id, business_type, category_id, item_name, subtitle, price, original_price, cover_url, tag_text, status, item_kind,
                                 business_attributes, usage_rules, refund_policy, notice, validity_days, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        storeId,
        request.businessType().trim().toLowerCase(),
        categoryId,
        request.title().trim(),
        clean(request.subtitle()),
        request.price(),
        request.originalPrice(),
        clean(request.coverUrl()),
        clean(request.tagText()),
        status,
        "takeaway".equalsIgnoreCase(request.businessType()) ? "food" : "service",
        clean(request.businessAttributes()),
        clean(request.usageRules()),
        clean(request.refundPolicy()),
        clean(request.notice()),
        request.validityDays() == null ? 90 : request.validityDays());
    Long itemId = jdbcTemplate.queryForObject("select max(id) from catalog_item where store_id = ?", Long.class, storeId);
    upsertDefaultSku(itemId, request.price(), request.stock() == null ? 999 : request.stock(), status);
    return itemId;
  }

  void updateItem(long itemId, CatalogItemUpsertRequest request, long categoryId, String status) {
    jdbcTemplate.update(
        """
        update catalog_item
        set business_type = ?, category_id = ?, item_name = ?, subtitle = ?, price = ?, original_price = ?, cover_url = coalesce(?, cover_url), tag_text = ?, status = ?,
            business_attributes = ?, usage_rules = ?, refund_policy = ?, notice = ?, validity_days = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.businessType().trim().toLowerCase(),
        categoryId,
        request.title().trim(),
        clean(request.subtitle()),
        request.price(),
        request.originalPrice(),
        blankToNull(request.coverUrl()),
        clean(request.tagText()),
        status,
        clean(request.businessAttributes()),
        clean(request.usageRules()),
        clean(request.refundPolicy()),
        clean(request.notice()),
        request.validityDays() == null ? 90 : request.validityDays(),
        itemId);
    upsertDefaultSku(itemId, request.price(), request.stock() == null ? 999 : request.stock(), status);
  }

  void updateItemStatus(long itemId, String status) {
    jdbcTemplate.update("update catalog_item set status = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", status, itemId);
    jdbcTemplate.update("update catalog_sku set status = ?, updated_at = current_timestamp where item_id = ? and sku_name = '默认' and is_deleted = 0", status, itemId);
  }

  void updateItemCover(long itemId, String coverUrl) {
    jdbcTemplate.update("update catalog_item set cover_url = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", coverUrl, itemId);
  }

  List<CategoryRow> listCategories(Long storeId, String businessType) {
    StringBuilder sql = new StringBuilder("""
        select id, store_id, business_type, category_code, category_name, sort_order, status, updated_at
        from catalog_category
        where is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (storeId != null) {
      sql.append(" and store_id = ?");
      params.add(storeId);
    }
    if (businessType != null && !businessType.isBlank()) {
      sql.append(" and business_type = ?");
      params.add(businessType.trim().toLowerCase());
    }
    sql.append(" order by sort_order, id");
    return jdbcTemplate.query(sql.toString(), this::mapCategory, params.toArray());
  }

  Optional<CategoryRow> findCategory(long categoryId) {
    List<CategoryRow> rows = jdbcTemplate.query(
        """
        select id, store_id, business_type, category_code, category_name, sort_order, status, updated_at
        from catalog_category
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapCategory,
        categoryId);
    return rows.stream().findFirst();
  }

  long insertCategory(long storeId, CatalogCategoryUpsertRequest request) {
    String code = request.businessType().trim().toLowerCase() + "_" + storeId + "_" + System.currentTimeMillis();
    jdbcTemplate.update(
        """
        insert into catalog_category(parent_id, store_id, category_code, category_name, business_type, category_level, sort_order, status, created_at, updated_at)
        values (null, ?, ?, ?, ?, 'store_item', ?, ?, current_timestamp, current_timestamp)
        """,
        storeId,
        code,
        request.categoryName().trim(),
        request.businessType().trim().toLowerCase(),
        request.sortOrder() == null ? 0 : request.sortOrder(),
        normalizeCategoryStatus(request.status()));
    return jdbcTemplate.queryForObject("select max(id) from catalog_category where store_id = ?", Long.class, storeId);
  }

  void updateCategory(long categoryId, CatalogCategoryUpsertRequest request) {
    jdbcTemplate.update(
        """
        update catalog_category
        set category_name = ?, business_type = ?, sort_order = ?, status = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.categoryName().trim(),
        request.businessType().trim().toLowerCase(),
        request.sortOrder() == null ? 0 : request.sortOrder(),
        normalizeCategoryStatus(request.status()),
        categoryId);
  }

  long countItemsByCategory(long categoryId) {
    return count("select count(1) from catalog_item where category_id = ? and is_deleted = 0", categoryId);
  }

  void softDeleteCategory(long categoryId) {
    jdbcTemplate.update("update catalog_category set is_deleted = 1, status = 'disabled', updated_at = current_timestamp where id = ? and is_deleted = 0", categoryId);
  }

  long getOrCreateDefaultCategory(long storeId, String businessType) {
    List<Long> rows = jdbcTemplate.query(
        """
        select id from catalog_category
        where store_id = ? and business_type = ? and is_deleted = 0
        order by sort_order, id
        limit 1
        """,
        (rs, rowNum) -> rs.getLong("id"),
        storeId,
        businessType);
    if (!rows.isEmpty()) return rows.get(0);
    String code = businessType + "_" + storeId + "_default";
    jdbcTemplate.update(
        """
        insert into catalog_category(parent_id, store_id, category_code, category_name, business_type, category_level, sort_order, status, created_at, updated_at)
        values (null, ?, ?, '默认分类', ?, 'store_item', 0, 'normal', current_timestamp, current_timestamp)
        """,
        storeId,
        code,
        businessType);
    return jdbcTemplate.queryForObject("select id from catalog_category where category_code = ?", Long.class, code);
  }

  List<ModuleRow> listModules() {
    return jdbcTemplate.query(
        """
        select id, category_code, category_name, business_type, sort_order
        from catalog_category
        where category_level = 'module' and is_deleted = 0 and status = 'normal'
        order by sort_order, id
        """,
        this::mapModule);
  }

  List<ItemRow> listRecommendations(int offset, int limit) {
    return jdbcTemplate.query(itemCardSelectSql() + """
        from catalog_item i
        join catalog_category c on c.id = i.category_id and c.is_deleted = 0 and c.status = 'normal'
        join merchant_store s on s.id = i.store_id and s.is_deleted = 0 and s.status = 'open'
        left join member_recommend_config r on r.item_id = i.id and r.scene = 'home_recommend' and r.status = 'normal'
        left join (
          select item_id, sum(case when status = 'on_sale' then stock else 0 end) as stock, max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku where is_deleted = 0 group by item_id
        ) sku on sku.item_id = i.id
        where i.is_deleted = 0 and i.status = 'on_sale'
        order by case when r.id is null then 1 else 0 end, coalesce(r.sort_order, 999999), i.sales_count desc, s.monthly_sales desc, s.rating desc, i.sort_order, i.id
        limit ? offset ?
        """,
        this::mapItem,
        limit,
        offset);
  }

  long countRecommendations() {
    return count(
        """
        select count(1)
        from catalog_item i
        join catalog_category c on c.id = i.category_id and c.is_deleted = 0 and c.status = 'normal'
        join merchant_store s on s.id = i.store_id and s.is_deleted = 0 and s.status = 'open'
        where i.is_deleted = 0 and i.status = 'on_sale'
        """);
  }

  List<StoreRow> listStoresByBusinessType(String businessType, int limit) {
    return jdbcTemplate.query(
        storeSelectSql() + " where s.business_type = ? and s.is_deleted = 0 and s.status = 'open' order by s.monthly_sales desc, s.rating desc, s.id limit ?",
        this::mapStore,
        businessType,
        limit);
  }

  List<ItemRow> listItemsByBusinessType(String businessType, int limit) {
    return jdbcTemplate.query(itemCardSelectSql() + """
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id, sum(case when status = 'on_sale' then stock else 0 end) as stock, max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku where is_deleted = 0 group by item_id
        ) sku on sku.item_id = i.id
        where i.business_type = ? and i.is_deleted = 0 and i.status = 'on_sale'
        order by i.sort_order, i.sales_count desc, i.id
        limit ?
        """,
        this::mapItem,
        businessType,
        limit);
  }

  Optional<ItemRow> findItem(long itemId) {
    List<ItemRow> rows = jdbcTemplate.query(itemCardSelectSql() + """
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id, sum(case when status = 'on_sale' then stock else 0 end) as stock, max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku where is_deleted = 0 group by item_id
        ) sku on sku.item_id = i.id
        where i.id = ? and i.is_deleted = 0
        limit 1
        """,
        this::mapItem,
        itemId);
    return rows.stream().findFirst();
  }

  List<CategorySimpleRow> listStoreCategories(long storeId) {
    return jdbcTemplate.query(
        """
        select id, category_name, sort_order
        from catalog_category
        where store_id = ? and is_deleted = 0 and status = 'normal'
        order by sort_order, id
        """,
        (rs, rowNum) -> new CategorySimpleRow(rs.getLong("id"), rs.getString("category_name"), rs.getInt("sort_order")),
        storeId);
  }

  List<ItemRow> listStoreItems(long storeId) {
    return jdbcTemplate.query(itemCardSelectSql() + """
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id, sum(case when status = 'on_sale' then stock else 0 end) as stock, max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku where is_deleted = 0 group by item_id
        ) sku on sku.item_id = i.id
        where i.store_id = ? and i.is_deleted = 0 and i.status = 'on_sale'
        order by c.sort_order, i.sort_order, i.id
        """,
        this::mapItem,
        storeId);
  }

  List<ItemRow> searchItems(long storeId, String keyword, int limit) {
    String normalized = keyword == null ? "" : keyword.trim();
    if (normalized.isEmpty()) return listStoreItemsForFill(storeId, List.of(), List.of(), limit);
    String like = "%" + normalized + "%";
    return jdbcTemplate.query(itemCardSelectSql() + """
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id, sum(case when status = 'on_sale' then stock else 0 end) as stock, max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku where is_deleted = 0 group by item_id
        ) sku on sku.item_id = i.id
        where i.store_id = ? and i.is_deleted = 0 and i.status = 'on_sale'
          and (i.item_name like ? or i.subtitle like ? or i.tag_text like ?)
        order by i.sort_order, i.sales_count desc, i.id
        limit ?
        """,
        this::mapItem,
        storeId,
        like,
        like,
        like,
        limit);
  }

  List<ItemRow> listStoreItemsForFill(long storeId, List<Long> excludedItemIds, List<Long> preferredCategoryIds, int limit) {
    StringBuilder sql = new StringBuilder(itemCardSelectSql() + """
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id, sum(case when status = 'on_sale' then stock else 0 end) as stock, max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku where is_deleted = 0 group by item_id
        ) sku on sku.item_id = i.id
        where i.store_id = ? and i.is_deleted = 0 and i.status = 'on_sale'
        """);
    List<Object> params = new ArrayList<>();
    params.add(storeId);
    if (excludedItemIds != null && !excludedItemIds.isEmpty()) {
      sql.append(" and i.id not in (").append(String.join(",", java.util.Collections.nCopies(excludedItemIds.size(), "?"))).append(")");
      params.addAll(excludedItemIds);
    }
    if (preferredCategoryIds != null && !preferredCategoryIds.isEmpty()) {
      sql.append(" order by case when i.category_id in (").append(String.join(",", java.util.Collections.nCopies(preferredCategoryIds.size(), "?"))).append(") then 0 else 1 end, c.sort_order, i.sort_order, i.sales_count desc, i.id");
      params.addAll(preferredCategoryIds);
    } else {
      sql.append(" order by c.sort_order, i.sort_order, i.sales_count desc, i.id");
    }
    sql.append(" limit ?");
    params.add(limit);
    return jdbcTemplate.query(sql.toString(), this::mapItem, params.toArray());
  }

  List<StoreRow> searchStores(String keyword, String businessType, int limit) {
    String normalized = keyword == null ? "" : keyword.trim();
    String normalizedType = businessType == null ? "" : businessType.trim();
    List<Object> params = new ArrayList<>();
    StringBuilder sql = new StringBuilder();
    if (normalized.isEmpty()) {
      sql.append(storeSelectSql()).append(" where s.is_deleted = 0 and s.status = 'open'");
      if (!normalizedType.isEmpty()) {
        sql.append(" and s.business_type = ?");
        params.add(normalizedType);
      }
      sql.append(" order by s.monthly_sales desc, s.rating desc, s.id limit ?");
      params.add(limit);
      return jdbcTemplate.query(sql.toString(), this::mapStore, params.toArray());
    }
    String like = "%" + normalized + "%";
    sql.append("""
        select distinct s.id, s.merchant_id, s.store_name, s.business_type, s.summary, s.address, s.distance_text,
               s.longitude, s.latitude, s.rating, s.monthly_sales, s.avg_price, s.status, s.business_hours_text, s.tag_text, s.cover_url, s.contact_phone, s.announcement, s.updated_at
        from merchant_store s
        left join catalog_item i on i.store_id = s.id and i.is_deleted = 0 and i.status = 'on_sale'
        where s.is_deleted = 0 and s.status = 'open'
        """);
    if (!normalizedType.isEmpty()) {
      sql.append(" and s.business_type = ?");
      params.add(normalizedType);
    }
    sql.append("""
          and (s.store_name like ? or s.summary like ? or s.tag_text like ? or s.address like ? or i.item_name like ? or i.subtitle like ? or i.tag_text like ?)
        order by s.monthly_sales desc, s.rating desc, s.id
        limit ?
        """);
    for (int i = 0; i < 7; i++) params.add(like);
    params.add(limit);
    return jdbcTemplate.query(sql.toString(), this::mapStore, params.toArray());
  }

  List<StoreRow> searchStoresByBusinessTypes(List<String> businessTypes, int limit) {
    if (businessTypes == null || businessTypes.isEmpty()) return searchStores("", null, limit);
    List<Object> params = new ArrayList<>(businessTypes);
    String placeholders = String.join(", ", java.util.Collections.nCopies(businessTypes.size(), "?"));
    params.add(limit);
    return jdbcTemplate.query(storeSelectSql()
            + " where s.is_deleted = 0 and s.status = 'open' and s.business_type in (" + placeholders + ") order by s.monthly_sales desc, s.rating desc, s.id limit ?",
        this::mapStore,
        params.toArray());
  }

  Optional<DeliveryRuleRow> findDeliveryRule(long storeId) {
    List<DeliveryRuleRow> rows = jdbcTemplate.query(
        """
        select delivery_fee, start_price, estimated_minutes, max_delivery_distance_km,
               package_fee_mode, package_fee_fixed, package_fee_per_item,
               distance_extra_threshold_km, distance_extra_fee, distance_extra_step_km, delivery_text
        from merchant_delivery_rule where store_id = ? and is_deleted = 0 limit 1
        """,
        this::mapDeliveryRule,
        storeId);
    return rows.stream().findFirst();
  }

  void upsertDeliveryRule(long storeId, BigDecimal deliveryFee, BigDecimal startPrice, int estimatedMinutes, BigDecimal maxDeliveryDistanceKm, String packageFeeMode, BigDecimal packageFeeFixed, BigDecimal packageFeePerItem, BigDecimal distanceExtraThresholdKm, BigDecimal distanceExtraFee, BigDecimal distanceExtraStepKm, String deliveryText) {
    int updated = jdbcTemplate.update(
        """
        update merchant_delivery_rule
        set delivery_fee = ?, start_price = ?, estimated_minutes = ?, max_delivery_distance_km = ?, package_fee_mode = ?, package_fee_fixed = ?, package_fee_per_item = ?,
            distance_extra_threshold_km = ?, distance_extra_fee = ?, distance_extra_step_km = ?, delivery_text = ?, updated_at = current_timestamp
        where store_id = ? and is_deleted = 0
        """,
        deliveryFee, startPrice, estimatedMinutes, maxDeliveryDistanceKm, packageFeeMode, packageFeeFixed, packageFeePerItem,
        distanceExtraThresholdKm, distanceExtraFee, distanceExtraStepKm, deliveryText, storeId);
    if (updated == 0) {
      jdbcTemplate.update(
          """
          insert into merchant_delivery_rule(store_id, delivery_fee, start_price, estimated_minutes, max_delivery_distance_km, package_fee_mode, package_fee_fixed, package_fee_per_item, distance_extra_threshold_km, distance_extra_fee, distance_extra_step_km, delivery_text, created_at, updated_at)
          values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
          """,
          storeId, deliveryFee, startPrice, estimatedMinutes, maxDeliveryDistanceKm, packageFeeMode, packageFeeFixed, packageFeePerItem,
          distanceExtraThresholdKm, distanceExtraFee, distanceExtraStepKm, deliveryText);
    }
  }

  Optional<TakeawaySettingRow> findTakeawaySetting(long storeId) {
    List<TakeawaySettingRow> rows = jdbcTemplate.query(
        """
        select s.id as store_id, s.store_name, coalesce(t.accept_mode, 'manual') as accept_mode
        from merchant_store s
        left join merchant_takeaway_setting t on t.store_id = s.id and t.is_deleted = 0
        where s.id = ? and s.business_type = 'takeaway' and s.is_deleted = 0
        limit 1
        """,
        (rs, rowNum) -> new TakeawaySettingRow(rs.getLong("store_id"), rs.getString("store_name"), rs.getString("accept_mode")),
        storeId);
    return rows.stream().findFirst();
  }

  void upsertTakeawaySetting(long storeId, String acceptMode, long updatedBy) {
    int updated = jdbcTemplate.update(
        """
        update merchant_takeaway_setting set accept_mode = ?, updated_by = ?, updated_at = current_timestamp, is_deleted = 0
        where store_id = ?
        """,
        acceptMode, updatedBy, storeId);
    if (updated == 0) {
      jdbcTemplate.update(
          """
          insert into merchant_takeaway_setting(store_id, accept_mode, updated_by, created_at, updated_at)
          values (?, ?, ?, current_timestamp, current_timestamp)
          """,
          storeId, acceptMode, updatedBy);
    }
  }

  List<MerchantItemRow> listTakeawayItems(long storeId, String status) {
    StringBuilder sql = new StringBuilder("""
        select i.id, i.store_id, i.item_name, i.subtitle, c.category_name, i.price, i.original_price,
               coalesce(sku.stock, 0) as stock, i.status, i.sales_count
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where i.store_id = ? and i.business_type = 'takeaway' and i.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    params.add(storeId);
    if (status != null && !status.isBlank()) {
      sql.append(" and i.status = ?");
      params.add(status);
    }
    sql.append(" order by c.sort_order, i.sort_order, i.id");
    return jdbcTemplate.query(sql.toString(), this::mapMerchantItem, params.toArray());
  }

  Optional<MerchantItemRow> findMerchantItem(long itemId) {
    List<MerchantItemRow> rows = jdbcTemplate.query(
        """
        select i.id, i.store_id, i.item_name, i.subtitle, c.category_name, i.price, i.original_price,
               coalesce(sku.stock, 0) as stock, i.status, i.sales_count
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where i.id = ? and i.business_type = 'takeaway' and i.is_deleted = 0
        limit 1
        """,
        this::mapMerchantItem,
        itemId);
    return rows.stream().findFirst();
  }

  void updateMerchantItem(long itemId, String title, String subtitle, BigDecimal price, int stock, String status) {
    jdbcTemplate.update("update catalog_item set item_name = ?, subtitle = ?, price = ?, status = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", title, subtitle, price, status, itemId);
    upsertDefaultSku(itemId, price, stock, status);
  }

  void updateMerchantItemStatus(long itemId, String status) {
    updateItemStatus(itemId, status);
  }

  Optional<InventoryIdempotencyRow> findInventoryIdempotency(String callerService, String apiAction, String idempotencyKey) {
    List<InventoryIdempotencyRow> rows = jdbcTemplate.query(
        """
        select id, status, result_summary
        from inventory_idempotency_record
        where caller_service = ? and api_action = ? and idempotency_key = ?
        limit 1
        """,
        this::mapInventoryIdempotency,
        callerService,
        apiAction,
        idempotencyKey);
    return rows.stream().findFirst();
  }

  void insertInventoryIdempotency(String callerService, String apiAction, String idempotencyKey, String requestSummary, String resultSummary, String status) {
    jdbcTemplate.update(
        """
        insert into inventory_idempotency_record(caller_service, api_action, idempotency_key, request_summary, result_summary, status, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        callerService,
        apiAction,
        idempotencyKey,
        requestSummary,
        resultSummary,
        status);
  }

  int decreaseSkuStock(long skuId, int quantity) {
    return jdbcTemplate.update(
        """
        update catalog_sku
        set stock = stock - ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0 and status = 'on_sale' and stock >= ?
        """,
        quantity,
        skuId,
        quantity);
  }

  void increaseSkuStock(long skuId, int quantity) {
    jdbcTemplate.update("update catalog_sku set stock = stock + ?, updated_at = current_timestamp where id = ? and is_deleted = 0", quantity, skuId);
  }

  private void upsertDefaultSku(long itemId, BigDecimal price, int stock, String status) {
    int updated = jdbcTemplate.update(
        """
        update catalog_sku
        set price = ?, stock = ?, status = ?, updated_at = current_timestamp
        where item_id = ? and sku_name = '默认' and is_deleted = 0
        """,
        price,
        stock,
        status,
        itemId);
    if (updated == 0) {
      jdbcTemplate.update("insert into catalog_sku(item_id, sku_name, price, stock, status, created_at, updated_at) values (?, '默认', ?, ?, ?, current_timestamp, current_timestamp)", itemId, price, stock, status);
    }
  }

  private String merchantSelectSql() {
    return """
        select m.id, m.account_id, m.merchant_name, m.contact_name, m.contact_phone, m.license_no, m.status, m.audit_status, m.settled_at,
               (select count(1) from merchant_store s where s.merchant_id = m.id and s.is_deleted = 0) as store_count,
               (select count(1) from catalog_item i join merchant_store s2 on s2.id = i.store_id where s2.merchant_id = m.id and i.is_deleted = 0 and s2.is_deleted = 0) as item_count
        from merchant_profile m
        """;
  }

  private String storeSelectSql() {
    return """
        select s.id, s.merchant_id, s.store_name, s.business_type, s.summary, s.address, s.distance_text, s.longitude, s.latitude, s.rating,
               s.monthly_sales, s.avg_price, s.status, s.business_hours_text, s.tag_text, s.cover_url, s.contact_phone, s.announcement, s.updated_at
        from merchant_store s
        """;
  }

  private String itemSelectSql() {
    return """
        select i.id, i.store_id, s.store_name, i.business_type, i.category_id, c.category_name, i.item_name,
               i.subtitle, i.price, i.original_price, coalesce(sku.stock, 0) as stock, i.status,
               i.cover_url, i.tag_text, i.sales_count, i.business_attributes, i.usage_rules, i.refund_policy, i.notice, i.validity_days, i.updated_at
        """;
  }

  private String itemCardSelectSql() {
    return """
        select i.id, i.item_name, i.subtitle, i.business_type, i.category_id, c.category_name,
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name,
               s.longitude as store_longitude, s.latitude as store_latitude,
               s.rating as store_rating, s.monthly_sales as store_monthly_sales,
               i.sort_order, i.sales_count,
               coalesce(sku.stock, 0) as stock, coalesce(sku.sku_status, 'sold_out') as sku_status,
               i.business_attributes, i.usage_rules, i.refund_policy, i.notice, i.validity_days
        """;
  }

  private void appendItemFilters(StringBuilder sql, List<Object> params, String businessType, String status, String keyword, Long storeId) {
    if (storeId != null) {
      sql.append(" and i.store_id = ?");
      params.add(storeId);
    }
    if (businessType != null && !businessType.isBlank()) {
      sql.append(" and i.business_type = ?");
      params.add(businessType.trim().toLowerCase());
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and i.status = ?");
      params.add(status.trim().toLowerCase());
    }
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and (i.item_name like ? or s.store_name like ?)");
      String value = "%" + keyword.trim() + "%";
      params.add(value);
      params.add(value);
    }
  }

  private MerchantRow mapMerchant(ResultSet rs, int rowNum) throws SQLException {
    Timestamp settledAt = rs.getTimestamp("settled_at");
    return new MerchantRow(rs.getLong("id"), nullableLong(rs, "account_id"), rs.getString("merchant_name"), rs.getString("contact_name"), rs.getString("contact_phone"), rs.getString("license_no"), rs.getString("status"), rs.getString("audit_status"), rs.getLong("store_count"), rs.getLong("item_count"), settledAt == null ? null : settledAt.toLocalDateTime());
  }

  private StoreRow mapStore(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new StoreRow(rs.getLong("id"), rs.getLong("merchant_id"), rs.getString("store_name"), rs.getString("business_type"), rs.getString("summary"), rs.getString("address"), rs.getString("distance_text"), getNullableBigDecimal(rs, "longitude"), getNullableBigDecimal(rs, "latitude"), rs.getBigDecimal("rating"), rs.getInt("monthly_sales"), rs.getBigDecimal("avg_price"), rs.getString("status"), rs.getString("business_hours_text"), rs.getString("tag_text"), rs.getString("cover_url"), rs.getString("contact_phone"), rs.getString("announcement"), updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private StoreAdminRow mapStoreAdmin(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new StoreAdminRow(rs.getLong("id"), rs.getLong("merchant_id"), rs.getString("merchant_name"), rs.getString("store_name"), rs.getString("business_type"), rs.getString("summary"), rs.getString("address"), rs.getString("status"), rs.getString("business_hours_text"), rs.getString("tag_text"), rs.getString("cover_url"), rs.getString("contact_phone"), rs.getString("announcement"), updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private ApplicationRow mapApplication(ResultSet rs, int rowNum) throws SQLException {
    Timestamp submittedAt = rs.getTimestamp("submitted_at");
    Timestamp auditedAt = rs.getTimestamp("audited_at");
    return new ApplicationRow(rs.getLong("id"), rs.getString("application_no"), nullableLong(rs, "account_id"), rs.getString("merchant_name"), rs.getString("contact_name"), rs.getString("contact_phone"), rs.getString("business_type"), rs.getString("store_name"), rs.getString("address"), rs.getString("status"), rs.getString("audit_remark"), submittedAt == null ? null : submittedAt.toLocalDateTime(), nullableLong(rs, "audited_by"), auditedAt == null ? null : auditedAt.toLocalDateTime());
  }

  private CertificationMaterialRow mapMaterial(ResultSet rs, int rowNum) throws SQLException {
    Timestamp submittedAt = rs.getTimestamp("submitted_at");
    Timestamp auditedAt = rs.getTimestamp("audited_at");
    String merchantName;
    try {
      merchantName = rs.getString("merchant_name");
    } catch (SQLException ex) {
      merchantName = "";
    }
    return new CertificationMaterialRow(rs.getLong("id"), nullableLong(rs, "merchant_id"), nullableLong(rs, "application_id"), merchantName, rs.getString("material_type"), rs.getString("material_name"), rs.getString("file_url"), rs.getString("status"), rs.getString("reject_reason"), submittedAt == null ? null : submittedAt.toLocalDateTime(), nullableLong(rs, "audited_by"), auditedAt == null ? null : auditedAt.toLocalDateTime());
  }

  private CatalogItemRow mapCatalogItem(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new CatalogItemRow(rs.getLong("id"), rs.getLong("store_id"), rs.getString("store_name"), rs.getString("business_type"), rs.getLong("category_id"), rs.getString("category_name"), rs.getString("item_name"), rs.getString("subtitle"), rs.getBigDecimal("price"), rs.getBigDecimal("original_price"), rs.getInt("stock"), rs.getString("status"), rs.getString("cover_url"), rs.getString("tag_text"), rs.getInt("sales_count"), rs.getString("business_attributes"), rs.getString("usage_rules"), rs.getString("refund_policy"), rs.getString("notice"), rs.getInt("validity_days"), updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private CategoryRow mapCategory(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    long storeId = rs.getLong("store_id");
    return new CategoryRow(rs.getLong("id"), rs.wasNull() ? null : storeId, rs.getString("business_type"), rs.getString("category_code"), rs.getString("category_name"), rs.getInt("sort_order"), rs.getString("status"), updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private ModuleRow mapModule(ResultSet rs, int rowNum) throws SQLException {
    return new ModuleRow(rs.getLong("id"), rs.getString("category_code"), rs.getString("category_name"), rs.getString("business_type"), rs.getInt("sort_order"));
  }

  private ItemRow mapItem(ResultSet rs, int rowNum) throws SQLException {
    return new ItemRow(rs.getLong("id"), rs.getString("item_name"), rs.getString("subtitle"), rs.getString("business_type"), rs.getLong("category_id"), rs.getString("category_name"), rs.getBigDecimal("price"), rs.getBigDecimal("original_price"), rs.getString("cover_url"), rs.getString("tag_text"), rs.getLong("store_id"), rs.getString("store_name"), rs.getBigDecimal("store_longitude"), rs.getBigDecimal("store_latitude"), rs.getBigDecimal("store_rating"), rs.getInt("store_monthly_sales"), rs.getInt("sort_order"), rs.getInt("sales_count"), rs.getInt("stock"), rs.getString("sku_status"), rs.getString("business_attributes"), rs.getString("usage_rules"), rs.getString("refund_policy"), rs.getString("notice"), rs.getInt("validity_days"));
  }

  private SkuRow mapSku(ResultSet rs, int rowNum) throws SQLException {
    return new SkuRow(rs.getLong("id"), rs.getLong("item_id"), rs.getString("sku_name"), rs.getBigDecimal("price"), rs.getInt("stock"), rs.getString("status"));
  }

  private MerchantItemRow mapMerchantItem(ResultSet rs, int rowNum) throws SQLException {
    return new MerchantItemRow(rs.getLong("id"), rs.getLong("store_id"), rs.getString("item_name"), rs.getString("subtitle"), rs.getString("category_name"), rs.getBigDecimal("price"), rs.getBigDecimal("original_price"), rs.getInt("stock"), rs.getString("status"), rs.getInt("sales_count"));
  }

  private DeliveryRuleRow mapDeliveryRule(ResultSet rs, int rowNum) throws SQLException {
    return new DeliveryRuleRow(rs.getBigDecimal("delivery_fee"), rs.getBigDecimal("start_price"), rs.getInt("estimated_minutes"), rs.getBigDecimal("max_delivery_distance_km"), rs.getString("package_fee_mode"), rs.getBigDecimal("package_fee_fixed"), rs.getBigDecimal("package_fee_per_item"), rs.getBigDecimal("distance_extra_threshold_km"), rs.getBigDecimal("distance_extra_fee"), rs.getBigDecimal("distance_extra_step_km"), rs.getString("delivery_text"));
  }

  private InventoryIdempotencyRow mapInventoryIdempotency(ResultSet rs, int rowNum) throws SQLException {
    return new InventoryIdempotencyRow(rs.getLong("id"), rs.getString("status"), rs.getString("result_summary"));
  }

  private BigDecimal getNullableBigDecimal(ResultSet rs, String column) throws SQLException {
    BigDecimal value = rs.getBigDecimal(column);
    return rs.wasNull() ? null : value;
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String normalizeCategoryStatus(String status) {
    String value = status == null || status.isBlank() ? "normal" : status.trim().toLowerCase();
    return switch (value) {
      case "normal", "disabled" -> value;
      default -> "normal";
    };
  }

  private String normalizeStoreStatusLenient(String status) {
    String value = status == null || status.isBlank() ? "open" : status.trim().toLowerCase();
    return switch (value) {
      case "open", "closed", "休息", "营业" -> value.equals("休息") ? "closed" : value.equals("营业") ? "open" : value;
      default -> "open";
    };
  }

  record MerchantRow(Long id, Long accountId, String merchantName, String contactName, String contactPhone, String licenseNo, String status, String auditStatus, long storeCount, long itemCount, LocalDateTime settledAt) {}
  record StoreRow(Long id, Long merchantId, String storeName, String businessType, String summary, String address, String distanceText, BigDecimal longitude, BigDecimal latitude, BigDecimal rating, int monthlySales, BigDecimal avgPrice, String status, String businessHoursText, String tagText, String coverUrl, String contactPhone, String announcement, LocalDateTime updatedAt) {}
  record StoreAdminRow(Long storeId, Long merchantId, String merchantName, String storeName, String businessType, String summary, String address, String status, String businessHoursText, String tagText, String coverUrl, String contactPhone, String announcement, LocalDateTime updatedAt) {}
  record ApplicationRow(Long id, String applicationNo, Long accountId, String merchantName, String contactName, String contactPhone, String businessType, String storeName, String address, String status, String auditRemark, LocalDateTime submittedAt, Long auditedBy, LocalDateTime auditedAt) {}
  record CertificationMaterialRow(Long id, Long merchantId, Long applicationId, String merchantName, String materialType, String materialName, String fileUrl, String status, String rejectReason, LocalDateTime submittedAt, Long auditedBy, LocalDateTime auditedAt) {}
  record CatalogItemRow(Long id, Long storeId, String storeName, String businessType, Long categoryId, String categoryName, String title, String subtitle, BigDecimal price, BigDecimal originalPrice, int stock, String status, String coverUrl, String tagText, int salesCount, String businessAttributes, String usageRules, String refundPolicy, String notice, int validityDays, LocalDateTime updatedAt) {}
  record CategoryRow(Long id, Long storeId, String businessType, String categoryCode, String categoryName, int sortOrder, String status, LocalDateTime updatedAt) {}
  record ModuleRow(Long id, String code, String name, String businessType, int sortOrder) {}
  record CategorySimpleRow(Long id, String name, int sortOrder) {}
  record ItemRow(Long id, String title, String subtitle, String businessType, Long categoryId, String categoryName, BigDecimal price, BigDecimal originalPrice, String coverUrl, String tagText, Long storeId, String storeName, BigDecimal storeLongitude, BigDecimal storeLatitude, BigDecimal storeRating, int storeMonthlySales, int sortOrder, int salesCount, int stock, String skuStatus, String businessAttributes, String usageRules, String refundPolicy, String notice, int validityDays) {}
  record MerchantItemRow(Long id, Long storeId, String title, String subtitle, String categoryName, BigDecimal price, BigDecimal originalPrice, int stock, String status, int salesCount) {}
  record DeliveryRuleRow(BigDecimal deliveryFee, BigDecimal startPrice, int estimatedMinutes, BigDecimal maxDeliveryDistanceKm, String packageFeeMode, BigDecimal packageFeeFixed, BigDecimal packageFeePerItem, BigDecimal distanceExtraThresholdKm, BigDecimal distanceExtraFee, BigDecimal distanceExtraStepKm, String deliveryText) {}
  record TakeawaySettingRow(Long storeId, String storeName, String acceptMode) {}
  record SkuRow(Long id, Long itemId, String skuName, BigDecimal price, int stock, String status) {}
  record InventoryIdempotencyRow(Long id, String status, String resultSummary) {}
}
