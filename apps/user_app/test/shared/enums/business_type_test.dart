import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/shared/enums/business_type.dart';

void main() {
  group('BusinessType mapping', () {
    test('兼容后端业务类型别名', () {
      expect(businessTypeFromApi('takeaway'), BusinessType.takeaway);
      expect(businessTypeFromApi('group'), BusinessType.groupBuy);
      expect(businessTypeFromApi('group_buy'), BusinessType.groupBuy);
      expect(businessTypeFromApi('groupbuy'), BusinessType.groupBuy);
      expect(businessTypeFromApi('fun'), BusinessType.entertainment);
      expect(businessTypeFromApi('unknown'), BusinessType.takeaway);
    });

    test('API code 与中文 label 稳定', () {
      expect(businessTypeApiCode(BusinessType.groupBuy), 'group_buy');
      expect(BusinessType.movie.label, '电影演出');
      expect(BusinessType.takeaway.isTakeaway, isTrue);
    });
  });

  group('Order kind and status mapping', () {
    test('订单类型未知时回退为 service', () {
      expect(orderKindFromApi('takeaway'), OrderKind.takeaway);
      expect(orderKindFromApi('unknown'), OrderKind.service);
    });

    test('订单状态 label 区分外卖与服务订单', () {
      expect(orderStatusFromApi('pending'), OrderStatus.pending);
      expect(orderStatusFromApi('bad'), OrderStatus.unpaid);
      expect(OrderStatus.pending.labelForKind(OrderKind.takeaway), '配送中');
      expect(OrderStatus.pending.labelForKind(OrderKind.service), '处理中');
      expect(OrderStatus.unused.labelForKind(OrderKind.service), '待使用');
      expect(OrderStatus.unused.labelForKind(OrderKind.takeaway), '已完成');
    });
  });
}
