CREATE TABLE IF NOT EXISTS merchant_takeaway_setting (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  accept_mode VARCHAR(20) NOT NULL DEFAULT 'manual',
  updated_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_takeaway_setting_store (store_id)
);
