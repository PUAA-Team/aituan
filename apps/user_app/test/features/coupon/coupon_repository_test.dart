import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/network/app_api_client.dart';
import 'package:aituan_user_app/features/coupon/data/coupon_repository.dart';

class FakeAppApiClient extends AppApiClient {
  FakeAppApiClient()
    : super(baseUrl: 'http://fake.local', tokenProvider: () => 'token');

  final calls = <String>[];
  final postBodies = <Map<String, dynamic>>[];
  final responses = <String, Map<String, dynamic>>{};

  @override
  Future<Map<String, dynamic>> get(String path) async {
    calls.add('GET $path');
    return responses[path] ?? {'code': 0, 'data': []};
  }

  @override
  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> body) async {
    calls.add('POST $path');
    postBodies.add(body);
    return responses[path] ?? {'code': 0, 'data': null};
  }
}

void main() {
  group('CouponRepository', () {
    test('fetchMyCoupons 传递状态参数并解析列表', () async {
      final client = FakeAppApiClient();
      client.responses['/api/app/account/coupons?status=usable'] = {
        'code': 0,
        'data': [
          {
            'id': 9001,
            'name': '满30减5',
            'type': 'full_reduction',
            'status': 'unused',
            'discountDesc': '减5元',
            'thresholdDesc': '满30可用',
            'expireAt': '2026-12-31T23:59:59',
          },
        ],
      };

      final coupons = await CouponRepository(client: client).fetchMyCoupons(
        'usable',
      );

      expect(client.calls, ['GET /api/app/account/coupons?status=usable']);
      expect(coupons, hasLength(1));
      expect(coupons.single.id, 9001);
      expect(coupons.single.discountDesc, '减5元');
    });

    test('claimCoupon 使用空 body 调用领取接口', () async {
      final client = FakeAppApiClient();

      await CouponRepository(client: client).claimCoupon(3);

      expect(client.calls, ['POST /api/app/account/coupons/3/claim']);
      expect(client.postBodies.single, isEmpty);
    });

    test('fetchUsableForOrder 传递订单金额并解析可用性', () async {
      final client = FakeAppApiClient();
      client.responses[
          '/api/app/account/coupons/usable-for-order?orderAmount=60.5'] = {
        'code': 0,
        'data': [
          {
            'userCouponId': 9002,
            'name': '满50减10',
            'discountDesc': '减10元',
            'discountAmount': 10,
            'usable': true,
          },
          {
            'userCouponId': 9004,
            'name': '满100减20',
            'discountDesc': '减20元',
            'discountAmount': 0,
            'usable': false,
            'reason': '未达到门槛',
          },
        ],
      };

      final options = await CouponRepository(client: client).fetchUsableForOrder(
        60.5,
      );

      expect(client.calls, [
        'GET /api/app/account/coupons/usable-for-order?orderAmount=60.5',
      ]);
      expect(options, hasLength(2));
      expect(options.first.usable, isTrue);
      expect(options.last.reason, '未达到门槛');
    });
  });
}
