package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.keyword;
import static com.aituan.ai.AiSkillSupport.like;
import static com.aituan.ai.AiSkillSupport.money;
import static com.aituan.ai.AiSkillSupport.params;
import static com.aituan.ai.AiSkillSupport.shouldRun;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class StoreLookupSkill implements AiSkill {
  private static final List<String> WORDS = List.of(
      "店", "商家", "门店", "附近", "推荐", "周边", "吃", "汉堡", "拌饭", "酒店", "电影", "密室", "按摩", "足疗", "spa", "SPA", "烤肉");
  private static final List<String> CANDIDATES = List.of(
      "汉堡", "拌饭", "炸鸡", "酒店", "电影", "密室", "按摩", "足疗", "SPA", "spa", "烤肉", "小馆", "美食", "外卖");

  private final JdbcTemplate jdbcTemplate;

  StoreLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "store_lookup";
  }

  @Override
  public String description() {
    return "读取公开真实店铺、附近/推荐门店、评分、销量、地址、配送规则和营业信息";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    String key = key(context.normalizedMessage());
    List<StoreRow> stores = jdbcTemplate.query(
        """
        select distinct s.id, s.store_name, s.business_type, s.summary, s.address, s.distance_text,
               s.rating, s.monthly_sales, s.avg_price, s.business_hours_text, s.tag_text,
               dr.delivery_fee, dr.start_price, dr.estimated_minutes, dr.delivery_text
        from merchant_store s
        left join merchant_delivery_rule dr on dr.store_id = s.id and dr.is_deleted = 0
        left join catalog_item i on i.store_id = s.id and i.is_deleted = 0 and i.status = 'on_sale'
        where s.is_deleted = 0 and s.status = 'open'
          and (? = '' or s.store_name like ? or s.summary like ? or s.tag_text like ? or s.address like ?
            or i.item_name like ? or i.subtitle like ? or i.tag_text like ?)
        order by s.monthly_sales desc, s.rating desc, s.id
        limit 6
        """,
        this::mapStore,
        key, like(key), like(key), like(key), like(key), like(key), like(key), like(key));
    if (stores.isEmpty()) {
      return Optional.of(AiSkillResult.text(name(), "店铺查询", "没有匹配到开放营业店铺，可换关键词或进入首页分类浏览。"));
    }
    StringBuilder summary = new StringBuilder("匹配到真实开放店铺：");
    for (StoreRow store : stores) {
      summary.append("\n- ").append(store.name()).append("，")
          .append(store.businessType()).append("，评分 ").append(store.rating())
          .append("，月售 ").append(store.monthlySales()).append("，人均 ")
          .append(money(store.avgPrice())).append("，").append(store.distanceText())
          .append("，").append(store.hours())
          .append(store.estimatedMinutes() == 0 ? "" : "，约 " + store.estimatedMinutes() + " 分钟送达")
          .append(store.deliveryFee() == null ? "" : "，配送费 " + money(store.deliveryFee()));
    }
    List<AiAssistantCard> cards = stores.stream()
        .map(store -> new AiAssistantCard(
            "store",
            store.name(),
            store.summary() + " · " + store.address(),
            "查看店铺",
            "/stores/detail",
            params("storeId", store.id(), "businessType", store.businessType())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "店铺查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("搜索店铺", null, "/search", params("keyword", key)))));
  }

  private String key(String text) {
    if (AiSkillSupport.containsAny(text, "附近", "推荐", "周边", "全部", "所有")) return "";
    return keyword(text, CANDIDATES);
  }

  private StoreRow mapStore(ResultSet rs, int rowNum) throws SQLException {
    return new StoreRow(
        rs.getLong("id"),
        rs.getString("store_name"),
        rs.getString("business_type"),
        rs.getString("summary"),
        rs.getString("address"),
        rs.getString("distance_text"),
        rs.getBigDecimal("rating"),
        rs.getInt("monthly_sales"),
        rs.getBigDecimal("avg_price"),
        rs.getString("business_hours_text"),
        rs.getString("tag_text"),
        rs.getBigDecimal("delivery_fee"),
        rs.getBigDecimal("start_price"),
        rs.getInt("estimated_minutes"),
        rs.getString("delivery_text"));
  }

  record StoreRow(long id, String name, String businessType, String summary, String address, String distanceText,
                  BigDecimal rating, int monthlySales, BigDecimal avgPrice, String hours, String tags,
                  BigDecimal deliveryFee, BigDecimal startPrice, int estimatedMinutes, String deliveryText) {}
}
