import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/network/app_api_client.dart';
import 'package:aituan_user_app/features/complaint/data/complaint_repository.dart';

class FakeComplaintApiClient extends AppApiClient {
  FakeComplaintApiClient()
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

Map<String, dynamic> complaintJson({int id = 1}) => {
  'id': id,
  'ticketNo': 'TS$id',
  'title': '配送超时',
  'status': 'pending',
  'category': 'delivery',
  'orderId': 9001,
  'orderNo': 'AT9001',
  'createdAt': '2026-06-08T10:00:00',
  'detail': '骑手长时间未送达',
  'storeName': '塔斯汀中国汉堡',
  'evidenceUrls': ['/uploads/a.png', 2],
};

void main() {
  group('ComplaintSummary model', () {
    test('解析投诉概要并兼容证据 URL 弱类型', () {
      final summary = ComplaintSummary.fromApi(complaintJson());

      expect(summary.ticketNo, 'TS1');
      expect(summary.status, 'pending');
      expect(summary.orderId, 9001);
      expect(summary.storeName, '塔斯汀中国汉堡');
      expect(summary.evidenceUrls, ['/uploads/a.png', '2']);
    });

    test('缺省状态和可空字段有稳定 fallback', () {
      final summary = ComplaintSummary.fromApi({'id': 2});

      expect(summary.status, 'pending');
      expect(summary.title, '');
      expect(summary.orderId, isNull);
      expect(summary.evidenceUrls, isEmpty);
    });
  });

  group('ComplaintRepository', () {
    test('fetchMy 拼接状态过滤并解析列表', () async {
      final client = FakeComplaintApiClient();
      client.responses['/api/app/complaints?page=1&pageSize=50&status=pending'] =
          {
            'code': 0,
            'data': {
              'list': [complaintJson(id: 7)],
            },
          };

      final complaints = await ComplaintRepository(
        client: client,
      ).fetchMy(status: 'pending');

      expect(client.calls, [
        'GET /api/app/complaints?page=1&pageSize=50&status=pending',
      ]);
      expect(complaints.single.id, 7);
      expect(complaints.single.evidenceUrls, ['/uploads/a.png', '2']);
    });

    test('fetchDetail 解析投诉详情和处理日志', () async {
      final client = FakeComplaintApiClient();
      client.responses['/api/app/complaints/7'] = {
        'code': 0,
        'data': {
          'complaint': complaintJson(id: 7),
          'logs': [
            {
              'action': 'accept',
              'operatorType': 'admin',
              'remark': '已受理',
              'createdAt': '2026-06-08T10:10:00',
            },
          ],
        },
      };

      final detail = await ComplaintRepository(client: client).fetchDetail(7);

      expect(client.calls, ['GET /api/app/complaints/7']);
      expect(detail.complaint.id, 7);
      expect(detail.logs.single.action, 'accept');
      expect(detail.logs.single.remark, '已受理');
    });

    test('submit 只在有订单时携带 orderId', () async {
      final client = FakeComplaintApiClient();
      client.responses['/api/app/complaints'] = {
        'code': 0,
        'data': complaintJson(id: 8),
      };

      final summary = await ComplaintRepository(client: client).submit(
        orderId: 9001,
        category: 'delivery',
        title: '配送超时',
        detail: '骑手长时间未送达',
        evidenceUrls: ['/uploads/a.png'],
      );

      expect(client.calls, ['POST /api/app/complaints']);
      expect(client.postBodies.single, {
        'category': 'delivery',
        'title': '配送超时',
        'detail': '骑手长时间未送达',
        'evidenceUrls': ['/uploads/a.png'],
        'orderId': 9001,
      });
      expect(summary.id, 8);
    });

    test('supplement 使用补充说明路径和 content body', () async {
      final client = FakeComplaintApiClient();
      client.responses['/api/app/complaints/8/supplements'] = {
        'code': 0,
        'data': {'complaint': complaintJson(id: 8), 'logs': const []},
      };

      final detail = await ComplaintRepository(
        client: client,
      ).supplement(8, '补充一张截图');

      expect(client.calls, ['POST /api/app/complaints/8/supplements']);
      expect(client.postBodies.single, {'content': '补充一张截图'});
      expect(detail.complaint.id, 8);
    });
  });
}
