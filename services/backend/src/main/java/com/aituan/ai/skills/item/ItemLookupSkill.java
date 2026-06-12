package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.keyword;
import static com.aituan.ai.AiSkillSupport.like;
import static com.aituan.ai.AiSkillSupport.money;
import static com.aituan.ai.AiSkillSupport.params;
import static com.aituan.ai.AiSkillSupport.shouldRun;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class ItemLookupSkill implements AiSkill {
  private static final List<String> WORDS = List.of(
      "商品", "套餐", "团购", "服务", "价格", "多少钱", "库存", "预约", "核销", "退款规则", "使用规则", "汉堡", "拌饭", "酒店", "电影",
      "密室", "按摩", "足疗", "景点", "门票", "观景", "休闲", "玩乐", "娱乐", "电玩城", "游玩", "SPA", "spa");
  private static final List<String> CANDIDATES = List.of(
      "团购", "汉堡", "拌饭", "炸鸡", "酒店", "电影", "密室", "按摩", "足疗", "景点", "门票", "观景", "休闲玩乐", "休闲", "玩乐",
      "娱乐", "电玩城", "SPA", "spa", "烤肉", "套餐", "券", "双人");

  private final JdbcTemplate jdbcTemplate;

  ItemLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "item_lookup";
  }

  @Override
  public String description() {
    return "读取公开真实商品/服务、价格、库存、销量、使用规则、退改政策";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    String message = context.normalizedMessage();
    List<String> businessTypes = AiSkillSupport.businessTypes(message);
    boolean groupBuyOnly = businessTypes.size() == 1 && "group_buy".equals(businessTypes.get(0));
    String scopeLabel = AiSkillSupport.businessTypeLabel(businessTypes);
    String key = key(message);
    List<Object> args = new ArrayList<>();
    String typeClause = "";
    if (!businessTypes.isEmpty()) {
      typeClause = "          and i.business_type in (" + placeholders(businessTypes.size()) + ")\n";
      args.addAll(businessTypes);
    }
    args.add(key);
    args.add(like(key));
    args.add(like(key));
    args.add(like(key));
    args.add(like(key));
    String sql = """
        select i.id, i.item_name, i.subtitle, i.price, i.original_price, i.store_id, s.store_name, s.business_type,
               coalesce(sku.stock, 0) as stock, i.sales_count, i.usage_rules, i.refund_policy, i.notice
        from catalog_item i
        join merchant_store s on s.id = i.store_id and s.is_deleted = 0 and s.status = 'open'
        left join (
          select item_id, sum(case when status = 'on_sale' then stock else 0 end) as stock
          from catalog_sku
          where is_deleted = 0
          group by item_id
        ) sku on sku.item_id = i.id
        where i.is_deleted = 0 and i.status = 'on_sale'
        """
        + typeClause
        + """
          and (? = '' or i.item_name like ? or i.subtitle like ? or i.tag_text like ? or s.store_name like ?)
        order by i.sales_count desc, i.id
        limit 6
        """;
    List<ItemRow> items = jdbcTemplate.query(sql, this::mapItem, args.toArray());
    if (items.isEmpty()) {
      return Optional.of(AiSkillResult.text(
          name(),
          "商品服务查询",
          groupBuyOnly ? "没有匹配到上架团购套餐，可换关键词或进入团购分类浏览。" : "没有匹配到上架" + scopeLabel + "商品或服务。"));
    }
    StringBuilder summary = new StringBuilder(groupBuyOnly ? "匹配到真实团购套餐：" : "匹配到真实上架" + scopeLabel + "商品/服务：");
    for (ItemRow item : items) {
      summary.append("\n- ").append(item.name()).append("，")
          .append(item.storeName()).append("，")
          .append(groupBuyOnly ? "团购价 ¥" : "¥").append(money(item.price()))
          .append(groupBuyOnly && item.originalPrice() != null ? "，门市价 ¥" + money(item.originalPrice()) : "")
          .append("，库存 ").append(item.stock()).append("，销量 ").append(item.salesCount())
          .append(item.usageRules() == null ? "" : "，规则：" + AiSkillSupport.limit(item.usageRules(), 36))
          .append(item.refundPolicy() == null ? "" : "，退改：" + AiSkillSupport.limit(item.refundPolicy(), 36));
    }
    List<AiAssistantCard> cards = items.stream()
        .map(item -> new AiAssistantCard(
            "item",
            item.name(),
            item.storeName() + " · " + (groupBuyOnly ? "团购价 ¥" : "¥") + money(item.price()) + " · 库存 " + item.stock(),
            "查看详情",
            "/items/detail",
            params(
                "itemId", item.id(),
                "storeId", item.storeId(),
                "businessType", item.businessType(),
                "price", item.price(),
                "originalPrice", item.originalPrice())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "商品服务查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("继续搜索", null, "/search", params("keyword", AiSkillSupport.searchKeyword(key, businessTypes))))));
  }

  private String key(String text) {
    if (AiSkillSupport.containsAny(text, "全部", "所有", "推荐", "商品", "服务", "有什么", "刚才", "第一个", "团购")) return "";
    return keyword(text, CANDIDATES);
  }

  private String placeholders(int size) {
    return String.join(", ", Collections.nCopies(size, "?"));
  }

  private ItemRow mapItem(ResultSet rs, int rowNum) throws SQLException {
    return new ItemRow(
        rs.getLong("id"),
        rs.getString("item_name"),
        rs.getString("subtitle"),
        rs.getBigDecimal("price"),
        rs.getBigDecimal("original_price"),
        rs.getLong("store_id"),
        rs.getString("store_name"),
        rs.getString("business_type"),
        rs.getInt("stock"),
        rs.getInt("sales_count"),
        rs.getString("usage_rules"),
        rs.getString("refund_policy"),
        rs.getString("notice"));
  }

  record ItemRow(long id, String name, String subtitle, BigDecimal price, BigDecimal originalPrice, long storeId,
                 String storeName, String businessType, int stock, int salesCount, String usageRules,
                 String refundPolicy, String notice) {}
}
