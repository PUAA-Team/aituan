CREATE TABLE IF NOT EXISTS file_asset (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  owner_type VARCHAR(40) NOT NULL,
  owner_id BIGINT,
  biz_type VARCHAR(40) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_type VARCHAR(30) NOT NULL DEFAULT 'local',
  object_key VARCHAR(255) NOT NULL,
  public_url VARCHAR(500) NOT NULL,
  mime_type VARCHAR(120) NOT NULL,
  size_bytes BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_file_asset_object (object_key)
);

ALTER TABLE merchant_store ADD COLUMN contact_phone VARCHAR(20);
ALTER TABLE merchant_store ADD COLUMN announcement VARCHAR(500);

ALTER TABLE delivery_task ADD COLUMN auto_advance_enabled TINYINT NOT NULL DEFAULT 1;
ALTER TABLE delivery_task ADD COLUMN paused_at DATETIME;
ALTER TABLE delivery_task ADD COLUMN abnormal_reason VARCHAR(255);
ALTER TABLE delivery_task ADD COLUMN last_advanced_by BIGINT;
ALTER TABLE delivery_task ADD COLUMN last_advanced_at DATETIME;

CREATE TABLE IF NOT EXISTS platform_announcement (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  target_client VARCHAR(40) NOT NULL DEFAULT 'all',
  cover_url VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'draft',
  start_at DATETIME,
  end_at DATETIME,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);
