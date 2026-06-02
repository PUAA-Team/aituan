-- Stage6 成员A：会员等级、优惠券模板、用户优惠券，以及站内消息泛化跳转字段
-- 兼容 H2(MODE=MySQL) 与 MySQL：不使用 JSON 类型，权益以 VARCHAR 存 JSON 字符串

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
  type VARCHAR(24) NOT NULL,                       -- full_reduction 满减 / discount 折扣
  face_value DECIMAL(10,2) NOT NULL,               -- 满减=减免金额；折扣=折扣率(如0.90)
  threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  business_scope VARCHAR(32) NOT NULL DEFAULT 'all',
  valid_kind VARCHAR(16) NOT NULL,                 -- absolute 固定日期 / relative 领后N天
  valid_start DATETIME,
  valid_end DATETIME,
  valid_days INT,
  total_qty INT NOT NULL DEFAULT 0,                -- 0 表示不限量
  issued_qty INT NOT NULL DEFAULT 0,
  per_user_limit INT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'enabled',   -- enabled / disabled
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

-- 用户优惠券（与到店核销券 order_voucher 严格区分，不复用）
CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'unused',    -- unused 可用 / used 已用 / expired 失效
  claimed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_at DATETIME NOT NULL,
  used_at DATETIME,
  used_order_id BIGINT,
  type_snapshot VARCHAR(24) NOT NULL,              -- 领取时的模板规则快照，防模板改动影响已领券
  face_value_snapshot DECIMAL(10,2) NOT NULL,
  threshold_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_user_coupon_user (user_id, status),
  KEY idx_user_coupon_template (template_id)
);

-- 站内消息增加泛化跳转字段（保留 related_order_id 兼容）
ALTER TABLE support_station_message ADD COLUMN related_target_type VARCHAR(16);
ALTER TABLE support_station_message ADD COLUMN related_target_id BIGINT;
