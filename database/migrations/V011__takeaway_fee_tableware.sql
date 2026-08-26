ALTER TABLE merchant_delivery_rule
  ADD COLUMN package_fee_mode VARCHAR(20) NOT NULL DEFAULT 'none';

ALTER TABLE merchant_delivery_rule
  ADD COLUMN package_fee_fixed DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE merchant_delivery_rule
  ADD COLUMN package_fee_per_item DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE merchant_delivery_rule
  ADD COLUMN distance_extra_threshold_km DECIMAL(6,2) NOT NULL DEFAULT 0;

ALTER TABLE merchant_delivery_rule
  ADD COLUMN distance_extra_fee DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE merchant_delivery_rule
  ADD COLUMN distance_extra_step_km DECIMAL(6,2) NOT NULL DEFAULT 1;

ALTER TABLE order_main
  ADD COLUMN package_fee DECIMAL(10,2) NOT NULL DEFAULT 0;

ALTER TABLE order_main
  ADD COLUMN tableware_option VARCHAR(30);

ALTER TABLE order_main
  ADD COLUMN tableware_count INT;
