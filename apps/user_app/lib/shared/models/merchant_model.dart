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
    this.coverUrl,
    this.status = 'open',
    this.businessHours = '10:00-22:00',
    this.monthlySales = 0,
    this.avgPrice = 0,
    this.deliveryRule = const DeliveryRuleModel(),
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
  final String? coverUrl;
  final String status;
  final String businessHours;
  final int monthlySales;
  final double avgPrice;
  final DeliveryRuleModel deliveryRule;
}

class DeliveryRuleModel {
  const DeliveryRuleModel({
    this.deliveryFee = 0,
    this.estimatedMinutes = 35,
    this.startPrice = 0,
    this.deliveryText = '',
  });

  final double deliveryFee;
  final int estimatedMinutes;
  final double startPrice;
  final String deliveryText;
}
