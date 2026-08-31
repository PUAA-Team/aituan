import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/network/app_api_client.dart';
import 'package:aituan_user_app/features/member/data/member_repository.dart';

class FakeMemberApiClient extends AppApiClient {
  FakeMemberApiClient()
    : super(baseUrl: 'http://fake.local', tokenProvider: () => 'token');

  final calls = <String>[];
  final responses = <String, Map<String, dynamic>>{};

  @override
  Future<Map<String, dynamic>> get(String path) async {
    calls.add('GET $path');
    return responses[path] ?? {'code': 0, 'data': {}};
  }
}

void main() {
  group('MemberInfo model', () {
    test('解析会员等级、成长值和权益列表', () {
      final info = MemberInfo.fromApi({
        'currentLevelCode': 'gold',
        'currentLevelName': '黄金会员',
        'currentColor': '#d4a017',
        'growthValue': 1280,
        'nextLevelName': '铂金会员',
        'nextLevelMinGrowth': 2000,
        'growthToNextLevel': 720,
        'progressPercent': 64,
        'benefits': [
          {'title': '专属优惠券', 'desc': '每月可领一张'},
        ],
      });

      expect(info.currentLevelCode, 'gold');
      expect(info.currentLevelName, '黄金会员');
      expect(info.growthValue, 1280);
      expect(info.progressPercent, 64);
      expect(info.isTopLevel, isFalse);
      expect(info.benefits.single.title, '专属优惠券');
    });

    test('缺省字段 fallback 且最高等级判断稳定', () {
      final info = MemberInfo.fromApi({});

      expect(info.currentLevelCode, '');
      expect(info.currentLevelName, '普通会员');
      expect(info.growthValue, 0);
      expect(info.progressPercent, 0);
      expect(info.nextLevelName, isNull);
      expect(info.isTopLevel, isTrue);
      expect(info.benefits, isEmpty);
    });
  });

  group('MemberRepository', () {
    test('fetchMemberInfo 使用会员中心接口并解析响应', () async {
      final client = FakeMemberApiClient();
      client.responses['/api/app/account/member/info'] = {
        'code': 0,
        'data': {
          'currentLevelCode': 'silver',
          'currentLevelName': '白银会员',
          'growthValue': 320,
          'progressPercent': 32,
          'benefits': const [],
        },
      };

      final info = await MemberRepository(client: client).fetchMemberInfo();

      expect(client.calls, ['GET /api/app/account/member/info']);
      expect(info.currentLevelName, '白银会员');
      expect(info.growthValue, 320);
    });
  });
}
