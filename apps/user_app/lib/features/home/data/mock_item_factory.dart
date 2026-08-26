import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';

ItemModel mockItem(
  String id,
  String title,
  String subtitle,
  BusinessType type,
  String category,
  double price,
  double? oldPrice,
  List<String> tags,
  String storeId,
) => ItemModel(
  id: id,
  title: title,
  subtitle: subtitle,
  type: type,
  category: category,
  price: price,
  oldPrice: oldPrice,
  tags: tags,
  storeId: storeId,
);
