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

  Future<List<MessageItem>> fetchMessages();
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
  Future<List<MessageItem>> fetchMessages() async => mock.messages;
}
