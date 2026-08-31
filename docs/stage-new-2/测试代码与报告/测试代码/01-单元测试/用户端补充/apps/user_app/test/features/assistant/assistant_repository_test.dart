import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/network/app_api_client.dart';
import 'package:aituan_user_app/features/assistant/data/assistant_repository.dart';

class FakeAssistantApiClient extends AppApiClient {
  FakeAssistantApiClient()
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
  Future<Map<String, dynamic>> post(
    String path,
    Map<String, dynamic> body,
  ) async {
    calls.add('POST $path');
    postBodies.add(body);
    return responses[path] ?? {'code': 0, 'data': {}};
  }
}

Map<String, dynamic> assistantResponseJson() => {
  'conversationId': 'cv-1',
  'reply': '可以为你推荐附近餐厅',
  'cards': [
    {
      'type': 'store',
      'title': '塔斯汀中国汉堡',
      'content': '距离你 1.2km',
      'actionLabel': '去看看',
      'route': '/store/1',
      'payload': {'storeId': 1},
    },
  ],
  'quickActions': [
    {
      'label': '查订单',
      'message': '帮我查订单',
      'route': '/orders',
      'payload': {'tab': 'all'},
    },
  ],
  'steps': [
    {'title': '理解需求', 'detail': '识别为推荐餐厅', 'status': 'done'},
  ],
  'usedSkills': ['recommendation', 2],
  'modelUsed': true,
};

void main() {
  group('AssistantResponse model', () {
    test('解析回复、卡片、快捷操作、步骤和技能列表', () {
      final response = AssistantResponse.fromApi(assistantResponseJson());

      expect(response.conversationId, 'cv-1');
      expect(response.reply, '可以为你推荐附近餐厅');
      expect(response.modelUsed, isTrue);
      expect(response.cards.single.payload['storeId'], 1);
      expect(response.quickActions.single.route, '/orders');
      expect(response.steps.single.status, 'done');
      expect(response.usedSkills, ['recommendation', '2']);
    });

    test('缺省列表和 payload fallback 稳定', () {
      final response = AssistantResponse.fromApi({'conversationId': 'cv-2'});

      expect(response.reply, '');
      expect(response.cards, isEmpty);
      expect(response.quickActions, isEmpty);
      expect(response.steps, isEmpty);
      expect(response.usedSkills, isEmpty);
      expect(response.modelUsed, isFalse);
    });
  });

  group('AssistantRepository', () {
    test('sendMessage 首次会话不携带 conversationId', () async {
      final client = FakeAssistantApiClient();
      client.responses['/api/app/ai/assistant/message'] = {
        'code': 0,
        'data': assistantResponseJson(),
      };

      final response = await AssistantRepository(
        client: client,
      ).sendMessage(content: '推荐附近餐厅');

      expect(client.calls, ['POST /api/app/ai/assistant/message']);
      expect(client.postBodies.single, {'content': '推荐附近餐厅'});
      expect(response.conversationId, 'cv-1');
    });

    test('sendMessage 继续会话时携带 conversationId', () async {
      final client = FakeAssistantApiClient();
      client.responses['/api/app/ai/assistant/message'] = {
        'code': 0,
        'data': assistantResponseJson(),
      };

      await AssistantRepository(
        client: client,
      ).sendMessage(content: '继续', conversationId: 'cv-1');

      expect(client.postBodies.single, {
        'content': '继续',
        'conversationId': 'cv-1',
      });
    });

    test('fetchCurrentConversation 解析历史消息', () async {
      final client = FakeAssistantApiClient();
      client.responses['/api/app/ai/assistant/conversations/current'] = {
        'code': 0,
        'data': {
          'conversationId': 'cv-1',
          'messages': [
            {'role': 'assistant', 'content': '您好', 'modelUsed': false},
          ],
        },
      };

      final history = await AssistantRepository(
        client: client,
      ).fetchCurrentConversation();

      expect(client.calls, ['GET /api/app/ai/assistant/conversations/current']);
      expect(history.conversationId, 'cv-1');
      expect(history.messages.single.role, 'assistant');
      expect(history.messages.single.content, '您好');
    });
  });
}
