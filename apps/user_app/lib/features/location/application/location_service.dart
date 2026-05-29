import 'package:geolocator/geolocator.dart';

const fallbackLocation = UserLocation(
  latitude: 39.9812,
  longitude: 116.3436,
  label: '北航学院路校区',
);

class UserLocation {
  const UserLocation({
    required this.latitude,
    required this.longitude,
    this.label = '当前位置',
  });

  final double latitude;
  final double longitude;
  final String label;
}

class LocationService {
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
    return UserLocation(
      latitude: position.latitude,
      longitude: position.longitude,
    );
  }
}

class LocationException implements Exception {
  const LocationException(this.message);

  final String message;

  @override
  String toString() => message;
}

final locationService = LocationService();
