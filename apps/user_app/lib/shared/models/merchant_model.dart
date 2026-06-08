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
    this.recommendReason = '',
    this.estimatedTimeText = '',
    this.longitude,
    this.latitude,
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
  final String recommendReason;
  final String estimatedTimeText;
  final double? longitude;
  final double? latitude;
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
    this.packageFeeFixed = 0,
    this.packageFeePerItem = 0,
    this.packageFeeMode = 'none',
    this.distanceExtraThresholdKm = 0,
    this.distanceExtraFee = 0,
    this.distanceExtraStepKm = 1,
    this.deliveryText = '',
  });

  final double deliveryFee;
  final int estimatedMinutes;
  final double startPrice;
  final double packageFeeFixed;
  final double packageFeePerItem;
  final String packageFeeMode;
  final double distanceExtraThresholdKm;
  final double distanceExtraFee;
  final double distanceExtraStepKm;
  final String deliveryText;

  String get packageFeeText => switch (packageFeeMode) {
    'fixed' =>
      packageFeeFixed > 0
          ? '打包费￥${packageFeeFixed.toStringAsFixed(1)}/单'
          : '不收打包费',
    'per_item' =>
      packageFeePerItem > 0
          ? '打包费￥${packageFeePerItem.toStringAsFixed(1)}/件'
          : '不收打包费',
    _ => '不收打包费',
  };

  String get distanceExtraText {
    if (distanceExtraThresholdKm <= 0 || distanceExtraFee <= 0) {
      return '无距离加价';
    }
    final step = distanceExtraStepKm <= 0 ? 1.0 : distanceExtraStepKm;
    return '超${distanceExtraThresholdKm.toStringAsFixed(1)}km后，每${step.toStringAsFixed(1)}km加￥${distanceExtraFee.toStringAsFixed(1)}';
  }
}
