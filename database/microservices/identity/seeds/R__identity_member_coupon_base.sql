INSERT INTO member_level (id, level_code, level_name, min_growth_value, benefits, color, sort_order, status) VALUES
  (2, 'SILVER', '白银会员', 0, '[{"title":"白银基础权益","desc":"每周自动刷新 1 张会员优惠券"},{"title":"成长规则","desc":"订单完成后按实付金额累计成长值，首次评价得 3 成长值"}]', '#8C8C8C', 1, 'enabled'),
  (3, 'GOLD', '黄金会员', 300, '[{"title":"黄金周券","desc":"每周自动刷新 2 张会员优惠券"},{"title":"优先客服","desc":"客服会话优先响应"}]', '#D79A00', 2, 'enabled'),
  (4, 'PLATINUM', '白金会员', 1000, '[{"title":"白金周券","desc":"每周自动刷新 2 张更高面额优惠券"},{"title":"专属活动","desc":"优先参与平台活动"}]', '#7B8794', 3, 'enabled'),
  (5, 'DIAMOND', '钻石会员', 3000, '[{"title":"钻石周券","desc":"每周自动刷新 3 张高额优惠券"},{"title":"售后优先","desc":"投诉与售后优先处理"}]', '#3B82F6', 4, 'enabled'),
  (6, 'RED_DIAMOND', '红钻会员', 10000, '[{"title":"红钻周券","desc":"每周自动刷新 4 张高额优惠券"},{"title":"平台礼遇","desc":"享受重点活动专属权益"}]', '#D71918', 5, 'enabled'),
  (7, 'BLACK_DIAMOND', '黑钻会员', 30000, '[{"title":"黑钻周券","desc":"每周自动刷新 5 张最高等级优惠券"},{"title":"顶级服务","desc":"享受平台最高等级服务响应"}]', '#111827', 6, 'enabled')
ON DUPLICATE KEY UPDATE level_name = VALUES(level_name), min_growth_value = VALUES(min_growth_value), benefits = VALUES(benefits), color = VALUES(color), sort_order = VALUES(sort_order), status = VALUES(status), updated_at = current_timestamp;

INSERT INTO coupon_template (id, name, type, face_value, threshold_amount, business_scope, valid_kind, valid_start, valid_end, valid_days, total_qty, issued_qty, per_user_limit, status) VALUES
  (1, '新用户满 30 减 8', 'full_reduction', 8.00, 30.00, 'all', 'relative', NULL, NULL, 30, 1000, 2, 1, 'enabled'),
  (2, '外卖满 50 减 10', 'full_reduction', 10.00, 50.00, 'takeaway', 'relative', NULL, NULL, 15, 500, 1, 2, 'enabled'),
  (3, '到店服务 9 折券', 'discount', 0.10, 0.00, 'service', 'relative', NULL, NULL, 20, 300, 0, 1, 'enabled')
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), face_value = VALUES(face_value), threshold_amount = VALUES(threshold_amount), business_scope = VALUES(business_scope), valid_kind = VALUES(valid_kind), valid_days = VALUES(valid_days), total_qty = VALUES(total_qty), issued_qty = VALUES(issued_qty), per_user_limit = VALUES(per_user_limit), status = VALUES(status), updated_at = current_timestamp;

INSERT INTO user_coupon (id, template_id, user_id, status, expire_at, used_order_id, type_snapshot, face_value_snapshot, threshold_snapshot) VALUES
  (8001, 1, 5001, 'unused', '2099-12-31 23:59:59', NULL, 'full_reduction', 8.00, 30.00),
  (8002, 2, 5001, 'unused', '2099-12-31 23:59:59', NULL, 'full_reduction', 10.00, 50.00)
ON DUPLICATE KEY UPDATE status = VALUES(status), expire_at = VALUES(expire_at), used_order_id = VALUES(used_order_id), type_snapshot = VALUES(type_snapshot), face_value_snapshot = VALUES(face_value_snapshot), threshold_snapshot = VALUES(threshold_snapshot), updated_at = current_timestamp;
