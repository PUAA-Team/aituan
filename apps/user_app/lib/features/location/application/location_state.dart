import 'package:flutter/foundation.dart';

import 'location_service.dart';

class LocationState extends ChangeNotifier {
  UserLocation? _current;
  Object? _error;
  bool _loading = false;

  UserLocation? get current => _current;
  Object? get error => _error;
  bool get loading => _loading;
  bool get hasLocation => _current != null;
  double? get latitude => _current?.latitude;
  double? get longitude => _current?.longitude;
  String get label {
    if (_loading) return '定位中';
    if (_current != null) return _current!.label;
    return '当前位置';
  }

  Future<void> refresh() async {
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      _current = await locationService.currentLocation();
    } catch (_) {
      _current = fallbackLocation;
      _error = null;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }
}

final locationState = LocationState();
