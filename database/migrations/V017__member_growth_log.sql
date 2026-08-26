CREATE TABLE IF NOT EXISTS member_growth_log (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  order_id BIGINT,
  source_type VARCHAR(32) NOT NULL,
  source_id BIGINT NOT NULL,
  delta INT NOT NULL,
  reason VARCHAR(100) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_growth_source (source_type, source_id)
);

CREATE INDEX idx_member_growth_user ON member_growth_log (user_id, created_at);
CREATE INDEX idx_member_growth_order ON member_growth_log (order_id, source_type);

UPDATE member_level
SET benefits = '[{"title":"白银基础权益","desc":"每周自动刷新 1 张会员优惠券"},{"title":"成长规则","desc":"订单完成后按实付金额累计成长值，首次评价得 3 成长值"}]',
    updated_at = current_timestamp
WHERE level_code = 'SILVER' AND is_deleted = 0;

INSERT INTO member_growth_log(user_id, order_id, source_type, source_id, delta, reason, created_at)
SELECT user_id, id, 'legacy_order_pay', id, FLOOR(coalesce(payable_amount, 0)), '历史支付成长值兼容记录', coalesce(paid_at, created_at)
FROM order_main
WHERE payment_status IN ('paid', 'refunded')
  AND is_deleted = 0
  AND coalesce(payable_amount, 0) >= 1;

INSERT INTO member_growth_log(user_id, order_id, source_type, source_id, delta, reason, created_at)
SELECT user_id, order_id, 'legacy_review_publish', id, 3, '历史评价成长值兼容记录', created_at
FROM review_record
WHERE is_deleted = 0;
