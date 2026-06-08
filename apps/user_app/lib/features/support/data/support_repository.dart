import '../../../app/app_state.dart';
import '../../../core/network/app_api_client.dart';

class SupportSession {
  SupportSession({
    required this.id,
    required this.sessionNo,
    required this.storeId,
    required this.storeName,
    required this.topic,
    required this.status,
    required this.relatedOrderId,
    required this.relatedOrderNo,
    required this.lastMessage,
    required this.lastMessageAt,
    required this.unreadCount,
    required this.createdAt,
    required this.closeReason,
    required this.serviceScope,
    required this.assistantMode,
    required this.platformInterventionStatus,
  });

  final int id;
  final String sessionNo;
  final int storeId;
  final String storeName;
  final String topic;
  final String status;
  final int? relatedOrderId;
  final String? relatedOrderNo;
  final String? lastMessage;
  final String? lastMessageAt;
  final int unreadCount;
  final String createdAt;
  final String? closeReason;
  final String serviceScope;
  final String assistantMode;
  final String platformInterventionStatus;

  bool get isPlatform => serviceScope == 'platform' || storeId == 0;
  bool get isAiMode => assistantMode == 'ai';

  factory SupportSession.fromApi(Map<String, dynamic> json) => SupportSession(
    id: (json['id'] as num).toInt(),
    sessionNo: (json['sessionNo'] ?? '') as String,
    storeId: (json['storeId'] as num?)?.toInt() ?? 0,
    storeName: (json['storeName'] ?? '') as String,
    topic: (json['topic'] ?? '') as String,
    status: (json['status'] ?? 'open') as String,
    relatedOrderId: (json['relatedOrderId'] as num?)?.toInt(),
    relatedOrderNo: json['relatedOrderNo'] as String?,
    lastMessage: json['lastMessage'] as String?,
    lastMessageAt: json['lastMessageAt'] as String?,
    unreadCount: (json['unreadCount'] as num?)?.toInt() ?? 0,
    createdAt: (json['createdAt'] ?? '') as String,
    closeReason: json['closeReason'] as String?,
    serviceScope: (json['serviceScope'] ?? 'merchant') as String,
    assistantMode: (json['assistantMode'] ?? 'human') as String,
    platformInterventionStatus:
        (json['platformInterventionStatus'] ?? 'none') as String,
  );
}

class SupportMessage {
  SupportMessage({
    required this.id,
    required this.senderType,
    required this.content,
    required this.createdAt,
  });

  final int id;
  final String senderType;
  final String content;
  final String createdAt;

  bool get isUser => senderType == 'user';
  bool get isMerchant => senderType == 'merchant';
  bool get isPlatform => senderType == 'platform';

  factory SupportMessage.fromApi(Map<String, dynamic> json) => SupportMessage(
    id: (json['id'] as num).toInt(),
    senderType: (json['senderType'] ?? 'user') as String,
    content: (json['content'] ?? '') as String,
    createdAt: (json['createdAt'] ?? '') as String,
  );
}

final supportRepository = SupportRepository();

class SupportRepository {
  SupportRepository({AppApiClient? client})
    : _client = client ?? AppApiClient(tokenProvider: () => appState.token);

  final AppApiClient _client;

  Future<List<SupportSession>> fetchSessions({String? status}) async {
    final query = StringBuffer('?page=1&pageSize=50');
    if (status != null) query.write('&status=$status');
    final json = await _client.get('/api/app/support/sessions$query');
    final list =
        ((json['data'] as Map<String, dynamic>?)?['list'] as List?) ?? const [];
    return list
        .map((e) => SupportSession.fromApi(e as Map<String, dynamic>))
        .toList();
  }

  Future<SupportSession> createSession({
    int? storeId,
    String? topic,
    int? relatedOrderId,
  }) async {
    final body = <String, dynamic>{};
    if (storeId != null) body['storeId'] = storeId;
    if (topic != null && topic.trim().isNotEmpty) body['topic'] = topic;
    if (relatedOrderId != null) body['relatedOrderId'] = relatedOrderId;
    final json = await _client.post('/api/app/support/sessions', body);
    return SupportSession.fromApi(json['data'] as Map<String, dynamic>);
  }

  Future<(SupportSession session, List<SupportMessage> messages)> fetchDetail(
    int sessionId,
  ) async {
    final json = await _client.get('/api/app/support/sessions/$sessionId');
    final data = json['data'] as Map<String, dynamic>;
    final session = SupportSession.fromApi(
      data['session'] as Map<String, dynamic>,
    );
    final msgs = (data['messages'] as List?) ?? const [];
    return (
      session,
      msgs
          .map((e) => SupportMessage.fromApi(e as Map<String, dynamic>))
          .toList(),
    );
  }

  Future<SupportMessage> sendMessage(int sessionId, String content) async {
    final json = await _client.post(
      '/api/app/support/sessions/$sessionId/messages',
      {'content': content},
    );
    return SupportMessage.fromApi(json['data'] as Map<String, dynamic>);
  }

  Future<SupportSession> handoffToHuman(int sessionId) async {
    final json = await _client.post(
      '/api/app/support/sessions/$sessionId/handoff',
      {},
    );
    return SupportSession.fromApi(json['data'] as Map<String, dynamic>);
  }

  Future<void> closeSession(int sessionId, {String? reason}) async {
    final body = <String, dynamic>{};
    if (reason != null) body['reason'] = reason;
    await _client.post('/api/app/support/sessions/$sessionId/close', body);
  }
}
