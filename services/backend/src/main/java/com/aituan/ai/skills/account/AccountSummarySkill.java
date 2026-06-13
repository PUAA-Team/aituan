package com.aituan.ai;

import static com.aituan.ai.AiSkillSupport.maskPhone;
import static com.aituan.ai.AiSkillSupport.params;
import static com.aituan.ai.AiSkillSupport.shouldRun;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class AccountSummarySkill implements AiSkill {
  private static final List<String> WORDS = List.of(
      "账号", "账户", "个人资料", "我的资料", "我的信息", "账号信息", "账号摘要",
      "账号总览", "个人总览", "我的情况", "个人情况", "账号情况", "全部信息", "所有信息", "总结一下");

  private final JdbcTemplate jdbcTemplate;

  AccountSummarySkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "account_summary";
  }

  @Override
  public String description() {
    return "读取当前用户账号、会员、地址、收藏、消息、订单、评价、投诉摘要";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    ProfileRow profile = jdbcTemplate.query(
        """
        select p.nickname, p.member_level_name, p.growth_value, a.phone, a.email,
               (select count(1) from user_address where user_id = p.id and is_deleted = 0) as address_count,
               (select count(1) from user_favorite where user_id = p.id and is_deleted = 0) as favorite_count,
               (select count(1) from support_station_message where user_id = p.id and read_status = 'unread' and is_deleted = 0) as unread_count,
               (select count(1) from order_main where user_id = p.id and is_deleted = 0) as order_count,
               (select count(1) from review_record where user_id = p.id and is_deleted = 0) as review_count,
               (select count(1) from complaint_ticket where user_id = p.id and is_deleted = 0) as complaint_count,
               (select count(1) from support_session where user_id = p.id and status = 'open' and is_deleted = 0) as open_support_count
        from user_profile p
        join iam_account a on a.id = p.account_id and a.is_deleted = 0
        where p.id = ? and p.is_deleted = 0
        limit 1
        """,
        rs -> rs.next() ? mapProfile(rs) : null,
        context.currentUser().userId());
    if (profile == null) return Optional.empty();
    String content = "账号真实摘要：" + profile.nickname()
        + "，" + profile.memberLevel()
        + "，成长值 " + profile.growthValue()
        + "，手机号 " + maskPhone(profile.phone())
        + "，邮箱 " + (profile.email() == null || profile.email().isBlank() ? "未绑定" : profile.email())
        + "，订单 " + profile.orderCount()
        + " 个，评价 " + profile.reviewCount()
        + " 条，投诉 " + profile.complaintCount()
        + " 个，地址 " + profile.addressCount()
        + " 个，收藏 " + profile.favoriteCount()
        + " 个，未读消息 " + profile.unreadCount()
        + " 条，进行中客服 " + profile.openSupportCount()
        + " 个。";
    return Optional.of(new AiSkillResult(
        name(),
        "账号摘要",
        content,
        List.of(),
        List.of(
            new AiAssistantAction("个人中心", null, "/profile", params()),
            new AiAssistantAction("我的收藏", null, "/favorites", params()),
            new AiAssistantAction("消息中心", null, "/messages", params()))));
  }

  private ProfileRow mapProfile(ResultSet rs) throws SQLException {
    return new ProfileRow(
        rs.getString("nickname"),
        rs.getString("member_level_name"),
        rs.getInt("growth_value"),
        rs.getString("phone"),
        rs.getString("email"),
        rs.getLong("address_count"),
        rs.getLong("favorite_count"),
        rs.getLong("unread_count"),
        rs.getLong("order_count"),
        rs.getLong("review_count"),
        rs.getLong("complaint_count"),
        rs.getLong("open_support_count"));
  }

  record ProfileRow(String nickname, String memberLevel, int growthValue, String phone, String email,
                    long addressCount, long favoriteCount, long unreadCount, long orderCount,
                    long reviewCount, long complaintCount, long openSupportCount) {}
}
