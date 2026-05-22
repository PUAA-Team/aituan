package com.aituan.admin;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AdminRepository {
  private final JdbcTemplate jdbcTemplate;

  AdminRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  long count(String sql, Object... params) {
    Long value = jdbcTemplate.queryForObject(sql, Long.class, params);
    return value == null ? 0 : value;
  }

  BigDecimal sum(String sql, Object... params) {
    BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, params);
    return value == null ? BigDecimal.ZERO : value;
  }

  List<MerchantRow> listMerchants(String keyword, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select m.id, m.merchant_name, m.contact_name, m.contact_phone, m.status, m.audit_status, m.settled_at,
               (select count(1) from merchant_store s where s.merchant_id = m.id and s.is_deleted = 0) as store_count,
               (select count(1) from catalog_item i join merchant_store s2 on s2.id = i.store_id where s2.merchant_id = m.id and i.is_deleted = 0 and s2.is_deleted = 0) as item_count
        from merchant_profile m
        where m.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and m.merchant_name like ?");
      params.add("%" + keyword.trim() + "%");
    }
    sql.append(" order by m.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapMerchant, params.toArray());
  }

  long countMerchants(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return count("select count(1) from merchant_profile where is_deleted = 0");
    }
    return count("select count(1) from merchant_profile where is_deleted = 0 and merchant_name like ?", "%" + keyword.trim() + "%");
  }

  List<StoreRow> listStores(Long merchantId, String businessType, String status, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select s.id, s.merchant_id, m.merchant_name, s.store_name, s.business_type, s.summary, s.address,
               s.status, s.cover_url, s.contact_phone, s.announcement, s.updated_at
        from merchant_store s
        join merchant_profile m on m.id = s.merchant_id
        where s.is_deleted = 0 and m.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (merchantId != null) {
      sql.append(" and s.merchant_id = ?");
      params.add(merchantId);
    }
    if (businessType != null && !businessType.isBlank()) {
      sql.append(" and s.business_type = ?");
      params.add(businessType.trim().toLowerCase());
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and s.status = ?");
      params.add(status.trim().toLowerCase());
    }
    sql.append(" order by s.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapStore, params.toArray());
  }

  long countStores(Long merchantId, String businessType, String status) {
    StringBuilder sql = new StringBuilder("select count(1) from merchant_store where is_deleted = 0");
    List<Object> params = new ArrayList<>();
    if (merchantId != null) {
      sql.append(" and merchant_id = ?");
      params.add(merchantId);
    }
    if (businessType != null && !businessType.isBlank()) {
      sql.append(" and business_type = ?");
      params.add(businessType.trim().toLowerCase());
    }
    if (status != null && !status.isBlank()) {
      sql.append(" and status = ?");
      params.add(status.trim().toLowerCase());
    }
    Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return count == null ? 0 : count;
  }

  void updateMerchantStatus(long merchantId, String status) {
    jdbcTemplate.update("update merchant_profile set status = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", status, merchantId);
  }

  void updateStoreStatus(long storeId, String status) {
    jdbcTemplate.update("update merchant_store set status = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", status, storeId);
  }

  void updateStoreCover(long storeId, String coverUrl) {
    jdbcTemplate.update("update merchant_store set cover_url = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", coverUrl, storeId);
  }

  List<UserRow> listUsers(String keyword, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select a.id as account_id, p.id as user_id, p.nickname, p.avatar_url, a.phone, a.email, a.status,
               p.created_at,
               (select count(1) from user_address ua where ua.user_id = p.id and ua.is_deleted = 0) as address_count,
               (select count(1) from order_main o where o.user_id = p.id and o.is_deleted = 0) as order_count
        from iam_account a
        join user_profile p on p.account_id = a.id
        where a.account_type = 'USER' and a.is_deleted = 0 and p.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (keyword != null && !keyword.isBlank()) {
      sql.append(" and (p.nickname like ? or a.phone like ? or a.email like ?)");
      String value = "%" + keyword.trim() + "%";
      params.add(value);
      params.add(value);
      params.add(value);
    }
    sql.append(" order by a.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapUser, params.toArray());
  }

  long countUsers(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return count("select count(1) from iam_account where account_type = 'USER' and is_deleted = 0");
    }
    String value = "%" + keyword.trim() + "%";
    return count("""
        select count(1)
        from iam_account a join user_profile p on p.account_id = a.id
        where a.account_type = 'USER' and a.is_deleted = 0 and p.is_deleted = 0 and (p.nickname like ? or a.phone like ? or a.email like ?)
        """, value, value, value);
  }

  void updateUserStatus(long accountId, String status) {
    jdbcTemplate.update("update iam_account set status = ?, updated_at = current_timestamp where id = ? and account_type = 'USER' and is_deleted = 0", status, accountId);
    jdbcTemplate.update("update user_profile set status = ?, updated_at = current_timestamp where account_id = ? and is_deleted = 0", status, accountId);
  }

  List<DeliveryTaskRow> listDeliveryTasks(String stage, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select dt.id, dt.order_id, o.order_no, o.store_name, dt.current_stage, dt.current_stage_text,
               dt.auto_advance_enabled, dt.paused_at, dt.abnormal_reason, dt.next_tick_at, dt.completed_at, dt.updated_at
        from delivery_task dt
        join order_main o on o.id = dt.order_id
        where dt.is_deleted = 0 and o.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (stage != null && !stage.isBlank()) {
      sql.append(" and dt.current_stage = ?");
      params.add(stage.trim());
    }
    sql.append(" order by dt.updated_at desc, dt.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapDeliveryTask, params.toArray());
  }

  long countDeliveryTasks(String stage) {
    if (stage == null || stage.isBlank()) {
      return count("select count(1) from delivery_task where is_deleted = 0");
    }
    return count("select count(1) from delivery_task where is_deleted = 0 and current_stage = ?", stage.trim());
  }

  Optional<DeliveryTaskRow> findDeliveryTask(long taskId) {
    List<DeliveryTaskRow> rows = jdbcTemplate.query(
        """
        select dt.id, dt.order_id, o.order_no, o.store_name, dt.current_stage, dt.current_stage_text,
               dt.auto_advance_enabled, dt.paused_at, dt.abnormal_reason, dt.next_tick_at, dt.completed_at, dt.updated_at
        from delivery_task dt
        join order_main o on o.id = dt.order_id
        where dt.id = ? and dt.is_deleted = 0 and o.is_deleted = 0
        limit 1
        """,
        this::mapDeliveryTask,
        taskId);
    return rows.stream().findFirst();
  }

  void updateDeliveryStage(DeliveryTaskRow task, String nextStage, String nextText, String displayStatus, boolean completed, LocalDateTime nextTickAt, Long operatorId, String remark) {
    if (completed) {
      jdbcTemplate.update(
          """
          update delivery_task
          set current_stage = ?, current_stage_text = ?, completed_at = current_timestamp, next_tick_at = null,
              last_advanced_by = ?, last_advanced_at = current_timestamp, updated_at = current_timestamp
          where id = ? and is_deleted = 0
          """,
          nextStage,
          nextText,
          operatorId,
          task.taskId());
    } else {
      jdbcTemplate.update(
          """
          update delivery_task
          set current_stage = ?, current_stage_text = ?, next_tick_at = ?,
              last_advanced_by = ?, last_advanced_at = current_timestamp, updated_at = current_timestamp
          where id = ? and is_deleted = 0
          """,
          nextStage,
          nextText,
          nextTickAt == null ? null : Timestamp.valueOf(nextTickAt),
          operatorId,
          task.taskId());
    }
    jdbcTemplate.update("update delivery_track_node set reached_at = current_timestamp, updated_at = current_timestamp where delivery_task_id = ? and node_code = ?", task.taskId(), nextStage);
    jdbcTemplate.update("update order_main set display_status = ?, fulfillment_status = ?, completed_at = case when ? = 1 then current_timestamp else completed_at end, updated_at = current_timestamp where id = ?", displayStatus, nextStage, completed ? 1 : 0, task.orderId());
    jdbcTemplate.update("insert into order_state_log(order_id, from_status, to_status, action_type, operator_type, operator_id, remark) values (?, ?, ?, 'admin_delivery_advance', 'admin', ?, ?)", task.orderId(), task.currentStage(), nextStage, operatorId, remark);
    jdbcTemplate.update("insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail) values ('admin', ?, 'delivery_task_advance', 'delivery_task', ?, ?)", operatorId, task.taskId(), remark == null ? nextText : remark);
  }

  void pauseDeliveryTask(long taskId, Long operatorId) {
    jdbcTemplate.update("update delivery_task set paused_at = current_timestamp, auto_advance_enabled = 0, updated_at = current_timestamp where id = ? and is_deleted = 0", taskId);
    jdbcTemplate.update("insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail) values ('admin', ?, 'delivery_task_pause', 'delivery_task', ?, '暂停模拟配送')", operatorId, taskId);
  }

  void resumeDeliveryTask(long taskId, Long operatorId) {
    jdbcTemplate.update("update delivery_task set paused_at = null, auto_advance_enabled = 1, updated_at = current_timestamp where id = ? and is_deleted = 0", taskId);
    jdbcTemplate.update("insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail) values ('admin', ?, 'delivery_task_resume', 'delivery_task', ?, '恢复模拟配送')", operatorId, taskId);
  }

  void markDeliveryAbnormal(long taskId, String reason, Long operatorId) {
    jdbcTemplate.update("update delivery_task set current_stage = 'abnormal', current_stage_text = '订单异常，待处理', abnormal_reason = ?, auto_advance_enabled = 0, paused_at = current_timestamp, updated_at = current_timestamp where id = ? and is_deleted = 0", reason, taskId);
    jdbcTemplate.update("update order_main set display_status = 'pending', fulfillment_status = 'abnormal', updated_at = current_timestamp where id = (select order_id from delivery_task where id = ?)", taskId);
    jdbcTemplate.update("insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail) values ('admin', ?, 'delivery_task_abnormal', 'delivery_task', ?, ?)", operatorId, taskId, reason);
  }

  String configValue(String key, String defaultValue) {
    List<String> rows = jdbcTemplate.query("select config_value from sys_config where config_key = ? limit 1", (rs, rowNum) -> rs.getString("config_value"), key);
    return rows.stream().findFirst().orElse(defaultValue);
  }

  void upsertConfig(String key, String value, String remark) {
    jdbcTemplate.update(
        """
        insert into sys_config(config_key, config_value, remark, created_at, updated_at)
        values (?, ?, ?, current_timestamp, current_timestamp)
        on duplicate key update config_value = values(config_value), remark = values(remark), updated_at = current_timestamp
        """,
        key,
        value,
        remark);
  }

  List<ConfigRow> listConfigs() {
    return jdbcTemplate.query("select config_key, config_value, remark, updated_at from sys_config order by config_key", this::mapConfig);
  }

  List<AnnouncementRow> listAnnouncements(String status, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select id, title, content, target_client, cover_url, status, start_at, end_at, sort_order, created_by, updated_at
        from platform_announcement
        where is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status = ?");
      params.add(status.trim().toLowerCase());
    }
    sql.append(" order by sort_order, id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapAnnouncement, params.toArray());
  }

  long countAnnouncements(String status) {
    if (status == null || status.isBlank()) {
      return count("select count(1) from platform_announcement where is_deleted = 0");
    }
    return count("select count(1) from platform_announcement where is_deleted = 0 and status = ?", status.trim().toLowerCase());
  }

  long insertAnnouncement(AdminAnnouncementUpsertRequest request, Long createdBy) {
    jdbcTemplate.update(
        """
        insert into platform_announcement(title, content, target_client, cover_url, status, start_at, end_at, sort_order, created_by, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        request.title().trim(),
        request.content().trim(),
        cleanOrDefault(request.targetClient(), "all"),
        clean(request.coverUrl()),
        cleanOrDefault(request.status(), "draft"),
        request.startAt() == null ? null : Timestamp.valueOf(request.startAt()),
        request.endAt() == null ? null : Timestamp.valueOf(request.endAt()),
        request.sortOrder() == null ? 0 : request.sortOrder(),
        createdBy);
    return jdbcTemplate.queryForObject("select max(id) from platform_announcement", Long.class);
  }

  void updateAnnouncement(long id, AdminAnnouncementUpsertRequest request) {
    jdbcTemplate.update(
        """
        update platform_announcement
        set title = ?, content = ?, target_client = ?, cover_url = ?, status = ?, start_at = ?, end_at = ?, sort_order = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.title().trim(),
        request.content().trim(),
        cleanOrDefault(request.targetClient(), "all"),
        clean(request.coverUrl()),
        cleanOrDefault(request.status(), "draft"),
        request.startAt() == null ? null : Timestamp.valueOf(request.startAt()),
        request.endAt() == null ? null : Timestamp.valueOf(request.endAt()),
        request.sortOrder() == null ? 0 : request.sortOrder(),
        id);
  }

  void updateAnnouncementStatus(long id, String status) {
    jdbcTemplate.update("update platform_announcement set status = ?, updated_at = current_timestamp where id = ? and is_deleted = 0", status, id);
  }

  Optional<AnnouncementRow> findAnnouncement(long id) {
    List<AnnouncementRow> rows = jdbcTemplate.query(
        """
        select id, title, content, target_client, cover_url, status, start_at, end_at, sort_order, created_by, updated_at
        from platform_announcement
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapAnnouncement,
        id);
    return rows.stream().findFirst();
  }

  List<AuditRow> listAuditLogs(String actionType, int offset, int limit) {
    StringBuilder sql = new StringBuilder("select id, actor_type, actor_id, action_type, target_type, target_id, detail, created_at from sys_audit_log where 1 = 1");
    List<Object> params = new ArrayList<>();
    if (actionType != null && !actionType.isBlank()) {
      sql.append(" and action_type = ?");
      params.add(actionType.trim());
    }
    sql.append(" order by created_at desc, id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapAudit, params.toArray());
  }

  long countAuditLogs(String actionType) {
    if (actionType == null || actionType.isBlank()) {
      return count("select count(1) from sys_audit_log");
    }
    return count("select count(1) from sys_audit_log where action_type = ?", actionType.trim());
  }

  private MerchantRow mapMerchant(ResultSet rs, int rowNum) throws SQLException {
    Timestamp settledAt = rs.getTimestamp("settled_at");
    return new MerchantRow(rs.getLong("id"), rs.getString("merchant_name"), rs.getString("contact_name"), rs.getString("contact_phone"), rs.getString("status"), rs.getString("audit_status"), rs.getLong("store_count"), rs.getLong("item_count"), settledAt == null ? null : settledAt.toLocalDateTime());
  }

  private StoreRow mapStore(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new StoreRow(rs.getLong("id"), rs.getLong("merchant_id"), rs.getString("merchant_name"), rs.getString("store_name"), rs.getString("business_type"), rs.getString("summary"), rs.getString("address"), rs.getString("status"), rs.getString("cover_url"), rs.getString("contact_phone"), rs.getString("announcement"), updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private UserRow mapUser(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new UserRow(rs.getLong("account_id"), rs.getLong("user_id"), rs.getString("nickname"), rs.getString("avatar_url"), rs.getString("phone"), rs.getString("email"), rs.getString("status"), rs.getLong("address_count"), rs.getLong("order_count"), createdAt == null ? null : createdAt.toLocalDateTime());
  }

  private DeliveryTaskRow mapDeliveryTask(ResultSet rs, int rowNum) throws SQLException {
    Timestamp pausedAt = rs.getTimestamp("paused_at");
    Timestamp nextTickAt = rs.getTimestamp("next_tick_at");
    Timestamp completedAt = rs.getTimestamp("completed_at");
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new DeliveryTaskRow(rs.getLong("id"), rs.getLong("order_id"), rs.getString("order_no"), rs.getString("store_name"), rs.getString("current_stage"), rs.getString("current_stage_text"), rs.getBoolean("auto_advance_enabled"), pausedAt == null ? null : pausedAt.toLocalDateTime(), rs.getString("abnormal_reason"), nextTickAt == null ? null : nextTickAt.toLocalDateTime(), completedAt == null ? null : completedAt.toLocalDateTime(), updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private ConfigRow mapConfig(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new ConfigRow(rs.getString("config_key"), rs.getString("config_value"), rs.getString("remark"), updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private AnnouncementRow mapAnnouncement(ResultSet rs, int rowNum) throws SQLException {
    Timestamp startAt = rs.getTimestamp("start_at");
    Timestamp endAt = rs.getTimestamp("end_at");
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new AnnouncementRow(rs.getLong("id"), rs.getString("title"), rs.getString("content"), rs.getString("target_client"), rs.getString("cover_url"), rs.getString("status"), startAt == null ? null : startAt.toLocalDateTime(), endAt == null ? null : endAt.toLocalDateTime(), rs.getInt("sort_order"), rs.getLong("created_by"), updatedAt == null ? null : updatedAt.toLocalDateTime());
  }

  private AuditRow mapAudit(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    return new AuditRow(rs.getLong("id"), rs.getString("actor_type"), rs.getLong("actor_id"), rs.getString("action_type"), rs.getString("target_type"), rs.getLong("target_id"), rs.getString("detail"), createdAt == null ? null : createdAt.toLocalDateTime());
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
  }

  private String cleanOrDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value.trim().toLowerCase();
  }

  record MerchantRow(Long merchantId, String merchantName, String contactName, String contactPhone, String status, String auditStatus, long storeCount, long itemCount, LocalDateTime settledAt) {}

  record StoreRow(Long storeId, Long merchantId, String merchantName, String storeName, String businessType, String summary, String address, String status, String coverUrl, String contactPhone, String announcement, LocalDateTime updatedAt) {}

  record UserRow(Long accountId, Long userId, String nickname, String avatarUrl, String phone, String email, String status, long addressCount, long orderCount, LocalDateTime createdAt) {}

  record DeliveryTaskRow(Long taskId, Long orderId, String orderNo, String storeName, String currentStage, String currentStageText, boolean autoAdvanceEnabled, LocalDateTime pausedAt, String abnormalReason, LocalDateTime nextTickAt, LocalDateTime completedAt, LocalDateTime updatedAt) {}

  record ConfigRow(String configKey, String configValue, String remark, LocalDateTime updatedAt) {}

  record AnnouncementRow(Long id, String title, String content, String targetClient, String coverUrl, String status, LocalDateTime startAt, LocalDateTime endAt, int sortOrder, Long createdBy, LocalDateTime updatedAt) {}

  record AuditRow(Long id, String actorType, Long actorId, String actionType, String targetType, Long targetId, String detail, LocalDateTime createdAt) {}
}
