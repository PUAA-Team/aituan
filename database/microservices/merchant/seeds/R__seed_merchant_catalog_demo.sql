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
ON DUPLICATE KEY UPDATE merchant_name = VALUES(merchant_name), contact_phone = VALUES(contact_phone), status = VALUES(status), updated_at = CURRENT_TIMESTAMP;

INSERT INTO merchant_store (id, merchant_id, store_name, business_type, summary, address, distance_text, longitude, latitude, rating, monthly_sales, avg_price, status, business_hours_text, tag_text, cover_url, contact_phone, announcement) VALUES
  (1, 1, '塔斯汀中国汉堡', 'takeaway', '现烤汉堡，附近高复购外卖', '城市广场 1 层', '900m', 116.313600, 39.982300, 4.8, 3290, 28.00, 'open', '09:30-22:30', '35分钟送达,配送费¥4,满减', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a28286abc091.jpg', '18810000001', ''),
  (2, 2, '松记炸鸡饭', 'takeaway', '热卖炸鸡饭和能量套餐', '湖畔商业街 2 层', '1.2km', 116.310800, 39.985700, 4.7, 2180, 24.00, 'open', '10:00-21:30', '出餐快,套餐多,免预约', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2829e1467ae.jpg', '18810000002', ''),
  (3, 3, '江南小馆', 'group_buy', '家常江南菜，到店套餐高性价比', '城市广场 4 层', '800m', 116.316100, 39.981100, 4.8, 1260, 86.00, 'open', '10:30-22:00', '团购,多人餐,可核销', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282a03017d4.jpg', '18810000003', ''),
  (4, 4, '云栖酒店', 'hotel', '商圈舒适酒店，干净安静', '云栖路 88 号', '2.8km', 116.327400, 39.990800, 4.6, 960, 328.00, 'open', '全天营业', '酒店,大床房,可核销', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282772d2664.jpg', '18810000004', ''),
  (5, 5, '星盒密室', 'entertainment', '沉浸式剧情密室和桌游空间', '青年街 18 号 5 层', '1.9km', 116.304900, 39.977700, 4.9, 870, 118.00, 'open', '12:00-23:30', '休闲娱乐,密室,朋友聚会', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a28298c123bf.jpg', '18810000005', ''),
  (6, 6, '光影剧场', 'movie', '热门电影演出优惠票', '时代中心 6 层', '1.5km', 116.321300, 39.984600, 4.7, 1420, 46.00, 'open', '10:00-24:00', '电影演出,优惠票,可核销', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2827dc9977c.jpg', '18810000006', ''),
  (7, 7, '轻颜护理', 'beauty', '皮肤护理和基础医美咨询', '望京街 66 号', '3.1km', 116.336800, 39.995200, 4.8, 760, 198.00, 'open', '10:00-21:00', '丽人医美,护理,到店核销', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282b0aebaef.png', '18810000007', ''),
  (8, 8, '城市观景', 'ticket', '城市地标观景门票', '中央公园南门', '4.5km', 116.289700, 39.967500, 4.6, 1680, 59.00, 'open', '09:00-20:00', '景点门票,亲子,电子券', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a28280795639.jpg', '18810000008', ''),
  (9, 9, '雅境足道', 'massage', '足疗按摩，环境安静', '湖畔路 9 号 3 层', '1.6km', 116.308200, 39.973900, 4.8, 1320, 128.00, 'open', '11:00-02:00', '洗脚,按摩,到店核销', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282b237a939.jpg', '18810000009', ''),
  (10, 10, '米村拌饭', 'takeaway', '热气石锅拌饭，工作日晚餐热门', '时代里 B1 层', '1.1km', 116.318600, 39.986200, 4.7, 2410, 31.00, 'open', '10:00-21:30', '35分钟送达,拌饭,套餐', 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282a9fdba50.jpg', '18810000010', '')
ON DUPLICATE KEY UPDATE store_name = VALUES(store_name), business_type = VALUES(business_type), summary = VALUES(summary), address = VALUES(address), status = VALUES(status), updated_at = CURRENT_TIMESTAMP;

INSERT INTO merchant_delivery_rule (id, store_id, delivery_fee, start_price, estimated_minutes, max_delivery_distance_km, package_fee_mode, package_fee_fixed, package_fee_per_item, distance_extra_threshold_km, distance_extra_fee, distance_extra_step_km, delivery_text) VALUES
  (1, 1, 4.00, 20.00, 35, 5.00, 'fixed', 1.00, 0.00, 3.00, 2.00, 1.00, '骑手模拟配送，预计 35 分钟送达'),
  (2, 2, 3.00, 18.00, 32, 5.00, 'fixed', 1.00, 0.00, 3.00, 2.00, 1.00, '骑手模拟配送，预计 32 分钟送达'),
  (3, 10, 4.00, 22.00, 36, 5.00, 'fixed', 1.00, 0.00, 3.00, 2.00, 1.00, '骑手模拟配送，预计 36 分钟送达')
ON DUPLICATE KEY UPDATE delivery_fee = VALUES(delivery_fee), start_price = VALUES(start_price), estimated_minutes = VALUES(estimated_minutes), updated_at = CURRENT_TIMESTAMP;

INSERT INTO merchant_takeaway_setting (id, store_id, accept_mode, updated_by) VALUES
  (1, 1, 'manual', 2),
  (2, 2, 'auto', 21),
  (3, 10, 'manual', 29)
ON DUPLICATE KEY UPDATE accept_mode = VALUES(accept_mode), updated_by = VALUES(updated_by), updated_at = CURRENT_TIMESTAMP, is_deleted = 0;

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
  (106, 1, 10, 'mc_rice', '石锅拌饭', 'takeaway', 'store_item', 1, 'normal'),
  (107, 1, 10, 'mc_combo', '套餐小食', 'takeaway', 'store_item', 2, 'normal'),
  (201, 2, 3, 'jn_family', '多人餐', 'group_buy', 'store_item', 1, 'normal'),
  (202, 2, 3, 'jn_single', '双人餐', 'group_buy', 'store_item', 2, 'normal'),
  (301, 3, 4, 'yq_room', '房型套餐', 'hotel', 'store_item', 1, 'normal'),
  (401, 4, 5, 'xh_escape', '密室套餐', 'entertainment', 'store_item', 1, 'normal'),
  (501, 5, 6, 'gy_ticket', '电影票', 'movie', 'store_item', 1, 'normal'),
  (601, 6, 7, 'qy_care', '基础护理', 'beauty', 'store_item', 1, 'normal'),
  (701, 7, 8, 'cg_ticket', '观景门票', 'ticket', 'store_item', 1, 'normal'),
  (801, 8, 9, 'yj_foot', '足疗按摩', 'massage', 'store_item', 1, 'normal')
ON DUPLICATE KEY UPDATE category_name = VALUES(category_name), sort_order = VALUES(sort_order), status = VALUES(status), updated_at = CURRENT_TIMESTAMP;

INSERT INTO catalog_item (id, store_id, business_type, category_id, item_name, subtitle, price, original_price, cover_url, rule_text, sales_count, status, item_kind, tag_text, sort_order, business_attributes, usage_rules, refund_policy, notice, validity_days) VALUES
  (1002, 1, 'takeaway', 101, '藤椒鸡腿堡', '微麻藤椒风味，搭配脆生菜', 19.90, 24.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2828962b219.jpg', '外卖商品不单独进入详情页', 1860, 'on_sale', 'takeaway', '微辣,高复购', 1, '', '外卖商品下单后由骑手配送', '已出餐不支持无理由退款', '请保持电话畅通', 1),
  (1003, 1, 'takeaway', 102, '双人汉堡套餐', '双堡 + 小食 + 饮品', 42.80, 52.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2828526c3ff.jpg', '外卖商品不单独进入详情页', 1420, 'on_sale', 'takeaway', '双人,套餐', 1, '', '外卖商品下单后由骑手配送', '已出餐不支持无理由退款', '请保持电话畅通', 1),
  (1004, 1, 'takeaway', 103, '香辣鸡翅', '外酥里嫩，适合加购', 13.90, 16.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2828a7d57d5.jpg', '外卖商品不单独进入详情页', 980, 'on_sale', 'takeaway', '小食,加购', 1, '', '外卖商品下单后由骑手配送', '已出餐不支持无理由退款', '请保持电话畅通', 1),
  (1101, 2, 'takeaway', 104, '招牌炸鸡饭', '整块炸鸡排 + 米饭 + 小菜', 23.80, 28.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2829ec3d1d8.jpg', '外卖商品不单独进入详情页', 1640, 'on_sale', 'takeaway', '热卖,饱腹', 1, '', '外卖商品下单后由骑手配送', '已出餐不支持无理由退款', '请保持电话畅通', 1),
  (1102, 2, 'takeaway', 105, '鸡排饭双拼套餐', '鸡排饭 + 小食 + 饮品', 35.80, 42.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2829f2b2f35.jpg', '外卖商品不单独进入详情页', 820, 'on_sale', 'takeaway', '套餐,午餐', 1, '', '外卖商品下单后由骑手配送', '已出餐不支持无理由退款', '请保持电话畅通', 1),
  (1201, 10, 'takeaway', 106, '招牌石锅拌饭', '牛肉、蔬菜和溏心蛋热拌', 29.80, 36.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282aa9563e8.jpg', '外卖商品不单独进入详情页', 1460, 'on_sale', 'takeaway', '热卖,拌饭', 1, '', '外卖商品下单后由骑手配送', '已出餐不支持无理由退款', '请保持电话畅通', 1),
  (2001, 3, 'group_buy', 201, '江南小馆 3-4 人餐', '招牌鱼头、东坡肉、时蔬组合', 168.00, 218.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282a0bb4344.jpg', '到店出示券码核销，节假日通用', 760, 'on_sale', 'service', '团购,多人餐', 1, '多人餐', '到店出示券码核销，节假日通用', '未核销可退', '高峰期可能排队', 90),
  (2002, 3, 'group_buy', 202, '江南小馆 双人餐', '双人精选菜品，适合工作日晚餐', 98.00, 128.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282a15e4152.jpg', '到店出示券码核销', 680, 'on_sale', 'service', '双人,高性价比', 1, '双人餐', '到店出示券码核销', '未核销可退', '高峰期可能排队', 90),
  (3001, 4, 'hotel', 301, '舒适大床房券', '商圈酒店大床房一晚，预约后入住', 299.00, 388.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282772cdee8.jpg', '首包按券码核销，复杂房态后续接入', 430, 'on_sale', 'service', '酒店,大床房', 1, '大床房', '入住前电话确认房态，到店核销', '未预约可退', '请携带身份证', 90),
  (5001, 6, 'movie', 501, '电影通兑票', '2D/3D 普通厅通兑，特殊厅补差', 39.90, 59.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2827dc999c1.jpg', '首包按券码核销，场次座位后续接入', 980, 'on_sale', 'service', '电影,通兑票', 1, '通兑票', '到店换票后使用', '未核销可退', '特殊厅补差', 90),
  (8001, 9, 'massage', 801, '经典足疗 60 分钟', '足浴放松，含肩颈舒缓', 118.00, 168.00, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a282b2bdcbc2.jpg', '到店出示券码核销', 1080, 'on_sale', 'service', '洗脚,放松', 1, '60分钟服务', '到店出示券码核销', '未核销可退', '建议提前预约', 90)
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name), subtitle = VALUES(subtitle), price = VALUES(price), original_price = VALUES(original_price), cover_url = VALUES(cover_url), status = VALUES(status), updated_at = CURRENT_TIMESTAMP, is_deleted = 0;

INSERT INTO catalog_sku (id, item_id, sku_name, price, stock, status) VALUES
  (2, 1002, '默认', 19.90, 500, 'on_sale'),
  (3, 1003, '默认', 42.80, 300, 'on_sale'),
  (4, 1004, '默认', 13.90, 500, 'on_sale'),
  (5, 1101, '默认', 23.80, 500, 'on_sale'),
  (6, 1102, '默认', 35.80, 3, 'on_sale'),
  (16, 1201, '默认', 29.80, 500, 'on_sale'),
  (7, 2001, '默认', 168.00, 100, 'on_sale'),
  (8, 2002, '默认', 98.00, 100, 'on_sale'),
  (9, 3001, '默认', 299.00, 80, 'on_sale'),
  (11, 5001, '默认', 39.90, 500, 'on_sale'),
  (14, 8001, '默认', 118.00, 150, 'on_sale')
ON DUPLICATE KEY UPDATE price = VALUES(price), stock = VALUES(stock), status = VALUES(status), updated_at = CURRENT_TIMESTAMP, is_deleted = 0;

INSERT INTO member_recommend_config (id, scene, business_type, store_id, item_id, sort_order, status) VALUES
  (1, 'home_recommend', 'takeaway', 1, 1002, 1, 'normal'),
  (2, 'home_recommend', 'takeaway', 2, 1101, 2, 'normal'),
  (3, 'home_recommend', 'group_buy', 3, 2001, 3, 'normal'),
  (4, 'home_recommend', 'hotel', 4, 3001, 4, 'normal'),
  (5, 'home_recommend', 'movie', 6, 5001, 5, 'normal'),
  (6, 'home_recommend', 'massage', 9, 8001, 6, 'normal'),
  (7, 'home_recommend', 'takeaway', 10, 1201, 7, 'normal')
ON DUPLICATE KEY UPDATE scene = VALUES(scene), business_type = VALUES(business_type), store_id = VALUES(store_id), item_id = VALUES(item_id), sort_order = VALUES(sort_order), status = VALUES(status), updated_at = CURRENT_TIMESTAMP;
