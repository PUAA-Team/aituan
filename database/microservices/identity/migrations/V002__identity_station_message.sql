CREATE TABLE IF NOT EXISTS support_station_message (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  message_type VARCHAR(40) NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(500) NOT NULL,
  badge_text VARCHAR(40),
  read_status VARCHAR(20) NOT NULL DEFAULT 'unread',
  related_order_id BIGINT,
  related_target_type VARCHAR(16),
  related_target_id BIGINT,
  idempotency_key VARCHAR(160),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_message_idempotency (idempotency_key)
);

CREATE INDEX idx_message_user_read ON support_station_message (user_id, read_status, created_at);
CREATE INDEX idx_message_target ON support_station_message (related_target_type, related_target_id);
