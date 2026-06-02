import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import 'mock_items.dart';
import 'mock_merchants.dart';

export 'mock_items.dart';
export 'mock_merchants.dart';
export 'mock_modules.dart';
export 'mock_orders.dart';

List<ItemModel> itemsByType(BusinessType type) =>
    allItems.where((item) => item.type == type).toList();

List<MerchantModel> merchantsByType(BusinessType type) =>
    merchants.where((merchant) => merchant.type == type).toList();

List<ItemModel> itemsForMerchant(MerchantModel merchant) => merchant.items
    .where((item) => item.storeId == merchant.id && item.type == merchant.type)
    .toList();

List<MerchantModel> searchMerchants(String keyword) {
  final query = keyword.trim().toLowerCase();
  if (query.isEmpty) return merchants;
  return merchants.where((merchant) {
    final merchantText =
        '${merchant.name}${merchant.summary}${merchant.tags.join()}'
            .toLowerCase();
    final itemText = merchant.items
        .map((item) => '${item.title}${item.subtitle}${item.tags.join()}')
        .join()
        .toLowerCase();
    return merchantText.contains(query) || itemText.contains(query);
  }).toList();
}

MerchantModel merchantById(String? id) => merchants.firstWhere(
  (item) => item.id == id,
  orElse: () => merchants.first,
);

ItemModel itemById(String? id) => allItems.firstWhere(
  (item) => item.id == id,
  orElse: () => serviceItems.first,
);
