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
      "店", "商家", "门店", "附近", "推荐", "周边", "吃", "团购", "汉堡", "拌饭", "酒店", "电影", "密室", "按摩", "足疗", "spa", "SPA", "烤肉");
  private static final List<String> CANDIDATES = List.of(
      "团购", "汉堡", "拌饭", "炸鸡", "酒店", "电影", "密室", "按摩", "足疗", "SPA", "spa", "烤肉", "小馆", "美食", "外卖");

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
    String message = context.normalizedMessage();
    boolean groupBuyOnly = groupBuyIntent(message);
    String key = key(message);
    List<StoreRow> stores = jdbcTemplate.query(
        """
        select distinct s.id, s.store_name, s.business_type, s.summary, s.address, s.distance_text,
               s.rating, s.monthly_sales, s.avg_price, s.business_hours_text, s.tag_text,
               dr.delivery_fee, dr.start_price, dr.estimated_minutes, dr.delivery_text,
               (select count(1)
                  from catalog_item gi
                 where gi.store_id = s.id and gi.is_deleted = 0 and gi.status = 'on_sale'
                   and gi.business_type = 'group_buy') as deal_count,
               (select min(gi.price)
                  from catalog_item gi
                 where gi.store_id = s.id and gi.is_deleted = 0 and gi.status = 'on_sale'
                   and gi.business_type = 'group_buy') as min_deal_price,
               (select gi.item_name
                  from catalog_item gi
                 where gi.store_id = s.id and gi.is_deleted = 0 and gi.status = 'on_sale'
                   and gi.business_type = 'group_buy'
                 order by gi.sales_count desc, gi.id
                 limit 1) as top_deal_name
        from merchant_store s
        left join merchant_delivery_rule dr on dr.store_id = s.id and dr.is_deleted = 0
        left join catalog_item i on i.store_id = s.id and i.is_deleted = 0 and i.status = 'on_sale'
        where s.is_deleted = 0 and s.status = 'open'
          and (? = 0 or s.business_type = 'group_buy')
          and (? = '' or s.store_name like ? or s.summary like ? or s.tag_text like ? or s.address like ?
            or i.item_name like ? or i.subtitle like ? or i.tag_text like ?)
        order by s.monthly_sales desc, s.rating desc, s.id
        limit 6
        """,
        this::mapStore,
        groupBuyOnly ? 1 : 0, key, like(key), like(key), like(key), like(key), like(key), like(key), like(key));
    if (stores.isEmpty()) {
      return Optional.of(AiSkillResult.text(
          name(),
          "店铺查询",
          groupBuyOnly ? "没有匹配到开放营业的团购店铺，可换关键词或进入团购分类浏览。" : "没有匹配到开放营业店铺，可换关键词或进入首页分类浏览。"));
    }
    StringBuilder summary = new StringBuilder(groupBuyOnly ? "匹配到真实团购店铺：" : "匹配到真实开放店铺：");
    for (StoreRow store : stores) {
      summary.append("\n- ").append(store.name()).append("，")
          .append(groupBuyOnly ? "团购" : store.businessType()).append("，评分 ").append(store.rating())
          .append("，月售 ").append(store.monthlySales()).append("，人均 ")
          .append(money(store.avgPrice())).append("，").append(store.distanceText())
          .append("，").append(store.hours())
          .append(store.dealCount() <= 0 ? "" : "，可买团购 " + store.dealCount() + " 个")
          .append(store.minDealPrice() == null ? "" : "，团购 ¥" + money(store.minDealPrice()) + " 起")
          .append(store.topDealName() == null ? "" : "，热门：" + store.topDealName())
          .append(store.estimatedMinutes() == 0 ? "" : "，约 " + store.estimatedMinutes() + " 分钟送达")
          .append(store.deliveryFee() == null ? "" : "，配送费 " + money(store.deliveryFee()));
    }
    List<AiAssistantCard> cards = stores.stream()
        .map(store -> new AiAssistantCard(
            "store",
            store.name(),
            storeCardContent(store, groupBuyOnly),
            "查看店铺",
            "/stores/detail",
            params(
                "storeId", store.id(),
                "businessType", store.businessType(),
                "dealCount", store.dealCount(),
                "minDealPrice", store.minDealPrice(),
                "topDealName", store.topDealName(),
                "rating", store.rating(),
                "monthlySales", store.monthlySales())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "店铺查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("搜索店铺", null, "/search", params("keyword", key)))));
  }

  private String key(String text) {
    if (AiSkillSupport.containsAny(text, "附近", "推荐", "周边", "全部", "所有", "团购")) return "";
    return keyword(text, CANDIDATES);
  }

  private boolean groupBuyIntent(String text) {
    return AiSkillSupport.containsAny(text, "团购", "到店套餐", "多人餐", "双人餐", "核销套餐");
  }

  private String storeCardContent(StoreRow store, boolean groupBuyOnly) {
    StringBuilder content = new StringBuilder();
    if (groupBuyOnly || "group_buy".equals(store.businessType())) {
      content.append("团购店");
      if (store.dealCount() > 0) content.append(" · ").append(store.dealCount()).append(" 个套餐");
      if (store.minDealPrice() != null) content.append(" · ¥").append(money(store.minDealPrice())).append("起");
      if (store.topDealName() != null && !store.topDealName().isBlank()) content.append(" · 热门：").append(store.topDealName());
    } else {
      content.append(store.summary());
    }
    content.append(" · 评分 ").append(store.rating())
        .append(" · 月售 ").append(store.monthlySales())
        .append(" · ").append(store.distanceText())
        .append(" · ").append(store.address());
    return content.toString();
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
        rs.getString("delivery_text"),
        rs.getInt("deal_count"),
        rs.getBigDecimal("min_deal_price"),
        rs.getString("top_deal_name"));
  }

  record StoreRow(long id, String name, String businessType, String summary, String address, String distanceText,
                  BigDecimal rating, int monthlySales, BigDecimal avgPrice, String hours, String tags,
                  BigDecimal deliveryFee, BigDecimal startPrice, int estimatedMinutes, String deliveryText,
                  int dealCount, BigDecimal minDealPrice, String topDealName) {}
}
