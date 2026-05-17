INSERT INTO iam_role (id, role_code, role_name, status, remark) VALUES
  (1, 'USER', '消费者', 'normal', '用户端账号'),
  (2, 'MERCHANT', '商家', 'normal', '商家端账号'),
  (3, 'ADMIN', '管理员', 'normal', '平台后台账号')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO iam_account (id, account_no, account_type, login_name, phone, email, password_hash, status) VALUES
  (1, 'U202605170001', 'USER', 'demo_user', '18800001111', 'user@example.com', '{noop}aituan123', 'normal'),
  (2, 'M202605170001', 'MERCHANT', 'demo_merchant', '18800002222', 'merchant@example.com', '{noop}aituan123', 'normal'),
  (3, 'A202605170001', 'ADMIN', 'demo_admin', '18800003333', 'admin@example.com', '{noop}aituan123', 'normal')
ON DUPLICATE KEY UPDATE login_name = VALUES(login_name), password_hash = VALUES(password_hash);

INSERT INTO iam_account_role (id, account_id, role_id) VALUES
  (1, 1, 1),
  (2, 2, 2),
  (3, 3, 3)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO user_profile (id, account_id, nickname, avatar_url, register_source, member_level_name, growth_value, status) VALUES
  (1, 1, '爱团用户', '', 'demo', '普通会员', 128, 'normal')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), growth_value = VALUES(growth_value);

INSERT INTO user_address (id, user_id, contact_name, contact_phone, province, city, district, detail_address, tag_name, is_default, delivery_note) VALUES
  (1, 1, '李同学', '18800001111', '北京市', '北京市', '海淀区', '城市广场 A 座 1208', '公司', 1, '送到前台'),
  (2, 1, '李同学', '18800001111', '北京市', '北京市', '朝阳区', '湖畔花园 3 号楼 1801', '家', 0, '电话联系')
ON DUPLICATE KEY UPDATE detail_address = VALUES(detail_address), is_default = VALUES(is_default);

INSERT INTO merchant_profile (id, merchant_no, account_id, merchant_name, contact_name, contact_phone, license_no, status, audit_status, settled_at) VALUES
  (1, 'MCH001', 2, '塔斯汀中国汉堡', '张店长', '18810000001', 'L001', 'normal', 'approved', CURRENT_TIMESTAMP),
  (2, 'MCH002', NULL, '松记炸鸡饭', '王店长', '18810000002', 'L002', 'normal', 'approved', CURRENT_TIMESTAMP),
  (3, 'MCH003', NULL, '江南小馆', '陈店长', '18810000003', 'L003', 'normal', 'approved', CURRENT_TIMESTAMP),
  (4, 'MCH004', NULL, '云栖酒店', '赵经理', '18810000004', 'L004', 'normal', 'approved', CURRENT_TIMESTAMP),
  (5, 'MCH005', NULL, '星盒密室', '孙经理', '18810000005', 'L005', 'normal', 'approved', CURRENT_TIMESTAMP),
  (6, 'MCH006', NULL, '光影剧场', '周经理', '18810000006', 'L006', 'normal', 'approved', CURRENT_TIMESTAMP),
  (7, 'MCH007', NULL, '轻颜护理', '吴经理', '18810000007', 'L007', 'normal', 'approved', CURRENT_TIMESTAMP),
  (8, 'MCH008', NULL, '城市观景', '郑经理', '18810000008', 'L008', 'normal', 'approved', CURRENT_TIMESTAMP),
  (9, 'MCH009', NULL, '雅境足道', '冯经理', '18810000009', 'L009', 'normal', 'approved', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE merchant_name = VALUES(merchant_name), status = VALUES(status);

INSERT INTO merchant_store (id, merchant_id, store_name, business_type, summary, address, distance_text, rating, monthly_sales, avg_price, status, business_hours_text, tag_text, cover_url) VALUES
  (1, 1, '塔斯汀中国汉堡', 'takeaway', '现烤汉堡，附近高复购外卖', '城市广场 1 层', '900m', 4.8, 3290, 28.00, 'open', '09:30-22:30', '35分钟送达,配送费¥4,满减', ''),
  (2, 2, '松记炸鸡饭', 'takeaway', '热卖炸鸡饭和能量套餐', '湖畔商业街 2 层', '1.2km', 4.7, 2180, 24.00, 'open', '10:00-21:30', '出餐快,套餐多,免预约', ''),
  (3, 3, '江南小馆', 'group_buy', '家常江南菜，到店套餐高性价比', '城市广场 4 层', '800m', 4.8, 1260, 86.00, 'open', '10:30-22:00', '团购,多人餐,可核销', ''),
  (4, 4, '云栖酒店', 'hotel', '商圈舒适酒店，干净安静', '云栖路 88 号', '2.8km', 4.6, 960, 328.00, 'open', '全天营业', '酒店,大床房,可核销', ''),
  (5, 5, '星盒密室', 'entertainment', '沉浸式剧情密室和桌游空间', '青年街 18 号 5 层', '1.9km', 4.9, 870, 118.00, 'open', '12:00-23:30', '休闲娱乐,密室,朋友聚会', ''),
  (6, 6, '光影剧场', 'movie', '热门电影演出优惠票', '时代中心 6 层', '1.5km', 4.7, 1420, 46.00, 'open', '10:00-24:00', '电影演出,优惠票,可核销', ''),
  (7, 7, '轻颜护理', 'beauty', '皮肤护理和基础医美咨询', '望京街 66 号', '3.1km', 4.8, 760, 198.00, 'open', '10:00-21:00', '丽人医美,护理,到店核销', ''),
  (8, 8, '城市观景', 'ticket', '城市地标观景门票', '中央公园南门', '4.5km', 4.6, 1680, 59.00, 'open', '09:00-20:00', '景点门票,亲子,电子券', ''),
  (9, 9, '雅境足道', 'massage', '足疗按摩，环境安静', '湖畔路 9 号 3 层', '1.6km', 4.8, 1320, 128.00, 'open', '11:00-02:00', '洗脚,按摩,到店核销', '')
ON DUPLICATE KEY UPDATE summary = VALUES(summary), business_type = VALUES(business_type), tag_text = VALUES(tag_text);

INSERT INTO merchant_delivery_rule (id, store_id, delivery_fee, start_price, estimated_minutes, delivery_text) VALUES
  (1, 1, 4.00, 20.00, 35, '骑手模拟配送，预计 35 分钟送达'),
  (2, 2, 3.00, 18.00, 32, '骑手模拟配送，预计 32 分钟送达')
ON DUPLICATE KEY UPDATE delivery_fee = VALUES(delivery_fee), estimated_minutes = VALUES(estimated_minutes);

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
  (701, 7, 8, 'cg_ticket', '观景门票', 'ticket', 'store_item', 1, 'normal'),
  (801, 8, 9, 'yj_foot', '足疗按摩', 'massage', 'store_item', 1, 'normal')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name), sort_order = VALUES(sort_order);

INSERT INTO catalog_item (id, store_id, business_type, category_id, item_name, subtitle, price, original_price, cover_url, rule_text, sales_count, status, item_kind, tag_text, sort_order) VALUES
  (1001, 1, 'takeaway', 101, '招牌中国汉堡', '现烤胚皮 · 酱香鸡腿排', 18.80, 22.00, '', '外卖商品不单独进入详情页', 2380, 'on_sale', 'takeaway', '热销,现做', 1),
  (1002, 1, 'takeaway', 101, '藤椒鸡腿堡', '微麻藤椒风味，搭配脆生菜', 19.90, 24.00, '', '外卖商品不单独进入详情页', 1860, 'on_sale', 'takeaway', '微辣,高复购', 2),
  (1003, 1, 'takeaway', 102, '双人汉堡套餐', '双堡 + 小食 + 饮品', 42.80, 52.00, '', '外卖商品不单独进入详情页', 1420, 'on_sale', 'takeaway', '双人,套餐', 1),
  (1004, 1, 'takeaway', 103, '香辣鸡翅', '外酥里嫩，适合加购', 13.90, 16.00, '', '外卖商品不单独进入详情页', 980, 'on_sale', 'takeaway', '小食,加购', 1),
  (1101, 2, 'takeaway', 104, '招牌炸鸡饭', '整块炸鸡排 + 米饭 + 小菜', 23.80, 28.00, '', '外卖商品不单独进入详情页', 1640, 'on_sale', 'takeaway', '热卖,饱腹', 1),
  (1102, 2, 'takeaway', 105, '鸡排饭双拼套餐', '鸡排饭 + 小食 + 饮品', 35.80, 42.00, '', '外卖商品不单独进入详情页', 820, 'on_sale', 'takeaway', '套餐,午餐', 1),
  (2001, 3, 'group_buy', 201, '江南小馆 3-4 人餐', '招牌鱼头、东坡肉、时蔬组合', 168.00, 218.00, '', '到店出示券码核销，节假日通用', 760, 'on_sale', 'service', '团购,多人餐', 1),
  (2002, 3, 'group_buy', 202, '江南小馆 双人餐', '双人精选菜品，适合工作日晚餐', 98.00, 128.00, '', '到店出示券码核销', 680, 'on_sale', 'service', '双人,高性价比', 1),
  (3001, 4, 'hotel', 301, '舒适大床房券', '商圈酒店大床房一晚，预约后入住', 299.00, 388.00, '', '首包按券码核销，复杂房态后续接入', 430, 'on_sale', 'service', '酒店,大床房', 1),
  (4001, 5, 'entertainment', 401, '星盒密室 4 人套票', '任选主题，提前电话预约', 188.00, 248.00, '', '到店核销后使用，具体场次以后接入', 520, 'on_sale', 'service', '密室,朋友聚会', 1),
  (5001, 6, 'movie', 501, '电影通兑票', '2D/3D 普通厅通兑，特殊厅补差', 39.90, 59.00, '', '首包按券码核销，场次座位后续接入', 980, 'on_sale', 'service', '电影,通兑票', 1),
  (6001, 7, 'beauty', 601, '基础皮肤护理', '清洁补水护理，到店核销', 168.00, 238.00, '', '使用前建议电话确认档期', 360, 'on_sale', 'service', '护理,补水', 1),
  (7001, 8, 'ticket', 701, '城市观景成人票', '地标观景台成人票，电子券核销', 59.00, 88.00, '', '首包不做日期票，按券码核销', 1220, 'on_sale', 'service', '门票,亲子', 1),
  (8001, 9, 'massage', 801, '经典足疗 60 分钟', '足浴放松，含肩颈舒缓', 118.00, 168.00, '', '到店出示券码核销', 1080, 'on_sale', 'service', '洗脚,放松', 1),
  (8002, 9, 'massage', 801, '肩颈舒缓 45 分钟', '适合久坐人群，到店核销', 98.00, 138.00, '', '到店出示券码核销', 760, 'on_sale', 'service', '按摩,肩颈', 2)
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name), price = VALUES(price), tag_text = VALUES(tag_text);

INSERT INTO catalog_sku (id, item_id, sku_name, price, stock, status) VALUES
  (1, 1001, '默认', 18.80, 500, 'on_sale'),
  (2, 1002, '默认', 19.90, 500, 'on_sale'),
  (3, 1003, '默认', 42.80, 300, 'on_sale'),
  (4, 1004, '默认', 13.90, 500, 'on_sale'),
  (5, 1101, '默认', 23.80, 500, 'on_sale'),
  (6, 1102, '默认', 35.80, 300, 'on_sale'),
  (7, 2001, '默认', 168.00, 100, 'on_sale'),
  (8, 2002, '默认', 98.00, 100, 'on_sale'),
  (9, 3001, '默认', 299.00, 80, 'on_sale'),
  (10, 4001, '默认', 188.00, 80, 'on_sale'),
  (11, 5001, '默认', 39.90, 500, 'on_sale'),
  (12, 6001, '默认', 168.00, 80, 'on_sale'),
  (13, 7001, '默认', 59.00, 300, 'on_sale'),
  (14, 8001, '默认', 118.00, 150, 'on_sale'),
  (15, 8002, '默认', 98.00, 150, 'on_sale')
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
  (9, 'home_recommend', 'massage', 9, 8001, 9, 'normal')
ON DUPLICATE KEY UPDATE sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO order_main (id, order_no, user_id, store_id, store_name, order_type, title, display_status, payment_status, fulfillment_status, payment_method, amount, delivery_fee, discount_amount, payable_amount, address_snapshot, voucher_summary, paid_at, completed_at, created_at) VALUES
  (1, 'AT202605170001', 1, 3, '江南小馆', 'group_buy', '江南小馆 双人餐', 'unpaid', 'unpaid', 'created', NULL, 98.00, 0.00, 0.00, 98.00, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP),
  (2, 'AT202605170002', 1, 1, '塔斯汀中国汉堡', 'takeaway', '招牌中国汉堡等2件', 'pending', 'paid', 'delivering', 'mock', 32.70, 4.00, 0.00, 36.70, '北京市海淀区城市广场 A 座 1208', NULL, CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (3, 'AT202605170003', 1, 3, '江南小馆', 'group_buy', '江南小馆 3-4 人餐', 'unused', 'paid', 'voucher_unused', 'mock', 168.00, 0.00, 0.00, 168.00, NULL, '券码 88001234', CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP),
  (4, 'AT202605170004', 1, 9, '雅境足道', 'massage', '经典足疗 60 分钟', 'used', 'paid', 'voucher_used', 'mock', 118.00, 0.00, 0.00, 118.00, NULL, '券码 88005678', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE display_status = VALUES(display_status), payment_status = VALUES(payment_status);

INSERT INTO order_item (id, order_id, item_id, item_name, item_subtitle, business_type, category_id, quantity, unit_price, total_price, cover_url, is_reviewed) VALUES
  (1, 1, 2002, '江南小馆 双人餐', '双人精选菜品，适合工作日晚餐', 'group_buy', 202, 1, 98.00, 98.00, '', 0),
  (2, 2, 1001, '招牌中国汉堡', '现烤胚皮 · 酱香鸡腿排', 'takeaway', 101, 1, 18.80, 18.80, '', 0),
  (3, 2, 1004, '香辣鸡翅', '外酥里嫩，适合加购', 'takeaway', 103, 1, 13.90, 13.90, '', 0),
  (4, 3, 2001, '江南小馆 3-4 人餐', '招牌鱼头、东坡肉、时蔬组合', 'group_buy', 201, 1, 168.00, 168.00, '', 0),
  (5, 4, 8001, '经典足疗 60 分钟', '足浴放松，含肩颈舒缓', 'massage', 801, 1, 118.00, 118.00, '', 1)
ON DUPLICATE KEY UPDATE total_price = VALUES(total_price), is_reviewed = VALUES(is_reviewed);

INSERT INTO order_payment_record (id, order_id, payment_no, payment_method, amount, status, provider_trade_no, paid_at) VALUES
  (1, 2, 'PAY202605170002', 'mock', 36.70, 'paid', 'MOCK202605170002', CURRENT_TIMESTAMP),
  (2, 3, 'PAY202605170003', 'mock', 168.00, 'paid', 'MOCK202605170003', CURRENT_TIMESTAMP),
  (3, 4, 'PAY202605170004', 'mock', 118.00, 'paid', 'MOCK202605170004', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE status = VALUES(status), amount = VALUES(amount);

INSERT INTO order_voucher (id, order_id, voucher_code, qr_payload, status, effective_to, verified_at, verified_by) VALUES
  (1, 3, '88001234', 'AITUAN:VOUCHER:88001234', 'unused', '2026-12-31 23:59:59', NULL, NULL),
  (2, 4, '88005678', 'AITUAN:VOUCHER:88005678', 'used', '2026-12-31 23:59:59', CURRENT_TIMESTAMP, 2)
ON DUPLICATE KEY UPDATE status = VALUES(status), qr_payload = VALUES(qr_payload);

INSERT INTO delivery_task (id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at) VALUES
  (1, 2, 'delivering', '骑手正在配送', 18, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE current_stage = VALUES(current_stage), current_stage_text = VALUES(current_stage_text);

INSERT INTO delivery_track_node (id, delivery_task_id, node_order, node_code, node_text, reached_at) VALUES
  (1, 1, 1, 'accepted', '商家已接单', CURRENT_TIMESTAMP),
  (2, 1, 2, 'preparing', '商家正在备餐', CURRENT_TIMESTAMP),
  (3, 1, 3, 'delivering', '骑手正在配送', CURRENT_TIMESTAMP),
  (4, 1, 4, 'delivered', '订单已送达', NULL)
ON DUPLICATE KEY UPDATE reached_at = VALUES(reached_at);

INSERT INTO review_record (id, order_id, store_id, user_id, rating, content, labels, status, replied) VALUES
  (1, 4, 9, 1, 5, '环境安静，服务稳定，券码核销很顺畅。', '环境好,服务细致', 'published', 0)
ON DUPLICATE KEY UPDATE content = VALUES(content), rating = VALUES(rating);

INSERT INTO support_station_message (id, user_id, message_type, title, content, badge_text, read_status, related_order_id) VALUES
  (1, 1, 'order', '外卖订单配送中', '塔斯汀中国汉堡订单正在配送，请留意电话。', '配送', 'unread', 2),
  (2, 1, 'order', '团购券待使用', '江南小馆 3-4 人餐券码已生成，可到店核销。', '券码', 'unread', 3),
  (3, 1, 'system', '欢迎使用爱团', '本地生活服务、外卖、团购和到店核销能力已准备好。', '系统', 'read', NULL)
ON DUPLICATE KEY UPDATE content = VALUES(content), read_status = VALUES(read_status);

INSERT INTO sys_config (id, config_key, config_value, remark) VALUES
  (1, 'mock_payment_enabled', 'true', '首包只开放模拟支付'),
  (2, 'delivery_tick_minutes', '3', '配送模拟推进间隔')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);
