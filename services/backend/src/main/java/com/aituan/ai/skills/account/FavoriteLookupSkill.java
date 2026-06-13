package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.limit;
import static com.aituan.ai.AiSkillSupport.params;
import static com.aituan.ai.AiSkillSupport.shouldRun;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class FavoriteLookupSkill implements AiSkill {
  private static final List<String> WORDS = List.of("收藏", "喜欢", "想去", "关注", "我的收藏");

  private final JdbcTemplate jdbcTemplate;

  FavoriteLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "favorite_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户真实收藏店铺/商品";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<FavoriteRow> rows = jdbcTemplate.query(
        """
        select id, favorite_type, target_id, target_name, subtitle, created_at
        from user_favorite
        where user_id = ? and is_deleted = 0
        order by created_at desc, id desc
        limit 8
        """,
        this::mapFavorite,
        context.currentUser().userId());
    if (rows.isEmpty()) return Optional.of(AiSkillResult.text(name(), "收藏查询", "当前账号暂无收藏内容。"));
    StringBuilder summary = new StringBuilder("当前用户收藏：");
    for (FavoriteRow row : rows) {
      summary.append("\n- ").append(typeLabel(row.type())).append("：")
          .append(row.name()).append(row.subtitle() == null ? "" : "，" + limit(row.subtitle(), 42));
    }
    List<AiAssistantCard> cards = rows.stream()
        .limit(4)
        .map(row -> new AiAssistantCard(row.type(), row.name(), row.subtitle(), "查看", route(row.type()), params("targetId", row.targetId())))
        .toList();
    return Optional.of(new AiSkillResult(
        name(), "收藏查询", summary.toString(), cards,
        List.of(new AiAssistantAction("我的收藏", null, "/favorites", params()))));
  }

  private String typeLabel(String type) {
    return switch (type == null ? "" : type) {
      case "store" -> "店铺";
      case "item" -> "商品";
      default -> type == null || type.isBlank() ? "收藏" : type;
    };
  }

  private String route(String type) {
    return "item".equals(type) ? "/items/detail" : "/stores/detail";
  }

  private FavoriteRow mapFavorite(ResultSet rs, int rowNum) throws SQLException {
    return new FavoriteRow(rs.getLong("id"), rs.getString("favorite_type"), rs.getLong("target_id"),
        rs.getString("target_name"), rs.getString("subtitle"));
  }

  record FavoriteRow(long id, String type, long targetId, String name, String subtitle) {}
}
