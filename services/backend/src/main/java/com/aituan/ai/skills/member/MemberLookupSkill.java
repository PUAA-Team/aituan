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
class MemberLookupSkill implements AiSkill {
  private static final List<String> WORDS = List.of("会员", "等级", "成长值", "权益", "升级", "每周券", "周券");

  private final JdbcTemplate jdbcTemplate;

  MemberLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "member_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户会员等级、成长值、下一等级和会员权益";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    MemberRow member = jdbcTemplate.query(
        """
        select p.member_level_name, p.growth_value
        from user_profile p
        where p.id = ? and p.is_deleted = 0
        limit 1
        """,
        rs -> rs.next() ? new MemberRow(rs.getString("member_level_name"), rs.getInt("growth_value")) : null,
        context.currentUser().userId());
    if (member == null) return Optional.empty();
    List<LevelRow> levels = jdbcTemplate.query(
        """
        select level_name, min_growth_value, benefits
        from member_level
        where status = 'enabled' and is_deleted = 0
        order by min_growth_value asc, sort_order asc, id asc
        """,
        this::mapLevel);
    LevelRow current = null;
    LevelRow next = null;
    for (LevelRow level : levels) {
      if (member.growthValue() >= level.minGrowth()) current = level;
      if (next == null && member.growthValue() < level.minGrowth()) next = level;
    }
    StringBuilder summary = new StringBuilder("会员真实信息：")
        .append(member.levelName()).append("，成长值 ").append(member.growthValue());
    if (current != null && current.benefits() != null) {
      summary.append("，当前权益 ").append(limit(current.benefits(), 80));
    }
    if (next != null) {
      summary.append("，距离 ").append(next.levelName()).append(" 还差 ")
          .append(Math.max(0, next.minGrowth() - member.growthValue())).append(" 成长值。");
    } else {
      summary.append("，已达到当前最高等级。");
    }
    return Optional.of(new AiSkillResult(
        name(), "会员查询", summary.toString(),
        List.of(new AiAssistantCard("member", "会员中心", summary.toString(), "查看会员", "/profile", params())),
        List.of(new AiAssistantAction("会员中心", null, "/profile", params()))));
  }

  private LevelRow mapLevel(ResultSet rs, int rowNum) throws SQLException {
    return new LevelRow(rs.getString("level_name"), rs.getInt("min_growth_value"), rs.getString("benefits"));
  }

  record MemberRow(String levelName, int growthValue) {}
  record LevelRow(String levelName, int minGrowth, String benefits) {}
}
