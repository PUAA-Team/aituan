-- Stage5-D: 非外卖业务差异化字段、预约记录、券码扩展

-- 1) catalog_item 增加业务差异化字段
ALTER TABLE catalog_item ADD COLUMN business_attributes VARCHAR(2000);
ALTER TABLE catalog_item ADD COLUMN usage_rules VARCHAR(1000);
ALTER TABLE catalog_item ADD COLUMN refund_policy VARCHAR(500);
ALTER TABLE catalog_item ADD COLUMN notice VARCHAR(1000);
ALTER TABLE catalog_item ADD COLUMN validity_days INT NOT NULL DEFAULT 90;

-- 2) order_voucher 增加扩展字段（用法说明快照、退改政策快照、店名快照）
ALTER TABLE order_voucher ADD COLUMN usage_rules_snapshot VARCHAR(1000);
ALTER TABLE order_voucher ADD COLUMN refund_policy_snapshot VARCHAR(500);
ALTER TABLE order_voucher ADD COLUMN store_name_snapshot VARCHAR(120);

-- 3) 预约记录：用于酒店、休闲娱乐、丽人医美、洗脚按摩等需要到店时间的业务
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
