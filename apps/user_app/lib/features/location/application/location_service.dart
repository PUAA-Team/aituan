import 'dart:async';

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

  Future<UserLocation?> cachedLocation() async {
    final total = Stopwatch()..start();
    final diagnostics = <String>[];
    final access = await _ensureLocationAccess(diagnostics, total);
    if (!access) return null;

    try {
      final cachedPosition = await Geolocator.getLastKnownPosition().timeout(
        const Duration(seconds: 2),
      );
      if (cachedPosition == null) return null;
      diagnostics.add(
        '优先使用缓存坐标：${_coord(cachedPosition.latitude)},${_coord(cachedPosition.longitude)}',
      );
      diagnostics.add('缓存定位耗时：${total.elapsedMilliseconds}ms');
      return _reverseGeocode(
        cachedPosition.latitude,
        cachedPosition.longitude,
        debugNote: diagnostics.join('；'),
      );
    } on Object catch (error) {
      diagnostics.add('缓存坐标读取失败：$error');
      return null;
    }
  }

  Future<UserLocation> currentLocation() async {
    final total = Stopwatch()..start();
    final diagnostics = <String>[];

    await _ensureLocationAccess(diagnostics, total);

    Position? cachedPosition;
    try {
      cachedPosition = await Geolocator.getLastKnownPosition().timeout(
        const Duration(seconds: 2),
      );
      diagnostics.add(
        cachedPosition == null
            ? '上次缓存坐标：无'
            : '上次缓存坐标：${_coord(cachedPosition.latitude)},${_coord(cachedPosition.longitude)}',
      );
    } on Object catch (error) {
      diagnostics.add('上次缓存坐标检查失败：$error');
    }

    try {
      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.medium,
          timeLimit: Duration(seconds: 8),
        ),
      );
      diagnostics.add(
        '当前系统定位成功：${_coord(position.latitude)},${_coord(position.longitude)}，精度 ${position.accuracy.toStringAsFixed(1)}m',
      );
      return _reverseGeocode(position.latitude, position.longitude);
    } on TimeoutException catch (error) {
      return _useCachedOrThrow(cachedPosition, diagnostics, total, error);
    } on Object catch (error) {
      return _useCachedOrThrow(cachedPosition, diagnostics, total, error);
    }
  }

  Future<UserLocation> _reverseGeocode(
    double latitude,
    double longitude, {
    String debugNote = '',
  }) async {
    try {
      final json = await _client.get(
        '/api/app/location/reverse-geocode?longitude=$longitude&latitude=$latitude',
      );
      final data = _map(json['data']);
      return UserLocation(
        latitude: _double(data['latitude'], fallback: latitude),
        longitude: _double(data['longitude'], fallback: longitude),
        label: _debugLabel(
          _string(data['label'], fallback: '当前位置'),
          debugNote,
        ),
        formattedAddress: _string(data['formattedAddress']),
        province: _string(data['province']),
        city: _string(data['city']),
        district: _string(data['district']),
        street: _string(data['street']),
      );
    } catch (_) {
      return UserLocation(
        latitude: latitude,
        longitude: longitude,
        label: _debugLabel('当前位置', debugNote),
      );
    }
  }
}

Future<bool> _ensureLocationAccess(
  List<String> diagnostics,
  Stopwatch total,
) async {
  final enabled = await Geolocator.isLocationServiceEnabled();
  diagnostics.add('系统定位服务：${enabled ? '已开启' : '未开启'}');
  if (!enabled) {
    _throwLocationFailure('系统定位服务检查', diagnostics, total, '系统定位服务未开启');
  }

  var permission = await Geolocator.checkPermission();
  diagnostics.add('初始权限：$permission');
  if (permission == LocationPermission.denied) {
    permission = await Geolocator.requestPermission();
    diagnostics.add('请求权限后：$permission');
  }
  if (permission == LocationPermission.denied) {
    _throwLocationFailure('定位权限检查', diagnostics, total, '用户未授予定位权限');
  }
  if (permission == LocationPermission.deniedForever) {
    _throwLocationFailure('定位权限检查', diagnostics, total, '定位权限已被系统永久禁止');
  }
  return true;
}

class LocationException implements Exception {
  const LocationException(this.message);

  final String message;

  @override
  String toString() => message;
}

Future<UserLocation> _useCachedOrThrow(
  Position? cachedPosition,
  List<String> diagnostics,
  Stopwatch total,
  Object currentError,
) {
  if (cachedPosition != null) {
    diagnostics.add('当前定位失败：$currentError');
    diagnostics.add('处理结果：已使用上次缓存坐标');
    diagnostics.add('定位链路耗时：${total.elapsedMilliseconds}ms');
    return locationService._reverseGeocode(
      cachedPosition.latitude,
      cachedPosition.longitude,
      debugNote: diagnostics.join('；'),
    );
  }
  _throwLocationFailure('系统当前定位 getCurrentPosition', diagnostics, total, currentError);
}

Never _throwLocationFailure(
  String stage,
  List<String> diagnostics,
  Stopwatch total,
  Object reason,
) {
  diagnostics.add('失败阶段：$stage');
  diagnostics.add('失败原因：$reason');
  diagnostics.add('定位链路耗时：${total.elapsedMilliseconds}ms');
  throw LocationException(diagnostics.join('；'));
}

String _coord(double value) => value.toStringAsFixed(6);

String _debugLabel(String label, String debugNote) {
  if (debugNote.isEmpty) return label;
  return '$label\n$debugNote';
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
