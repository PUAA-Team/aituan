INSERT INTO iam_role (id, role_code, role_name, status, remark) VALUES
  (1, 'USER', '消费者用户', 'normal', 'APP 用户'),
  (2, 'MERCHANT', '商家账号', 'normal', '商家端账号'),
  (3, 'ADMIN', '平台管理员', 'normal', '后台管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = VALUES(status), remark = VALUES(remark), updated_at = current_timestamp;

INSERT INTO iam_account (id, account_no, account_type, login_name, phone, email, password_hash, status) VALUES
  (1, 'U1001', 'USER', 'demo_user', '13800000001', 'user@aituan.local', '123456', 'normal'),
  (2, 'M2001', 'MERCHANT', 'demo_merchant', '13800000002', 'merchant@aituan.local', '123456', 'normal'),
  (3, 'A3001', 'ADMIN', 'demo_admin', '13800000003', 'admin@aituan.local', '123456', 'normal'),
  (21, 'M202605170021', 'MERCHANT', 'demo_takeaway_merchant', '18800002021', 'takeaway@example.com', '123456', 'normal'),
  (22, 'M202605170022', 'MERCHANT', 'demo_groupbuy_merchant', '18800002022', 'groupbuy@example.com', '123456', 'normal'),
  (23, 'M202605170023', 'MERCHANT', 'demo_hotel_merchant', '18800002023', 'hotel@example.com', '123456', 'normal'),
  (24, 'M202605170024', 'MERCHANT', 'demo_entertainment_merchant', '18800002024', 'entertainment@example.com', '123456', 'normal'),
  (25, 'M202605170025', 'MERCHANT', 'demo_movie_merchant', '18800002025', 'movie@example.com', '123456', 'normal'),
  (26, 'M202605170026', 'MERCHANT', 'demo_beauty_merchant', '18800002026', 'beauty@example.com', '123456', 'normal'),
  (27, 'M202605170027', 'MERCHANT', 'demo_ticket_merchant', '18800002027', 'ticket@example.com', '123456', 'normal'),
  (28, 'M202605170028', 'MERCHANT', 'demo_massage_merchant', '18800002028', 'massage@example.com', '123456', 'normal'),
  (29, 'M202605170029', 'MERCHANT', 'demo_bibimbap_merchant', '18800002029', 'bibimbap@example.com', '123456', 'normal'),
  (30, 'M202605170030', 'MERCHANT', 'demo_bbq_merchant', '18800002030', 'bbq@example.com', '123456', 'normal'),
  (31, 'M202605170031', 'MERCHANT', 'demo_hotel_room_merchant', '18800002031', 'hotelroom@example.com', '123456', 'normal'),
  (32, 'M202605170032', 'MERCHANT', 'demo_arcade_merchant', '18800002032', 'arcade@example.com', '123456', 'normal'),
  (33, 'M202605170033', 'MERCHANT', 'demo_spa_merchant', '18800002033', 'spa@example.com', '123456', 'normal')
ON DUPLICATE KEY UPDATE login_name = VALUES(login_name), phone = VALUES(phone), email = VALUES(email), password_hash = VALUES(password_hash), status = VALUES(status), updated_at = current_timestamp;

INSERT INTO iam_account_role (account_id, role_id) VALUES
  (1, 1),
  (2, 2),
  (3, 3),
  (21, 2),
  (22, 2),
  (23, 2),
  (24, 2),
  (25, 2),
  (26, 2),
  (27, 2),
  (28, 2),
  (29, 2),
  (30, 2),
  (31, 2),
  (32, 2),
  (33, 2)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_profile (id, account_id, nickname, avatar_url, register_source, member_level_name, growth_value, status) VALUES
  (5001, 1, '爱团用户', '', 'app', '白银会员', 120, 'normal')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), avatar_url = VALUES(avatar_url), member_level_name = VALUES(member_level_name), growth_value = VALUES(growth_value), status = VALUES(status), updated_at = current_timestamp;

INSERT INTO user_address (id, user_id, contact_name, contact_phone, province, city, district, detail_address, longitude, latitude, tag_name, is_default, delivery_note) VALUES
  (7001, 5001, '王同学', '13800000001', '北京市', '北京市', '海淀区', '学院路 37 号', 116.352000, 39.984000, '学校', 1, '送到校门口')
ON DUPLICATE KEY UPDATE contact_name = VALUES(contact_name), contact_phone = VALUES(contact_phone), province = VALUES(province), city = VALUES(city), district = VALUES(district), detail_address = VALUES(detail_address), longitude = VALUES(longitude), latitude = VALUES(latitude), tag_name = VALUES(tag_name), is_default = VALUES(is_default), delivery_note = VALUES(delivery_note), updated_at = current_timestamp;
