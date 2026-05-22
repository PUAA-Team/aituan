class AddressData {
  const AddressData({
    required this.id,
    required this.contactName,
    required this.contactPhone,
    required this.province,
    required this.city,
    required this.district,
    required this.detailAddress,
    required this.tagName,
    required this.isDefault,
    required this.deliveryNote,
    required this.createdAt,
  });

  final String id;
  final String contactName;
  final String contactPhone;
  final String province;
  final String city;
  final String district;
  final String detailAddress;
  final String tagName;
  final bool isDefault;
  final String deliveryNote;
  final DateTime? createdAt;

  String get fullAddress => [
    province,
    city,
    district,
    detailAddress,
  ].where((entry) => entry.trim().isNotEmpty).join('');

  factory AddressData.fromApi(Map<String, dynamic> json) => AddressData(
    id: _string(json['id']),
    contactName: _string(json['contactName']),
    contactPhone: _string(json['contactPhone']),
    province: _string(json['province']),
    city: _string(json['city']),
    district: _string(json['district']),
    detailAddress: _string(json['detailAddress']),
    tagName: _string(json['tagName'], fallback: '家'),
    isDefault: _bool(json['isDefault']),
    deliveryNote: _string(json['deliveryNote']),
    createdAt: _dateTime(json['createdAt']),
  );
}

class AddressFormData {
  const AddressFormData({
    required this.contactName,
    required this.contactPhone,
    required this.province,
    required this.city,
    required this.district,
    required this.detailAddress,
    required this.tagName,
    required this.isDefault,
    required this.deliveryNote,
  });

  final String contactName;
  final String contactPhone;
  final String province;
  final String city;
  final String district;
  final String detailAddress;
  final String tagName;
  final bool isDefault;
  final String deliveryNote;

  factory AddressFormData.fromAddress(AddressData address) => AddressFormData(
    contactName: address.contactName,
    contactPhone: address.contactPhone,
    province: address.province,
    city: address.city,
    district: address.district,
    detailAddress: address.detailAddress,
    tagName: address.tagName,
    isDefault: address.isDefault,
    deliveryNote: address.deliveryNote,
  );

  Map<String, dynamic> toApi() => {
    'contactName': contactName,
    'contactPhone': contactPhone,
    'province': province,
    'city': city,
    'district': district,
    'detailAddress': detailAddress,
    'tagName': tagName,
    'isDefault': isDefault,
    'deliveryNote': deliveryNote,
  };
}

String _string(dynamic value, {String fallback = ''}) {
  final text = value?.toString().trim();
  return (text == null || text.isEmpty) ? fallback : text;
}

bool _bool(dynamic value) =>
    value is bool ? value : value?.toString() == 'true';

DateTime? _dateTime(dynamic value) {
  if (value == null) return null;
  return DateTime.tryParse(value.toString())?.toLocal();
}
