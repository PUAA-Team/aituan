INSERT INTO iam_role (id, role_code, role_name, status, remark) VALUES
  (1, 'USER', '消费者用户', 'normal', 'APP 用户'),
  (2, 'MERCHANT', '商家账号', 'normal', '商家端账号'),
  (3, 'ADMIN', '平台管理员', 'normal', '后台管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status), remark = VALUES(remark), updated_at = current_timestamp;

INSERT INTO iam_account (id, account_no, account_type, login_name, phone, email, password_hash, status) VALUES
  (1001, 'U1001', 'USER', 'demo_user', '13800000001', 'user@aituan.local', '123456', 'normal'),
  (2001, 'M2001', 'MERCHANT', 'demo_merchant', '13800000002', 'merchant@aituan.local', '123456', 'normal'),
  (3001, 'A3001', 'ADMIN', 'demo_admin', '13800000003', 'admin@aituan.local', '123456', 'normal')
ON DUPLICATE KEY UPDATE login_name = VALUES(login_name), phone = VALUES(phone), email = VALUES(email), password_hash = VALUES(password_hash), status = VALUES(status), updated_at = current_timestamp;

INSERT INTO iam_account_role (account_id, role_id) VALUES
  (1001, 1),
  (2001, 2),
  (3001, 3)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_profile (id, account_id, nickname, avatar_url, register_source, member_level_name, growth_value, status) VALUES
  (5001, 1001, '爱团用户', '', 'app', '白银会员', 120, 'normal')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), avatar_url = VALUES(avatar_url), member_level_name = VALUES(member_level_name), growth_value = VALUES(growth_value), status = VALUES(status), updated_at = current_timestamp;

INSERT INTO user_address (id, user_id, contact_name, contact_phone, province, city, district, detail_address, longitude, latitude, tag_name, is_default, delivery_note) VALUES
  (7001, 5001, '王同学', '13800000001', '北京市', '北京市', '海淀区', '学院路 37 号', 116.352000, 39.984000, '学校', 1, '送到校门口')
ON DUPLICATE KEY UPDATE contact_name = VALUES(contact_name), contact_phone = VALUES(contact_phone), province = VALUES(province), city = VALUES(city), district = VALUES(district), detail_address = VALUES(detail_address), longitude = VALUES(longitude), latitude = VALUES(latitude), tag_name = VALUES(tag_name), is_default = VALUES(is_default), delivery_note = VALUES(delivery_note), updated_at = current_timestamp;
