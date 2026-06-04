CREATE TABLE IF NOT EXISTS merchant_support_auto_reply_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  keywords VARCHAR(255) NOT NULL,
  reply_content VARCHAR(500) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_support_auto_reply_merchant
  ON merchant_support_auto_reply_rule (merchant_id, enabled, is_deleted);
