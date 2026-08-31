import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/network/app_api_client.dart';
import 'package:aituan_user_app/features/support/data/support_repository.dart';

class FakeSupportApiClient extends AppApiClient {
  FakeSupportApiClient()
    : super(baseUrl: 'http://fake.local', tokenProvider: () => 'token');

  final calls = <String>[];
  final postBodies = <Map<String, dynamic>>[];
  final responses = <String, Map<String, dynamic>>{};

  @override
  Future<Map<String, dynamic>> get(String path) async {
    calls.add('GET $path');
    return responses[path] ?? {'code': 0, 'data': {}};
  }

  @override
  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> body) async {
    calls.add('POST $path');
    postBodies.add(body);
    return responses[path] ?? {'code': 0, 'data': {}};
  }
}

void main() {
  group('SupportSession and SupportMessage models', () {
    test('平台客服与 AI 模式判断稳定', () {
      final session = SupportSession.fromApi({
        'id': 1,
        'sessionNo': 'SS001',
        'storeId': 0,
        'storeName': '平台客服',
        'topic': '平台客服',
        'status': 'open',
        'unreadCount': 2,
        'createdAt': '2026-06-08T10:00:00',
        'serviceScope': 'platform',
        'assistantMode': 'ai',
      });

      expect(session.isPlatform, isTrue);
      expect(session.isAiMode, isTrue);
      expect(session.unreadCount, 2);
    });

    test('消息发送方便捷判断稳定', () {
      expect(SupportMessage.fromApi({'id': 1, 'senderType': 'user'}).isUser, isTrue);
      expect(
        SupportMessage.fromApi({'id': 2, 'senderType': 'merchant'}).isMerchant,
        isTrue,
      );
      expect(
        SupportMessage.fromApi({'id': 3, 'senderType': 'platform'}).isPlatform,
        isTrue,
      );
    });
  });

  group('SupportRepository', () {
    test('fetchSessions 拼接默认分页和状态过滤', () async {
      final client = FakeSupportApiClient();
      client.responses['/api/app/support/sessions?page=1&pageSize=50&status=open'] = {
        'code': 0,
        'data': {
          'list': [
            {
              'id': 9,
              'sessionNo': 'SS009',
              'storeId': 1,
              'storeName': '塔斯汀中国汉堡',
              'topic': '配送咨询',
              'status': 'open',
              'unreadCount': 0,
              'createdAt': '2026-06-08T10:00:00',
            },
          ],
        },
      };

      final sessions = await SupportRepository(client: client).fetchSessions(
        status: 'open',
      );

      expect(client.calls, [
        'GET /api/app/support/sessions?page=1&pageSize=50&status=open',
      ]);
      expect(sessions.single.storeName, '塔斯汀中国汉堡');
      expect(sessions.single.isPlatform, isFalse);
    });

    test('createSession 只发送非空字段', () async {
      final client = FakeSupportApiClient();
      client.responses['/api/app/support/sessions'] = {
        'code': 0,
        'data': {
          'id': 10,
          'sessionNo': 'SS010',
          'storeId': 0,
          'storeName': '平台客服',
          'topic': '平台客服',
          'status': 'open',
          'createdAt': '2026-06-08T10:00:00',
          'serviceScope': 'platform',
        },
      };

      final session = await SupportRepository(client: client).createSession(
        topic: '  ',
        relatedOrderId: 9011,
      );

      expect(client.calls, ['POST /api/app/support/sessions']);
      expect(client.postBodies.single, {'relatedOrderId': 9011});
      expect(session.isPlatform, isTrue);
    });

    test('sendMessage 与 handoff 使用正确路径', () async {
      final client = FakeSupportApiClient();
      client.responses['/api/app/support/sessions/7/messages'] = {
        'code': 0,
        'data': {
          'id': 71,
          'senderType': 'user',
          'content': '我要转人工',
          'createdAt': '2026-06-08T10:00:00',
        },
      };
      client.responses['/api/app/support/sessions/7/handoff'] = {
        'code': 0,
        'data': {
          'id': 7,
          'sessionNo': 'SS007',
          'storeId': 0,
          'storeName': '平台客服',
          'topic': '平台客服',
          'status': 'open',
          'createdAt': '2026-06-08T10:00:00',
          'assistantMode': 'human',
          'serviceScope': 'platform',
        },
      };
      final repository = SupportRepository(client: client);

      final message = await repository.sendMessage(7, '我要转人工');
      final session = await repository.handoffToHuman(7);

      expect(message.isUser, isTrue);
      expect(session.isAiMode, isFalse);
      expect(client.calls, [
        'POST /api/app/support/sessions/7/messages',
        'POST /api/app/support/sessions/7/handoff',
      ]);
      expect(client.postBodies.first, {'content': '我要转人工'});
    });
  });
}
