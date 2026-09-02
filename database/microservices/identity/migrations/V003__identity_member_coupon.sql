CREATE TABLE IF NOT EXISTS member_level (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  level_code VARCHAR(32) NOT NULL,
  level_name VARCHAR(64) NOT NULL,
  min_growth_value INT NOT NULL DEFAULT 0,
  benefits VARCHAR(1000),
  icon_url VARCHAR(255),
  color VARCHAR(16),
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'enabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_level_code (level_code)
);

CREATE TABLE IF NOT EXISTS coupon_template (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  type VARCHAR(24) NOT NULL,
  face_value DECIMAL(10,2) NOT NULL,
  threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  business_scope VARCHAR(32) NOT NULL DEFAULT 'all',
  valid_kind VARCHAR(16) NOT NULL,
  valid_start DATETIME,
  valid_end DATETIME,
  valid_days INT,
  total_qty INT NOT NULL DEFAULT 0,
  issued_qty INT NOT NULL DEFAULT 0,
  per_user_limit INT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'enabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'unused',
  claimed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_at DATETIME NOT NULL,
  used_at DATETIME,
  used_order_id BIGINT,
  type_snapshot VARCHAR(24) NOT NULL,
  face_value_snapshot DECIMAL(10,2) NOT NULL,
  threshold_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_user_coupon_user ON user_coupon (user_id, status);
CREATE INDEX idx_user_coupon_template ON user_coupon (template_id);
CREATE INDEX idx_user_coupon_order ON user_coupon (used_order_id);
