INSERT INTO iam_role (id, role_code, role_name, status, remark) VALUES
  (1, 'USER', '消费者', 'normal', '用户端账号'),
  (2, 'MERCHANT', '商家', 'normal', '商家端账号'),
  (3, 'ADMIN', '管理员', 'normal', '平台后台账号')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO iam_account (id, account_no, account_type, login_name, phone, email, password_hash, status) VALUES
  (1, 'U202605170001', 'USER', 'demo_user', '18800001111', 'user@example.com', '{noop}123456', 'normal'),
  (2, 'M202605170001', 'MERCHANT', 'demo_merchant', '18800002222', 'merchant@example.com', '{noop}123456', 'normal'),
  (3, 'A202605170001', 'ADMIN', 'demo_admin', '18800003333', 'admin@example.com', '{noop}123456', 'normal'),
  (21, 'M202605170021', 'MERCHANT', 'demo_takeaway_merchant', '18800002021', 'takeaway@example.com', '{noop}123456', 'normal'),
  (22, 'M202605170022', 'MERCHANT', 'demo_groupbuy_merchant', '18800002022', 'groupbuy@example.com', '{noop}123456', 'normal'),
  (23, 'M202605170023', 'MERCHANT', 'demo_hotel_merchant', '18800002023', 'hotel@example.com', '{noop}123456', 'normal'),
  (24, 'M202605170024', 'MERCHANT', 'demo_entertainment_merchant', '18800002024', 'entertainment@example.com', '{noop}123456', 'normal'),
  (25, 'M202605170025', 'MERCHANT', 'demo_movie_merchant', '18800002025', 'movie@example.com', '{noop}123456', 'normal'),
  (26, 'M202605170026', 'MERCHANT', 'demo_beauty_merchant', '18800002026', 'beauty@example.com', '{noop}123456', 'normal'),
  (27, 'M202605170027', 'MERCHANT', 'demo_ticket_merchant', '18800002027', 'ticket@example.com', '{noop}123456', 'normal'),
  (28, 'M202605170028', 'MERCHANT', 'demo_massage_merchant', '18800002028', 'massage@example.com', '{noop}123456', 'normal'),
  (29, 'M202605170029', 'MERCHANT', 'demo_bibimbap_merchant', '18800002029', 'bibimbap@example.com', '{noop}123456', 'normal'),
  (30, 'M202605170030', 'MERCHANT', 'demo_bbq_merchant', '18800002030', 'bbq@example.com', '{noop}123456', 'normal'),
  (31, 'M202605170031', 'MERCHANT', 'demo_hotel_room_merchant', '18800002031', 'hotelroom@example.com', '{noop}123456', 'normal'),
  (32, 'M202605170032', 'MERCHANT', 'demo_arcade_merchant', '18800002032', 'arcade@example.com', '{noop}123456', 'normal'),
  (33, 'M202605170033', 'MERCHANT', 'demo_spa_merchant', '18800002033', 'spa@example.com', '{noop}123456', 'normal')
ON DUPLICATE KEY UPDATE login_name = VALUES(login_name), password_hash = VALUES(password_hash), status = VALUES(status);

INSERT INTO iam_account_role (id, account_id, role_id) VALUES
  (1, 1, 1),
  (2, 2, 2),
  (3, 3, 3),
  (21, 21, 2),
  (22, 22, 2),
  (23, 23, 2),
  (24, 24, 2),
  (25, 25, 2),
  (26, 26, 2),
  (27, 27, 2),
  (28, 28, 2),
  (29, 29, 2),
  (30, 30, 2),
  (31, 31, 2),
  (32, 32, 2),
  (33, 33, 2)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_profile (id, account_id, nickname, avatar_url, register_source, member_level_name, growth_value, status) VALUES
  (1, 1, '爱团用户', 'https://picsum.photos/seed/aituan-avatar/240/240', 'demo', '白银会员', 128, 'normal')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), avatar_url = VALUES(avatar_url), member_level_name = VALUES(member_level_name), growth_value = VALUES(growth_value);

INSERT INTO user_address (id, user_id, contact_name, contact_phone, province, city, district, detail_address, tag_name, is_default, delivery_note) VALUES
  (1, 1, '李同学', '18800001111', '北京市', '北京市', '海淀区', '城市广场 A 座 1208', '公司', 1, '送到前台'),
  (2, 1, '李同学', '18800001111', '北京市', '北京市', '朝阳区', '湖畔花园 3 号楼 1801', '家', 0, '电话联系')
ON DUPLICATE KEY UPDATE detail_address = VALUES(detail_address), is_default = VALUES(is_default);

INSERT INTO merchant_profile (id, merchant_no, account_id, merchant_name, contact_name, contact_phone, license_no, status, audit_status, settled_at) VALUES
  (1, 'MCH001', 2, '塔斯汀中国汉堡', '张店长', '18810000001', 'L001', 'normal', 'approved', CURRENT_TIMESTAMP),
  (2, 'MCH002', 21, '松记炸鸡饭', '王店长', '18810000002', 'L002', 'normal', 'approved', CURRENT_TIMESTAMP),
  (3, 'MCH003', 22, '江南小馆', '陈店长', '18810000003', 'L003', 'normal', 'approved', CURRENT_TIMESTAMP),
  (4, 'MCH004', 23, '云栖酒店', '赵经理', '18810000004', 'L004', 'normal', 'approved', CURRENT_TIMESTAMP),
  (5, 'MCH005', 24, '星盒密室', '孙经理', '18810000005', 'L005', 'normal', 'approved', CURRENT_TIMESTAMP),
  (6, 'MCH006', 25, '光影剧场', '周经理', '18810000006', 'L006', 'normal', 'approved', CURRENT_TIMESTAMP),
  (7, 'MCH007', 26, '轻颜护理', '吴经理', '18810000007', 'L007', 'normal', 'approved', CURRENT_TIMESTAMP),
  (8, 'MCH008', 27, '城市观景', '郑经理', '18810000008', 'L008', 'normal', 'approved', CURRENT_TIMESTAMP),
  (9, 'MCH009', 28, '雅境足道', '冯经理', '18810000009', 'L009', 'normal', 'approved', CURRENT_TIMESTAMP),
  (10, 'MCH010', 29, '米村拌饭', '刘店长', '18810000010', 'L010', 'normal', 'approved', CURRENT_TIMESTAMP),
  (11, 'MCH011', 30, '琥珀烤肉', '许经理', '18810000011', 'L011', 'normal', 'approved', CURRENT_TIMESTAMP),
  (12, 'MCH012', 31, '曼居影院酒店', '钱经理', '18810000012', 'L012', 'normal', 'approved', CURRENT_TIMESTAMP),
  (13, 'MCH013', 32, '趣动电玩城', '马经理', '18810000013', 'L013', 'normal', 'approved', CURRENT_TIMESTAMP),
  (14, 'MCH014', 33, '悦己SPA', '林经理', '18810000014', 'L014', 'normal', 'approved', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE account_id = VALUES(account_id), merchant_name = VALUES(merchant_name), status = VALUES(status), updated_at = CURRENT_TIMESTAMP;

INSERT INTO merchant_store (id, merchant_id, store_name, business_type, summary, address, distance_text, longitude, latitude, rating, monthly_sales, avg_price, status, business_hours_text, tag_text, cover_url) VALUES
  (1, 1, '塔斯汀中国汉堡', 'takeaway', '现烤汉堡，附近高复购外卖', '城市广场 1 层', '900m', 116.313600, 39.982300, 4.8, 3290, 28.00, 'open', '09:30-22:30', '35分钟送达,配送费¥4,满减', 'https://picsum.photos/seed/aituan-store-burger/900/520'),
  (2, 2, '松记炸鸡饭', 'takeaway', '热卖炸鸡饭和能量套餐', '湖畔商业街 2 层', '1.2km', 116.310800, 39.985700, 4.7, 2180, 24.00, 'open', '10:00-21:30', '出餐快,套餐多,免预约', 'https://picsum.photos/seed/aituan-store-chicken-rice/900/520'),
  (3, 3, '江南小馆', 'group_buy', '家常江南菜，到店套餐高性价比', '城市广场 4 层', '800m', 116.316100, 39.981100, 4.8, 1260, 86.00, 'open', '10:30-22:00', '团购,多人餐,可核销', 'https://picsum.photos/seed/aituan-store-jiangnan/900/520'),
  (4, 4, '云栖酒店', 'hotel', '商圈舒适酒店，干净安静', '云栖路 88 号', '2.8km', 116.327400, 39.990800, 4.6, 960, 328.00, 'open', '全天营业', '酒店,大床房,可核销', 'https://picsum.photos/seed/aituan-store-hotel/900/520'),
  (5, 5, '星盒密室', 'entertainment', '沉浸式剧情密室和桌游空间', '青年街 18 号 5 层', '1.9km', 116.304900, 39.977700, 4.9, 870, 118.00, 'open', '12:00-23:30', '休闲娱乐,密室,朋友聚会', 'https://picsum.photos/seed/aituan-store-escape/900/520'),
  (6, 6, '光影剧场', 'movie', '热门电影演出优惠票', '时代中心 6 层', '1.5km', 116.321300, 39.984600, 4.7, 1420, 46.00, 'open', '10:00-24:00', '电影演出,优惠票,可核销', 'https://picsum.photos/seed/aituan-store-cinema/900/520'),
  (7, 7, '轻颜护理', 'beauty', '皮肤护理和基础医美咨询', '望京街 66 号', '3.1km', 116.336800, 39.995200, 4.8, 760, 198.00, 'open', '10:00-21:00', '丽人医美,护理,到店核销', 'https://picsum.photos/seed/aituan-store-beauty/900/520'),
  (8, 8, '城市观景', 'ticket', '城市地标观景门票', '中央公园南门', '4.5km', 116.289700, 39.967500, 4.6, 1680, 59.00, 'open', '09:00-20:00', '景点门票,亲子,电子券', 'https://picsum.photos/seed/aituan-store-ticket/900/520'),
  (9, 9, '雅境足道', 'massage', '足疗按摩，环境安静', '湖畔路 9 号 3 层', '1.6km', 116.308200, 39.973900, 4.8, 1320, 128.00, 'open', '11:00-02:00', '洗脚,按摩,到店核销', 'https://picsum.photos/seed/aituan-store-massage/900/520'),
  (10, 10, '米村拌饭', 'takeaway', '热气石锅拌饭，工作日晚餐热门', '时代里 B1 层', '1.1km', 116.318600, 39.986200, 4.7, 2410, 31.00, 'open', '10:00-21:30', '35分钟送达,拌饭,套餐', 'https://picsum.photos/seed/aituan-store-bibimbap/900/520'),
  (11, 11, '琥珀烤肉', 'group_buy', '烤肉双人餐和家庭聚会套餐', '悦街 3 层 302', '2.2km', 116.302500, 39.988500, 4.8, 1120, 138.00, 'open', '11:00-22:30', '团购,烤肉,多人餐', 'https://picsum.photos/seed/aituan-store-bbq/900/520'),
  (12, 12, '曼居影院酒店', 'hotel', '影音房和商旅房可在线购买到店核销', '新城路 16 号', '3.6km', 116.331700, 39.972400, 4.7, 690, 358.00, 'open', '全天营业', '酒店,影音房,可核销', 'https://picsum.photos/seed/aituan-store-room/900/520'),
  (13, 13, '趣动电玩城', 'entertainment', '电玩城代币、VR 和双人畅玩套餐', '活力中心 4 层', '1.7km', 116.299400, 39.979800, 4.6, 980, 88.00, 'open', '10:00-23:00', '休闲娱乐,电玩城,朋友聚会', 'https://picsum.photos/seed/aituan-store-arcade/900/520'),
  (14, 14, '悦己SPA', 'beauty', '身体护理和肩颈放松项目', '林荫路 12 号 2 层', '2.4km', 116.324000, 39.969200, 4.9, 540, 268.00, 'open', '10:00-22:00', '丽人医美,SPA,预约优先', 'https://picsum.photos/seed/aituan-store-spa/900/520')
ON DUPLICATE KEY UPDATE summary = VALUES(summary), business_type = VALUES(business_type), distance_text = VALUES(distance_text), longitude = VALUES(longitude), latitude = VALUES(latitude), tag_text = VALUES(tag_text), cover_url = VALUES(cover_url);

INSERT INTO merchant_delivery_rule (id, store_id, delivery_fee, start_price, estimated_minutes, delivery_text) VALUES
  (1, 1, 4.00, 20.00, 35, '骑手模拟配送，预计 35 分钟送达'),
  (2, 2, 3.00, 18.00, 32, '骑手模拟配送，预计 32 分钟送达'),
  (3, 10, 4.00, 22.00, 36, '骑手模拟配送，预计 36 分钟送达')
ON DUPLICATE KEY UPDATE delivery_fee = VALUES(delivery_fee), estimated_minutes = VALUES(estimated_minutes);

INSERT INTO merchant_takeaway_setting (id, store_id, accept_mode, updated_by) VALUES
  (1, 1, 'manual', 2),
  (2, 2, 'auto', 21),
  (3, 10, 'manual', 29)
ON DUPLICATE KEY UPDATE accept_mode = VALUES(accept_mode), updated_by = VALUES(updated_by), is_deleted = 0;

INSERT INTO merchant_support_auto_reply_rule (id, merchant_id, keywords, reply_content, enabled) VALUES
  (1, 1, '配送,多久,时间,催,慢', '您好，系统已收到您的催单/时效问题，商家会尽快确认处理。', 1),
  (2, 1, '退款,退单,取消', '您好，退款/取消问题建议先说明订单情况，商家会结合订单状态回复。', 1),
  (3, 1, '发票,票据', '您好，发票或票据问题已记录，请补充抬头和联系方式。', 1)
ON DUPLICATE KEY UPDATE keywords = VALUES(keywords), reply_content = VALUES(reply_content), enabled = VALUES(enabled), is_deleted = 0;

INSERT INTO catalog_category (id, parent_id, store_id, category_code, category_name, business_type, category_level, sort_order, status) VALUES
  (1, NULL, NULL, 'takeaway', '外卖', 'takeaway', 'module', 1, 'normal'),
  (2, NULL, NULL, 'group', '团购', 'group_buy', 'module', 2, 'normal'),
  (3, NULL, NULL, 'hotel', '酒店', 'hotel', 'module', 3, 'normal'),
  (4, NULL, NULL, 'fun', '休闲娱乐', 'entertainment', 'module', 4, 'normal'),
  (5, NULL, NULL, 'movie', '电影演出', 'movie', 'module', 5, 'normal'),
  (6, NULL, NULL, 'beauty', '丽人医美', 'beauty', 'module', 6, 'normal'),
  (7, NULL, NULL, 'ticket', '景点门票', 'ticket', 'module', 7, 'normal'),
  (8, NULL, NULL, 'massage', '洗脚', 'massage', 'module', 8, 'normal'),
  (101, 1, 1, 'tst_burger', '汉堡', 'takeaway', 'store_item', 1, 'normal'),
  (102, 1, 1, 'tst_combo', '套餐', 'takeaway', 'store_item', 2, 'normal'),
  (103, 1, 1, 'tst_snack', '小食', 'takeaway', 'store_item', 3, 'normal'),
  (104, 1, 2, 'sj_rice', '招牌饭', 'takeaway', 'store_item', 1, 'normal'),
  (105, 1, 2, 'sj_combo', '能量套餐', 'takeaway', 'store_item', 2, 'normal'),
  (201, 2, 3, 'jn_family', '多人餐', 'group_buy', 'store_item', 1, 'normal'),
  (202, 2, 3, 'jn_single', '双人餐', 'group_buy', 'store_item', 2, 'normal'),
  (301, 3, 4, 'yq_room', '房型套餐', 'hotel', 'store_item', 1, 'normal'),
  (401, 4, 5, 'xh_escape', '密室套餐', 'entertainment', 'store_item', 1, 'normal'),
  (501, 5, 6, 'gy_ticket', '电影票', 'movie', 'store_item', 1, 'normal'),
  (601, 6, 7, 'qy_care', '基础护理', 'beauty', 'store_item', 1, 'normal'),
  (602, 6, 14, 'yj_spa', 'SPA护理', 'beauty', 'store_item', 1, 'normal'),
  (603, 6, 14, 'yj_relax', '肩颈放松', 'beauty', 'store_item', 2, 'normal'),
  (701, 7, 8, 'cg_ticket', '观景门票', 'ticket', 'store_item', 1, 'normal'),
  (801, 8, 9, 'yj_foot', '足疗按摩', 'massage', 'store_item', 1, 'normal'),
  (106, 1, 10, 'mc_rice', '石锅拌饭', 'takeaway', 'store_item', 1, 'normal'),
  (107, 1, 10, 'mc_combo', '套餐小食', 'takeaway', 'store_item', 2, 'normal'),
  (203, 2, 11, 'hp_bbq', '烤肉套餐', 'group_buy', 'store_item', 1, 'normal'),
  (204, 2, 11, 'hp_family', '家庭聚会', 'group_buy', 'store_item', 2, 'normal'),
  (302, 3, 12, 'mj_room', '影音房', 'hotel', 'store_item', 1, 'normal'),
  (303, 3, 12, 'mj_business', '商旅房', 'hotel', 'store_item', 2, 'normal'),
  (402, 4, 13, 'qd_arcade', '电玩城套餐', 'entertainment', 'store_item', 1, 'normal')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name), sort_order = VALUES(sort_order);

INSERT INTO catalog_item (id, store_id, business_type, category_id, item_name, subtitle, price, original_price, cover_url, rule_text, sales_count, status, item_kind, tag_text, sort_order) VALUES
  (1001, 1, 'takeaway', 101, '招牌中国汉堡', '现烤胚皮 · 酱香鸡腿排', 18.80, 22.00, 'https://picsum.photos/seed/aituan-item-1001/720/540', '外卖商品不单独进入详情页', 2380, 'on_sale', 'takeaway', '热销,现做', 1),
  (1002, 1, 'takeaway', 101, '藤椒鸡腿堡', '微麻藤椒风味，搭配脆生菜', 19.90, 24.00, 'https://picsum.photos/seed/aituan-item-1002/720/540', '外卖商品不单独进入详情页', 1860, 'on_sale', 'takeaway', '微辣,高复购', 2),
  (1003, 1, 'takeaway', 102, '双人汉堡套餐', '双堡 + 小食 + 饮品', 42.80, 52.00, 'https://picsum.photos/seed/aituan-item-1003/720/540', '外卖商品不单独进入详情页', 1420, 'on_sale', 'takeaway', '双人,套餐', 1),
  (1004, 1, 'takeaway', 103, '香辣鸡翅', '外酥里嫩，适合加购', 13.90, 16.00, 'https://picsum.photos/seed/aituan-item-1004/720/540', '外卖商品不单独进入详情页', 980, 'on_sale', 'takeaway', '小食,加购', 1),
  (1101, 2, 'takeaway', 104, '招牌炸鸡饭', '整块炸鸡排 + 米饭 + 小菜', 23.80, 28.00, 'https://picsum.photos/seed/aituan-item-1101/720/540', '外卖商品不单独进入详情页', 1640, 'on_sale', 'takeaway', '热卖,饱腹', 1),
  (1102, 2, 'takeaway', 105, '鸡排饭双拼套餐', '鸡排饭 + 小食 + 饮品', 35.80, 42.00, 'https://picsum.photos/seed/aituan-item-1102/720/540', '外卖商品不单独进入详情页', 820, 'on_sale', 'takeaway', '套餐,午餐', 1),
  (2001, 3, 'group_buy', 201, '江南小馆 3-4 人餐', '招牌鱼头、东坡肉、时蔬组合', 168.00, 218.00, 'https://picsum.photos/seed/aituan-item-2001/720/540', '到店出示券码核销，节假日通用', 760, 'on_sale', 'service', '团购,多人餐', 1),
  (2002, 3, 'group_buy', 202, '江南小馆 双人餐', '双人精选菜品，适合工作日晚餐', 98.00, 128.00, 'https://picsum.photos/seed/aituan-item-2002/720/540', '到店出示券码核销', 680, 'on_sale', 'service', '双人,高性价比', 1),
  (3001, 4, 'hotel', 301, '舒适大床房券', '商圈酒店大床房一晚，预约后入住', 299.00, 388.00, 'https://picsum.photos/seed/aituan-item-3001/720/540', '首包按券码核销，复杂房态后续接入', 430, 'on_sale', 'service', '酒店,大床房', 1),
  (4001, 5, 'entertainment', 401, '星盒密室 4 人套票', '任选主题，提前电话预约', 188.00, 248.00, 'https://picsum.photos/seed/aituan-item-4001/720/540', '到店核销后使用，具体场次以后接入', 520, 'on_sale', 'service', '密室,朋友聚会', 1),
  (5001, 6, 'movie', 501, '电影通兑票', '2D/3D 普通厅通兑，特殊厅补差', 39.90, 59.00, 'https://picsum.photos/seed/aituan-item-5001/720/540', '首包按券码核销，场次座位后续接入', 980, 'on_sale', 'service', '电影,通兑票', 1),
  (6001, 7, 'beauty', 601, '基础皮肤护理', '清洁补水护理，到店核销', 168.00, 238.00, 'https://picsum.photos/seed/aituan-item-6001/720/540', '使用前建议电话确认档期', 360, 'on_sale', 'service', '护理,补水', 1),
  (7001, 8, 'ticket', 701, '城市观景成人票', '地标观景台成人票，电子券核销', 59.00, 88.00, 'https://picsum.photos/seed/aituan-item-7001/720/540', '首包不做日期票，按券码核销', 1220, 'on_sale', 'service', '门票,亲子', 1),
  (8001, 9, 'massage', 801, '经典足疗 60 分钟', '足浴放松，含肩颈舒缓', 118.00, 168.00, 'https://picsum.photos/seed/aituan-item-8001/720/540', '到店出示券码核销', 1080, 'on_sale', 'service', '洗脚,放松', 1),
  (8002, 9, 'massage', 801, '肩颈舒缓 45 分钟', '适合久坐人群，到店核销', 98.00, 138.00, 'https://picsum.photos/seed/aituan-item-8002/720/540', '到店出示券码核销', 760, 'on_sale', 'service', '按摩,肩颈', 2),
  (1201, 10, 'takeaway', 106, '招牌石锅拌饭', '牛肉、蔬菜和溏心蛋热拌', 29.80, 36.00, 'https://picsum.photos/seed/aituan-item-1201/720/540', '外卖商品不单独进入详情页', 1460, 'on_sale', 'takeaway', '热卖,拌饭', 1),
  (1202, 10, 'takeaway', 106, '肥牛泡菜拌饭', '肥牛片搭配泡菜和海苔碎', 32.80, 39.00, 'https://picsum.photos/seed/aituan-item-1202/720/540', '外卖商品不单独进入详情页', 980, 'on_sale', 'takeaway', '肥牛,微辣', 2),
  (1203, 10, 'takeaway', 107, '双人拌饭套餐', '两份拌饭 + 小食 + 饮品', 68.00, 82.00, 'https://picsum.photos/seed/aituan-item-1203/720/540', '外卖商品不单独进入详情页', 720, 'on_sale', 'takeaway', '双人,套餐', 1),
  (1204, 10, 'takeaway', 107, '冰粉小食组合', '冰粉、薯角和饮品任选', 16.80, 22.00, 'https://picsum.photos/seed/aituan-item-1204/720/540', '外卖商品不单独进入详情页', 540, 'on_sale', 'takeaway', '小食,加购', 2),
  (2101, 11, 'group_buy', 203, '琥珀烤肉双人餐', '精选牛五花、梅花肉和蔬菜拼盘', 158.00, 218.00, 'https://picsum.photos/seed/aituan-item-2101/720/540', '到店出示券码核销，周末通用', 620, 'on_sale', 'service', '烤肉,双人', 1),
  (2102, 11, 'group_buy', 204, '烤肉家庭 4 人餐', '多肉拼盘 + 主食 + 饮品', 298.00, 388.00, 'https://picsum.photos/seed/aituan-item-2102/720/540', '到店出示券码核销', 420, 'on_sale', 'service', '多人餐,聚会', 1),
  (3101, 12, 'hotel', 302, '影音大床房券', '百寸投影影音房一晚，需预约', 339.00, 438.00, 'https://picsum.photos/seed/aituan-item-3101/720/540', '入住前电话确认房态，到店核销', 360, 'on_sale', 'service', '影音房,大床', 1),
  (3102, 12, 'hotel', 303, '商旅双床房券', '双床房一晚，含双早', 368.00, 468.00, 'https://picsum.photos/seed/aituan-item-3102/720/540', '入住前电话确认房态，到店核销', 280, 'on_sale', 'service', '商旅,双床', 1),
  (4101, 13, 'entertainment', 402, '电玩城 120 币套餐', '游戏币 120 枚，适合双人游玩', 79.00, 120.00, 'https://picsum.photos/seed/aituan-item-4101/720/540', '到店前台核销后取币', 880, 'on_sale', 'service', '电玩城,双人', 1),
  (4102, 13, 'entertainment', 402, 'VR 双人畅玩票', 'VR 项目双人体验，节假日可用', 128.00, 168.00, 'https://picsum.photos/seed/aituan-item-4102/720/540', '到店核销后排队体验', 510, 'on_sale', 'service', 'VR,朋友聚会', 2),
  (6101, 14, 'beauty', 602, '全身舒缓 SPA', '90 分钟身体护理，预约优先', 298.00, 398.00, 'https://picsum.photos/seed/aituan-item-6101/720/540', '使用前建议电话预约', 260, 'on_sale', 'service', 'SPA,护理', 1),
  (6102, 14, 'beauty', 603, '肩颈放松 45 分钟', '肩颈舒缓和热敷护理', 168.00, 238.00, 'https://picsum.photos/seed/aituan-item-6102/720/540', '使用前建议电话预约', 330, 'on_sale', 'service', '肩颈,放松', 1)
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name), price = VALUES(price), cover_url = VALUES(cover_url), tag_text = VALUES(tag_text);

INSERT INTO catalog_sku (id, item_id, sku_name, price, stock, status) VALUES
  (1, 1001, '默认', 18.80, 500, 'on_sale'),
  (2, 1002, '默认', 19.90, 500, 'on_sale'),
  (3, 1003, '默认', 42.80, 300, 'on_sale'),
  (4, 1004, '默认', 13.90, 500, 'on_sale'),
  (5, 1101, '默认', 23.80, 500, 'on_sale'),
  (6, 1102, '默认', 35.80, 3, 'on_sale'),
  (7, 2001, '默认', 168.00, 100, 'on_sale'),
  (8, 2002, '默认', 98.00, 100, 'on_sale'),
  (9, 3001, '默认', 299.00, 80, 'on_sale'),
  (10, 4001, '默认', 188.00, 80, 'on_sale'),
  (11, 5001, '默认', 39.90, 500, 'on_sale'),
  (12, 6001, '默认', 168.00, 80, 'on_sale'),
  (13, 7001, '默认', 59.00, 300, 'on_sale'),
  (14, 8001, '默认', 118.00, 150, 'on_sale'),
  (15, 8002, '默认', 98.00, 150, 'on_sale'),
  (16, 1201, '默认', 29.80, 500, 'on_sale'),
  (17, 1202, '默认', 32.80, 500, 'on_sale'),
  (18, 1203, '默认', 68.00, 300, 'on_sale'),
  (19, 1204, '默认', 16.80, 0, 'on_sale'),
  (20, 2101, '默认', 158.00, 120, 'on_sale'),
  (21, 2102, '默认', 298.00, 80, 'on_sale'),
  (22, 3101, '默认', 339.00, 60, 'on_sale'),
  (23, 3102, '默认', 368.00, 60, 'on_sale'),
  (24, 4101, '默认', 79.00, 200, 'on_sale'),
  (25, 4102, '默认', 128.00, 120, 'on_sale'),
  (26, 6101, '默认', 298.00, 80, 'on_sale'),
  (27, 6102, '默认', 168.00, 80, 'on_sale')
ON DUPLICATE KEY UPDATE price = VALUES(price), stock = VALUES(stock);

INSERT INTO member_recommend_config (id, scene, business_type, store_id, item_id, sort_order, status) VALUES
  (1, 'home_recommend', 'takeaway', 1, 1001, 1, 'normal'),
  (2, 'home_recommend', 'takeaway', 2, 1101, 2, 'normal'),
  (3, 'home_recommend', 'group_buy', 3, 2001, 3, 'normal'),
  (4, 'home_recommend', 'hotel', 4, 3001, 4, 'normal'),
  (5, 'home_recommend', 'entertainment', 5, 4001, 5, 'normal'),
  (6, 'home_recommend', 'movie', 6, 5001, 6, 'normal'),
  (7, 'home_recommend', 'beauty', 7, 6001, 7, 'normal'),
  (8, 'home_recommend', 'ticket', 8, 7001, 8, 'normal'),
  (9, 'home_recommend', 'massage', 9, 8001, 9, 'normal'),
  (10, 'home_recommend', 'takeaway', 10, 1201, 10, 'normal'),
  (11, 'home_recommend', 'group_buy', 11, 2101, 11, 'normal'),
  (12, 'home_recommend', 'hotel', 12, 3101, 12, 'normal'),
  (13, 'home_recommend', 'entertainment', 13, 4101, 13, 'normal'),
  (14, 'home_recommend', 'beauty', 14, 6101, 14, 'normal'),
  (15, 'home_recommend', 'takeaway', 10, 1203, 15, 'normal')
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO order_main (id, order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status, fulfillment_status, payment_method, amount, delivery_fee, discount_amount, payable_amount, address_snapshot, voucher_summary, paid_at, completed_at, created_at) VALUES
  (1, 'AT202605170001', 1, 3, '江南小馆', 'group_buy', '江南小馆 双人餐', 'unpaid', 'unpaid', 'created', NULL, 98.00, 0.00, 0.00, 98.00, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP),
  (2, 'AT202605170002', 1, 1, '塔斯汀中国汉堡', 'takeaway', '招牌中国汉堡等2件', 'pending', 'paid', 'delivering', 'mock', 32.70, 4.00, 0.00, 36.70, '北京市海淀区城市广场 A 座 1208', NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (3, 'AT202605170003', 1, 3, '江南小馆', 'group_buy', '江南小馆 3-4 人餐', 'unused', 'paid', 'voucher_unused', 'mock', 168.00, 0.00, 0.00, 168.00, NULL, '券码 88001234', CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (4, 'AT202605170004', 1, 9, '雅境足道', 'massage', '经典足疗 60 分钟', 'used', 'paid', 'voucher_used', 'mock', 118.00, 0.00, 0.00, 118.00, NULL, '券码 88005678', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9005, 'AT202605179005', 1, 10, '米村拌饭', 'takeaway', '肥牛泡菜拌饭等2件', 'used', 'paid', 'completed', 'mock', 49.60, 4.00, 3.00, 50.60, '北京市朝阳区湖畔花园 3 号楼 1801', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9006, 'AT202605179006', 1, 12, '曼居影院酒店', 'hotel', '影音大床房券', 'unused', 'paid', 'voucher_unused', 'mock', 339.00, 0.00, 0.00, 339.00, NULL, '券码 88009006', CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (9007, 'AT202605179007', 1, 6, '光影剧场', 'movie', '电影通兑票', 'unpaid', 'unpaid', 'created', NULL, 39.90, 0.00, 0.00, 39.90, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP),
  (9008, 'AT202605179008', 1, 14, '悦己SPA', 'beauty', '全身舒缓 SPA', 'unused', 'paid', 'voucher_unused', 'mock', 298.00, 0.00, 20.00, 278.00, NULL, '券码 88009008', CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (9009, 'AT202605179009', 1, 11, '琥珀烤肉', 'group_buy', '琥珀烤肉双人餐', 'used', 'paid', 'voucher_used', 'mock', 158.00, 0.00, 0.00, 158.00, NULL, '券码 88009009', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  (9010, 'AT202605179010', 1, 2, '松记炸鸡饭', 'takeaway', '鸡排饭双拼套餐', 'unpaid', 'unpaid', 'created', NULL, 35.80, 3.00, 0.00, 38.80, '北京市海淀区城市广场 A 座 1208', NULL, NULL, NULL, CURRENT_TIMESTAMP),
  (9011, 'AT202605179011', 1, 1, '塔斯汀中国汉堡', 'takeaway', '藤椒鸡腿堡等2件', 'pending', 'paid', 'merchant_pending', 'mock', 33.80, 4.00, 0.00, 37.80, '北京市海淀区城市广场 A 座 1208', NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (9012, 'AT202605179012', 1, 2, '松记炸鸡饭', 'takeaway', '招牌炸鸡饭', 'pending', 'paid', 'accepted', 'mock', 23.80, 3.00, 0.00, 26.80, '北京市海淀区城市广场 A 座 1208', NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (9013, 'AT202605179013', 1, 1, '塔斯汀中国汉堡', 'takeaway', '双人汉堡套餐', 'pending', 'paid', 'preparing', 'mock', 42.80, 4.00, 4.00, 42.80, '北京市海淀区城市广场 A 座 1208', NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (9014, 'AT202605179014', 1, 10, '米村拌饭', 'takeaway', '双人拌饭套餐', 'pending', 'paid', 'ready_for_delivery', 'mock', 68.00, 4.00, 6.00, 66.00, '北京市朝阳区湖畔花园 3 号楼 1801', NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (9015, 'AT202605179015', 1, 2, '松记炸鸡饭', 'takeaway', '招牌炸鸡饭等2件', 'pending', 'paid', 'delivering', 'mock', 59.60, 3.00, 5.00, 57.60, '北京市海淀区城市广场 A 座 1208', NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (9016, 'AT202605179016', 1, 10, '米村拌饭', 'takeaway', '招牌石锅拌饭', 'pending', 'paid', 'delivered', 'mock', 29.80, 4.00, 0.00, 33.80, '北京市朝阳区湖畔花园 3 号楼 1801', NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (9017, 'AT202605179017', 1, 1, '塔斯汀中国汉堡', 'takeaway', '招牌中国汉堡', 'used', 'paid', 'completed', 'mock', 18.80, 4.00, 0.00, 22.80, '北京市海淀区城市广场 A 座 1208', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE display_status = VALUES(display_status), payment_status = VALUES(payment_status), fulfillment_status = VALUES(fulfillment_status);

INSERT INTO order_item (id, order_id, item_id, item_name, item_subtitle, business_type, category_id, quantity, unit_price, total_price, cover_url, is_reviewed) VALUES
  (1, 1, 2002, '江南小馆 双人餐', '双人精选菜品，适合工作日晚餐', 'group_buy', 202, 1, 98.00, 98.00, 'https://picsum.photos/seed/aituan-item-2002/720/540', 0),
  (2, 2, 1001, '招牌中国汉堡', '现烤胚皮 · 酱香鸡腿排', 'takeaway', 101, 1, 18.80, 18.80, 'https://picsum.photos/seed/aituan-item-1001/720/540', 0),
  (3, 2, 1004, '香辣鸡翅', '外酥里嫩，适合加购', 'takeaway', 103, 1, 13.90, 13.90, 'https://picsum.photos/seed/aituan-item-1004/720/540', 0),
  (4, 3, 2001, '江南小馆 3-4 人餐', '招牌鱼头、东坡肉、时蔬组合', 'group_buy', 201, 1, 168.00, 168.00, 'https://picsum.photos/seed/aituan-item-2001/720/540', 0),
  (5, 4, 8001, '经典足疗 60 分钟', '足浴放松，含肩颈舒缓', 'massage', 801, 1, 118.00, 118.00, 'https://picsum.photos/seed/aituan-item-8001/720/540', 1),
  (9005, 9005, 1202, '肥牛泡菜拌饭', '肥牛片搭配泡菜和海苔碎', 'takeaway', 106, 1, 32.80, 32.80, 'https://picsum.photos/seed/aituan-item-1202/720/540', 1),
  (9006, 9005, 1204, '冰粉小食组合', '冰粉、薯角和饮品任选', 'takeaway', 107, 1, 16.80, 16.80, 'https://picsum.photos/seed/aituan-item-1204/720/540', 1),
  (9007, 9006, 3101, '影音大床房券', '百寸投影影音房一晚，需预约', 'hotel', 302, 1, 339.00, 339.00, 'https://picsum.photos/seed/aituan-item-3101/720/540', 0),
  (9008, 9007, 5001, '电影通兑票', '2D/3D 普通厅通兑，特殊厅补差', 'movie', 501, 1, 39.90, 39.90, 'https://picsum.photos/seed/aituan-item-5001/720/540', 0),
  (9009, 9008, 6101, '全身舒缓 SPA', '90 分钟身体护理，预约优先', 'beauty', 602, 1, 298.00, 298.00, 'https://picsum.photos/seed/aituan-item-6101/720/540', 0),
  (9010, 9009, 2101, '琥珀烤肉双人餐', '精选牛五花、梅花肉和蔬菜拼盘', 'group_buy', 203, 1, 158.00, 158.00, 'https://picsum.photos/seed/aituan-item-2101/720/540', 1),
  (9011, 9010, 1102, '鸡排饭双拼套餐', '鸡排饭 + 小食 + 饮品', 'takeaway', 105, 1, 35.80, 35.80, 'https://picsum.photos/seed/aituan-item-1102/720/540', 0),
  (9012, 9011, 1002, '藤椒鸡腿堡', '微麻藤椒风味，搭配脆生菜', 'takeaway', 101, 1, 19.90, 19.90, 'https://picsum.photos/seed/aituan-item-1002/720/540', 0),
  (9013, 9011, 1004, '香辣鸡翅', '外酥里嫩，适合加购', 'takeaway', 103, 1, 13.90, 13.90, 'https://picsum.photos/seed/aituan-item-1004/720/540', 0),
  (9014, 9012, 1101, '招牌炸鸡饭', '整块炸鸡排 + 米饭 + 小菜', 'takeaway', 104, 1, 23.80, 23.80, 'https://picsum.photos/seed/aituan-item-1101/720/540', 0),
  (9015, 9013, 1003, '双人汉堡套餐', '双堡 + 小食 + 饮品', 'takeaway', 102, 1, 42.80, 42.80, 'https://picsum.photos/seed/aituan-item-1003/720/540', 0),
  (9016, 9014, 1203, '双人拌饭套餐', '两份拌饭 + 小食 + 饮品', 'takeaway', 107, 1, 68.00, 68.00, 'https://picsum.photos/seed/aituan-item-1203/720/540', 0),
  (9017, 9015, 1101, '招牌炸鸡饭', '整块炸鸡排 + 米饭 + 小菜', 'takeaway', 104, 1, 23.80, 23.80, 'https://picsum.photos/seed/aituan-item-1101/720/540', 0),
  (9018, 9015, 1102, '鸡排饭双拼套餐', '鸡排饭 + 小食 + 饮品', 'takeaway', 105, 1, 35.80, 35.80, 'https://picsum.photos/seed/aituan-item-1102/720/540', 0),
  (9019, 9016, 1201, '招牌石锅拌饭', '牛肉、蔬菜和溏心蛋热拌', 'takeaway', 106, 1, 29.80, 29.80, 'https://picsum.photos/seed/aituan-item-1201/720/540', 0),
  (9020, 9017, 1001, '招牌中国汉堡', '现烤胚皮 · 酱香鸡腿排', 'takeaway', 101, 1, 18.80, 18.80, 'https://picsum.photos/seed/aituan-item-1001/720/540', 1)
ON DUPLICATE KEY UPDATE total_price = VALUES(total_price), cover_url = VALUES(cover_url), is_reviewed = VALUES(is_reviewed);

INSERT INTO order_payment_record (id, order_id, payment_no, payment_method, amount, status, provider_trade_no, paid_at) VALUES
  (1, 2, 'PAY202605170002', 'mock', 36.70, 'paid', 'MOCK202605170002', CURRENT_TIMESTAMP),
  (2, 3, 'PAY202605170003', 'mock', 168.00, 'paid', 'MOCK202605170003', CURRENT_TIMESTAMP),
  (3, 4, 'PAY202605170004', 'mock', 118.00, 'paid', 'MOCK202605170004', CURRENT_TIMESTAMP),
  (9005, 9005, 'PAY202605179005', 'mock', 50.60, 'paid', 'MOCK202605179005', CURRENT_TIMESTAMP),
  (9006, 9006, 'PAY202605179006', 'mock', 339.00, 'paid', 'MOCK202605179006', CURRENT_TIMESTAMP),
  (9008, 9008, 'PAY202605179008', 'mock', 278.00, 'paid', 'MOCK202605179008', CURRENT_TIMESTAMP),
  (9009, 9009, 'PAY202605179009', 'mock', 158.00, 'paid', 'MOCK202605179009', CURRENT_TIMESTAMP),
  (9011, 9011, 'PAY202605179011', 'mock', 37.80, 'paid', 'MOCK202605179011', CURRENT_TIMESTAMP),
  (9012, 9012, 'PAY202605179012', 'mock', 26.80, 'paid', 'MOCK202605179012', CURRENT_TIMESTAMP),
  (9013, 9013, 'PAY202605179013', 'mock', 42.80, 'paid', 'MOCK202605179013', CURRENT_TIMESTAMP),
  (9014, 9014, 'PAY202605179014', 'mock', 66.00, 'paid', 'MOCK202605179014', CURRENT_TIMESTAMP),
  (9015, 9015, 'PAY202605179015', 'mock', 57.60, 'paid', 'MOCK202605179015', CURRENT_TIMESTAMP),
  (9016, 9016, 'PAY202605179016', 'mock', 33.80, 'paid', 'MOCK202605179016', CURRENT_TIMESTAMP),
  (9017, 9017, 'PAY202605179017', 'mock', 22.80, 'paid', 'MOCK202605179017', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE status = VALUES(status), amount = VALUES(amount);

INSERT INTO order_voucher (id, order_id, voucher_code, qr_payload, status, effective_to, verified_at, verified_by) VALUES
  (1, 3, '88001234', 'AITUAN:VOUCHER:88001234', 'unused', '2026-12-31 23:59:59', NULL, NULL),
  (2, 4, '88005678', 'AITUAN:VOUCHER:88005678', 'used', '2026-12-31 23:59:59', CURRENT_TIMESTAMP, 2),
  (9006, 9006, '88009006', 'AITUAN:VOUCHER:88009006', 'unused', '2026-12-31 23:59:59', NULL, NULL),
  (9008, 9008, '88009008', 'AITUAN:VOUCHER:88009008', 'unused', '2026-12-31 23:59:59', NULL, NULL),
  (9009, 9009, '88009009', 'AITUAN:VOUCHER:88009009', 'used', '2026-12-31 23:59:59', CURRENT_TIMESTAMP, 2)
ON DUPLICATE KEY UPDATE status = VALUES(status), qr_payload = VALUES(qr_payload);

INSERT INTO delivery_task (id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, completed_at) VALUES
  (1, 2, 'delivering', '骑手正在配送', 18, NULL, NULL),
  (9005, 9005, 'completed', '订单已完成', 0, NULL, CURRENT_TIMESTAMP),
  (9011, 9011, 'merchant_pending', '待商家接单', 35, NULL, NULL),
  (9012, 9012, 'accepted', '商家已接单', 32, NULL, NULL),
  (9013, 9013, 'preparing', '商家正在备餐', 28, NULL, NULL),
  (9014, 9014, 'ready_for_delivery', '餐品已出餐，待配送', 22, NULL, NULL),
  (9015, 9015, 'delivering', '骑手正在配送', 16, NULL, NULL),
  (9016, 9016, 'delivered', '订单已送达', 0, NULL, NULL),
  (9017, 9017, 'completed', '订单已完成', 0, NULL, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE current_stage = VALUES(current_stage), current_stage_text = VALUES(current_stage_text), completed_at = VALUES(completed_at);

INSERT INTO delivery_track_node (id, delivery_task_id, node_order, node_code, node_text, reached_at) VALUES
  (11, 1, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (12, 1, 2, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (13, 1, 3, 'preparing', '商家正在备餐', CURRENT_TIMESTAMP),
  (14, 1, 4, 'ready_for_delivery', '餐品已出餐，待配送', CURRENT_TIMESTAMP),
  (15, 1, 5, 'delivering', '骑手正在配送', CURRENT_TIMESTAMP),
  (16, 1, 6, 'delivered', '订单已送达', NULL),
  (17, 1, 7, 'completed', '订单已完成', NULL),
  (90050, 9005, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (90051, 9005, 2, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (90052, 9005, 3, 'preparing', '商家正在备餐', CURRENT_TIMESTAMP),
  (90053, 9005, 4, 'ready_for_delivery', '餐品已出餐，待配送', CURRENT_TIMESTAMP),
  (90054, 9005, 5, 'delivering', '骑手正在配送', CURRENT_TIMESTAMP),
  (90055, 9005, 6, 'delivered', '订单已送达', CURRENT_TIMESTAMP),
  (90056, 9005, 7, 'completed', '订单已完成', CURRENT_TIMESTAMP),
  (90110, 9011, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (90111, 9011, 2, 'accepted', '商家已接单', NULL),
  (90112, 9011, 3, 'preparing', '商家正在备餐', NULL),
  (90113, 9011, 4, 'ready_for_delivery', '餐品已出餐，待配送', NULL),
  (90114, 9011, 5, 'delivering', '骑手正在配送', NULL),
  (90115, 9011, 6, 'delivered', '订单已送达', NULL),
  (90116, 9011, 7, 'completed', '订单已完成', NULL),
  (90120, 9012, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (90121, 9012, 2, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (90122, 9012, 3, 'preparing', '商家正在备餐', NULL),
  (90123, 9012, 4, 'ready_for_delivery', '餐品已出餐，待配送', NULL),
  (90124, 9012, 5, 'delivering', '骑手正在配送', NULL),
  (90125, 9012, 6, 'delivered', '订单已送达', NULL),
  (90126, 9012, 7, 'completed', '订单已完成', NULL),
  (90130, 9013, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (90131, 9013, 2, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (90132, 9013, 3, 'preparing', '商家正在备餐', CURRENT_TIMESTAMP),
  (90133, 9013, 4, 'ready_for_delivery', '餐品已出餐，待配送', NULL),
  (90134, 9013, 5, 'delivering', '骑手正在配送', NULL),
  (90135, 9013, 6, 'delivered', '订单已送达', NULL),
  (90136, 9013, 7, 'completed', '订单已完成', NULL),
  (90140, 9014, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (90141, 9014, 2, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (90142, 9014, 3, 'preparing', '商家正在备餐', CURRENT_TIMESTAMP),
  (90143, 9014, 4, 'ready_for_delivery', '餐品已出餐，待配送', CURRENT_TIMESTAMP),
  (90144, 9014, 5, 'delivering', '骑手正在配送', NULL),
  (90145, 9014, 6, 'delivered', '订单已送达', NULL),
  (90146, 9014, 7, 'completed', '订单已完成', NULL),
  (90150, 9015, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (90151, 9015, 2, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (90152, 9015, 3, 'preparing', '商家正在备餐', CURRENT_TIMESTAMP),
  (90153, 9015, 4, 'ready_for_delivery', '餐品已出餐，待配送', CURRENT_TIMESTAMP),
  (90154, 9015, 5, 'delivering', '骑手正在配送', CURRENT_TIMESTAMP),
  (90155, 9015, 6, 'delivered', '订单已送达', NULL),
  (90156, 9015, 7, 'completed', '订单已完成', NULL),
  (90160, 9016, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (90161, 9016, 2, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (90162, 9016, 3, 'preparing', '商家正在备餐', CURRENT_TIMESTAMP),
  (90163, 9016, 4, 'ready_for_delivery', '餐品已出餐，待配送', CURRENT_TIMESTAMP),
  (90164, 9016, 5, 'delivering', '骑手正在配送', CURRENT_TIMESTAMP),
  (90165, 9016, 6, 'delivered', '订单已送达', CURRENT_TIMESTAMP),
  (90166, 9016, 7, 'completed', '订单已完成', NULL),
  (90170, 9017, 1, 'merchant_pending', '待商家接单', CURRENT_TIMESTAMP),
  (90171, 9017, 2, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (90172, 9017, 3, 'preparing', '商家正在备餐', CURRENT_TIMESTAMP),
  (90173, 9017, 4, 'ready_for_delivery', '餐品已出餐，待配送', CURRENT_TIMESTAMP),
  (90174, 9017, 5, 'delivering', '骑手正在配送', CURRENT_TIMESTAMP),
  (90175, 9017, 6, 'delivered', '订单已送达', CURRENT_TIMESTAMP),
  (90176, 9017, 7, 'completed', '订单已完成', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE node_text = VALUES(node_text), reached_at = VALUES(reached_at);

INSERT INTO review_record (id, order_id, store_id, user_id, rating, content, labels, image_urls, helpful_count, reported_count, status, replied) VALUES
  (1, 4, 9, 1, 5, '环境安静，服务稳定，券码核销很顺畅。', '环境好,服务细致', 'https://picsum.photos/seed/aituan-review-1-a/720/540,https://picsum.photos/seed/aituan-review-1-b/720/540', 12, 0, 'published', 1),
  (9005, 9005, 10, 1, 5, '拌饭送到还是热的，配菜足，适合工作日晚餐。', '送达快,份量足', 'https://picsum.photos/seed/aituan-review-9005-a/720/540', 8, 0, 'published', 1),
  (9009, 9009, 11, 1, 5, '双人餐肉量不错，核销顺利，店员会主动确认券码。', '核销快,适合聚会', 'https://picsum.photos/seed/aituan-review-9009-a/720/540,https://picsum.photos/seed/aituan-review-9009-b/720/540', 17, 1, 'published', 0),
  (9017, 9017, 1, 1, 3, '配送等待较久，希望商家加快出餐节奏。', '出餐慢', 'https://picsum.photos/seed/aituan-review-9017-a/720/540', 2, 2, 'hidden', 0)
ON DUPLICATE KEY UPDATE content = VALUES(content), rating = VALUES(rating), image_urls = VALUES(image_urls), helpful_count = VALUES(helpful_count), reported_count = VALUES(reported_count), status = VALUES(status), replied = VALUES(replied);

-- 商家回复
INSERT INTO review_reply (id, review_id, merchant_id, reply_content) VALUES
  (1, 1, 9, '感谢您的肯定，欢迎下次光临雅境足道！'),
  (9005, 9005, 10, '感谢您喜欢米村拌饭，欢迎继续点单！')
ON DUPLICATE KEY UPDATE reply_content = VALUES(reply_content), is_deleted = 0;

-- 点"有用"（仅用户 1，演示按钮高亮）
INSERT INTO review_helpful (id, review_id, user_id) VALUES
  (1, 9005, 1),
  (2, 9009, 1)
ON DUPLICATE KEY UPDATE is_deleted = 0;

-- 评价举报（用户 1 举报 9009 与 9017，作为后台审核演示）
INSERT INTO review_report (id, review_id, reporter_user_id, reason, detail, status, handled_by, handled_at) VALUES
  (1, 9009, 1, '内容不实', '与到店实际情况不符，请核实', 'submitted', NULL, NULL),
  (2, 9017, 1, '内容不实', '反复刷差评影响商家排序', 'handled', 3, CURRENT_TIMESTAMP),
  (3, 9017, 1, '广告引流', '评价里夹带其他门店推广链接', 'handled', 3, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE reason = VALUES(reason), detail = VALUES(detail), status = VALUES(status), handled_by = VALUES(handled_by), handled_at = VALUES(handled_at), is_deleted = 0;

-- 评价审核轨迹（review 9017 被屏蔽）
INSERT INTO review_audit_log (id, review_id, action, from_status, to_status, operator_id, remark) VALUES
  (1, 9017, 'hide', 'published', 'hidden', 3, '违规内容已屏蔽，已通知商家'),
  (2, 9009, 'pass', 'published', 'published', 3, '举报核实后维持发布')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);

-- 客服会话（2 个进行中 + 2 个已关闭）
INSERT INTO support_session (id, session_no, user_id, store_id, merchant_id, topic, status, related_order_id, last_message_id, last_message_at, user_unread_count, merchant_unread_count, closed_at, closed_by_type, closed_by_id, close_reason) VALUES
  (1, 'SS202605170001', 1, 1, 1, '想咨询配送时长能否再快一点', 'open', 9011, 3, CURRENT_TIMESTAMP, 1, 0, NULL, NULL, NULL, NULL),
  (2, 'SS202605170002', 1, 10, 10, '订单完成后想申请补送一份小食', 'open', 9005, 6, CURRENT_TIMESTAMP, 0, 2, NULL, NULL, NULL, NULL),
  (3, 'SS202605170003', 1, 3, 3, '团购套餐能否调整使用时间', 'closed', 3, 9, CURRENT_TIMESTAMP, 0, 0, CURRENT_TIMESTAMP, 'merchant', 22, '问题已解决'),
  (4, 'SS202605170004', 1, 9, 9, '足道预约时段咨询', 'closed', 4, 12, CURRENT_TIMESTAMP, 0, 0, CURRENT_TIMESTAMP, 'user', 1, '已自行预约')
ON DUPLICATE KEY UPDATE topic = VALUES(topic), status = VALUES(status), last_message_id = VALUES(last_message_id), last_message_at = VALUES(last_message_at), user_unread_count = VALUES(user_unread_count), merchant_unread_count = VALUES(merchant_unread_count), closed_at = VALUES(closed_at), closed_by_type = VALUES(closed_by_type), closed_by_id = VALUES(closed_by_id), close_reason = VALUES(close_reason), is_deleted = 0;

-- 客服消息
INSERT INTO support_message (id, session_id, sender_type, sender_id, content, message_kind) VALUES
  (1, 1, 'user', 1, '你好，订单 AT202605179011 已经接单 30 分钟了，是不是要更久？', 'text'),
  (2, 1, 'merchant', 2, '已经在加急出餐，预计还有 10 分钟出门，请耐心稍候。', 'text'),
  (3, 1, 'user', 1, '好的，麻烦了。', 'text'),
  (4, 2, 'user', 1, '上一单忘记加冰粉，能否补送？', 'text'),
  (5, 2, 'merchant', 29, '您好，我们这边登记一下，下一单帮您带上。', 'text'),
  (6, 2, 'merchant', 29, '已经登记完毕，请放心。', 'text'),
  (7, 3, 'user', 1, '套餐能否改到周五晚上使用？', 'text'),
  (8, 3, 'merchant', 22, '可以的，您到店时跟我们说一下即可。', 'text'),
  (9, 3, 'user', 1, '收到，谢谢。', 'text'),
  (10, 4, 'user', 1, '请问周末晚上还有空档吗？', 'text'),
  (11, 4, 'merchant', 28, '周六 20:00 还有空，需要预约吗？', 'text'),
  (12, 4, 'user', 1, '不用啦，我自己再约。', 'text')
ON DUPLICATE KEY UPDATE content = VALUES(content), is_deleted = 0;

-- 投诉工单（3 单：pending / processing / resolved）
INSERT INTO complaint_ticket (id, ticket_no, user_id, order_id, store_id, merchant_id, category, title, detail, evidence_urls, status, accepted_by, accepted_at, resolved_by, resolved_at, closed_at) VALUES
  (1, 'CP202605170001', 1, 9011, 1, 1, 'delivery', '配送等待时间过长', '订单接单后 30 分钟仍未出餐，希望平台协调商家。', 'https://picsum.photos/seed/aituan-complaint-1/720/540', 'pending', NULL, NULL, NULL, NULL, NULL),
  (2, 'CP202605170002', 1, 9007, 6, 6, 'service', '电影票无法核销', '到影院后核销码扫描失败，现场排队 20 分钟未解决。', NULL, 'processing', 3, CURRENT_TIMESTAMP, NULL, NULL, NULL),
  (3, 'CP202605170003', 1, 4, 9, 9, 'quality', '足疗按摩师傅迟到', '预约 19:00 到店，实际 19:30 才开始服务，希望补偿优惠券。', NULL, 'resolved', 3, CURRENT_TIMESTAMP, 3, CURRENT_TIMESTAMP, NULL)
ON DUPLICATE KEY UPDATE category = VALUES(category), title = VALUES(title), detail = VALUES(detail), evidence_urls = VALUES(evidence_urls), status = VALUES(status), accepted_by = VALUES(accepted_by), accepted_at = VALUES(accepted_at), resolved_by = VALUES(resolved_by), resolved_at = VALUES(resolved_at), closed_at = VALUES(closed_at), is_deleted = 0;

-- 投诉处理日志
INSERT INTO complaint_log (id, ticket_id, action, operator_type, operator_id, remark) VALUES
  (1, 1, 'submit', 'user', 1, NULL),
  (2, 2, 'submit', 'user', 1, NULL),
  (3, 2, 'accept', 'admin', 3, '已通知商家联系用户'),
  (4, 3, 'submit', 'user', 1, NULL),
  (5, 3, 'accept', 'admin', 3, '已联系商家'),
  (6, 3, 'resolve', 'admin', 3, '商家承诺补送优惠券，工单完成')
ON DUPLICATE KEY UPDATE remark = VALUES(remark);

-- 平台审计日志（覆盖 user / merchant / admin 三类 actor，跨多种 action 演示）
INSERT INTO sys_audit_log (id, actor_type, actor_id, action_type, target_type, target_id, detail) VALUES
  (1, 'user', 1, 'review_report', 'review', 9009, '举报评价 9009：内容不实'),
  (2, 'user', 1, 'review_report', 'review', 9017, '举报评价 9017：内容不实'),
  (3, 'admin', 3, 'review_audit_hide', 'review', 9017, '评价 9017 审核：published->hidden'),
  (4, 'admin', 3, 'review_audit_pass', 'review', 9009, '评价 9009 审核：published->published'),
  (5, 'merchant', 2, 'review_reply', 'review', 9005, '商家回复评价 9005'),
  (6, 'merchant', 28, 'review_reply', 'review', 1, '商家回复评价 1'),
  (7, 'user', 1, 'support_session_create', 'support_session', 1, '用户发起咨询：想咨询配送时长能否再快一点'),
  (8, 'user', 1, 'support_session_create', 'support_session', 2, '用户发起咨询：订单完成后想申请补送一份小食'),
  (9, 'user', 1, 'complaint_submit', 'complaint', 1, '用户提交投诉：配送等待时间过长'),
  (10, 'user', 1, 'complaint_submit', 'complaint', 2, '用户提交投诉：电影票无法核销'),
  (11, 'admin', 3, 'complaint_accept', 'complaint', 2, '受理投诉 #CP202605170002'),
  (12, 'user', 1, 'complaint_submit', 'complaint', 3, '用户提交投诉：足疗按摩师傅迟到'),
  (13, 'admin', 3, 'complaint_accept', 'complaint', 3, '受理投诉 #CP202605170003'),
  (14, 'admin', 3, 'complaint_resolve', 'complaint', 3, '处理完成 #CP202605170003')
ON DUPLICATE KEY UPDATE detail = VALUES(detail);

INSERT INTO support_station_message (id, user_id, message_type, title, content, badge_text, read_status, related_order_id) VALUES
  (1, 1, 'order', '外卖订单配送中', '塔斯汀中国汉堡订单正在配送，请留意电话。', '配送', 'unread', 2),
  (2, 1, 'order', '团购券待使用', '江南小馆 3-4 人餐券码已生成，可到店核销。', '券码', 'unread', 3),
  (3, 1, 'system', '欢迎使用爱团', '本地生活服务、外卖、团购和到店核销能力已准备好。', '系统', 'read', NULL),
  (9005, 1, 'order', '外卖订单已完成', '米村拌饭订单已送达，欢迎对本次配送进行评价。', '完成', 'read', 9005),
  (9006, 1, 'order', '酒店券待使用', '曼居影院酒店影音大床房券码已生成，请入住前电话确认房态。', '酒店', 'unread', 9006),
  (9007, 1, 'order', '电影票待付款', '光影剧场电影通兑票订单尚未支付，完成支付后可获得券码。', '待付款', 'unread', 9007),
  (9008, 1, 'order', 'SPA券待预约', '悦己SPA券码已生成，建议提前电话预约到店时间。', '预约', 'read', 9008),
  (9009, 1, 'promotion', '周末到店精选', '烤肉、酒店、电玩城等到店套餐已补充，可在各模块页查看。', '精选', 'unread', NULL)
ON DUPLICATE KEY UPDATE content = VALUES(content), read_status = VALUES(read_status);

INSERT INTO user_favorite (id, user_id, favorite_type, target_id, target_name, cover_url, subtitle) VALUES
  (9001, 1, 'store', 1, '塔斯汀中国汉堡', 'https://picsum.photos/seed/aituan-store-burger/900/520', '现烤汉堡，附近高复购外卖'),
  (9002, 1, 'item', 2001, '江南小馆 3-4 人餐', 'https://picsum.photos/seed/aituan-item-2001/720/540', '招牌鱼头、东坡肉、时蔬组合'),
  (9003, 1, 'store', 9, '雅境足道', 'https://picsum.photos/seed/aituan-store-massage/900/520', '足疗按摩，环境安静'),
  (9004, 1, 'item', 8001, '经典足疗 60 分钟', 'https://picsum.photos/seed/aituan-item-8001/720/540', '足浴放松，含肩颈舒缓'),
  (9005, 1, 'store', 10, '米村拌饭', 'https://picsum.photos/seed/aituan-store-bibimbap/900/520', '热气石锅拌饭，工作日晚餐热门'),
  (9006, 1, 'item', 6101, '全身舒缓 SPA', 'https://picsum.photos/seed/aituan-item-6101/720/540', '90 分钟身体护理，预约优先')
ON DUPLICATE KEY UPDATE target_name = VALUES(target_name), cover_url = VALUES(cover_url), subtitle = VALUES(subtitle), is_deleted = 0;

INSERT INTO platform_announcement (id, title, content, target_client, cover_url, status, start_at, end_at, sort_order, created_by) VALUES
  (9001, '周末本地生活精选上新', '外卖、团购、酒店、娱乐等演示商品已补充图片和券码流程，可用于完整联调展示。', 'all', 'https://picsum.photos/seed/aituan-announcement-weekend/1200/520', 'published', CURRENT_TIMESTAMP, NULL, 1, 3),
  (9002, '商家控制台支持图片维护', '商家可在商品管理和门店资料中上传图片，并维护自动接单、履约规则和券码核销。', 'merchant', 'https://picsum.photos/seed/aituan-announcement-merchant/1200/520', 'published', CURRENT_TIMESTAMP, NULL, 2, 3),
  (9003, '平台治理后台能力扩展', '后台已补充订单治理、商户门店、用户状态、商品上下架、配送任务和公告配置入口。', 'admin', 'https://picsum.photos/seed/aituan-announcement-admin/1200/520', 'published', CURRENT_TIMESTAMP, NULL, 3, 3)
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), target_client = VALUES(target_client), cover_url = VALUES(cover_url), status = VALUES(status), sort_order = VALUES(sort_order), is_deleted = 0;

INSERT INTO sys_config (id, config_key, config_value, remark) VALUES
  (1, 'mock_payment_enabled', 'true', '首包只开放模拟支付'),
  (2, 'delivery_tick_minutes', '3', '配送模拟推进间隔'),
  (3, 'upload_storage_type', 'local', '当前图片上传使用本地文件夹，后续可替换对象存储实现')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), remark = VALUES(remark);

-- Stage6-A/Stage7：会员等级、优惠券与消息跳转演示数据
INSERT INTO member_level (id, level_code, level_name, min_growth_value, benefits, color, sort_order, status) VALUES
  (1, 'NORMAL', '普通会员', 0, '[{"title":"旧等级","desc":"Stage7 已停用普通会员，统一使用白银会员起步"}]', '#8C8C8C', 0, 'disabled'),
  (2, 'SILVER', '白银会员', 0, '[{"title":"白银基础权益","desc":"每周自动刷新 1 张会员优惠券"},{"title":"成长规则","desc":"订单完成后按实付金额累计成长值，首次评价得 3 成长值"}]', '#8C8C8C', 1, 'enabled'),
  (3, 'GOLD', '黄金会员', 300, '[{"title":"黄金周券","desc":"每周自动刷新 2 张会员优惠券"},{"title":"优先客服","desc":"客服会话优先响应"}]', '#D79A00', 2, 'enabled'),
  (4, 'PLATINUM', '白金会员', 1000, '[{"title":"白金周券","desc":"每周自动刷新 2 张更高面额优惠券"},{"title":"专属活动","desc":"优先参与平台活动"}]', '#7B8794', 3, 'enabled'),
  (5, 'DIAMOND', '钻石会员', 3000, '[{"title":"钻石周券","desc":"每周自动刷新 3 张高额优惠券"},{"title":"售后优先","desc":"投诉与售后优先处理"}]', '#3B82F6', 4, 'enabled'),
  (6, 'RED_DIAMOND', '红钻会员', 10000, '[{"title":"红钻周券","desc":"每周自动刷新 4 张高额优惠券"},{"title":"平台礼遇","desc":"享受重点活动专属权益"}]', '#D71918', 5, 'enabled'),
  (7, 'BLACK_DIAMOND', '黑钻会员', 30000, '[{"title":"黑钻周券","desc":"每周自动刷新 5 张最高等级优惠券"},{"title":"顶级服务","desc":"享受平台最高等级服务响应"}]', '#111827', 6, 'enabled')
ON DUPLICATE KEY UPDATE level_name = VALUES(level_name), min_growth_value = VALUES(min_growth_value), benefits = VALUES(benefits), color = VALUES(color), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO coupon_template (id, name, type, face_value, threshold_amount, business_scope, valid_kind, valid_start, valid_end, valid_days, total_qty, issued_qty, per_user_limit, status) VALUES
  (1, '满30减5', 'full_reduction', 5.00, 30.00, 'all', 'absolute', '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 1000, 0, 1, 'enabled'),
  (2, '满50减10', 'full_reduction', 10.00, 50.00, 'all', 'absolute', '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 1000, 0, 1, 'enabled'),
  (3, '新人9折券', 'discount', 0.90, 0.00, 'all', 'relative', NULL, NULL, 30, 0, 0, 1, 'enabled'),
  (4, '满100减20', 'full_reduction', 20.00, 100.00, 'all', 'absolute', '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 500, 0, 1, 'enabled'),
  (5, '已下架体验券', 'full_reduction', 8.00, 40.00, 'all', 'absolute', '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 100, 0, 1, 'disabled')
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), face_value = VALUES(face_value), threshold_amount = VALUES(threshold_amount), business_scope = VALUES(business_scope), valid_kind = VALUES(valid_kind), valid_start = VALUES(valid_start), valid_end = VALUES(valid_end), valid_days = VALUES(valid_days), total_qty = VALUES(total_qty), per_user_limit = VALUES(per_user_limit), status = VALUES(status);

INSERT INTO user_coupon (id, template_id, user_id, status, claimed_at, expire_at, used_at, used_order_id, type_snapshot, face_value_snapshot, threshold_snapshot) VALUES
  (9001, 1, 1, 'unused', CURRENT_TIMESTAMP, '2026-12-31 23:59:59', NULL, NULL, 'full_reduction', 5.00, 30.00),
  (9002, 2, 1, 'unused', CURRENT_TIMESTAMP, '2026-12-31 23:59:59', NULL, NULL, 'full_reduction', 10.00, 50.00),
  (9003, 3, 1, 'used', CURRENT_TIMESTAMP, '2026-12-31 23:59:59', CURRENT_TIMESTAMP, 9005, 'discount', 0.90, 0.00),
  (9004, 4, 1, 'expired', CURRENT_TIMESTAMP, '2026-01-31 23:59:59', NULL, NULL, 'full_reduction', 20.00, 100.00)
ON DUPLICATE KEY UPDATE status = VALUES(status), expire_at = VALUES(expire_at), used_at = VALUES(used_at), used_order_id = VALUES(used_order_id), is_deleted = 0;

UPDATE support_station_message
SET related_target_type = 'order', related_target_id = related_order_id
WHERE related_order_id IS NOT NULL;

INSERT INTO sys_audit_log (id, actor_type, actor_id, action_type, target_type, target_id, detail) VALUES
  (9001, 'admin', 3, 'member_level_update', 'member_level', 3, '更新金卡会员权益说明'),
  (9002, 'admin', 3, 'coupon_template_create', 'coupon_template', 4, '新增满100减20优惠券模板')
ON DUPLICATE KEY UPDATE detail = VALUES(detail);

-- Stage5-D：补齐 7 类非外卖差异化字段
-- business_attributes 串格式：`key:value;key:value`，由用户端按业务类型解析
UPDATE catalog_item SET
  business_attributes = '套餐内容:招牌鱼头、东坡肉、时蔬六道;适用门店:江南小馆全部门店;有效期:购买后 90 天内;不可用日期:除夕、春节当日',
  usage_rules = '到店出示券码，可与节假日通用，单单一码',
  refund_policy = '未核销随时申请退款；已核销不支持退款',
  notice = '高峰建议提前电话留位；可拼桌',
  validity_days = 90
WHERE id = 2001;

UPDATE catalog_item SET
  business_attributes = '套餐内容:双人精选菜品 4 道 + 例汤;适用门店:本店通用;有效期:购买后 60 天内',
  usage_rules = '工作日通用，节假日补差 20 元',
  refund_policy = '未核销支持原路退款',
  notice = '节假日预约请提前 1 天电话确认',
  validity_days = 60
WHERE id = 2002;

UPDATE catalog_item SET
  business_attributes = '房型:大床房 30㎡;床型:1.8m 大床;入住时间:14:00 起;离店时间:次日 12:00;包含:免费早餐 1 人份',
  usage_rules = '请提前 1 天提交预约信息，到店出示券码办理入住',
  refund_policy = '入住前 24 小时支持取消，否则收取首晚费用',
  notice = '法定节假日房态紧张，建议尽早预约',
  validity_days = 180
WHERE id = 3001;

UPDATE catalog_item SET
  business_attributes = '项目类型:沉浸剧情密室;人数:4 人;时长:60-90 分钟;包间:整场独立场地;到店须知:提前 15 分钟到店签字',
  usage_rules = '请提前 1 天电话预约场次，到店核销',
  refund_policy = '未核销前 2 小时支持改期',
  notice = '幽闭恐惧症慎玩；建议穿运动鞋',
  validity_days = 90
WHERE id = 4001;

UPDATE catalog_item SET
  business_attributes = '影片/演出:2D/3D 普通厅通兑;影院/场馆:光影剧场全部门店;日期:购买后 30 天内任选;票档:成人;入场规则:凭券到柜台取票',
  usage_rules = '特殊厅（IMAX、CINITY）补差 30 元',
  refund_policy = '未取票前可申请退款',
  notice = '工作日 22:00 后不可入场',
  validity_days = 30
WHERE id = 5001;

UPDATE catalog_item SET
  business_attributes = '项目流程:清洁-补水-冷膜-头部放松;服务时长:60 分钟;适用人群:一般肌肤;禁忌:严重过敏期、术后恢复期不建议;资质说明:技师持证上岗',
  usage_rules = '请提前 1 天预约时段',
  refund_policy = '未核销前支持退款',
  notice = '过敏体质请提前告知；建议素颜到店',
  validity_days = 90
WHERE id = 6001;

UPDATE catalog_item SET
  business_attributes = '票种:成人电子票;入园日期:购买后 30 天内任选一天;开放时间:09:00-18:00;成人/儿童规则:身高 1.2m 以上按成人;入园地址:中央公园南门;退改规则:入园当日不可退',
  usage_rules = '凭电子票二维码闸机入园',
  refund_policy = '入园前 24 小时支持原路退款',
  notice = '景区高峰建议错峰入园；雨天观景台关闭',
  validity_days = 30
WHERE id = 7001;

UPDATE catalog_item SET
  business_attributes = '项目时长:60 分钟;到店/上门:到店;预约时间:11:00-02:00 自选;技师说明:可指定;注意事项:饭后 1 小时再做;地址要求:仅本店',
  usage_rules = '请提前 30 分钟到店登记',
  refund_policy = '未核销前可改期一次',
  notice = '高血压、孕期不建议；可指定男女技师',
  validity_days = 90
WHERE id = 8001;

UPDATE catalog_item SET
  business_attributes = '项目:肩颈放松;时长:45 分钟;适用人群:久坐人群;到店/上门:到店',
  usage_rules = '到店出示券码即可核销',
  refund_policy = '未核销前支持退款',
  notice = '颈椎严重退变请先就医评估',
  validity_days = 90
WHERE id = 8002;

UPDATE catalog_item SET
  business_attributes = '套餐内容:精选牛五花、梅花肉、蔬菜拼盘、米饭、饮品;适用门店:琥珀烤肉全部门店;有效期:购买后 90 天内',
  usage_rules = '周末通用，节假日不另收差价',
  refund_policy = '未核销随时退款',
  notice = '工作日大堂排位，节假日建议预约包间',
  validity_days = 90
WHERE id = 2101;

UPDATE catalog_item SET
  business_attributes = '套餐内容:多肉拼盘 + 主食 + 饮品;人数:4 人;适用门店:本店通用',
  usage_rules = '到店出示券码核销',
  refund_policy = '未核销支持退款',
  notice = '4 人以上请提前 30 分钟到店',
  validity_days = 90
WHERE id = 2102;

UPDATE catalog_item SET
  business_attributes = '房型:影音大床房 35㎡;床型:1.8m 大床;入住时间:14:00 起;离店时间:次日 12:00;设备:百寸投影 + 杜比音响',
  usage_rules = '入住前 1 天电话确认房态',
  refund_policy = '入住前 24 小时支持取消',
  notice = '影音房需提前 1 天预约，节假日不接散客',
  validity_days = 180
WHERE id = 3101;

UPDATE catalog_item SET
  business_attributes = '房型:商旅双床房;床型:1.5m 双床;入住时间:14:00 起;离店时间:次日 12:00;包含:双早 2 人份',
  usage_rules = '到店出示券码办理入住',
  refund_policy = '入住前 24 小时支持取消',
  notice = '法定节假日不接散客',
  validity_days = 180
WHERE id = 3102;

UPDATE catalog_item SET
  business_attributes = '项目:电玩城游戏币 120 枚;人数:2 人推荐;时长:不限;场地说明:大厅自选机台',
  usage_rules = '到前台核销后凭票据取币',
  refund_policy = '未核销前支持退款',
  notice = '电玩币使用后不退还现金',
  validity_days = 180
WHERE id = 4101;

UPDATE catalog_item SET
  business_attributes = '项目:VR 双人畅玩;时长:90 分钟;场地说明:VR 区域 2 号机',
  usage_rules = '节假日 19:00 后可用',
  refund_policy = '未核销前可改期',
  notice = '心脏病、3D 眩晕慎玩',
  validity_days = 60
WHERE id = 4102;

UPDATE catalog_item SET
  business_attributes = '项目:全身舒缓 SPA;时长:90 分钟;适用人群:一般体质;预约方式:电话或门店预约',
  usage_rules = '请提前 1 天预约时段',
  refund_policy = '未核销前可退',
  notice = '孕期、严重高血压不建议',
  validity_days = 90
WHERE id = 6101;

UPDATE catalog_item SET
  business_attributes = '项目:肩颈热敷放松;时长:45 分钟',
  usage_rules = '到店核销',
  refund_policy = '未核销前可退',
  notice = '颈椎严重退变请先就医',
  validity_days = 90
WHERE id = 6102;

-- Stage5-D：补齐七类业务的演示券码（在已支付未核销订单上）
-- 团购、酒店、娱乐、电影、丽人、景点、按摩各至少一张
INSERT INTO order_voucher (id, order_id, voucher_code, qr_payload, status, effective_to, verified_at, verified_by) VALUES
  (9201, 9008, '88009008', 'AITUAN:VOUCHER:88009008', 'unused', '2026-12-31 23:59:59', NULL, NULL),
  (9202, 9006, '88009006', 'AITUAN:VOUCHER:88009006', 'unused', '2026-12-31 23:59:59', NULL, NULL)
ON DUPLICATE KEY UPDATE qr_payload = VALUES(qr_payload), status = VALUES(status), effective_to = VALUES(effective_to);

-- Stage5-D：演示预约记录（酒店、丽人、娱乐、按摩）
INSERT INTO order_booking_record (id, order_id, business_type, contact_name, contact_phone, booking_date, booking_time_slot, guest_count, store_confirm_status, store_confirm_remark, confirmed_at, confirmed_by) VALUES
  (9301, 9006, 'hotel', '李同学', '13800000001', '2026-06-08', '影音大床房 / 当日 14:00 入住', 2, 'pending', NULL, NULL, NULL),
  (9302, 9008, 'beauty', '王小姐', '13800000002', '2026-05-30', '14:00-15:30', 1, 'confirmed', '已电话沟通，到店签字', CURRENT_TIMESTAMP, 3),
  (9303, 4, 'massage', '李同学', '13800000001', '2026-05-25', '20:00-21:00', 1, 'confirmed', '已到店核销', CURRENT_TIMESTAMP, 2)
ON DUPLICATE KEY UPDATE
  contact_name = VALUES(contact_name),
  contact_phone = VALUES(contact_phone),
  booking_date = VALUES(booking_date),
  booking_time_slot = VALUES(booking_time_slot),
  guest_count = VALUES(guest_count),
  store_confirm_status = VALUES(store_confirm_status);
