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
