package com.aituan.discovery;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class DiscoveryRepository {
  private final JdbcTemplate jdbcTemplate;

  DiscoveryRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
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
    return jdbcTemplate.query(
        """
        select i.id, i.item_name, i.subtitle, i.business_type, i.category_id, c.category_name,
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name,
               s.longitude as store_longitude, s.latitude as store_latitude,
               s.rating as store_rating, s.monthly_sales as store_monthly_sales,
               i.sort_order, i.sales_count,
               coalesce(sku.stock, 0) as stock, coalesce(sku.sku_status, 'sold_out') as sku_status,
               i.business_attributes, i.usage_rules, i.refund_policy, i.notice, i.validity_days
        from catalog_item i
        join catalog_category c on c.id = i.category_id and c.is_deleted = 0 and c.status = 'normal'
        join merchant_store s on s.id = i.store_id and s.is_deleted = 0 and s.status = 'open'
        left join member_recommend_config r on r.item_id = i.id and r.scene = 'home_recommend' and r.status = 'normal'
        left join (
          select item_id,
                 sum(case when status = 'on_sale' then stock else 0 end) as stock,
                 max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku
          where is_deleted = 0
          group by item_id
        ) sku on sku.item_id = i.id
        where i.is_deleted = 0 and i.status = 'on_sale'
        order by case when r.id is null then 1 else 0 end,
                 coalesce(r.sort_order, 999999),
                 i.sales_count desc,
                 s.monthly_sales desc,
                 s.rating desc,
                 i.sort_order,
                 i.id
        limit ? offset ?
        """,
        this::mapItem,
        limit,
        offset);
  }

  long countRecommendations() {
    Long count = jdbcTemplate.queryForObject(
        """
        select count(1)
        from catalog_item i
        join catalog_category c on c.id = i.category_id and c.is_deleted = 0 and c.status = 'normal'
        join merchant_store s on s.id = i.store_id and s.is_deleted = 0 and s.status = 'open'
        where i.is_deleted = 0 and i.status = 'on_sale'
        """,
        Long.class);
    return count == null ? 0 : count;
  }

  List<StoreRow> listStoresByBusinessType(String businessType, int limit) {
    return jdbcTemplate.query(
        """
        select id, merchant_id, store_name, business_type, summary, address, distance_text, longitude, latitude, rating,
               monthly_sales, avg_price, status, business_hours_text, tag_text, cover_url
        from merchant_store
        where business_type = ? and is_deleted = 0 and status = 'open'
        order by monthly_sales desc, rating desc, id
        limit ?
        """,
        this::mapStore,
        businessType,
        limit);
  }

  List<ItemRow> listItemsByBusinessType(String businessType, int limit) {
    return jdbcTemplate.query(
        """
        select i.id, i.item_name, i.subtitle, i.business_type, i.category_id, c.category_name,
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name,
               s.longitude as store_longitude, s.latitude as store_latitude,
               s.rating as store_rating, s.monthly_sales as store_monthly_sales,
               i.sort_order, i.sales_count,
               coalesce(sku.stock, 0) as stock, coalesce(sku.sku_status, 'sold_out') as sku_status,
               i.business_attributes, i.usage_rules, i.refund_policy, i.notice, i.validity_days
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id,
                 sum(case when status = 'on_sale' then stock else 0 end) as stock,
                 max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku
          where is_deleted = 0
          group by item_id
        ) sku on sku.item_id = i.id
        where i.business_type = ? and i.is_deleted = 0 and i.status = 'on_sale'
        order by i.sort_order, i.sales_count desc, i.id
        limit ?
        """,
        this::mapItem,
        businessType,
        limit);
  }

  Optional<StoreRow> findStore(long storeId) {
    List<StoreRow> rows = jdbcTemplate.query(
        """
        select id, merchant_id, store_name, business_type, summary, address, distance_text, longitude, latitude, rating,
               monthly_sales, avg_price, status, business_hours_text, tag_text, cover_url
        from merchant_store
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapStore,
        storeId);
    return rows.stream().findFirst();
  }

  Optional<ItemRow> findItem(long itemId) {
    List<ItemRow> rows = jdbcTemplate.query(
        """
        select i.id, i.item_name, i.subtitle, i.business_type, i.category_id, c.category_name,
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name,
               s.longitude as store_longitude, s.latitude as store_latitude,
               s.rating as store_rating, s.monthly_sales as store_monthly_sales,
               i.sort_order, i.sales_count,
               coalesce(sku.stock, 0) as stock, coalesce(sku.sku_status, 'sold_out') as sku_status,
               i.business_attributes, i.usage_rules, i.refund_policy, i.notice, i.validity_days
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id,
                 sum(case when status = 'on_sale' then stock else 0 end) as stock,
                 max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku
          where is_deleted = 0
          group by item_id
        ) sku on sku.item_id = i.id
        where i.id = ? and i.is_deleted = 0
        limit 1
        """,
        this::mapItem,
        itemId);
    return rows.stream().findFirst();
  }

  List<CategoryRow> listStoreCategories(long storeId) {
    return jdbcTemplate.query(
        """
        select id, category_name, sort_order
        from catalog_category
        where store_id = ? and is_deleted = 0 and status = 'normal'
        order by sort_order, id
        """,
        this::mapCategory,
        storeId);
  }

  List<ItemRow> listStoreItems(long storeId) {
    return jdbcTemplate.query(
        """
        select i.id, i.item_name, i.subtitle, i.business_type, i.category_id, c.category_name,
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name,
               s.longitude as store_longitude, s.latitude as store_latitude,
               s.rating as store_rating, s.monthly_sales as store_monthly_sales,
               i.sort_order, i.sales_count,
               coalesce(sku.stock, 0) as stock, coalesce(sku.sku_status, 'sold_out') as sku_status,
               i.business_attributes, i.usage_rules, i.refund_policy, i.notice, i.validity_days
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id,
                 sum(case when status = 'on_sale' then stock else 0 end) as stock,
                 max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku
          where is_deleted = 0
          group by item_id
        ) sku on sku.item_id = i.id
        where i.store_id = ? and i.is_deleted = 0 and i.status = 'on_sale'
        order by c.sort_order, i.sort_order, i.id
        """,
        this::mapItem,
        storeId);
  }

  List<ItemRow> searchItems(long storeId, String keyword, int limit) {
    String normalized = keyword == null ? "" : keyword.trim();
    if (normalized.isEmpty()) {
      return listStoreItemsForFill(storeId, List.of(), List.of(), limit);
    }
    String like = "%" + normalized + "%";
    return jdbcTemplate.query(
        """
        select i.id, i.item_name, i.subtitle, i.business_type, i.category_id, c.category_name,
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name,
               s.longitude as store_longitude, s.latitude as store_latitude,
               s.rating as store_rating, s.monthly_sales as store_monthly_sales,
               i.sort_order, i.sales_count,
               coalesce(sku.stock, 0) as stock, coalesce(sku.sku_status, 'sold_out') as sku_status,
               i.business_attributes, i.usage_rules, i.refund_policy, i.notice, i.validity_days
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id,
                 sum(case when status = 'on_sale' then stock else 0 end) as stock,
                 max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku
          where is_deleted = 0
          group by item_id
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
    StringBuilder sql = new StringBuilder("""
        select i.id, i.item_name, i.subtitle, i.business_type, i.category_id, c.category_name,
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name,
               s.longitude as store_longitude, s.latitude as store_latitude,
               s.rating as store_rating, s.monthly_sales as store_monthly_sales,
               i.sort_order, i.sales_count,
               coalesce(sku.stock, 0) as stock, coalesce(sku.sku_status, 'sold_out') as sku_status,
               i.business_attributes, i.usage_rules, i.refund_policy, i.notice, i.validity_days
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        left join (
          select item_id,
                 sum(case when status = 'on_sale' then stock else 0 end) as stock,
                 max(case when status = 'on_sale' then 'on_sale' else status end) as sku_status
          from catalog_sku
          where is_deleted = 0
          group by item_id
        ) sku on sku.item_id = i.id
        where i.store_id = ? and i.is_deleted = 0 and i.status = 'on_sale'
        """);
    List<Object> params = new java.util.ArrayList<>();
    params.add(storeId);
    if (excludedItemIds != null && !excludedItemIds.isEmpty()) {
      sql.append(" and i.id not in (").append("?,".repeat(excludedItemIds.size()).replaceAll(",$", "")).append(")");
      params.addAll(excludedItemIds);
    }
    if (preferredCategoryIds != null && !preferredCategoryIds.isEmpty()) {
      sql.append(" order by case when i.category_id in (")
          .append("?,".repeat(preferredCategoryIds.size()).replaceAll(",$", ""))
          .append(") then 0 else 1 end, c.sort_order, i.sort_order, i.sales_count desc, i.id");
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
    String typeFilter = normalizedType.isEmpty() ? "" : " and s.business_type = ?";
    List<Object> params = new ArrayList<>();
    StringBuilder sql = new StringBuilder();
    if (normalized.isEmpty()) {
      sql.append("""
          select s.id, s.merchant_id, s.store_name, s.business_type, s.summary, s.address, s.distance_text,
                 s.longitude, s.latitude, s.rating, s.monthly_sales, s.avg_price, s.status, s.business_hours_text, s.tag_text, s.cover_url
          from merchant_store s
          where s.is_deleted = 0 and s.status = 'open'
          """);
      if (!normalizedType.isEmpty()) {
        sql.append(typeFilter);
        params.add(normalizedType);
      }
      sql.append(" order by s.monthly_sales desc, s.rating desc, s.id limit ?");
      params.add(limit);
      return jdbcTemplate.query(sql.toString(), this::mapStore, params.toArray());
    }
    String like = "%" + normalized + "%";
    sql.append("""
        select distinct s.id, s.merchant_id, s.store_name, s.business_type, s.summary, s.address, s.distance_text,
               s.longitude, s.latitude, s.rating, s.monthly_sales, s.avg_price, s.status, s.business_hours_text, s.tag_text, s.cover_url
        from merchant_store s
        left join catalog_item i on i.store_id = s.id and i.is_deleted = 0 and i.status = 'on_sale'
        where s.is_deleted = 0 and s.status = 'open'
        """);
    if (!normalizedType.isEmpty()) {
      sql.append(typeFilter);
      params.add(normalizedType);
    }
    sql.append("""
          and (
            s.store_name like ? or s.summary like ? or s.tag_text like ? or s.address like ?
            or i.item_name like ? or i.subtitle like ? or i.tag_text like ?
          )
        order by s.monthly_sales desc, s.rating desc, s.id
        limit ?
        """);
    params.add(like);
    params.add(like);
    params.add(like);
    params.add(like);
    params.add(like);
    params.add(like);
    params.add(like);
    params.add(limit);
    return jdbcTemplate.query(sql.toString(), this::mapStore, params.toArray());
  }

  List<StoreRow> searchStoresByBusinessTypes(List<String> businessTypes, int limit) {
    if (businessTypes == null || businessTypes.isEmpty()) {
      return searchStores("", null, limit);
    }
    List<Object> params = new ArrayList<>(businessTypes);
    String placeholders = String.join(", ", java.util.Collections.nCopies(businessTypes.size(), "?"));
    params.add(limit);
    return jdbcTemplate.query(
        """
        select s.id, s.merchant_id, s.store_name, s.business_type, s.summary, s.address, s.distance_text,
               s.longitude, s.latitude, s.rating, s.monthly_sales, s.avg_price, s.status, s.business_hours_text, s.tag_text, s.cover_url
        from merchant_store s
        where s.is_deleted = 0 and s.status = 'open'
          and s.business_type in (
        """
            + placeholders
            + """
          )
        order by s.monthly_sales desc, s.rating desc, s.id
        limit ?
        """,
        this::mapStore,
        params.toArray());
  }

  List<PreferenceSignalRow> userPreferenceSignals(long userId) {
    return jdbcTemplate.query(
        """
        select i.business_type, i.category_id, i.store_id, i.id as item_id, 8 as weight, 'favorite_item' as source
        from user_favorite f
        join catalog_item i on i.id = f.target_id and i.is_deleted = 0 and i.status = 'on_sale'
        where f.user_id = ? and f.favorite_type = 'item' and f.is_deleted = 0
        union all
        select s.business_type, null as category_id, s.id as store_id, null as item_id, 6 as weight, 'favorite_store' as source
        from user_favorite f
        join merchant_store s on s.id = f.target_id and s.is_deleted = 0 and s.status = 'open'
        where f.user_id = ? and f.favorite_type = 'store' and f.is_deleted = 0
        union all
        select oi.business_type, oi.category_id, om.store_id, oi.item_id, least(sum(oi.quantity) * 3, 30) as weight, 'order_item' as source
        from order_main om
        join order_item oi on oi.order_id = om.id and oi.is_deleted = 0
        where om.user_id = ? and om.is_deleted = 0
        group by oi.business_type, oi.category_id, om.store_id, oi.item_id
        """,
        (rs, rowNum) -> new PreferenceSignalRow(
            rs.getString("business_type"),
            nullableLong(rs, "category_id"),
            nullableLong(rs, "store_id"),
            nullableLong(rs, "item_id"),
            rs.getInt("weight"),
            rs.getString("source")),
        userId,
        userId,
        userId);
  }

  ReviewSummaryRow reviewSummary(long storeId) {
    return jdbcTemplate.query(
        """
        select avg(rating) as avg_rating, count(1) as review_count
        from review_record
        where store_id = ? and is_deleted = 0 and status = 'published'
        """,
        rs -> {
          if (!rs.next()) {
            return new ReviewSummaryRow(BigDecimal.ZERO, 0L, List.of());
          }
          BigDecimal rating = rs.getBigDecimal("avg_rating");
          if (rating == null) {
            rating = BigDecimal.ZERO;
          } else {
            rating = rating.setScale(1, BigDecimal.ROUND_HALF_UP);
          }
          long count = rs.getLong("review_count");
          return new ReviewSummaryRow(rating, count, List.of());
        },
        storeId);
  }

  List<String> reviewHighlights(long storeId) {
    return jdbcTemplate.query(
        """
        select labels
        from review_record
        where store_id = ? and is_deleted = 0 and status = 'published' and labels is not null and labels <> ''
        order by id desc
        limit 3
        """,
        (rs, rowNum) -> rs.getString("labels"),
        storeId).stream()
        .flatMap(value -> java.util.Arrays.stream(value.split(",")))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .distinct()
        .limit(4)
        .toList();
  }

  Optional<DeliveryRuleRow> findDeliveryRule(long storeId) {
    List<DeliveryRuleRow> rows = jdbcTemplate.query(
        """
        select delivery_fee, estimated_minutes, start_price, package_fee_fixed, package_fee_per_item,
               package_fee_mode, distance_extra_threshold_km, distance_extra_fee, distance_extra_step_km, delivery_text
        from merchant_delivery_rule where store_id = ? and is_deleted = 0 limit 1
        """,
        (rs, rowNum) -> new DeliveryRuleRow(
            rs.getBigDecimal("delivery_fee"),
            rs.getInt("estimated_minutes"),
            rs.getBigDecimal("start_price"),
            rs.getBigDecimal("package_fee_fixed"),
            rs.getBigDecimal("package_fee_per_item"),
            rs.getString("package_fee_mode"),
            rs.getBigDecimal("distance_extra_threshold_km"),
            rs.getBigDecimal("distance_extra_fee"),
            rs.getBigDecimal("distance_extra_step_km"),
            rs.getString("delivery_text")),
        storeId);
    return rows.stream().findFirst();
  }

  long countUnreadMessages(long userId) {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from support_station_message where user_id = ? and is_deleted = 0 and read_status = 'unread'",
        Long.class,
        userId);
    return count == null ? 0 : count;
  }

  private ModuleRow mapModule(ResultSet rs, int rowNum) throws SQLException {
    return new ModuleRow(rs.getLong("id"), rs.getString("category_code"), rs.getString("category_name"), rs.getString("business_type"), rs.getInt("sort_order"));
  }

  private StoreRow mapStore(ResultSet rs, int rowNum) throws SQLException {
    return new StoreRow(
        rs.getLong("id"),
        rs.getLong("merchant_id"),
        rs.getString("store_name"),
        rs.getString("business_type"),
        rs.getString("summary"),
        rs.getString("address"),
        rs.getString("distance_text"),
        rs.getBigDecimal("longitude"),
        rs.getBigDecimal("latitude"),
        rs.getBigDecimal("rating"),
        rs.getInt("monthly_sales"),
        rs.getBigDecimal("avg_price"),
        rs.getString("status"),
        rs.getString("business_hours_text"),
        rs.getString("tag_text"),
        rs.getString("cover_url"));
  }

  private CategoryRow mapCategory(ResultSet rs, int rowNum) throws SQLException {
    return new CategoryRow(rs.getLong("id"), rs.getString("category_name"), rs.getInt("sort_order"));
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private ItemRow mapItem(ResultSet rs, int rowNum) throws SQLException {
    return new ItemRow(
        rs.getLong("id"),
        rs.getString("item_name"),
        rs.getString("subtitle"),
        rs.getString("business_type"),
        rs.getLong("category_id"),
        rs.getString("category_name"),
        rs.getBigDecimal("price"),
        rs.getBigDecimal("original_price"),
        rs.getString("cover_url"),
        rs.getString("tag_text"),
        rs.getLong("store_id"),
        rs.getString("store_name"),
        rs.getBigDecimal("store_longitude"),
        rs.getBigDecimal("store_latitude"),
        rs.getBigDecimal("store_rating"),
        rs.getInt("store_monthly_sales"),
        rs.getInt("sort_order"),
        rs.getInt("sales_count"),
        rs.getInt("stock"),
        rs.getString("sku_status"),
        rs.getString("business_attributes"),
        rs.getString("usage_rules"),
        rs.getString("refund_policy"),
        rs.getString("notice"),
        rs.getInt("validity_days"));
  }

  record ModuleRow(Long id, String code, String name, String businessType, int sortOrder) {}

  record StoreRow(
      Long id,
      Long merchantId,
      String name,
      String businessType,
      String summary,
      String address,
      String distanceText,
      BigDecimal longitude,
      BigDecimal latitude,
      BigDecimal rating,
      int monthlySales,
      BigDecimal avgPrice,
      String status,
      String businessHoursText,
      String tagText,
      String coverUrl) {}

  record CategoryRow(Long id, String name, int sortOrder) {}

  record ItemRow(
      Long id,
      String title,
      String subtitle,
      String businessType,
      Long categoryId,
      String categoryName,
      BigDecimal price,
      BigDecimal originalPrice,
      String coverUrl,
      String tagText,
      Long storeId,
      String storeName,
      BigDecimal storeLongitude,
      BigDecimal storeLatitude,
      BigDecimal storeRating,
      int storeMonthlySales,
      int sortOrder,
      int salesCount,
      int stock,
      String skuStatus,
      String businessAttributes,
      String usageRules,
      String refundPolicy,
      String notice,
      int validityDays) {}

  record PreferenceSignalRow(
      String businessType,
      Long categoryId,
      Long storeId,
      Long itemId,
      int weight,
      String source) {}

  record ReviewSummaryRow(BigDecimal rating, long count, List<String> highlights) {}

  record DeliveryRuleRow(
      BigDecimal deliveryFee,
      int estimatedMinutes,
      BigDecimal startPrice,
      BigDecimal packageFeeFixed,
      BigDecimal packageFeePerItem,
      String packageFeeMode,
      BigDecimal distanceExtraThresholdKm,
      BigDecimal distanceExtraFee,
      BigDecimal distanceExtraStepKm,
      String deliveryText) {}
}
