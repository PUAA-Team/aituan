ALTER TABLE merchant_delivery_rule
  ADD COLUMN max_delivery_distance_km DECIMAL(6,2) NOT NULL DEFAULT 5.00;

ALTER TABLE order_main
  ADD COLUMN delivery_distance_km DECIMAL(8,2) NULL;

ALTER TABLE order_main
  ADD COLUMN estimated_arrival_at DATETIME NULL;

ALTER TABLE merchant_store MODIFY COLUMN cover_url VARCHAR(1000);
ALTER TABLE catalog_item MODIFY COLUMN cover_url VARCHAR(1000);
ALTER TABLE order_item MODIFY COLUMN cover_url VARCHAR(1000);
