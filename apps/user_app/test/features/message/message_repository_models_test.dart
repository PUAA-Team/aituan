import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/app/app_state.dart';
import 'package:aituan_user_app/core/network/app_api_client.dart';
import 'package:aituan_user_app/features/home/data/backend_app_repository.dart';
import 'package:aituan_user_app/shared/models/message_item.dart';

class FakeMessageApiClient extends AppApiClient {
  FakeMessageApiClient()
    : super(baseUrl: 'http://fake.local', tokenProvider: () => 'token');

  final calls = <String>[];
  final postBodies = <Map<String, dynamic>>[];
  final patchBodies = <Map<String, dynamic>>[];
  final responses = <String, Map<String, dynamic>>{};

  @override
  Future<Map<String, dynamic>> get(String path) async {
    calls.add('GET $path');
    return responses[path] ?? {'code': 0, 'data': {}};
  }

  @override
  Future<Map<String, dynamic>> post(
    String path,
    Map<String, dynamic> body,
  ) async {
    calls.add('POST $path');
    postBodies.add(body);
    return responses[path] ?? {'code': 0, 'data': {}};
  }

  @override
  Future<Map<String, dynamic>> patch(
    String path,
    Map<String, dynamic> body,
  ) async {
    calls.add('PATCH $path');
    patchBodies.add(body);
    return {'code': 0, 'data': null};
  }
}

void main() {
  tearDown(appState.logout);

  group('MessageItem', () {
    test('解析分组、跳转目标和时间', () {
      final item = MessageItem.fromApi({
        'id': 21,
        'type': 'complaint',
        'title': '投诉进度更新',
        'content': '商家已回复',
        'badgeText': '待查看',
        'unread': true,
        'relatedTargetType': 'complaint',
        'relatedTargetId': 9001,
        'createdAt': '2026-06-08T10:05:00',
      });

      expect(item.group, 'complaint');
      expect(item.relatedTargetType, 'complaint');
      expect(item.relatedTargetId, 9001);
      expect(item.time, '10:05');
      expect(item.unread, isTrue);
    });

    test('未知类型归入系统分组并兼容 relatedOrderId', () {
      final item = MessageItem.fromApi({
        'id': 22,
        'type': 'promotion',
        'relatedOrderId': 7002,
      });

      expect(item.group, 'system');
      expect(item.relatedOrderId, 7002);
    });
  });

  group('BackendAppRepository messages', () {
    test('fetchMessages 传递类型和分页参数', () async {
      final client = FakeMessageApiClient();
      client.responses['/api/app/message/station?page=2&pageSize=30&type=order'] =
          {
            'code': 0,
            'data': {
              'list': [
                {
                  'id': 31,
                  'type': 'order',
                  'title': '订单消息',
                  'createdAt': '2026-06-08T11:00:00',
                },
              ],
              'hasNext': false,
            },
          };

      final messages = await BackendAppRepository(
        client: client,
      ).fetchMessages(type: 'order', page: 2, pageSize: 30);

      expect(client.calls, [
        'GET /api/app/message/station?page=2&pageSize=30&type=order',
      ]);
      expect(messages.single.id, 31);
      expect(messages.single.group, 'order');
    });

    test('fetchAllMessages 按 hasNext 拉取所有分页', () async {
      final client = FakeMessageApiClient();
      client.responses['/api/app/message/station?page=1&pageSize=50'] = {
        'code': 0,
        'data': {
          'list': [
            {'id': 41, 'type': 'review'},
          ],
          'hasNext': true,
        },
      };
      client.responses['/api/app/message/station?page=2&pageSize=50'] = {
        'code': 0,
        'data': {
          'list': [
            {'id': 42, 'type': 'support'},
          ],
          'hasNext': false,
        },
      };

      final messages = await BackendAppRepository(
        client: client,
      ).fetchAllMessages();

      expect(client.calls, [
        'GET /api/app/message/station?page=1&pageSize=50',
        'GET /api/app/message/station?page=2&pageSize=50',
      ]);
      expect(messages.map((message) => message.id), [41, 42]);
    });

    test('login 使用账号资料接口补齐未读消息数', () async {
      final client = FakeMessageApiClient();
      client.responses['/api/open/auth/user/login/password'] = {
        'code': 0,
        'data': {
          'token': 'token-1',
          'profile': {'nickname': '演示用户', 'memberLevelName': '普通会员'},
        },
      };
      client.responses['/api/app/account/profile'] = {
        'code': 0,
        'data': {
          'nickname': '演示用户',
          'memberLevelName': '黄金会员',
          'unreadMessageCount': 3,
        },
      };

      final session = await BackendAppRepository(
        client: client,
      ).login('demo_user', '123456');

      expect(client.calls, [
        'POST /api/open/auth/user/login/password',
        'GET /api/app/account/profile',
      ]);
      expect(client.postBodies.single, {
        'account': 'demo_user',
        'password': '123456',
      });
      expect(session.unreadMessageCount, 3);
      expect(session.memberLevelName, '黄金会员');
    });

    test('已读接口路径正确且使用空 body', () async {
      final client = FakeMessageApiClient();
      final repository = BackendAppRepository(client: client);

      await repository.markMessageRead(52);
      await repository.markAllMessagesRead();

      expect(client.calls, [
        'PATCH /api/app/message/station/52/read',
        'PATCH /api/app/message/station/read-all',
      ]);
      expect(client.patchBodies, hasLength(2));
      expect(client.patchBodies, everyElement(isEmpty));
    });
  });
}
