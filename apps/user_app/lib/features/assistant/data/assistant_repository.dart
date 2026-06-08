import '../../../app/app_state.dart';
import '../../../core/network/app_api_client.dart';

class AssistantResponse {
  const AssistantResponse({
    required this.conversationId,
    required this.reply,
    required this.cards,
    required this.quickActions,
    required this.usedSkills,
    required this.modelUsed,
  });

  final String conversationId;
  final String reply;
  final List<AssistantCard> cards;
  final List<AssistantAction> quickActions;
  final List<String> usedSkills;
  final bool modelUsed;

  factory AssistantResponse.fromApi(Map<String, dynamic> json) =>
      AssistantResponse(
        conversationId: (json['conversationId'] ?? '') as String,
        reply: (json['reply'] ?? '') as String,
        cards: ((json['cards'] as List?) ?? const [])
            .map((e) => AssistantCard.fromApi(e as Map<String, dynamic>))
            .toList(),
        quickActions: ((json['quickActions'] as List?) ?? const [])
            .map((e) => AssistantAction.fromApi(e as Map<String, dynamic>))
            .toList(),
        usedSkills: ((json['usedSkills'] as List?) ?? const [])
            .map((e) => e.toString())
            .toList(),
        modelUsed: json['modelUsed'] == true,
      );
}

class AssistantCard {
  const AssistantCard({
    required this.type,
    required this.title,
    required this.content,
    required this.actionLabel,
    required this.route,
  });

  final String type;
  final String title;
  final String content;
  final String? actionLabel;
  final String? route;

  factory AssistantCard.fromApi(Map<String, dynamic> json) => AssistantCard(
    type: (json['type'] ?? '') as String,
    title: (json['title'] ?? '') as String,
    content: (json['content'] ?? '') as String,
    actionLabel: json['actionLabel'] as String?,
    route: json['route'] as String?,
  );
}

class AssistantAction {
  const AssistantAction({
    required this.label,
    required this.message,
    required this.route,
  });

  final String label;
  final String? message;
  final String? route;

  factory AssistantAction.fromApi(Map<String, dynamic> json) => AssistantAction(
    label: (json['label'] ?? '') as String,
    message: json['message'] as String?,
    route: json['route'] as String?,
  );
}

final assistantRepository = AssistantRepository();

class AssistantRepository {
  AssistantRepository({AppApiClient? client})
    : _client = client ?? AppApiClient(tokenProvider: () => appState.token);

  final AppApiClient _client;

  Future<AssistantResponse> sendMessage({
    required String content,
    String? conversationId,
  }) async {
    final body = <String, dynamic>{'content': content};
    if (conversationId != null && conversationId.isNotEmpty) {
      body['conversationId'] = conversationId;
    }
    final json = await _client.post('/api/app/ai/assistant/message', body);
    return AssistantResponse.fromApi(json['data'] as Map<String, dynamic>);
  }
}
