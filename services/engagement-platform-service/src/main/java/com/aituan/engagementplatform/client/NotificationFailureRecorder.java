package com.aituan.engagementplatform.client;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class NotificationFailureRecorder {
  private static final String CALLER_SERVICE = "engagement-platform-service";
  private final JdbcTemplate jdbcTemplate;

  NotificationFailureRecorder(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  void record(String type, Long targetId, String idempotencyKey, String error) {
    String detail = truncate("站内消息发送失败: " + error, 500);
    try {
      jdbcTemplate.update(
          """
          insert into sys_audit_log(
            actor_type, action_type, target_type, target_id, detail, caller_service, idempotency_key)
          values ('system', 'station_message_failed', ?, ?, ?, ?, ?)
          """,
          type, targetId, detail, CALLER_SERVICE, idempotencyKey);
    } catch (DuplicateKeyException ignored) {
      // 同一业务通知使用稳定幂等键，只保留一条失败轨迹。
    }
  }

  private String truncate(String value, int maxLength) {
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
