import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/features/merchant/presentation/takeaway_amount_utils.dart';

void main() {
  group('takeaway amount utils', () {
    test('起送差额使用 epsilon 避免浮点误差', () {
      final startPrice = 20.0;
      final withinEpsilon = startPrice - takeawayMoneyEpsilon / 2;
      final beyondEpsilon = startPrice - takeawayMoneyEpsilon * 2;

      expect(takeawayStartMissing(withinEpsilon, startPrice), 0);
      expect(takeawayStartMet(withinEpsilon, startPrice), isTrue);
      expect(
        takeawayStartMissing(beyondEpsilon, startPrice),
        closeTo(takeawayMoneyEpsilon * 2, 0.0001),
      );
      expect(takeawayStartMet(beyondEpsilon, startPrice), isFalse);
    });

    test('金额展示向上保留到必要小数位', () {
      expect(takeawayMoneyText(0), '0');
      expect(takeawayMoneyText(12), '12');
      expect(takeawayMoneyText(12.1), '12.1');
      expect(takeawayMoneyText(12.101), '12.11');
      expect(takeawayMoneyText(12.999), '13');
    });
  });
}
