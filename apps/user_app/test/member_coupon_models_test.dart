import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/features/coupon/data/coupon_repository.dart';
import 'package:aituan_user_app/features/member/data/member_repository.dart';

// 账号资产相关模型的解析单测（纯逻辑，无网络）
void main() {
  group('MemberInfo.fromApi', () {
    test('解析普通等级，含下一级成长进度与权益', () {
      final info = MemberInfo.fromApi({
        'currentLevelCode': 'SILVER',
        'currentLevelName': '银卡会员',
        'currentColor': '#9AA4B2',
        'growthValue': 128,
        'nextLevelName': '金卡会员',
        'nextLevelMinGrowth': 800,
        'growthToNextLevel': 672,
        'progressPercent': 16,
        'benefits': [
          {'title': '专享券', 'desc': '每月可领银卡专享优惠券'},
        ],
      });
      expect(info.currentLevelName, '银卡会员');
      expect(info.growthValue, 128);
      expect(info.isTopLevel, isFalse);
      expect(info.nextLevelName, '金卡会员');
      expect(info.progressPercent, 16);
      expect(info.benefits, hasLength(1));
      expect(info.benefits.first.title, '专享券');
    });

    test('最高等级时 nextLevelName 为空且 isTopLevel 为真', () {
      final info = MemberInfo.fromApi({
        'currentLevelCode': 'PLATINUM',
        'currentLevelName': '铂金会员',
        'growthValue': 3000,
        'progressPercent': 100,
        'benefits': <dynamic>[],
      });
      expect(info.isTopLevel, isTrue);
      expect(info.nextLevelName, isNull);
      expect(info.benefits, isEmpty);
    });
  });

  group('UserCoupon.fromApi', () {
    test('解析可用券字段与有效期', () {
      final coupon = UserCoupon.fromApi({
        'id': 9001,
        'name': '满30减5',
        'type': 'full_reduction',
        'status': 'unused',
        'discountDesc': '减5元',
        'thresholdDesc': '满30可用',
        'expireAt': '2026-12-31T23:59:59',
        'usedAt': null,
      });
      expect(coupon.id, 9001);
      expect(coupon.status, 'unused');
      expect(coupon.discountDesc, '减5元');
      expect(coupon.expireAt, isNotNull);
      expect(coupon.usedAt, isNull);
    });
  });

  group('OrderCouponOption.fromApi', () {
    test('解析下单可用券', () {
      final coupon = OrderCouponOption.fromApi({
        'userCouponId': 9001,
        'name': '满30减5',
        'discountDesc': '减5元',
        'discountAmount': 5,
        'usable': true,
      });
      expect(coupon.userCouponId, 9001);
      expect(coupon.discountAmount, 5);
      expect(coupon.usable, isTrue);
      expect(coupon.reason, isNull);
    });
  });

  group('AvailableCoupon.fromApi', () {
    test('解析限量与不可领原因', () {
      final coupon = AvailableCoupon.fromApi({
        'templateId': 1,
        'name': '满30减5',
        'type': 'full_reduction',
        'discountDesc': '减5元',
        'thresholdDesc': '满30可用',
        'validDesc': '有效期至2026-12-31',
        'remaining': 5,
        'claimable': false,
        'reason': '已达领取上限',
      });
      expect(coupon.templateId, 1);
      expect(coupon.remaining, 5);
      expect(coupon.claimable, isFalse);
      expect(coupon.reason, '已达领取上限');
    });

    test('remaining 缺省为 null 表示不限量', () {
      final coupon = AvailableCoupon.fromApi({
        'templateId': 3,
        'name': '新人9折券',
        'type': 'discount',
        'discountDesc': '9折',
        'thresholdDesc': '无门槛',
        'validDesc': '领取后30天内有效',
        'claimable': true,
      });
      expect(coupon.remaining, isNull);
      expect(coupon.claimable, isTrue);
      expect(coupon.reason, isNull);
    });
  });
}
