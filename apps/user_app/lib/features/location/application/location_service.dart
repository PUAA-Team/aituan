import 'package:geolocator/geolocator.dart';

import '../../../core/network/app_api_client.dart';

const fallbackLocation = UserLocation(
  latitude: 39.9812,
  longitude: 116.3436,
  label: '北航学院路校区',
  formattedAddress: '北京市海淀区北京航空航天大学学院路校区',
  province: '北京市',
  city: '北京市',
  district: '海淀区',
  street: '学院路',
);

class UserLocation {
  const UserLocation({
    required this.latitude,
    required this.longitude,
    this.label = '当前位置',
    this.formattedAddress = '',
    this.province = '',
    this.city = '',
    this.district = '',
    this.street = '',
  });

  final double latitude;
  final double longitude;
  final String label;
  final String formattedAddress;
  final String province;
  final String city;
  final String district;
  final String street;

  String get addressText {
    if (formattedAddress.trim().isNotEmpty) return formattedAddress.trim();
    if (label.trim().isNotEmpty && label != '当前位置') return label.trim();
    return '';
  }
}

class LocationService {
  LocationService({AppApiClient? client}) : _client = client ?? AppApiClient();

  final AppApiClient _client;

  Future<UserLocation> currentLocation() async {
    final enabled = await Geolocator.isLocationServiceEnabled();
    if (!enabled) {
      throw const LocationException('系统定位服务未开启');
    }

    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.denied) {
      throw const LocationException('未获得定位权限');
    }
    if (permission == LocationPermission.deniedForever) {
      throw const LocationException('定位权限已被系统禁止，请到设置中开启');
    }

    final position = await Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.medium,
        timeLimit: Duration(seconds: 8),
      ),
    );
    return _reverseGeocode(position.latitude, position.longitude);
  }

  Future<UserLocation> _reverseGeocode(
    double latitude,
    double longitude,
  ) async {
    try {
      final json = await _client.get(
        '/api/app/location/reverse-geocode?longitude=$longitude&latitude=$latitude',
      );
      final data = _map(json['data']);
      return UserLocation(
        latitude: _double(data['latitude'], fallback: latitude),
        longitude: _double(data['longitude'], fallback: longitude),
        label: _string(data['label'], fallback: '当前位置'),
        formattedAddress: _string(data['formattedAddress']),
        province: _string(data['province']),
        city: _string(data['city']),
        district: _string(data['district']),
        street: _string(data['street']),
      );
    } catch (_) {
      return UserLocation(latitude: latitude, longitude: longitude);
    }
  }
}

class LocationException implements Exception {
  const LocationException(this.message);

  final String message;

  @override
  String toString() => message;
}

Map<String, dynamic> _map(dynamic value) =>
    value is Map<String, dynamic> ? value : <String, dynamic>{};

String _string(dynamic value, {String fallback = ''}) {
  final text = value?.toString().trim();
  return (text == null || text.isEmpty) ? fallback : text;
}

double _double(dynamic value, {required double fallback}) {
  if (value is num) return value.toDouble();
  return double.tryParse(value?.toString() ?? '') ?? fallback;
}

final locationService = LocationService();
