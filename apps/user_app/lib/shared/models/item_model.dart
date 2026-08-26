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
    this.storeName = '',
    this.coverUrl,
    this.recommendReason = '',
    this.stock = 999,
    this.saleStatus = 'on_sale',
    this.businessAttributes = '',
    this.usageRules = '',
    this.refundPolicy = '',
    this.notice = '',
    this.validityDays = 90,
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
  final String storeName;
  final String? coverUrl;
  final String recommendReason;
  final int stock;
  final String saleStatus;
  // Stage5-D 非外卖差异化字段（key:value;key:value 串）
  final String businessAttributes;
  final String usageRules;
  final String refundPolicy;
  final String notice;
  final int validityDays;

  bool get soldOut => stock <= 0 || saleStatus != 'on_sale';

  factory ItemModel.fromApi(Map<String, dynamic> json) => ItemModel(
    id: _text(json['id']),
    title: _text(json['title']),
    subtitle: _text(json['subtitle']),
    type: businessTypeFromApi(_text(json['businessType'])),
    category: _text(json['categoryName'], fallback: _text(json['category'])),
    price: _double(json['price']),
    oldPrice: _optionalDouble(json['originalPrice'] ?? json['oldPrice']),
    tags: _tags(json['tags']),
    storeId: _text(json['storeId']),
    storeName: _text(json['storeName']),
    coverUrl: _nullableText(json['coverUrl']),
    recommendReason: _text(json['recommendReason']),
    stock: _int(json['stock'], fallback: 999),
    saleStatus: _text(json['saleStatus'], fallback: 'on_sale'),
    businessAttributes: _text(json['businessAttributes']),
    usageRules: _text(json['usageRules']),
    refundPolicy: _text(json['refundPolicy']),
    notice: _text(json['notice']),
    validityDays: _int(json['validityDays'], fallback: 90),
  );
}

double _double(dynamic value) =>
    value is num ? value.toDouble() : double.tryParse('$value') ?? 0;

int _int(dynamic value, {int fallback = 0}) =>
    value is num ? value.toInt() : int.tryParse('$value') ?? fallback;

double? _optionalDouble(dynamic value) {
  if (value == null || value == '') return null;
  return value is num ? value.toDouble() : double.tryParse('$value');
}

String _text(dynamic value, {String fallback = ''}) {
  final text = value?.toString().trim();
  return (text == null || text.isEmpty) ? fallback : text;
}

String? _nullableText(dynamic value) {
  final text = value?.toString().trim();
  return text == null || text.isEmpty ? null : text;
}

List<String> _tags(dynamic value) {
  if (value is List) {
    return value
        .map((entry) => entry.toString())
        .where((entry) => entry.isNotEmpty)
        .toList();
  }
  if (value == null) return const [];
  return value
      .toString()
      .split(',')
      .map((entry) => entry.trim())
      .where((entry) => entry.isNotEmpty)
      .toList();
}
