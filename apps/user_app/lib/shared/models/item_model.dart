import '../enums/business_type.dart';

class ItemModel {
  const ItemModel({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.type,
    required this.category,
    required this.price,
    required this.oldPrice,
    required this.tags,
    required this.storeId,
  });

  final String id;
  final String title;
  final String subtitle;
  final BusinessType type;
  final String category;
  final double price;
  final double? oldPrice;
  final List<String> tags;
  final String storeId;
}
