import '../../../app/app_state.dart';
import '../../../core/network/app_api_client.dart';

class AssistantResponse {
  const AssistantResponse({
    required this.conversationId,
    required this.reply,
    required this.cards,
    required this.quickActions,
    required this.steps,
    required this.usedSkills,
    required this.modelUsed,
  });

  final String conversationId;
  final String reply;
  final List<AssistantCard> cards;
  final List<AssistantAction> quickActions;
  final List<AssistantStep> steps;
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
        steps: ((json['steps'] as List?) ?? const [])
            .map((e) => AssistantStep.fromApi(e as Map<String, dynamic>))
            .toList(),
        usedSkills: ((json['usedSkills'] as List?) ?? const [])
            .map((e) => e.toString())
            .toList(),
        modelUsed: json['modelUsed'] == true,
      );
}

class AssistantHistory {
  const AssistantHistory({
    required this.conversationId,
    required this.messages,
  });

  final String? conversationId;
  final List<AssistantMessage> messages;

  factory AssistantHistory.fromApi(Map<String, dynamic> json) =>
      AssistantHistory(
        conversationId: json['conversationId'] as String?,
        messages: ((json['messages'] as List?) ?? const [])
            .map((e) => AssistantMessage.fromApi(e as Map<String, dynamic>))
            .toList(),
      );
}

class AssistantMessage {
  const AssistantMessage({
    required this.role,
    required this.content,
    required this.cards,
    required this.quickActions,
    required this.steps,
    required this.usedSkills,
    required this.modelUsed,
  });

  final String role;
  final String content;
  final List<AssistantCard> cards;
  final List<AssistantAction> quickActions;
  final List<AssistantStep> steps;
  final List<String> usedSkills;
  final bool modelUsed;

  factory AssistantMessage.fromApi(Map<String, dynamic> json) =>
      AssistantMessage(
        role: (json['role'] ?? '') as String,
        content: (json['content'] ?? '') as String,
        cards: ((json['cards'] as List?) ?? const [])
            .map((e) => AssistantCard.fromApi(e as Map<String, dynamic>))
            .toList(),
        quickActions: ((json['quickActions'] as List?) ?? const [])
            .map((e) => AssistantAction.fromApi(e as Map<String, dynamic>))
            .toList(),
        steps: ((json['steps'] as List?) ?? const [])
            .map((e) => AssistantStep.fromApi(e as Map<String, dynamic>))
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

class AssistantStep {
  const AssistantStep({
    required this.title,
    required this.detail,
    required this.status,
  });

  final String title;
  final String? detail;
  final String? status;

  factory AssistantStep.fromApi(Map<String, dynamic> json) => AssistantStep(
    title: (json['title'] ?? '') as String,
    detail: json['detail'] as String?,
    status: json['status'] as String?,
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

  Future<AssistantHistory> fetchCurrentConversation() async {
    final json = await _client.get(
      '/api/app/ai/assistant/conversations/current',
    );
    return AssistantHistory.fromApi(json['data'] as Map<String, dynamic>);
  }
}
