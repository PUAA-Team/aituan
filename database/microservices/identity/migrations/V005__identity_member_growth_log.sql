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
