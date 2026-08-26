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
  UNIQUE KEY uk_merchant_no (merchant_no)
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
  cover_url VARCHAR(255),
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
  delivery_text VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_delivery_rule_store (store_id)
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
  cover_url VARCHAR(255),
  rule_text VARCHAR(500),
  sales_count INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'on_sale',
  item_kind VARCHAR(30) NOT NULL DEFAULT 'service',
  tag_text VARCHAR(255),
  sort_order INT NOT NULL DEFAULT 0,
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
