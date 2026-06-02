package com.aituan.member;

import static com.aituan.common.jdbc.JdbcGeneratedKeys.insertAndReturnId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class MemberRepository {
  private final JdbcTemplate jdbcTemplate;

  MemberRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // userId 为 user_profile.id
  Integer findGrowthValue(long userId) {
    List<Integer> rows = jdbcTemplate.query(
        "select growth_value from user_profile where id = ? and is_deleted = 0",
        (rs, n) -> rs.getInt("growth_value"),
        userId);
    return rows.stream().findFirst().orElse(null);
  }

  // 启用等级，按成长值升序
  List<MemberLevelRow> listEnabledLevels() {
    return jdbcTemplate.query(
        """
        select id, level_code, level_name, min_growth_value, benefits, icon_url, color, sort_order, status
        from member_level
        where status = 'enabled' and is_deleted = 0
        order by min_growth_value asc, id asc
        """,
        this::mapLevel);
  }

  // 后台全部等级，按排序
  List<MemberLevelRow> listAllLevels() {
    return jdbcTemplate.query(
        """
        select id, level_code, level_name, min_growth_value, benefits, icon_url, color, sort_order, status
        from member_level
        where is_deleted = 0
        order by sort_order asc, min_growth_value asc, id asc
        """,
        this::mapLevel);
  }

  Optional<MemberLevelRow> findLevel(long id) {
    List<MemberLevelRow> rows = jdbcTemplate.query(
        """
        select id, level_code, level_name, min_growth_value, benefits, icon_url, color, sort_order, status
        from member_level
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapLevel,
        id);
    return rows.stream().findFirst();
  }

  Long insertLevel(MemberLevelUpsertRequest request, String benefitsJson) {
    return insertAndReturnId(
        jdbcTemplate,
        """
        insert into member_level(level_code, level_name, min_growth_value, benefits, icon_url, color, sort_order, status)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        request.levelCode().trim(),
        request.levelName().trim(),
        request.minGrowthValue(),
        benefitsJson,
        request.iconUrl(),
        request.color(),
        request.sortOrder() == null ? 0 : request.sortOrder(),
        normalizeStatus(request.status()));
  }

  void updateLevel(long id, MemberLevelUpsertRequest request, String benefitsJson) {
    jdbcTemplate.update(
        """
        update member_level
        set level_code = ?, level_name = ?, min_growth_value = ?, benefits = ?, icon_url = ?, color = ?, sort_order = ?, status = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.levelCode().trim(),
        request.levelName().trim(),
        request.minGrowthValue(),
        benefitsJson,
        request.iconUrl(),
        request.color(),
        request.sortOrder() == null ? 0 : request.sortOrder(),
        normalizeStatus(request.status()),
        id);
  }

  void insertAudit(Long actorId, String actionType, String targetType, Long targetId, String detail) {
    jdbcTemplate.update(
        """
        insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail)
        values ('admin', ?, ?, ?, ?, ?)
        """,
        actorId,
        actionType,
        targetType,
        targetId,
        detail);
  }

  private String normalizeStatus(String status) {
    return status == null || status.isBlank() ? "enabled" : status.trim();
  }

  private MemberLevelRow mapLevel(ResultSet rs, int rowNum) throws SQLException {
    return new MemberLevelRow(
        rs.getLong("id"),
        rs.getString("level_code"),
        rs.getString("level_name"),
        rs.getInt("min_growth_value"),
        rs.getString("benefits"),
        rs.getString("icon_url"),
        rs.getString("color"),
        rs.getInt("sort_order"),
        rs.getString("status"));
  }

  record MemberLevelRow(Long id, String levelCode, String levelName, int minGrowthValue, String benefits,
                        String iconUrl, String color, int sortOrder, String status) {}
}
