import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import '../../../shared/models/message_item.dart';
import '../../../shared/models/module_entry.dart';
import '../../../shared/models/order_model.dart';
import 'mock_data.dart' as mock;

abstract class AppRepository {
  Future<List<ModuleEntry>> fetchModules();

  Future<List<ItemModel>> fetchRecommendations();

  Future<List<MerchantModel>> searchStores(String keyword);

  Future<List<OrderModel>> fetchOrders();

  Future<List<MessageItem>> fetchMessages({
    String? type,
    int page = 1,
    int pageSize = 20,
  });
}

class MockAppRepository implements AppRepository {
  const MockAppRepository();

  @override
  Future<List<ModuleEntry>> fetchModules() async => mock.modules;

  @override
  Future<List<ItemModel>> fetchRecommendations() async => mock.allItems;

  @override
  Future<List<MerchantModel>> searchStores(String keyword) async =>
      mock.searchMerchants(keyword);

  @override
  Future<List<OrderModel>> fetchOrders() async => mock.orders;

  @override
  Future<List<MessageItem>> fetchMessages({
    String? type,
    int page = 1,
    int pageSize = 20,
  }) async {
    final filtered = type == null || type.isEmpty
        ? mock.messages
        : mock.messages.where((message) => message.type == type).toList();
    final safePage = page < 1 ? 1 : page;
    final safePageSize = pageSize < 1 ? 20 : pageSize;
    final start = ((safePage - 1) * safePageSize)
        .clamp(0, filtered.length)
        .toInt();
    final end = (start + safePageSize).clamp(start, filtered.length).toInt();
    return filtered.sublist(start, end);
  }
}
