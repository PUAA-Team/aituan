import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/network/app_api_client.dart';
import 'package:aituan_user_app/features/review/data/review_repository.dart';

class FakeReviewApiClient extends AppApiClient {
  FakeReviewApiClient()
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

Map<String, dynamic> reviewJson({int id = 1}) => {
  'id': id,
  'orderId': 7001,
  'orderTitle': '双人汉堡套餐',
  'storeName': '塔斯汀中国汉堡',
  'userMaskedNickname': '爱团用户',
  'rating': 5,
  'content': '味道不错，配送很快',
  'labels': ['好吃', null, ''],
  'imageUrls': ['/uploads/review-a.png', 3],
  'helpfulCount': 2,
  'reportedCount': 1,
  'helpfulByMe': true,
  'status': 'published',
  'replied': true,
  'replyContent': '感谢评价',
  'repliedAt': '2026-06-08T11:00:00',
  'createdAt': '2026-06-08T10:00:00',
};

void main() {
  group('ReviewSummary model', () {
    test('解析评分、标签、图片和商家回复', () {
      final review = ReviewSummary.fromApi(reviewJson());

      expect(review.orderId, 7001);
      expect(review.rating, 5);
      expect(review.labels, ['好吃']);
      expect(review.imageUrls, ['/uploads/review-a.png', '3']);
      expect(review.helpfulByMe, isTrue);
      expect(review.replied, isTrue);
      expect(review.replyContent, '感谢评价');
    });

    test('缺省数值和状态 fallback 稳定', () {
      final review = ReviewSummary.fromApi({'id': 2});

      expect(review.orderId, 0);
      expect(review.rating, 0);
      expect(review.status, 'published');
      expect(review.labels, isEmpty);
      expect(review.imageUrls, isEmpty);
      expect(review.helpfulByMe, isFalse);
      expect(review.replied, isFalse);
    });
  });

  group('ReviewRepository', () {
    test('fetchMyReviews 拼接分页和状态过滤', () async {
      final client = FakeReviewApiClient();
      client.responses['/api/app/interaction/reviews/me?page=2&pageSize=5&status=published'] =
          {
            'code': 0,
            'data': {
              'list': [reviewJson(id: 11)],
            },
          };

      final reviews = await ReviewRepository(
        client: client,
      ).fetchMyReviews(status: 'published', page: 2, pageSize: 5);

      expect(client.calls, [
        'GET /api/app/interaction/reviews/me?page=2&pageSize=5&status=published',
      ]);
      expect(reviews.single.id, 11);
    });

    test('fetchStoreReviews 和 fetchDetail 使用正确路径', () async {
      final client = FakeReviewApiClient();
      client.responses['/api/app/interaction/stores/3/reviews?page=1&pageSize=10'] =
          {
            'code': 0,
            'data': {
              'list': [reviewJson(id: 12)],
            },
          };
      client.responses['/api/app/interaction/reviews/12'] = {
        'code': 0,
        'data': reviewJson(id: 12),
      };
      final repository = ReviewRepository(client: client);

      final storeReviews = await repository.fetchStoreReviews(3);
      final detail = await repository.fetchDetail(12);

      expect(client.calls, [
        'GET /api/app/interaction/stores/3/reviews?page=1&pageSize=10',
        'GET /api/app/interaction/reviews/12',
      ]);
      expect(storeReviews.single.id, 12);
      expect(detail.id, 12);
    });

    test('fetchByOrder 在未评价时返回 null', () async {
      final client = FakeReviewApiClient();
      client.responses['/api/app/interaction/orders/7001/review'] = {
        'code': 0,
        'data': null,
      };

      final review = await ReviewRepository(
        client: client,
      ).fetchByOrder('7001');

      expect(client.calls, ['GET /api/app/interaction/orders/7001/review']);
      expect(review, isNull);
    });

    test('submit 发送评价内容、标签和图片', () async {
      final client = FakeReviewApiClient();
      client.responses['/api/app/interaction/orders/7001/review'] = {
        'code': 0,
        'data': reviewJson(id: 13),
      };

      final review = await ReviewRepository(client: client).submit(
        orderId: '7001',
        rating: 4,
        content: '整体不错',
        labels: ['干净'],
        imageUrls: ['/uploads/a.png'],
      );

      expect(client.calls, ['POST /api/app/interaction/orders/7001/review']);
      expect(client.postBodies.single, {
        'rating': 4,
        'content': '整体不错',
        'labels': ['干净'],
        'imageUrls': ['/uploads/a.png'],
      });
      expect(review.id, 13);
    });

    test('toggleHelpful 和 report 使用稳定 body', () async {
      final client = FakeReviewApiClient();
      client.responses['/api/app/interaction/reviews/13/helpful'] = {
        'code': 0,
        'data': {'helpful': true, 'helpfulCount': 6},
      };
      final repository = ReviewRepository(client: client);

      final result = await repository.toggleHelpful(13);
      await repository.report(
        13,
        'fake',
        detail: '疑似虚假评价',
        evidenceUrls: ['/uploads/evidence.png'],
      );
      await repository.report(14, 'spam', detail: '');

      expect(result.$1, isTrue);
      expect(result.$2, 6);
      expect(client.calls, [
        'POST /api/app/interaction/reviews/13/helpful',
        'POST /api/app/interaction/reviews/13/report',
        'POST /api/app/interaction/reviews/14/report',
      ]);
      expect(client.postBodies[0], isEmpty);
      expect(client.postBodies[1], {
        'reason': 'fake',
        'detail': '疑似虚假评价',
        'evidenceUrls': ['/uploads/evidence.png'],
      });
      expect(client.postBodies[2], {'reason': 'spam'});
    });
  });
}
