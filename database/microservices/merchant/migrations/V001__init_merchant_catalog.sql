CREATE TABLE IF NOT EXISTS merchant_profile (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_no VARCHAR(32) NOT NULL,
  account_id BIGINT,
  merchant_name VARCHAR(120) NOT NULL,
  contact_name VARCHAR(60),
  contact_phone VARCHAR(20),
  license_no VARCHAR(80),
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  audit_status VARCHAR(20) NOT NULL DEFAULT 'approved',
  settled_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_merchant_no (merchant_no),
  UNIQUE KEY uk_merchant_profile_account (account_id)
);

CREATE TABLE IF NOT EXISTS merchant_store (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT NOT NULL,
  store_name VARCHAR(120) NOT NULL,
  business_type VARCHAR(40) NOT NULL,
  summary VARCHAR(255) NOT NULL,
  address VARCHAR(255) NOT NULL,
  distance_text VARCHAR(40) NOT NULL DEFAULT '1km',
  longitude DECIMAL(10,6),
  latitude DECIMAL(10,6),
  rating DECIMAL(3,1) NOT NULL DEFAULT 5.0,
  monthly_sales INT NOT NULL DEFAULT 0,
  avg_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'open',
  business_hours_text VARCHAR(120),
  tag_text VARCHAR(255),
  cover_url VARCHAR(1000),
  contact_phone VARCHAR(20),
  announcement VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS merchant_delivery_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  start_price DECIMAL(10,2) NOT NULL DEFAULT 0,
  estimated_minutes INT NOT NULL DEFAULT 35,
  max_delivery_distance_km DECIMAL(6,2) NOT NULL DEFAULT 5.00,
  package_fee_mode VARCHAR(20) NOT NULL DEFAULT 'none',
  package_fee_fixed DECIMAL(10,2) NOT NULL DEFAULT 0,
  package_fee_per_item DECIMAL(10,2) NOT NULL DEFAULT 0,
  distance_extra_threshold_km DECIMAL(6,2) NOT NULL DEFAULT 0,
  distance_extra_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  distance_extra_step_km DECIMAL(6,2) NOT NULL DEFAULT 1,
  delivery_text VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_delivery_rule_store (store_id)
);

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

CREATE TABLE IF NOT EXISTS merchant_application (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  application_no VARCHAR(32) NOT NULL,
  account_id BIGINT,
  merchant_name VARCHAR(120) NOT NULL,
  contact_name VARCHAR(60) NOT NULL,
  contact_phone VARCHAR(20) NOT NULL,
  business_type VARCHAR(40) NOT NULL,
  store_name VARCHAR(120) NOT NULL,
  address VARCHAR(255) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'pending',
  audit_remark VARCHAR(500),
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  audited_by BIGINT,
  audited_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_merchant_application_no (application_no)
);

CREATE TABLE IF NOT EXISTS merchant_certification_material (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  merchant_id BIGINT,
  application_id BIGINT,
  material_type VARCHAR(40) NOT NULL,
  material_name VARCHAR(120) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'pending',
  reject_reason VARCHAR(500),
  submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  audited_by BIGINT,
  audited_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS merchant_audit_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  target_type VARCHAR(40) NOT NULL,
  target_id BIGINT NOT NULL,
  action VARCHAR(40) NOT NULL,
  result VARCHAR(30) NOT NULL,
  remark VARCHAR(500),
  operated_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS catalog_category (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT,
  store_id BIGINT,
  category_code VARCHAR(80) NOT NULL,
  category_name VARCHAR(80) NOT NULL,
  business_type VARCHAR(40) NOT NULL,
  category_level VARCHAR(30) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_catalog_category_code (category_code)
);

CREATE TABLE IF NOT EXISTS catalog_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  business_type VARCHAR(40) NOT NULL,
  category_id BIGINT NOT NULL,
  item_name VARCHAR(120) NOT NULL,
  subtitle VARCHAR(255) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2),
  cover_url VARCHAR(1000),
  rule_text VARCHAR(500),
  sales_count INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'on_sale',
  item_kind VARCHAR(30) NOT NULL DEFAULT 'service',
  tag_text VARCHAR(255),
  sort_order INT NOT NULL DEFAULT 0,
  business_attributes VARCHAR(2000),
  usage_rules VARCHAR(1000),
  refund_policy VARCHAR(500),
  notice VARCHAR(1000),
  validity_days INT NOT NULL DEFAULT 90,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS catalog_sku (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  item_id BIGINT NOT NULL,
  sku_name VARCHAR(120) NOT NULL DEFAULT '默认',
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 999,
  status VARCHAR(20) NOT NULL DEFAULT 'on_sale',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_catalog_sku_item_name (item_id, sku_name)
);

CREATE TABLE IF NOT EXISTS catalog_item_tag (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  tag_name VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_catalog_item_tag_name (tag_name)
);

CREATE TABLE IF NOT EXISTS catalog_item_tag_rel (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  item_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_catalog_item_tag_rel (item_id, tag_id)
);

CREATE TABLE IF NOT EXISTS ops_banner_config (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  banner_code VARCHAR(80) NOT NULL,
  title VARCHAR(120) NOT NULL,
  subtitle VARCHAR(255),
  target_type VARCHAR(40),
  target_id BIGINT,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_ops_banner_code (banner_code)
);

CREATE TABLE IF NOT EXISTS member_recommend_config (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  scene VARCHAR(40) NOT NULL,
  business_type VARCHAR(40),
  store_id BIGINT,
  item_id BIGINT,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'normal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_member_recommend_scene_item (scene, item_id)
);

CREATE INDEX idx_merchant_store_type_status ON merchant_store (business_type, status, is_deleted);
CREATE INDEX idx_catalog_item_store_type ON catalog_item (store_id, business_type, status, is_deleted);
CREATE INDEX idx_catalog_item_category ON catalog_item (category_id, sort_order);
CREATE INDEX idx_catalog_category_store ON catalog_category (store_id, business_type, category_level, sort_order);
CREATE INDEX idx_member_recommend_scene ON member_recommend_config (scene, business_type, sort_order);
