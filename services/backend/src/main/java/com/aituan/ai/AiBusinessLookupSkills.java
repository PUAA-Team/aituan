package com.aituan.ai;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class StoreLookupSkill implements AiSkill {
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
    return "读取公开店铺、附近/推荐门店、评分、销量、地址和营业信息";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    String text = context.normalizedMessage();
    if (!containsAny(text, "店", "商家", "附近", "推荐", "吃", "汉堡", "拌饭", "酒店", "电影", "密室", "按摩", "足疗", "spa", "SPA", "烤肉")) {
      return Optional.empty();
    }
    List<StoreRow> stores = jdbcTemplate.query(
        """
        select distinct s.id, s.store_name, s.business_type, s.summary, s.address, s.distance_text,
               s.rating, s.monthly_sales, s.avg_price, s.business_hours_text, s.tag_text
        from merchant_store s
        left join catalog_item i on i.store_id = s.id and i.is_deleted = 0 and i.status = 'on_sale'
        where s.is_deleted = 0 and s.status = 'open'
          and (
            ? = '' or s.store_name like ? or s.summary like ? or s.tag_text like ? or s.address like ?
            or i.item_name like ? or i.subtitle like ? or i.tag_text like ?
          )
        order by s.monthly_sales desc, s.rating desc, s.id
        limit 4
        """,
        this::mapStore,
        keyword(text),
        like(text),
        like(text),
        like(text),
        like(text),
        like(text),
        like(text),
        like(text));
    if (stores.isEmpty()) {
      return Optional.of(AiSkillResult.text(name(), "店铺查询", "暂未匹配到开放营业的店铺，可换一个关键词或进入首页按分类浏览。"));
    }
    StringBuilder summary = new StringBuilder("匹配到店铺：");
    for (StoreRow store : stores) {
      summary.append("\n- ")
          .append(store.name())
          .append("，评分 ")
          .append(store.rating())
          .append("，月售 ")
          .append(store.monthlySales())
          .append("，人均 ")
          .append(store.avgPrice())
          .append("，")
          .append(store.distanceText())
          .append("，")
          .append(store.hours());
    }
    List<AiAssistantCard> cards = stores.stream()
        .map(store -> new AiAssistantCard(
            "store",
            store.name(),
            store.summary() + " · " + store.address(),
            "查看店铺",
            "/stores/detail",
            java.util.Map.of("storeId", store.id())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "店铺查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("搜索店铺", null, "/search", java.util.Map.of("keyword", keyword(text))))));
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
        rs.getString("tag_text"));
  }

  private String keyword(String text) {
    for (String candidate : List.of("汉堡", "拌饭", "炸鸡", "酒店", "电影", "密室", "按摩", "足疗", "SPA", "spa", "烤肉", "小馆")) {
      if (text.contains(candidate)) return candidate;
    }
    if (containsAny(text, "附近", "推荐", "周边")) return "";
    String cleaned = text.replaceAll("[，。！？、,.!?]", " ").trim();
    if (cleaned.length() > 20) return "";
    return cleaned;
  }

  private String like(String text) {
    String value = keyword(text);
    return value.isBlank() ? "" : "%" + value + "%";
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  record StoreRow(long id, String name, String businessType, String summary, String address, String distanceText,
                  BigDecimal rating, int monthlySales, BigDecimal avgPrice, String hours, String tags) {}
}

@Component
class ItemLookupSkill implements AiSkill {
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
    return "读取公开商品/服务、价格、库存、使用规则和退款规则";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    String text = context.normalizedMessage();
    if (!containsAny(text, "商品", "套餐", "券", "服务", "价格", "多少钱", "库存", "预约", "核销", "退款规则", "汉堡", "拌饭", "酒店", "电影", "SPA", "spa")) {
      return Optional.empty();
    }
    String key = keyword(text);
    List<ItemRow> items = jdbcTemplate.query(
        """
        select i.id, i.item_name, i.subtitle, i.price, i.original_price, i.store_id, s.store_name,
               coalesce(sku.stock, 0) as stock, i.usage_rules, i.refund_policy, i.notice
        from catalog_item i
        join merchant_store s on s.id = i.store_id and s.is_deleted = 0 and s.status = 'open'
        left join (
          select item_id, sum(case when status = 'on_sale' then stock else 0 end) as stock
          from catalog_sku
          where is_deleted = 0
          group by item_id
        ) sku on sku.item_id = i.id
        where i.is_deleted = 0 and i.status = 'on_sale'
          and (? = '' or i.item_name like ? or i.subtitle like ? or i.tag_text like ? or s.store_name like ?)
        order by i.sales_count desc, i.id
        limit 4
        """,
        this::mapItem,
        key,
        like(key),
        like(key),
        like(key),
        like(key));
    if (items.isEmpty()) return Optional.empty();
    StringBuilder summary = new StringBuilder("匹配到商品/服务：");
    for (ItemRow item : items) {
      summary.append("\n- ")
          .append(item.name())
          .append("，")
          .append(item.storeName())
          .append("，¥")
          .append(item.price())
          .append("，库存 ")
          .append(item.stock())
          .append(item.usageRules() == null ? "" : "，" + item.usageRules());
    }
    List<AiAssistantCard> cards = items.stream()
        .map(item -> new AiAssistantCard(
            "item",
            item.name(),
            item.storeName() + " · ¥" + item.price() + " · 库存 " + item.stock(),
            "查看详情",
            "/items/detail",
            java.util.Map.of("itemId", item.id(), "storeId", item.storeId())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "商品服务查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("继续搜索", null, "/search", java.util.Map.of("keyword", key)))));
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
        rs.getInt("stock"),
        rs.getString("usage_rules"),
        rs.getString("refund_policy"),
        rs.getString("notice"));
  }

  private String keyword(String text) {
    for (String candidate : List.of("汉堡", "拌饭", "炸鸡", "酒店", "电影", "密室", "按摩", "足疗", "SPA", "spa", "烤肉", "套餐", "券")) {
      if (text.contains(candidate)) return candidate;
    }
    String cleaned = text.replaceAll("[，。！？、,.!?]", " ").trim();
    if (cleaned.length() > 20) return "";
    return cleaned;
  }

  private String like(String key) {
    return key.isBlank() ? "" : "%" + key + "%";
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  record ItemRow(long id, String name, String subtitle, BigDecimal price, BigDecimal originalPrice, long storeId,
                 String storeName, int stock, String usageRules, String refundPolicy, String notice) {}
}

@Component
class ReviewLookupSkill implements AiSkill {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
  private final JdbcTemplate jdbcTemplate;

  ReviewLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "review_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户最近评价、评分、商家回复、点赞举报入口";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    String text = context.normalizedMessage();
    if (!containsAny(text, "评价", "评分", "评论", "点赞", "举报", "商家回复", "我的评价")) {
      return Optional.empty();
    }
    List<ReviewRow> reviews = jdbcTemplate.query(
        """
        select r.id, r.order_id, o.store_name, r.rating, r.content, r.helpful_count, r.reported_count,
               r.status, r.replied, rr.reply_content, r.created_at
        from review_record r
        join order_main o on o.id = r.order_id and o.is_deleted = 0
        left join review_reply rr on rr.review_id = r.id and rr.is_deleted = 0
        where r.user_id = ? and r.is_deleted = 0
        order by r.created_at desc, r.id desc
        limit 3
        """,
        this::mapReview,
        context.currentUser().userId());
    if (reviews.isEmpty()) {
      return Optional.of(new AiSkillResult(
          name(),
          "评价查询",
          "当前账号还没有评价记录。完成订单后可进入订单详情发布评价。",
          List.of(),
          List.of(new AiAssistantAction("查看订单", null, "/orders", java.util.Map.of()))));
    }
    StringBuilder summary = new StringBuilder("用户最近评价：");
    for (ReviewRow review : reviews) {
      summary.append("\n- ")
          .append(review.storeName())
          .append("，")
          .append(review.rating())
          .append("星，")
          .append(statusLabel(review.status()))
          .append("，有用 ")
          .append(review.helpfulCount())
          .append("，举报 ")
          .append(review.reportedCount())
          .append(review.replied() ? "，商家已回复" : "，商家未回复");
    }
    List<AiAssistantCard> cards = reviews.stream()
        .map(review -> new AiAssistantCard(
            "review",
            review.storeName() + " · " + review.rating() + "星",
            limit(review.content(), 50),
            "查看评价",
            "/review/detail",
            java.util.Map.of("reviewId", review.id())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(),
        "评价查询",
        summary.toString(),
        cards,
        List.of(new AiAssistantAction("我的评价", null, "/review/my", java.util.Map.of()))));
  }

  private ReviewRow mapReview(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new ReviewRow(
        rs.getLong("id"),
        rs.getLong("order_id"),
        rs.getString("store_name"),
        rs.getInt("rating"),
        rs.getString("content"),
        rs.getInt("helpful_count"),
        rs.getInt("reported_count"),
        rs.getString("status"),
        rs.getInt("replied") == 1,
        rs.getString("reply_content"),
        createdAt == null ? null : DATE_FORMATTER.format(createdAt.toLocalDateTime()));
  }

  private String statusLabel(String status) {
    return switch (status == null ? "" : status) {
      case "published" -> "已发布";
      case "hidden" -> "已隐藏";
      case "pending" -> "待审核";
      default -> status == null || status.isBlank() ? "状态未知" : status;
    };
  }

  private String limit(String value, int max) {
    if (value == null) return "";
    return value.length() <= max ? value : value.substring(0, max - 1) + "…";
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  record ReviewRow(long id, long orderId, String storeName, int rating, String content, int helpfulCount,
                   int reportedCount, String status, boolean replied, String replyContent, String createdAt) {}
}

@Component
class ComplaintLookupSkill implements AiSkill {
  private final JdbcTemplate jdbcTemplate;

  ComplaintLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "complaint_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户投诉工单状态、关联订单和补充材料入口";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    String text = context.normalizedMessage();
    if (!containsAny(text, "投诉", "工单", "处理进度", "平台处理", "纠纷", "证据")) {
      return Optional.empty();
    }
    List<TicketRow> tickets = jdbcTemplate.query(
        """
        select t.id, t.ticket_no, t.title, t.category, t.status, ms.store_name, o.order_no
        from complaint_ticket t
        left join merchant_store ms on ms.id = t.store_id and ms.is_deleted = 0
        left join order_main o on o.id = t.order_id and o.is_deleted = 0
        where t.user_id = ? and t.is_deleted = 0
        order by t.created_at desc, t.id desc
        limit 3
        """,
        this::mapTicket,
        context.currentUser().userId());
    String content;
    if (tickets.isEmpty()) {
      content = "当前账号暂无投诉工单。涉及退款、食品安全、服务态度或评价争议时，可提交投诉并上传图片证据。";
    } else {
      StringBuilder builder = new StringBuilder("用户投诉工单：");
      for (TicketRow ticket : tickets) {
        builder.append("\n- ")
            .append(ticket.ticketNo())
            .append("，")
            .append(ticket.title())
            .append("，")
            .append(ticket.status())
            .append(ticket.storeName() == null ? "" : "，" + ticket.storeName());
      }
      content = builder.toString();
    }
    return Optional.of(new AiSkillResult(
        name(),
        "投诉工单查询",
        content,
        List.of(new AiAssistantCard("complaint", "投诉与建议", "查看工单处理进度，或补充图片证据和说明。", "查看投诉", "/complaint/list", java.util.Map.of())),
        List.of(
            new AiAssistantAction("查看投诉", null, "/complaint/list", java.util.Map.of()),
            new AiAssistantAction("提交投诉", null, "/complaint/submit", java.util.Map.of()))));
  }

  private TicketRow mapTicket(ResultSet rs, int rowNum) throws SQLException {
    return new TicketRow(
        rs.getLong("id"),
        rs.getString("ticket_no"),
        rs.getString("title"),
        rs.getString("category"),
        rs.getString("status"),
        rs.getString("store_name"),
        rs.getString("order_no"));
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  record TicketRow(long id, String ticketNo, String title, String category, String status, String storeName, String orderNo) {}
}

@Component
class UserInsightSkill implements AiSkill {
  private final JdbcTemplate jdbcTemplate;

  UserInsightSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "account_summary";
  }

  @Override
  public String description() {
    return "读取当前用户账号、会员、地址、收藏和站内消息摘要";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    String text = context.normalizedMessage();
    if (!containsAny(text, "我的", "账号", "会员", "地址", "收藏", "喜欢", "消息", "通知", "资料", "个人")) {
      return Optional.empty();
    }
    long userId = context.currentUser().userId();
    ProfileRow profile = jdbcTemplate.query(
        """
        select p.nickname, p.member_level_name, p.growth_value, a.phone,
               (select count(1) from user_address where user_id = p.id and is_deleted = 0) as address_count,
               (select count(1) from user_favorite where user_id = p.id and is_deleted = 0) as favorite_count,
               (select count(1) from support_station_message where user_id = p.id and read_status = 'unread' and is_deleted = 0) as unread_count
        from user_profile p
        join iam_account a on a.id = p.account_id and a.is_deleted = 0
        where p.id = ? and p.is_deleted = 0
        limit 1
        """,
        rs -> rs.next() ? new ProfileRow(
            rs.getString("nickname"),
            rs.getString("member_level_name"),
            rs.getInt("growth_value"),
            maskPhone(rs.getString("phone")),
            rs.getLong("address_count"),
            rs.getLong("favorite_count"),
            rs.getLong("unread_count")) : null,
        userId);
    if (profile == null) return Optional.empty();
    String content = "账号摘要：" + profile.nickname()
        + "，" + profile.memberLevel()
        + "，成长值 " + profile.growthValue()
        + "，手机号 " + profile.maskedPhone()
        + "，地址 " + profile.addressCount()
        + " 个，收藏 " + profile.favoriteCount()
        + " 个，未读消息 " + profile.unreadCount()
        + " 条。";
    return Optional.of(new AiSkillResult(
        name(),
        "账号摘要",
        content,
        List.of(),
        List.of(
            new AiAssistantAction("个人中心", null, "/profile", java.util.Map.of()),
            new AiAssistantAction("我的收藏", null, "/favorites", java.util.Map.of()),
            new AiAssistantAction("消息中心", null, "/messages", java.util.Map.of()))));
  }

  private String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) return "未绑定";
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }

  record ProfileRow(String nickname, String memberLevel, int growthValue, String maskedPhone,
                    long addressCount, long favoriteCount, long unreadCount) {}
}
