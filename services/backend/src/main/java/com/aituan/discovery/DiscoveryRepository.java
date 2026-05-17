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
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name, i.sort_order
        from member_recommend_config r
        join catalog_item i on i.id = r.item_id and i.is_deleted = 0 and i.status = 'on_sale'
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        where r.scene = 'home_recommend' and r.status = 'normal'
        order by r.sort_order, r.id
        limit ? offset ?
        """,
        this::mapItem,
        limit,
        offset);
  }

  long countRecommendations() {
    Long count = jdbcTemplate.queryForObject(
        "select count(1) from member_recommend_config where scene = 'home_recommend' and status = 'normal'",
        Long.class);
    return count == null ? 0 : count;
  }

  List<StoreRow> listStoresByBusinessType(String businessType, int limit) {
    return jdbcTemplate.query(
        """
        select id, merchant_id, store_name, business_type, summary, address, distance_text, rating,
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
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name, i.sort_order
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
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
        select id, merchant_id, store_name, business_type, summary, address, distance_text, rating,
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
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name, i.sort_order
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
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
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name, i.sort_order
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
        where i.store_id = ? and i.is_deleted = 0 and i.status = 'on_sale'
        order by c.sort_order, i.sort_order, i.id
        """,
        this::mapItem,
        storeId);
  }

  List<ItemRow> searchItems(long storeId, String keyword, int limit) {
    String like = "%" + keyword + "%";
    return jdbcTemplate.query(
        """
        select i.id, i.item_name, i.subtitle, i.business_type, i.category_id, c.category_name,
               i.price, i.original_price, i.cover_url, i.tag_text, i.store_id, s.store_name, i.sort_order
        from catalog_item i
        join catalog_category c on c.id = i.category_id
        join merchant_store s on s.id = i.store_id
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

  List<StoreRow> searchStores(String keyword, int limit) {
    String like = "%" + keyword + "%";
    return jdbcTemplate.query(
        """
        select distinct s.id, s.merchant_id, s.store_name, s.business_type, s.summary, s.address, s.distance_text,
               s.rating, s.monthly_sales, s.avg_price, s.status, s.business_hours_text, s.tag_text, s.cover_url
        from merchant_store s
        left join catalog_item i on i.store_id = s.id and i.is_deleted = 0 and i.status = 'on_sale'
        where s.is_deleted = 0 and s.status = 'open'
          and (
            s.store_name like ? or s.summary like ? or s.tag_text like ? or s.address like ?
            or i.item_name like ? or i.subtitle like ? or i.tag_text like ?
          )
        order by s.monthly_sales desc, s.rating desc, s.id
        limit ?
        """,
        this::mapStore,
        like,
        like,
        like,
        like,
        like,
        like,
        like,
        limit);
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
        "select delivery_fee, estimated_minutes, start_price, delivery_text from merchant_delivery_rule where store_id = ? and is_deleted = 0 limit 1",
        (rs, rowNum) -> new DeliveryRuleRow(
            rs.getBigDecimal("delivery_fee"),
            rs.getInt("estimated_minutes"),
            rs.getBigDecimal("start_price"),
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
        rs.getInt("sort_order"));
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
      int sortOrder) {}

  record ReviewSummaryRow(BigDecimal rating, long count, List<String> highlights) {}

  record DeliveryRuleRow(BigDecimal deliveryFee, int estimatedMinutes, BigDecimal startPrice, String deliveryText) {}
}
