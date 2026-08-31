CREATE TABLE IF NOT EXISTS member_weekly_coupon_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  level_code VARCHAR(32) NOT NULL,
  template_id BIGINT NOT NULL,
  issue_quantity INT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'enabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_weekly_coupon_rule (level_code, template_id)
);

CREATE TABLE IF NOT EXISTS member_weekly_coupon_batch (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  week_start_date DATE NOT NULL,
  level_code VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_weekly_coupon_batch (user_id, week_start_date)
);

CREATE TABLE IF NOT EXISTS member_weekly_coupon_issue (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  rule_id BIGINT NOT NULL,
  seq_no INT NOT NULL,
  user_coupon_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_weekly_coupon_issue (batch_id, rule_id, seq_no)
);

CREATE INDEX idx_member_weekly_rule_level ON member_weekly_coupon_rule (level_code, status);
CREATE INDEX idx_member_weekly_issue_coupon ON member_weekly_coupon_issue (user_coupon_id);
