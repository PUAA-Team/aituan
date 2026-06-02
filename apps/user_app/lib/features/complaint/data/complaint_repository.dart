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
    required this.detail,
    required this.storeName,
    required this.evidenceUrls,
  });

  final int id;
  final String ticketNo;
  final String title;
  final String status;
  final String category;
  final int? orderId;
  final String? orderNo;
  final String createdAt;
  final String detail;
  final String? storeName;
  final List<String> evidenceUrls;

  factory ComplaintSummary.fromApi(Map<String, dynamic> json) =>
      ComplaintSummary(
        id: (json['id'] as num).toInt(),
        ticketNo: (json['ticketNo'] ?? '') as String,
        title: (json['title'] ?? '') as String,
        status: (json['status'] ?? 'pending') as String,
        category: (json['category'] ?? '') as String,
        orderId: (json['orderId'] as num?)?.toInt(),
        orderNo: json['orderNo'] as String?,
        createdAt: (json['createdAt'] ?? '') as String,
        detail: (json['detail'] ?? '') as String,
        storeName: json['storeName'] as String?,
        evidenceUrls: ((json['evidenceUrls'] as List?) ?? const [])
            .map((e) => e.toString())
            .toList(),
      );
}

class ComplaintLog {
  ComplaintLog({
    required this.action,
    required this.operatorType,
    required this.remark,
    required this.createdAt,
  });

  final String action;
  final String operatorType;
  final String? remark;
  final String createdAt;

  factory ComplaintLog.fromApi(Map<String, dynamic> json) => ComplaintLog(
    action: (json['action'] ?? '') as String,
    operatorType: (json['operatorType'] ?? '') as String,
    remark: json['remark'] as String?,
    createdAt: (json['createdAt'] ?? '') as String,
  );
}

class ComplaintDetail {
  ComplaintDetail({required this.complaint, required this.logs});

  final ComplaintSummary complaint;
  final List<ComplaintLog> logs;

  factory ComplaintDetail.fromApi(Map<String, dynamic> json) => ComplaintDetail(
    complaint: ComplaintSummary.fromApi(
      json['complaint'] as Map<String, dynamic>,
    ),
    logs: ((json['logs'] as List?) ?? const [])
        .map((e) => ComplaintLog.fromApi(e as Map<String, dynamic>))
        .toList(),
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
    final list =
        ((json['data'] as Map<String, dynamic>?)?['list'] as List?) ?? const [];
    return list
        .map((e) => ComplaintSummary.fromApi(e as Map<String, dynamic>))
        .toList();
  }

  Future<ComplaintDetail> fetchDetail(int id) async {
    final json = await _client.get('/api/app/complaints/$id');
    return ComplaintDetail.fromApi(json['data'] as Map<String, dynamic>);
  }

  Future<ComplaintDetail> supplement(int id, String content) async {
    final json = await _client.post('/api/app/complaints/$id/supplements', {
      'content': content,
    });
    return ComplaintDetail.fromApi(json['data'] as Map<String, dynamic>);
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
