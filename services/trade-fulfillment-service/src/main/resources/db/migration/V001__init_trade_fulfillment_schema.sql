CREATE TABLE IF NOT EXISTS cart (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_cart_user_store (user_id, store_id)
);

CREATE TABLE IF NOT EXISTS cart_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cart_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_cart_item (cart_id, item_id)
);

CREATE TABLE IF NOT EXISTS order_main (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  store_name VARCHAR(120) NOT NULL,
  order_type VARCHAR(40) NOT NULL,
  title VARCHAR(255) NOT NULL,
  display_status VARCHAR(30) NOT NULL,
  payment_status VARCHAR(30) NOT NULL,
  fulfillment_status VARCHAR(40) NOT NULL DEFAULT 'created',
  payment_method VARCHAR(30),
  amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  delivery_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  package_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  payable_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  delivery_distance_km DECIMAL(8,2),
  estimated_arrival_at DATETIME,
  tableware_option VARCHAR(30),
  tableware_count INT,
  address_snapshot VARCHAR(500),
  voucher_summary VARCHAR(255),
  remark VARCHAR(255),
  idempotency_key VARCHAR(120),
  refund_status VARCHAR(30) NOT NULL DEFAULT 'none',
  refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  refund_reason VARCHAR(255),
  refunded_at DATETIME,
  refund_initiator_type VARCHAR(40),
  refund_initiator_id BIGINT,
  paid_at DATETIME,
  completed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_no (order_no),
  UNIQUE KEY uk_order_idempotency (user_id, idempotency_key)
);

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  item_name VARCHAR(120) NOT NULL,
  item_subtitle VARCHAR(255),
  business_type VARCHAR(40) NOT NULL,
  category_id BIGINT,
  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL,
  total_price DECIMAL(10,2) NOT NULL,
  cover_url VARCHAR(1000),
  is_reviewed TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS order_payment_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  payment_no VARCHAR(40) NOT NULL,
  payment_method VARCHAR(30) NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  provider_trade_no VARCHAR(120),
  paid_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_payment_no (payment_no)
);

CREATE TABLE IF NOT EXISTS order_voucher (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  voucher_code VARCHAR(40) NOT NULL,
  qr_payload VARCHAR(255) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'unused',
  usage_rules_snapshot VARCHAR(1000),
  refund_policy_snapshot VARCHAR(500),
  store_name_snapshot VARCHAR(120),
  effective_from DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  effective_to DATETIME,
  verified_at DATETIME,
  verified_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_voucher_code (voucher_code)
);

CREATE TABLE IF NOT EXISTS order_booking_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  business_type VARCHAR(40) NOT NULL,
  contact_name VARCHAR(60),
  contact_phone VARCHAR(20),
  booking_date DATE,
  booking_time_slot VARCHAR(60),
  guest_count INT NOT NULL DEFAULT 1,
  store_confirm_status VARCHAR(30) NOT NULL DEFAULT 'pending',
  store_confirm_remark VARCHAR(500),
  confirmed_at DATETIME,
  confirmed_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_order_booking_order (order_id)
);

CREATE TABLE IF NOT EXISTS order_refund_record (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  refund_no VARCHAR(40) NOT NULL,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  store_id BIGINT NOT NULL,
  refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'succeeded',
  initiator_type VARCHAR(40) NOT NULL,
  initiator_id BIGINT,
  reason VARCHAR(255),
  provider_refund_no VARCHAR(120),
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_refund_no (refund_no),
  UNIQUE KEY uk_refund_order (order_id)
);

CREATE TABLE IF NOT EXISTS delivery_task (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  current_stage VARCHAR(40) NOT NULL DEFAULT 'accepted',
  current_stage_text VARCHAR(120) NOT NULL DEFAULT '商家已接单',
  eta_minutes INT NOT NULL DEFAULT 35,
  next_tick_at DATETIME,
  completed_at DATETIME,
  auto_advance_enabled TINYINT NOT NULL DEFAULT 1,
  paused_at DATETIME,
  abnormal_reason VARCHAR(255),
  last_advanced_by BIGINT,
  last_advanced_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_delivery_task_order (order_id)
);

CREATE TABLE IF NOT EXISTS delivery_track_node (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  delivery_task_id BIGINT NOT NULL,
  node_order INT NOT NULL,
  node_code VARCHAR(40) NOT NULL,
  node_text VARCHAR(120) NOT NULL,
  reached_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_delivery_node (delivery_task_id, node_code)
);

CREATE TABLE IF NOT EXISTS order_state_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(40),
  to_status VARCHAR(40) NOT NULL,
  action_type VARCHAR(40) NOT NULL,
  operator_type VARCHAR(40) NOT NULL,
  operator_id BIGINT,
  remark VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_main_user_status ON order_main (user_id, display_status, created_at);
CREATE INDEX idx_order_main_refund_status ON order_main (refund_status, created_at);
CREATE INDEX idx_order_item_order ON order_item (order_id);
CREATE INDEX idx_order_refund_store_status ON order_refund_record (store_id, status, created_at);
CREATE INDEX idx_order_refund_user ON order_refund_record (user_id, created_at);
CREATE INDEX idx_order_voucher_order_status ON order_voucher (order_id, status);
CREATE INDEX idx_delivery_task_tick ON delivery_task (current_stage, next_tick_at, is_deleted);
