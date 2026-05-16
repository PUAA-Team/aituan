import '../enums/business_type.dart';
import 'item_model.dart';

class MerchantModel {
  const MerchantModel({
    required this.id,
    required this.name,
    required this.type,
    required this.distance,
    required this.rating,
    required this.summary,
    required this.address,
    required this.tags,
    required this.items,
  });

  final String id;
  final String name;
  final BusinessType type;
  final String distance;
  final double rating;
  final String summary;
  final String address;
  final List<String> tags;
  final List<ItemModel> items;
}
