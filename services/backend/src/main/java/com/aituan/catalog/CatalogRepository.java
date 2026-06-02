package com.aituan.catalog;

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
class CatalogRepository {
  private final JdbcTemplate jdbcTemplate;

  CatalogRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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
    Long count = jdbcTemplate.queryForObject(
        """
        select count(1)
        from merchant_store s
        join merchant_profile m on m.id = s.merchant_id
        where s.id = ? and m.account_id = ? and s.is_deleted = 0 and m.is_deleted = 0
        """,
        Long.class,
        storeId,
        accountId);
    return count != null && count > 0;
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
    return jdbcTemplate.query(sql.toString(), this::mapItem, params.toArray());
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
    return jdbcTemplate.query(sql.toString(), this::mapItem, params.toArray());
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
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return count == null ? 0 : count;
  }

  Optional<CatalogItemRow> findItem(long itemId) {
    List<CatalogItemRow> rows = jdbcTemplate.query(
        itemSelectSql() + """
        from catalog_item i
        join merchant_store s on s.id = i.store_id
        join catalog_category c on c.id = i.category_id
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where i.id = ? and i.is_deleted = 0
        limit 1
        """,
        this::mapItem,
        itemId);
    return rows.stream().findFirst();
  }

  long insertItem(long storeId, CatalogItemUpsertRequest request, long categoryId, String status) {
    jdbcTemplate.update(
        """
        insert into catalog_item(store_id, business_type, category_id, item_name, subtitle, price, original_price, cover_url, tag_text, status, item_kind, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
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
        "takeaway".equalsIgnoreCase(request.businessType()) ? "food" : "service");
    Long itemId = jdbcTemplate.queryForObject("select max(id) from catalog_item where store_id = ?", Long.class, storeId);
    upsertDefaultSku(itemId, request.price(), request.stock() == null ? 999 : request.stock(), status);
    return itemId;
  }

  void updateItem(long itemId, CatalogItemUpsertRequest request, long categoryId, String status) {
    jdbcTemplate.update(
        """
        update catalog_item
        set business_type = ?, category_id = ?, item_name = ?, subtitle = ?, price = ?, original_price = ?, cover_url = coalesce(?, cover_url), tag_text = ?, status = ?, updated_at = current_timestamp
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
        values (null, ?, ?, ?, ?, 'store', ?, ?, current_timestamp, current_timestamp)
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
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from catalog_item where category_id = ? and is_deleted = 0",
        Long.class,
        categoryId);
    return count == null ? 0 : count;
  }

  void softDeleteCategory(long categoryId) {
    jdbcTemplate.update(
        """
        update catalog_category
        set is_deleted = 1, status = 'disabled', updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        categoryId);
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
    if (!rows.isEmpty()) {
      return rows.get(0);
    }
    String code = businessType + "_" + storeId + "_default";
    jdbcTemplate.update(
        """
        insert into catalog_category(parent_id, store_id, category_code, category_name, business_type, category_level, sort_order, status, created_at, updated_at)
        values (null, ?, ?, '默认分类', ?, 'store', 0, 'normal', current_timestamp, current_timestamp)
        """,
        storeId,
        code,
        businessType);
    return jdbcTemplate.queryForObject("select id from catalog_category where category_code = ?", Long.class, code);
  }

  private String itemSelectSql() {
    return """
        select i.id, i.store_id, s.store_name, i.business_type, i.category_id, c.category_name, i.item_name,
               i.subtitle, i.price, i.original_price, coalesce(sku.stock, 0) as stock, i.status,
               i.cover_url, i.tag_text, i.sales_count, i.updated_at
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
      jdbcTemplate.update(
          """
          insert into catalog_sku(item_id, sku_name, price, stock, status, created_at, updated_at)
          values (?, '默认', ?, ?, ?, current_timestamp, current_timestamp)
          """,
          itemId,
          price,
          stock,
          status);
    }
  }

  private CatalogItemRow mapItem(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new CatalogItemRow(
        rs.getLong("id"),
        rs.getLong("store_id"),
        rs.getString("store_name"),
        rs.getString("business_type"),
        rs.getLong("category_id"),
        rs.getString("category_name"),
        rs.getString("item_name"),
        rs.getString("subtitle"),
        rs.getBigDecimal("price"),
        rs.getBigDecimal("original_price"),
        rs.getInt("stock"),
        rs.getString("status"),
        rs.getString("cover_url"),
        rs.getString("tag_text"),
        rs.getInt("sales_count"),
        updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private CategoryRow mapCategory(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    long storeId = rs.getLong("store_id");
    return new CategoryRow(
        rs.getLong("id"),
        rs.wasNull() ? null : storeId,
        rs.getString("business_type"),
        rs.getString("category_code"),
        rs.getString("category_name"),
        rs.getInt("sort_order"),
        rs.getString("status"),
        updatedAt == null ? null : updatedAt.toLocalDateTime());
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

  record CatalogItemRow(Long id, Long storeId, String storeName, String businessType, Long categoryId, String categoryName, String title, String subtitle, BigDecimal price, BigDecimal originalPrice, int stock, String status, String coverUrl, String tagText, int salesCount, LocalDateTime updatedAt) {}

  record CategoryRow(Long id, Long storeId, String businessType, String categoryCode, String categoryName, int sortOrder, String status, LocalDateTime updatedAt) {}
}
