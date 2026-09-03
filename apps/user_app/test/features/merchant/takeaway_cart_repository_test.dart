import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/core/network/app_api_client.dart';
import 'package:aituan_user_app/features/home/data/backend_app_repository.dart';

class FakeCartApiClient extends AppApiClient {
  FakeCartApiClient()
    : super(baseUrl: 'http://fake.local', tokenProvider: () => 'token');

  final calls = <String>[];
  final bodies = <Map<String, dynamic>>[];
  final responses = <String, Map<String, dynamic>>{};

  @override
  Future<Map<String, dynamic>> get(String path) async {
    calls.add('GET $path');
    return responses[path] ?? _cartResponse();
  }

  @override
  Future<Map<String, dynamic>> post(
    String path,
    Map<String, dynamic> body,
  ) async {
    calls.add('POST $path');
    bodies.add(body);
    return responses[path] ?? _cartResponse();
  }

  @override
  Future<Map<String, dynamic>> put(
    String path,
    Map<String, dynamic> body,
  ) async {
    calls.add('PUT $path');
    bodies.add(body);
    return responses[path] ?? _cartResponse();
  }

  @override
  Future<Map<String, dynamic>> delete(String path) async {
    calls.add('DELETE $path');
    return responses[path] ?? _cartResponse();
  }
}

Map<String, dynamic> _cartResponse({
  bool catalogAvailable = true,
  String? notice,
}) => {
  'code': 0,
  'data': {
    'storeId': 1,
    'storeName': '爱团炸鸡中关村店',
    'amount': 39.8,
    'catalogAvailable': catalogAvailable,
    'notice': notice,
    'items': [
      {
        'itemId': 1002,
        'itemName': '吮指原味鸡',
        'subtitle': '两块装',
        'categoryName': '炸鸡',
        'unitPrice': 19.9,
        'quantity': 2,
        'totalPrice': 39.8,
        'stock': 20,
        'status': 'on_sale',
        'soldOut': false,
      },
    ],
  },
};

void main() {
  group('BackendAppRepository cart', () {
    test('解析商品服务不可用时的购物车快照和提示', () async {
      const notice = '商品服务暂不可用，已显示最近一次购物车快照；暂不可新增商品或修改数量，仍可移除或清空。';
      final client = FakeCartApiClient();
      client.responses['/api/app/trade/cart?storeId=1'] = _cartResponse(
        catalogAvailable: false,
        notice: notice,
      );

      final cart = await BackendAppRepository(client: client).fetchCart(1);

      expect(client.calls, ['GET /api/app/trade/cart?storeId=1']);
      expect(cart.catalogAvailable, isFalse);
      expect(cart.notice, notice);
      expect(cart.storeName, '爱团炸鸡中关村店');
      expect(cart.amount, 39.8);
      expect(cart.items.single.itemName, '吮指原味鸡');
      expect(cart.items.single.quantity, 2);
    });

    test('加号固定增加一件，修改数量发送目标数量', () async {
      final client = FakeCartApiClient();
      final repository = BackendAppRepository(client: client);

      await repository.addCartItem(storeId: 1, itemId: 1002);
      await repository.updateCartItem(storeId: 1, itemId: 1002, quantity: 1);

      expect(client.calls, [
        'POST /api/app/trade/cart/items',
        'PUT /api/app/trade/cart/items/1002',
      ]);
      expect(client.bodies, [
        {'storeId': 1, 'itemId': 1002, 'quantity': 1},
        {'storeId': 1, 'quantity': 1},
      ]);
    });

    test('移除和清空使用故障期间仍可用的 DELETE 接口', () async {
      final client = FakeCartApiClient();
      final repository = BackendAppRepository(client: client);

      await repository.removeCartItem(storeId: 1, itemId: 1002);
      await repository.clearCart(1);

      expect(client.calls, [
        'DELETE /api/app/trade/cart/items/1002?storeId=1',
        'DELETE /api/app/trade/cart?storeId=1',
      ]);
    });
  });
}
