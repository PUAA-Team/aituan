import '../enums/business_type.dart';

class OrderModel {
  const OrderModel({
    required this.id,
    required this.title,
    required this.storeName,
    required this.kind,
    required this.status,
    required this.fulfillmentStatus,
    required this.businessType,
    required this.amount,
    required this.desc,
    this.coverUrl,
  });

  final String id;
  final String title;
  final String storeName;
  final OrderKind kind;
  final OrderStatus status;
  final String fulfillmentStatus;
  final BusinessType businessType;
  final double amount;
  final String desc;
  final String? coverUrl;
}
