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
    this.stock = 999,
    this.saleStatus = 'on_sale',
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
  final int stock;
  final String saleStatus;

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
    stock: _int(json['stock'], fallback: 999),
    saleStatus: _text(json['saleStatus'], fallback: 'on_sale'),
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
