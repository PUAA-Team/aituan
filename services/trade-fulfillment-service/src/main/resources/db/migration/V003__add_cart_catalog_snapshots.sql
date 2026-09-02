ALTER TABLE cart ADD COLUMN store_name_snapshot VARCHAR(120) NULL;
ALTER TABLE cart ADD COLUMN business_type_snapshot VARCHAR(40) NULL;

ALTER TABLE cart_item ADD COLUMN item_name_snapshot VARCHAR(120) NULL;
ALTER TABLE cart_item ADD COLUMN item_subtitle_snapshot VARCHAR(255) NULL;
ALTER TABLE cart_item ADD COLUMN category_name_snapshot VARCHAR(120) NULL;
ALTER TABLE cart_item ADD COLUMN unit_price_snapshot DECIMAL(10,2) NULL;
ALTER TABLE cart_item ADD COLUMN stock_snapshot INT NULL;
ALTER TABLE cart_item ADD COLUMN status_snapshot VARCHAR(30) NULL;

UPDATE cart
SET store_name_snapshot = CONCAT('门店 ', store_id),
    business_type_snapshot = 'takeaway'
WHERE store_name_snapshot IS NULL;

UPDATE cart_item
SET item_name_snapshot = CONCAT('商品 ', item_id),
    unit_price_snapshot = 0,
    stock_snapshot = 0,
    status_snapshot = 'snapshot_only'
WHERE item_name_snapshot IS NULL;
