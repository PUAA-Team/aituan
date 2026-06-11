package com.aituan.ai;

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
class CartLookupSkill implements AiSkill {
  private static final List<String> WORDS = List.of("购物车", "车里", "加购", "已选", "结算", "买哪些");

  private final JdbcTemplate jdbcTemplate;

  CartLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "cart_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户真实购物车门店、商品、数量和小计";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<CartRow> rows = jdbcTemplate.query(
        """
        select c.id as cart_id, c.store_id, s.store_name, ci.item_id, i.item_name, ci.quantity, i.price,
               i.status, coalesce(sku.stock, 0) as stock
        from cart c
        join merchant_store s on s.id = c.store_id and s.is_deleted = 0
        join cart_item ci on ci.cart_id = c.id and ci.is_deleted = 0 and ci.quantity > 0
        join catalog_item i on i.id = ci.item_id and i.is_deleted = 0
        left join catalog_sku sku on sku.item_id = i.id and sku.sku_name = '默认' and sku.is_deleted = 0
        where c.user_id = ? and c.is_deleted = 0
        order by c.updated_at desc, ci.updated_at desc
        limit 10
        """,
        this::mapCart,
        context.currentUser().userId());
    if (rows.isEmpty()) return Optional.of(AiSkillResult.text(name(), "购物车查询", "当前购物车为空。"));
    BigDecimal total = BigDecimal.ZERO;
    StringBuilder summary = new StringBuilder("当前购物车真实内容：");
    for (CartRow row : rows) {
      BigDecimal subtotal = row.price().multiply(BigDecimal.valueOf(row.quantity()));
      total = total.add(subtotal);
      summary.append("\n- ").append(row.storeName()).append("：")
          .append(row.itemName()).append(" x").append(row.quantity())
          .append("，单价 ").append(money(row.price()))
          .append("，小计 ").append(money(subtotal))
          .append("，库存 ").append(row.stock())
          .append("，状态 ").append(row.status());
    }
    summary.append("\n购物车估算合计：").append(money(total)).append("。");
    return Optional.of(new AiSkillResult(
        name(), "购物车查询", summary.toString(),
        List.of(), List.of(new AiAssistantAction("去结算", null, "/orders", params()))));
  }

  private CartRow mapCart(ResultSet rs, int rowNum) throws SQLException {
    return new CartRow(rs.getLong("cart_id"), rs.getLong("store_id"), rs.getString("store_name"),
        rs.getLong("item_id"), rs.getString("item_name"), rs.getInt("quantity"),
        rs.getBigDecimal("price"), rs.getString("status"), rs.getInt("stock"));
  }

  record CartRow(long cartId, long storeId, String storeName, long itemId, String itemName, int quantity,
                 BigDecimal price, String status, int stock) {}
}
