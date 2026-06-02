import '../../../app/app_state.dart';
import '../../../core/network/app_api_client.dart';

class ComplaintSummary {
  ComplaintSummary({
    required this.id,
    required this.ticketNo,
    required this.title,
    required this.status,
    required this.category,
    required this.orderId,
    required this.orderNo,
    required this.createdAt,
  });

  final int id;
  final String ticketNo;
  final String title;
  final String status;
  final String category;
  final int? orderId;
  final String? orderNo;
  final String createdAt;

  factory ComplaintSummary.fromApi(Map<String, dynamic> json) => ComplaintSummary(
        id: (json['id'] as num).toInt(),
        ticketNo: (json['ticketNo'] ?? '') as String,
        title: (json['title'] ?? '') as String,
        status: (json['status'] ?? 'pending') as String,
        category: (json['category'] ?? '') as String,
        orderId: (json['orderId'] as num?)?.toInt(),
        orderNo: json['orderNo'] as String?,
        createdAt: (json['createdAt'] ?? '') as String,
      );
}

final complaintRepository = ComplaintRepository();

class ComplaintRepository {
  ComplaintRepository({AppApiClient? client})
    : _client = client ?? AppApiClient(tokenProvider: () => appState.token);

  final AppApiClient _client;

  Future<List<ComplaintSummary>> fetchMy({String? status}) async {
    final query = StringBuffer('?page=1&pageSize=50');
    if (status != null) query.write('&status=$status');
    final json = await _client.get('/api/app/complaints$query');
    final list = ((json['data'] as Map<String, dynamic>?)?['list'] as List?) ?? const [];
    return list.map((e) => ComplaintSummary.fromApi(e as Map<String, dynamic>)).toList();
  }

  Future<ComplaintSummary> submit({
    int? orderId,
    required String category,
    required String title,
    required String detail,
    List<String> evidenceUrls = const [],
  }) async {
    final body = <String, dynamic>{
      'category': category,
      'title': title,
      'detail': detail,
      'evidenceUrls': evidenceUrls,
    };
    if (orderId != null) body['orderId'] = orderId;
    final json = await _client.post('/api/app/complaints', body);
    return ComplaintSummary.fromApi(json['data'] as Map<String, dynamic>);
  }
}
