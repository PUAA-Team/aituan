import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/features/order/presentation/takeaway_fulfillment_text.dart';
import 'package:aituan_user_app/shared/enums/business_type.dart';

void main() {
  group('takeaway fulfillment text', () {
    test('订单主状态优先于履约状态', () {
      expect(takeawayStatusLabel(OrderStatus.unpaid, 'delivering'), '待付款');
      expect(takeawayStatusDescription(OrderStatus.cancelled, 'delivering'), '订单已取消，本单不会继续配送。');
      expect(takeawayStatusTag(OrderStatus.refunded, 'delivering'), '已退款');
    });

    test('外卖履约状态文案稳定', () {
      expect(takeawayStatusLabel(OrderStatus.pending, 'merchant_pending'), '待商家接单');
      expect(takeawayStatusLabel(OrderStatus.pending, 'preparing'), '备餐中');
      expect(takeawayStatusLabel(OrderStatus.pending, 'delivering'), '配送中');
      expect(takeawayStatusDescription(OrderStatus.pending, 'completed'), contains('评价'));
      expect(takeawayStatusTag(OrderStatus.pending, 'abnormal'), '异常');
    });

    test('未知履约状态回退到订单状态文案', () {
      expect(takeawayStatusLabel(OrderStatus.pending, 'unknown'), '配送中');
      expect(takeawayStatusDescription(OrderStatus.used, 'unknown'), contains('评价'));
      expect(takeawayStatusTag(OrderStatus.used, 'unknown'), '已完成');
    });

    test('时间格式缺省为未到达', () {
      expect(formatTimelineTime(null), '未到达');
      expect(formatTimelineTime(DateTime(2026, 6, 8, 9, 5)), '09:05');
    });
  });
}
