-- Stage6：订单退款闭环。兼容 MySQL 8 与 H2 MODE=MySQL，避免使用单方言语法。
ALTER TABLE order_main ADD COLUMN refund_status VARCHAR(30) NOT NULL DEFAULT 'none';
ALTER TABLE order_main ADD COLUMN refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE order_main ADD COLUMN refund_reason VARCHAR(255);
ALTER TABLE order_main ADD COLUMN refunded_at DATETIME;
ALTER TABLE order_main ADD COLUMN refund_initiator_type VARCHAR(40);
ALTER TABLE order_main ADD COLUMN refund_initiator_id BIGINT;

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

CREATE INDEX idx_order_main_refund_status ON order_main (refund_status, created_at);
CREATE INDEX idx_order_refund_store_status ON order_refund_record (store_id, status, created_at);
CREATE INDEX idx_order_refund_user ON order_refund_record (user_id, created_at);
CREATE INDEX idx_order_voucher_order_status ON order_voucher (order_id, status);
