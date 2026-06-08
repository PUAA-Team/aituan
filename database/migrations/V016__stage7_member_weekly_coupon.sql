-- Stage7：会员等级、成长值展示口径与每周会员券发放记录
-- 兼容 MySQL 8 与 H2(MODE=MySQL)：不使用触发器、存储过程和 JSON 专有类型。

CREATE TABLE IF NOT EXISTS member_weekly_coupon_rule (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  level_code VARCHAR(32) NOT NULL,
  template_id BIGINT NOT NULL,
  issue_quantity INT NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'enabled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_weekly_coupon_rule (level_code, template_id)
);

CREATE TABLE IF NOT EXISTS member_weekly_coupon_batch (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  week_start_date DATE NOT NULL,
  level_code VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_weekly_coupon_batch (user_id, week_start_date)
);

CREATE TABLE IF NOT EXISTS member_weekly_coupon_issue (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  rule_id BIGINT NOT NULL,
  seq_no INT NOT NULL,
  user_coupon_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_member_weekly_coupon_issue (batch_id, rule_id, seq_no)
);

CREATE INDEX idx_member_weekly_rule_level ON member_weekly_coupon_rule (level_code, status);
CREATE INDEX idx_member_weekly_issue_coupon ON member_weekly_coupon_issue (user_coupon_id);

UPDATE member_level
SET status = 'disabled', updated_at = current_timestamp
WHERE level_code = 'NORMAL';

INSERT INTO member_level (level_code, level_name, min_growth_value, benefits, color, sort_order, status) VALUES
  ('SILVER', '白银会员', 0, '[{"title":"白银基础权益","desc":"每周自动刷新 1 张会员优惠券"},{"title":"成长规则","desc":"消费 1 元得 1 成长值，首次评价得 3 成长值"}]', '#8C8C8C', 1, 'enabled'),
  ('GOLD', '黄金会员', 300, '[{"title":"黄金周券","desc":"每周自动刷新 2 张会员优惠券"},{"title":"优先客服","desc":"客服会话优先响应"}]', '#D79A00', 2, 'enabled'),
  ('PLATINUM', '白金会员', 1000, '[{"title":"白金周券","desc":"每周自动刷新 2 张更高面额优惠券"},{"title":"专属活动","desc":"优先参与平台活动"}]', '#7B8794', 3, 'enabled'),
  ('DIAMOND', '钻石会员', 3000, '[{"title":"钻石周券","desc":"每周自动刷新 3 张高额优惠券"},{"title":"售后优先","desc":"投诉与售后优先处理"}]', '#3B82F6', 4, 'enabled'),
  ('RED_DIAMOND', '红钻会员', 10000, '[{"title":"红钻周券","desc":"每周自动刷新 4 张高额优惠券"},{"title":"平台礼遇","desc":"享受重点活动专属权益"}]', '#D71918', 5, 'enabled'),
  ('BLACK_DIAMOND', '黑钻会员', 30000, '[{"title":"黑钻周券","desc":"每周自动刷新 5 张最高等级优惠券"},{"title":"顶级服务","desc":"享受平台最高等级服务响应"}]', '#111827', 6, 'enabled')
ON DUPLICATE KEY UPDATE level_name = VALUES(level_name), min_growth_value = VALUES(min_growth_value), benefits = VALUES(benefits), color = VALUES(color), sort_order = VALUES(sort_order), status = VALUES(status), updated_at = current_timestamp;

UPDATE user_profile
SET member_level_name = CASE
  WHEN growth_value >= 30000 THEN '黑钻会员'
  WHEN growth_value >= 10000 THEN '红钻会员'
  WHEN growth_value >= 3000 THEN '钻石会员'
  WHEN growth_value >= 1000 THEN '白金会员'
  WHEN growth_value >= 300 THEN '黄金会员'
  ELSE '白银会员'
END,
updated_at = current_timestamp
WHERE is_deleted = 0;

INSERT INTO coupon_template (id, name, type, face_value, threshold_amount, business_scope, valid_kind, valid_start, valid_end, valid_days, total_qty, issued_qty, per_user_limit, status) VALUES
  (9101, '白银每周券·满30减3', 'full_reduction', 3.00, 30.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9102, '黄金每周券·满30减5', 'full_reduction', 5.00, 30.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9103, '黄金每周券·满50减8', 'full_reduction', 8.00, 50.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9104, '白金每周券·满50减10', 'full_reduction', 10.00, 50.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9105, '白金每周券·满80减15', 'full_reduction', 15.00, 80.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9106, '钻石每周券·满50减12', 'full_reduction', 12.00, 50.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9107, '钻石每周券·满100减20', 'full_reduction', 20.00, 100.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9108, '钻石每周券·满150减30', 'full_reduction', 30.00, 150.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9109, '红钻每周券·满50减15', 'full_reduction', 15.00, 50.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9110, '红钻每周券·满100减25', 'full_reduction', 25.00, 100.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9111, '红钻每周券·满150减35', 'full_reduction', 35.00, 150.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9112, '红钻每周券·满200减50', 'full_reduction', 50.00, 200.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9113, '黑钻每周券·满50减20', 'full_reduction', 20.00, 50.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9114, '黑钻每周券·满100减30', 'full_reduction', 30.00, 100.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9115, '黑钻每周券·满150减45', 'full_reduction', 45.00, 150.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9116, '黑钻每周券·满200减60', 'full_reduction', 60.00, 200.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled'),
  (9117, '黑钻每周券·满300减90', 'full_reduction', 90.00, 300.00, 'member_weekly', 'relative', NULL, NULL, 7, 0, 0, 0, 'enabled')
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), face_value = VALUES(face_value), threshold_amount = VALUES(threshold_amount), business_scope = VALUES(business_scope), valid_kind = VALUES(valid_kind), valid_days = VALUES(valid_days), total_qty = VALUES(total_qty), per_user_limit = VALUES(per_user_limit), status = VALUES(status), updated_at = current_timestamp;

INSERT INTO member_weekly_coupon_rule (id, level_code, template_id, issue_quantity, sort_order, status) VALUES
  (9101, 'SILVER', 9101, 1, 1, 'enabled'),
  (9102, 'GOLD', 9102, 1, 1, 'enabled'),
  (9103, 'GOLD', 9103, 1, 2, 'enabled'),
  (9104, 'PLATINUM', 9104, 1, 1, 'enabled'),
  (9105, 'PLATINUM', 9105, 1, 2, 'enabled'),
  (9106, 'DIAMOND', 9106, 1, 1, 'enabled'),
  (9107, 'DIAMOND', 9107, 1, 2, 'enabled'),
  (9108, 'DIAMOND', 9108, 1, 3, 'enabled'),
  (9109, 'RED_DIAMOND', 9109, 1, 1, 'enabled'),
  (9110, 'RED_DIAMOND', 9110, 1, 2, 'enabled'),
  (9111, 'RED_DIAMOND', 9111, 1, 3, 'enabled'),
  (9112, 'RED_DIAMOND', 9112, 1, 4, 'enabled'),
  (9113, 'BLACK_DIAMOND', 9113, 1, 1, 'enabled'),
  (9114, 'BLACK_DIAMOND', 9114, 1, 2, 'enabled'),
  (9115, 'BLACK_DIAMOND', 9115, 1, 3, 'enabled'),
  (9116, 'BLACK_DIAMOND', 9116, 1, 4, 'enabled'),
  (9117, 'BLACK_DIAMOND', 9117, 1, 5, 'enabled')
ON DUPLICATE KEY UPDATE issue_quantity = VALUES(issue_quantity), sort_order = VALUES(sort_order), status = VALUES(status), updated_at = current_timestamp;
