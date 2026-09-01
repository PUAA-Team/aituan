ALTER TABLE review_record ADD COLUMN order_mark_locked_at DATETIME NULL;
CREATE INDEX idx_review_order_mark_pending
  ON review_record(order_marked, order_mark_attempts, order_mark_locked_at, is_deleted);
