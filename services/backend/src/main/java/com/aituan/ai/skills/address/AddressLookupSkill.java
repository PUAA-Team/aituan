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
class AddressLookupSkill implements AiSkill {
  private static final List<String> WORDS = List.of("地址", "收货", "配送地址", "默认地址", "联系", "电话", "送到哪");

  private final JdbcTemplate jdbcTemplate;

  AddressLookupSkill(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public String name() {
    return "address_lookup";
  }

  @Override
  public String description() {
    return "读取当前用户真实收货地址、默认地址和配送备注";
  }

  @Override
  public Optional<AiSkillResult> evaluate(AiSkillContext context) {
    if (!shouldRun(context, WORDS)) return Optional.empty();
    List<AddressRow> addresses = jdbcTemplate.query(
        """
        select id, contact_name, contact_phone, province, city, district, detail_address, tag_name, is_default, delivery_note
        from user_address
        where user_id = ? and is_deleted = 0
        order by is_default desc, id desc
        limit 5
        """,
        this::mapAddress,
        context.currentUser().userId());
    if (addresses.isEmpty()) {
      return Optional.of(new AiSkillResult(
          name(), "地址查询", "当前账号暂无收货地址。下单前需要先添加地址。",
          List.of(), List.of(new AiAssistantAction("管理地址", null, "/profile", params()))));
    }
    StringBuilder summary = new StringBuilder("当前用户收货地址：");
    for (AddressRow row : addresses) {
      summary.append("\n- ").append(row.isDefault() ? "默认：" : "")
          .append(row.contactName()).append(" ").append(maskPhone(row.phone())).append("，")
          .append(row.province()).append(row.city()).append(row.district()).append(row.detail())
          .append(row.tag() == null ? "" : "，标签 " + row.tag())
          .append(row.note() == null || row.note().isBlank() ? "" : "，备注 " + row.note());
    }
    return Optional.of(new AiSkillResult(
        name(), "地址查询", summary.toString(),
        List.of(), List.of(new AiAssistantAction("管理地址", null, "/profile", params()))));
  }

  private AddressRow mapAddress(ResultSet rs, int rowNum) throws SQLException {
    return new AddressRow(
        rs.getLong("id"), rs.getString("contact_name"), rs.getString("contact_phone"),
        rs.getString("province"), rs.getString("city"), rs.getString("district"),
        rs.getString("detail_address"), rs.getString("tag_name"), rs.getInt("is_default") == 1,
        rs.getString("delivery_note"));
  }

  record AddressRow(long id, String contactName, String phone, String province, String city, String district,
                    String detail, String tag, boolean isDefault, String note) {}
}
