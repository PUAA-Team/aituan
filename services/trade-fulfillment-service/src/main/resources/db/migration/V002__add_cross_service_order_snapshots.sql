ALTER TABLE order_main ADD COLUMN merchant_id BIGINT NULL AFTER store_id;
ALTER TABLE order_main ADD COLUMN coupon_id BIGINT NULL AFTER merchant_id;
ALTER TABLE order_main ADD COLUMN review_id BIGINT NULL AFTER coupon_id;
ALTER TABLE order_item ADD COLUMN sku_id BIGINT NULL AFTER item_id;

CREATE INDEX idx_order_main_merchant ON order_main (merchant_id, created_at);
CREATE INDEX idx_order_main_coupon ON order_main (coupon_id);
CREATE INDEX idx_order_main_review ON order_main (review_id);
CREATE INDEX idx_order_item_sku ON order_item (sku_id);
