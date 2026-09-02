INSERT INTO order_main(
  id, order_no, user_id, store_id, merchant_id, coupon_id, store_name, order_type, title,
  display_status, payment_status, fulfillment_status, payment_method, amount, delivery_fee,
  package_fee, discount_amount, payable_amount, address_snapshot, idempotency_key, paid_at
) VALUES (
  9001, 'T202609010001', 5001, 1, 1, NULL, '塔斯汀中国汉堡', 'takeaway', '藤椒鸡腿堡',
  'pending', 'paid', 'merchant_pending', 'wechat', 39.80, 4.00,
  1.00, 0.00, 44.80, '李同学 18800001111 北京市北京市海淀区城市广场 A 座 1208',
  'seed:trade:order:9001', CURRENT_TIMESTAMP
) ON DUPLICATE KEY UPDATE
  merchant_id = VALUES(merchant_id), coupon_id = VALUES(coupon_id), store_name = VALUES(store_name),
  title = VALUES(title), payment_status = VALUES(payment_status), fulfillment_status = VALUES(fulfillment_status),
  payable_amount = VALUES(payable_amount), updated_at = CURRENT_TIMESTAMP, is_deleted = 0;

INSERT INTO order_item(
  id, order_id, item_id, sku_id, item_name, item_subtitle, business_type, category_id,
  quantity, unit_price, total_price, cover_url, is_reviewed
) VALUES (
  9001, 9001, 1002, 2, '藤椒鸡腿堡', '微麻藤椒风味，搭配脆生菜', 'takeaway', 101,
  2, 19.90, 39.80, 'https://2bpic.oss-cn-beijing.aliyuncs.com/2026/06/09/6a2828962b219.jpg', 0
) ON DUPLICATE KEY UPDATE
  sku_id = VALUES(sku_id), item_name = VALUES(item_name), quantity = VALUES(quantity),
  unit_price = VALUES(unit_price), total_price = VALUES(total_price), updated_at = CURRENT_TIMESTAMP, is_deleted = 0;

INSERT INTO order_payment_record(
  id, order_id, payment_no, payment_method, amount, status, provider_trade_no, paid_at
) VALUES (
  9001, 9001, 'PAY202609010001', 'wechat', 44.80, 'paid', 'DEMO-PAY-9001', CURRENT_TIMESTAMP
) ON DUPLICATE KEY UPDATE
  status = VALUES(status), amount = VALUES(amount), paid_at = VALUES(paid_at), updated_at = CURRENT_TIMESTAMP, is_deleted = 0;

INSERT INTO delivery_task(
  id, order_id, current_stage, current_stage_text, eta_minutes, next_tick_at, auto_advance_enabled
) VALUES (
  9001, 9001, 'merchant_pending', '待商家接单', 35, NULL, 0
) ON DUPLICATE KEY UPDATE
  current_stage = VALUES(current_stage), current_stage_text = VALUES(current_stage_text),
  eta_minutes = VALUES(eta_minutes), updated_at = CURRENT_TIMESTAMP, is_deleted = 0;

INSERT INTO order_state_log(
  id, order_id, from_status, to_status, action_type, operator_type, operator_id, remark
) VALUES (
  9001, 9001, 'created', 'merchant_pending', 'pay', 'user', 5001, '四库一致性演示订单'
) ON DUPLICATE KEY UPDATE remark = VALUES(remark);
