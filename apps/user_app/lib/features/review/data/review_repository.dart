import '../../../app/app_state.dart';
import '../../../core/network/app_api_client.dart';

class ReviewSummary {
  ReviewSummary({
    required this.id,
    required this.orderId,
    required this.orderTitle,
    required this.storeName,
    required this.userMaskedNickname,
    required this.rating,
    required this.content,
    required this.labels,
    required this.imageUrls,
    required this.helpfulCount,
    required this.reportedCount,
    required this.helpfulByMe,
    required this.status,
    required this.replied,
    required this.replyContent,
    required this.repliedAt,
    required this.createdAt,
  });

  final int id;
  final int orderId;
  final String orderTitle;
  final String storeName;
  final String? userMaskedNickname;
  final int rating;
  final String content;
  final List<String> labels;
  final List<String> imageUrls;
  final int helpfulCount;
  final int reportedCount;
  final bool helpfulByMe;
  final String status;
  final bool replied;
  final String? replyContent;
  final String? repliedAt;
  final String createdAt;

  factory ReviewSummary.fromApi(Map<String, dynamic> json) {
    List<String> stringList(dynamic raw) => raw is List
        ? raw
              .map((e) => e?.toString() ?? '')
              .where((e) => e.isNotEmpty)
              .toList()
        : <String>[];
    return ReviewSummary(
      id: (json['id'] as num).toInt(),
      orderId: (json['orderId'] as num?)?.toInt() ?? 0,
      orderTitle: (json['orderTitle'] ?? '') as String,
      storeName: (json['storeName'] ?? '') as String,
      userMaskedNickname: json['userMaskedNickname'] as String?,
      rating: (json['rating'] as num?)?.toInt() ?? 0,
      content: (json['content'] ?? '') as String,
      labels: stringList(json['labels']),
      imageUrls: stringList(json['imageUrls']),
      helpfulCount: (json['helpfulCount'] as num?)?.toInt() ?? 0,
      reportedCount: (json['reportedCount'] as num?)?.toInt() ?? 0,
      helpfulByMe: (json['helpfulByMe'] as bool?) ?? false,
      status: (json['status'] ?? 'published') as String,
      replied: (json['replied'] as bool?) ?? false,
      replyContent: json['replyContent'] as String?,
      repliedAt: json['repliedAt'] as String?,
      createdAt: (json['createdAt'] ?? '') as String,
    );
  }
}

final reviewRepository = ReviewRepository();

class ReviewRepository {
  ReviewRepository({AppApiClient? client})
    : _client = client ?? AppApiClient(tokenProvider: () => appState.token);

  final AppApiClient _client;

  Future<List<ReviewSummary>> fetchMyReviews({
    String? status,
    int page = 1,
    int pageSize = 20,
  }) async {
    final query = StringBuffer('?page=$page&pageSize=$pageSize');
    if (status != null && status.isNotEmpty) query.write('&status=$status');
    final json = await _client.get('/api/app/interaction/reviews/me$query');
    final data = json['data'] as Map<String, dynamic>? ?? const {};
    final list = (data['list'] as List?) ?? const [];
    return list
        .map((e) => ReviewSummary.fromApi(e as Map<String, dynamic>))
        .toList();
  }

  Future<ReviewSummary?> fetchByOrder(String orderId) async {
    final json = await _client.get(
      '/api/app/interaction/orders/$orderId/review',
    );
    final data = json['data'];
    if (data == null) return null;
    return ReviewSummary.fromApi(data as Map<String, dynamic>);
  }

  Future<List<ReviewSummary>> fetchStoreReviews(
    int storeId, {
    int page = 1,
    int pageSize = 10,
  }) async {
    final json = await _client.get(
      '/api/app/interaction/stores/$storeId/reviews?page=$page&pageSize=$pageSize',
    );
    final data = json['data'] as Map<String, dynamic>? ?? const {};
    final list = (data['list'] as List?) ?? const [];
    return list
        .map((e) => ReviewSummary.fromApi(e as Map<String, dynamic>))
        .toList();
  }

  Future<ReviewSummary> fetchDetail(int reviewId) async {
    final json = await _client.get('/api/app/interaction/reviews/$reviewId');
    return ReviewSummary.fromApi(json['data'] as Map<String, dynamic>);
  }

  Future<ReviewSummary> submit({
    required String orderId,
    required int rating,
    required String content,
    required List<String> labels,
    List<String> imageUrls = const [],
  }) async {
    final json = await _client.post(
      '/api/app/interaction/orders/$orderId/review',
      {
        'rating': rating,
        'content': content,
        'labels': labels,
        'imageUrls': imageUrls,
      },
    );
    return ReviewSummary.fromApi(json['data'] as Map<String, dynamic>);
  }

  Future<(bool helpful, int count)> toggleHelpful(int reviewId) async {
    final json = await _client.post(
      '/api/app/interaction/reviews/$reviewId/helpful',
      const {},
    );
    final data = json['data'] as Map<String, dynamic>;
    return (
      (data['helpful'] as bool?) ?? false,
      (data['helpfulCount'] as num?)?.toInt() ?? 0,
    );
  }

  Future<void> report(
    int reviewId,
    String reason, {
    String? detail,
    List<String> evidenceUrls = const [],
  }) async {
    await _client.post('/api/app/interaction/reviews/$reviewId/report', {
      'reason': reason,
      if (detail != null && detail.isNotEmpty) 'detail': detail,
      if (evidenceUrls.isNotEmpty) 'evidenceUrls': evidenceUrls,
    });
  }
}
