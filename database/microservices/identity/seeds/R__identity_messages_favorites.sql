INSERT INTO support_station_message (id, user_id, message_type, title, content, badge_text, read_status, related_order_id, related_target_type, related_target_id, idempotency_key) VALUES
  (6001, 5001, 'order', '订单状态更新', '您的订单已支付成功，商家正在准备。', '订单', 'unread', 9001, 'order', 9001, 'seed:message:6001'),
  (6002, 5001, 'coupon', '优惠券到账', '新用户优惠券已放入账户，可在结算时使用。', '优惠', 'read', NULL, 'coupon', 8001, 'seed:message:6002')
ON DUPLICATE KEY UPDATE title = VALUES(title), content = VALUES(content), badge_text = VALUES(badge_text), read_status = VALUES(read_status), related_order_id = VALUES(related_order_id), related_target_type = VALUES(related_target_type), related_target_id = VALUES(related_target_id), updated_at = current_timestamp;

INSERT INTO user_favorite (id, user_id, favorite_type, target_id, target_name, cover_url, subtitle) VALUES
  (8101, 5001, 'store', 4001, '爱团演示门店', '', '收藏对象快照由商家服务提供'),
  (8102, 5001, 'item', 3001, '招牌套餐', '', '商品快照由商家商品服务提供')
ON DUPLICATE KEY UPDATE target_name = VALUES(target_name), cover_url = VALUES(cover_url), subtitle = VALUES(subtitle), is_deleted = 0;

INSERT INTO member_growth_log(user_id, order_id, source_type, source_id, delta, reason, created_at) VALUES
  (5001, NULL, 'demo_seed', 5001, 120, '演示成长值初始化', current_timestamp)
ON DUPLICATE KEY UPDATE delta = VALUES(delta), reason = VALUES(reason);
