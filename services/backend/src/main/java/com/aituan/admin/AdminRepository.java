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

  Optional<AdminProfileRow> findAdminProfile(long accountId) {
    List<AdminProfileRow> rows = jdbcTemplate.query(
        """
        select id, account_no, account_type, login_name, phone, email, status, created_at, last_login_at
        from iam_account
        where id = ? and account_type = 'ADMIN' and is_deleted = 0
        limit 1
        """,
        this::mapAdminProfile,
        accountId);
    return rows.stream().findFirst();
  }

  List<MerchantRow> listMerchants(String keyword, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select m.id, m.account_id, m.merchant_name, m.contact_name, m.contact_phone, m.license_no, m.status, m.audit_status, m.settled_at,
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

  List<ApplicationRow> listApplications(String status, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select id, application_no, account_id, merchant_name, contact_name, contact_phone, business_type, store_name, address,
               status, audit_remark, submitted_at, audited_by, audited_at
        from merchant_application
        where is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and status = ?");
      params.add(status.trim().toLowerCase());
    }
    sql.append(" order by submitted_at desc, id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapApplication, params.toArray());
  }

  long countApplications(String status) {
    if (status == null || status.isBlank()) {
      return count("select count(1) from merchant_application where is_deleted = 0");
    }
    return count("select count(1) from merchant_application where is_deleted = 0 and status = ?", status.trim().toLowerCase());
  }

  Optional<ApplicationRow> findApplication(long id) {
    List<ApplicationRow> rows = jdbcTemplate.query(
        """
        select id, application_no, account_id, merchant_name, contact_name, contact_phone, business_type, store_name, address,
               status, audit_remark, submitted_at, audited_by, audited_at
        from merchant_application
        where id = ? and is_deleted = 0
        limit 1
        """,
        this::mapApplication,
        id);
    return rows.stream().findFirst();
  }

  long insertMerchantAccount(String accountNo, String loginName, String passwordHash) {
    jdbcTemplate.update(
        """
        insert into iam_account(account_no, account_type, login_name, password_hash, status, created_at, updated_at)
        values (?, 'MERCHANT', ?, ?, 'normal', current_timestamp, current_timestamp)
        """,
        accountNo,
        loginName,
        passwordHash);
    return jdbcTemplate.queryForObject("select max(id) from iam_account where account_no = ?", Long.class, accountNo);
  }

  void insertAccountRole(long accountId, long roleId) {
    jdbcTemplate.update("insert into iam_account_role(account_id, role_id) values (?, ?) on duplicate key update role_id = values(role_id)", accountId, roleId);
  }

  long insertMerchantFromApplication(String merchantNo, long accountId, ApplicationRow application) {
    jdbcTemplate.update(
        """
        insert into merchant_profile(merchant_no, account_id, merchant_name, contact_name, contact_phone, license_no, status, audit_status, settled_at, created_at, updated_at)
        values (?, ?, ?, ?, ?, '', 'normal', 'approved', current_timestamp, current_timestamp, current_timestamp)
        """,
        merchantNo,
        accountId,
        application.merchantName(),
        application.contactName(),
        application.contactPhone());
    return jdbcTemplate.queryForObject("select max(id) from merchant_profile where merchant_no = ?", Long.class, merchantNo);
  }

  long insertStoreFromApplication(long merchantId, ApplicationRow application) {
    jdbcTemplate.update(
        """
        insert into merchant_store(merchant_id, store_name, business_type, summary, address, status, business_hours_text, tag_text, contact_phone, announcement, created_at, updated_at)
        values (?, ?, ?, ?, ?, 'open', '09:00-22:00', '', ?, '', current_timestamp, current_timestamp)
        """,
        merchantId,
        application.storeName(),
        application.businessType(),
        application.merchantName() + " 已通过入驻审核",
        application.address(),
        application.contactPhone());
    return jdbcTemplate.queryForObject("select max(id) from merchant_store where merchant_id = ?", Long.class, merchantId);
  }

  void updateApplicationAudit(long id, String status, Long accountId, Long auditorId, String remark) {
    jdbcTemplate.update(
        """
        update merchant_application
        set status = ?, account_id = ?, audit_remark = ?, audited_by = ?, audited_at = current_timestamp, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        status,
        accountId,
        remark,
        auditorId,
        id);
  }

  void insertMerchantAuditLog(String targetType, long targetId, String action, String result, String remark, Long operatedBy) {
    jdbcTemplate.update(
        "insert into merchant_audit_log(target_type, target_id, action, result, remark, operated_by) values (?, ?, ?, ?, ?, ?)",
        targetType,
        targetId,
        action,
        result,
        remark,
        operatedBy);
  }

  void insertSysAuditLog(Long actorId, String actionType, String targetType, long targetId, String detail) {
    jdbcTemplate.update(
        "insert into sys_audit_log(actor_type, actor_id, action_type, target_type, target_id, detail) values ('admin', ?, ?, ?, ?, ?)",
        actorId,
        actionType,
        targetType,
        targetId,
        detail);
  }

  Optional<MerchantRow> findMerchant(long merchantId) {
    List<MerchantRow> rows = jdbcTemplate.query(
        """
        select m.id, m.account_id, m.merchant_name, m.contact_name, m.contact_phone, m.license_no, m.status, m.audit_status, m.settled_at,
               (select count(1) from merchant_store s where s.merchant_id = m.id and s.is_deleted = 0) as store_count,
               (select count(1) from catalog_item i join merchant_store s2 on s2.id = i.store_id where s2.merchant_id = m.id and i.is_deleted = 0 and s2.is_deleted = 0) as item_count
        from merchant_profile m
        where m.id = ? and m.is_deleted = 0
        limit 1
        """,
        this::mapMerchant,
        merchantId);
    return rows.stream().findFirst();
  }

  long insertMerchant(String merchantNo, AdminMerchantUpsertRequest request, String status, String auditStatus) {
    jdbcTemplate.update(
        """
        insert into merchant_profile(merchant_no, account_id, merchant_name, contact_name, contact_phone, license_no, status, audit_status, settled_at, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, case when ? = 'approved' then current_timestamp else null end, current_timestamp, current_timestamp)
        """,
        merchantNo,
        request.accountId(),
        request.merchantName().trim(),
        clean(request.contactName()),
        clean(request.contactPhone()),
        clean(request.licenseNo()),
        status,
        auditStatus,
        auditStatus);
    return jdbcTemplate.queryForObject("select max(id) from merchant_profile", Long.class);
  }

  void updateMerchant(long merchantId, AdminMerchantUpsertRequest request, String status, String auditStatus) {
    jdbcTemplate.update(
        """
        update merchant_profile
        set account_id = ?, merchant_name = ?, contact_name = ?, contact_phone = ?, license_no = ?, status = ?, audit_status = ?,
            settled_at = case when ? = 'approved' and settled_at is null then current_timestamp else settled_at end,
            updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.accountId(),
        request.merchantName().trim(),
        clean(request.contactName()),
        clean(request.contactPhone()),
        clean(request.licenseNo()),
        status,
        auditStatus,
        auditStatus,
        merchantId);
  }

  List<StoreRow> listStores(Long merchantId, String businessType, String status, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select s.id, s.merchant_id, m.merchant_name, s.store_name, s.business_type, s.summary, s.address,
               s.status, s.business_hours_text, s.tag_text, s.cover_url, s.contact_phone, s.announcement, s.updated_at
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

  Optional<StoreRow> findStore(long storeId) {
    List<StoreRow> rows = jdbcTemplate.query(
        """
        select s.id, s.merchant_id, m.merchant_name, s.store_name, s.business_type, s.summary, s.address,
               s.status, s.business_hours_text, s.tag_text, s.cover_url, s.contact_phone, s.announcement, s.updated_at
        from merchant_store s
        join merchant_profile m on m.id = s.merchant_id
        where s.id = ? and s.is_deleted = 0 and m.is_deleted = 0
        limit 1
        """,
        this::mapStore,
        storeId);
    return rows.stream().findFirst();
  }

  long insertStore(AdminStoreUpsertRequest request, String businessType, String status) {
    jdbcTemplate.update(
        """
        insert into merchant_store(merchant_id, store_name, business_type, summary, address, status, business_hours_text, tag_text, cover_url, contact_phone, announcement, created_at, updated_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        request.merchantId(),
        request.storeName().trim(),
        businessType,
        request.summary().trim(),
        request.address().trim(),
        status,
        clean(request.businessHoursText()),
        clean(request.tagText()),
        clean(request.coverUrl()),
        clean(request.contactPhone()),
        clean(request.announcement()));
    return jdbcTemplate.queryForObject("select max(id) from merchant_store", Long.class);
  }

  void updateStore(long storeId, AdminStoreUpsertRequest request, String businessType, String status) {
    jdbcTemplate.update(
        """
        update merchant_store
        set merchant_id = ?, store_name = ?, business_type = ?, summary = ?, address = ?, status = ?, business_hours_text = ?,
            tag_text = ?, cover_url = ?, contact_phone = ?, announcement = ?, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        request.merchantId(),
        request.storeName().trim(),
        businessType,
        request.summary().trim(),
        request.address().trim(),
        status,
        clean(request.businessHoursText()),
        clean(request.tagText()),
        clean(request.coverUrl()),
        clean(request.contactPhone()),
        clean(request.announcement()),
        storeId);
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

  List<CertificationMaterialRow> listCertificationMaterials(String status, int offset, int limit) {
    StringBuilder sql = new StringBuilder("""
        select cm.id, cm.merchant_id, cm.application_id, coalesce(m.merchant_name, '') as merchant_name,
               cm.material_type, cm.material_name, cm.file_url, cm.status, cm.reject_reason, cm.submitted_at, cm.audited_by, cm.audited_at
        from merchant_certification_material cm
        left join merchant_profile m on m.id = cm.merchant_id and m.is_deleted = 0
        where cm.is_deleted = 0
        """);
    List<Object> params = new ArrayList<>();
    if (status != null && !status.isBlank()) {
      sql.append(" and cm.status = ?");
      params.add(status.trim().toLowerCase());
    }
    sql.append(" order by cm.submitted_at desc, cm.id desc limit ? offset ?");
    params.add(limit);
    params.add(offset);
    return jdbcTemplate.query(sql.toString(), this::mapCertificationMaterial, params.toArray());
  }

  long countCertificationMaterials(String status) {
    if (status == null || status.isBlank()) {
      return count("select count(1) from merchant_certification_material where is_deleted = 0");
    }
    return count("select count(1) from merchant_certification_material where is_deleted = 0 and status = ?", status.trim().toLowerCase());
  }

  Optional<CertificationMaterialRow> findCertificationMaterial(long id) {
    List<CertificationMaterialRow> rows = jdbcTemplate.query(
        """
        select cm.id, cm.merchant_id, cm.application_id, coalesce(m.merchant_name, '') as merchant_name,
               cm.material_type, cm.material_name, cm.file_url, cm.status, cm.reject_reason, cm.submitted_at, cm.audited_by, cm.audited_at
        from merchant_certification_material cm
        left join merchant_profile m on m.id = cm.merchant_id and m.is_deleted = 0
        where cm.id = ? and cm.is_deleted = 0
        limit 1
        """,
        this::mapCertificationMaterial,
        id);
    return rows.stream().findFirst();
  }

  void updateCertificationMaterialStatus(long id, String status, String rejectReason, Long auditorId) {
    jdbcTemplate.update(
        """
        update merchant_certification_material
        set status = ?, reject_reason = ?, audited_by = ?, audited_at = current_timestamp, updated_at = current_timestamp
        where id = ? and is_deleted = 0
        """,
        status,
        rejectReason,
        auditorId,
        id);
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

  Optional<UserRow> findUser(long accountId) {
    List<UserRow> rows = jdbcTemplate.query(
        """
        select a.id as account_id, p.id as user_id, p.nickname, p.avatar_url, a.phone, a.email, a.status,
               p.created_at,
               (select count(1) from user_address ua where ua.user_id = p.id and ua.is_deleted = 0) as address_count,
               (select count(1) from order_main o where o.user_id = p.id and o.is_deleted = 0) as order_count
        from iam_account a
        join user_profile p on p.account_id = a.id
        where a.id = ? and a.account_type = 'USER' and a.is_deleted = 0 and p.is_deleted = 0
        limit 1
        """,
        this::mapUser,
        accountId);
    return rows.stream().findFirst();
  }

  void updateUser(long accountId, AdminUserUpdateRequest request, String status) {
    jdbcTemplate.update(
        """
        update iam_account
        set phone = ?, email = ?, status = ?, updated_at = current_timestamp
        where id = ? and account_type = 'USER' and is_deleted = 0
        """,
        cleanToNull(request.phone()),
        cleanToNull(request.email()),
        status,
        accountId);
    jdbcTemplate.update(
        """
        update user_profile
        set nickname = ?, avatar_url = ?, status = ?, updated_at = current_timestamp
        where account_id = ? and is_deleted = 0
        """,
        request.nickname().trim(),
        clean(request.avatarUrl()),
        status,
        accountId);
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

  private ApplicationRow mapApplication(ResultSet rs, int rowNum) throws SQLException {
    Timestamp submittedAt = rs.getTimestamp("submitted_at");
    Timestamp auditedAt = rs.getTimestamp("audited_at");
    return new ApplicationRow(
        rs.getLong("id"),
        rs.getString("application_no"),
        rs.getObject("account_id", Long.class),
        rs.getString("merchant_name"),
        rs.getString("contact_name"),
        rs.getString("contact_phone"),
        rs.getString("business_type"),
        rs.getString("store_name"),
        rs.getString("address"),
        rs.getString("status"),
        rs.getString("audit_remark"),
        submittedAt == null ? null : submittedAt.toLocalDateTime(),
        rs.getObject("audited_by", Long.class),
        auditedAt == null ? null : auditedAt.toLocalDateTime());
  }

  private CertificationMaterialRow mapCertificationMaterial(ResultSet rs, int rowNum) throws SQLException {
    Timestamp submittedAt = rs.getTimestamp("submitted_at");
    Timestamp auditedAt = rs.getTimestamp("audited_at");
    return new CertificationMaterialRow(
        rs.getLong("id"),
        rs.getObject("merchant_id", Long.class),
        rs.getObject("application_id", Long.class),
        rs.getString("merchant_name"),
        rs.getString("material_type"),
        rs.getString("material_name"),
        rs.getString("file_url"),
        rs.getString("status"),
        rs.getString("reject_reason"),
        submittedAt == null ? null : submittedAt.toLocalDateTime(),
        rs.getObject("audited_by", Long.class),
        auditedAt == null ? null : auditedAt.toLocalDateTime());
  }

  private MerchantRow mapMerchant(ResultSet rs, int rowNum) throws SQLException {
    Timestamp settledAt = rs.getTimestamp("settled_at");
    Long accountId = rs.getObject("account_id", Long.class);
    return new MerchantRow(rs.getLong("id"), accountId, rs.getString("merchant_name"), rs.getString("contact_name"), rs.getString("contact_phone"), rs.getString("license_no"), rs.getString("status"), rs.getString("audit_status"), rs.getLong("store_count"), rs.getLong("item_count"), settledAt == null ? null : settledAt.toLocalDateTime());
  }

  private StoreRow mapStore(ResultSet rs, int rowNum) throws SQLException {
    Timestamp updatedAt = rs.getTimestamp("updated_at");
    return new StoreRow(rs.getLong("id"), rs.getLong("merchant_id"), rs.getString("merchant_name"), rs.getString("store_name"), rs.getString("business_type"), rs.getString("summary"), rs.getString("address"), rs.getString("status"), rs.getString("business_hours_text"), rs.getString("tag_text"), rs.getString("cover_url"), rs.getString("contact_phone"), rs.getString("announcement"), updatedAt == null ? null : updatedAt.toLocalDateTime());
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

  private AdminProfileRow mapAdminProfile(ResultSet rs, int rowNum) throws SQLException {
    Timestamp createdAt = rs.getTimestamp("created_at");
    Timestamp lastLoginAt = rs.getTimestamp("last_login_at");
    return new AdminProfileRow(
        rs.getLong("id"),
        rs.getString("account_no"),
        rs.getString("account_type"),
        rs.getString("login_name"),
        rs.getString("phone"),
        rs.getString("email"),
        rs.getString("status"),
        createdAt == null ? null : createdAt.toLocalDateTime(),
        lastLoginAt == null ? null : lastLoginAt.toLocalDateTime());
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
  }

  private String cleanToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String cleanOrDefault(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value.trim().toLowerCase();
  }

  record AdminProfileRow(Long accountId, String accountNo, String accountType, String nickname, String phone, String email, String status, LocalDateTime createdAt, LocalDateTime lastLoginAt) {}

  record MerchantRow(Long merchantId, Long accountId, String merchantName, String contactName, String contactPhone, String licenseNo, String status, String auditStatus, long storeCount, long itemCount, LocalDateTime settledAt) {}

  record ApplicationRow(Long id, String applicationNo, Long accountId, String merchantName, String contactName, String contactPhone, String businessType, String storeName, String address, String status, String auditRemark, LocalDateTime submittedAt, Long auditedBy, LocalDateTime auditedAt) {}

  record CertificationMaterialRow(Long id, Long merchantId, Long applicationId, String merchantName, String materialType, String materialName, String fileUrl, String status, String rejectReason, LocalDateTime submittedAt, Long auditedBy, LocalDateTime auditedAt) {}

  record StoreRow(Long storeId, Long merchantId, String merchantName, String storeName, String businessType, String summary, String address, String status, String businessHoursText, String tagText, String coverUrl, String contactPhone, String announcement, LocalDateTime updatedAt) {}

  record UserRow(Long accountId, Long userId, String nickname, String avatarUrl, String phone, String email, String status, long addressCount, long orderCount, LocalDateTime createdAt) {}

  record DeliveryTaskRow(Long taskId, Long orderId, String orderNo, String storeName, String currentStage, String currentStageText, boolean autoAdvanceEnabled, LocalDateTime pausedAt, String abnormalReason, LocalDateTime nextTickAt, LocalDateTime completedAt, LocalDateTime updatedAt) {}

  record ConfigRow(String configKey, String configValue, String remark, LocalDateTime updatedAt) {}

  record AnnouncementRow(Long id, String title, String content, String targetClient, String coverUrl, String status, LocalDateTime startAt, LocalDateTime endAt, int sortOrder, Long createdBy, LocalDateTime updatedAt) {}

  record AuditRow(Long id, String actorType, Long actorId, String actionType, String targetType, Long targetId, String detail, LocalDateTime createdAt) {}
}
