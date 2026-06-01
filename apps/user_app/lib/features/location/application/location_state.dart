import 'package:flutter/foundation.dart';

import 'location_service.dart';

class LocationState extends ChangeNotifier {
  static const _debugErrors = bool.fromEnvironment('LOCATION_DEBUG_ERRORS');

  int _refreshSeq = 0;
  UserLocation? _current;
  Object? _error;
  String _debugNote = '';
  bool _loading = false;

  UserLocation? get current => _current;
  Object? get error => _error;
  String get debugNote => _debugNote;
  bool get debugErrors => _debugErrors;
  bool get loading => _loading;
  bool get hasLocation => _current != null;
  double? get latitude => _current?.latitude;
  double? get longitude => _current?.longitude;
  String get label {
    if (_loading) return '定位中';
    final current = _current;
    if (current != null) return current.label.split('\n').first;
    return '当前位置';
  }

  Future<void> refresh() async {
    final seq = ++_refreshSeq;
    _loading = true;
    _error = null;
    notifyListeners();

    try {
      final cached = await locationService.cachedLocation();
      if (seq != _refreshSeq) return;
      if (cached != null) {
        _current = cached;
        _debugNote = _debugErrors ? _extractDebugNote(cached) : '';
        _loading = false;
        notifyListeners();
        _refreshCurrentInBackground(seq);
        return;
      }

      _current = await locationService.currentLocation();
      if (seq != _refreshSeq) return;
      _debugNote = _debugErrors ? _extractDebugNote(_current) : '';
    } catch (error) {
      if (seq != _refreshSeq) return;
      _current = fallbackLocation;
      _debugNote = '';
      _error = _debugErrors ? error : null;
    } finally {
      if (seq == _refreshSeq) {
        _loading = false;
        notifyListeners();
      }
    }
  }

  Future<void> _refreshCurrentInBackground(int seq) async {
    try {
      final current = await locationService.currentLocation();
      if (seq != _refreshSeq) return;
      _current = current;
      _debugNote = _debugErrors ? _extractDebugNote(current) : '';
      _error = null;
      notifyListeners();
    } catch (error) {
      if (seq != _refreshSeq) return;
      _error = _debugErrors ? error : null;
      notifyListeners();
    }
  }
}

String _extractDebugNote(UserLocation? location) {
  final parts = location?.label.split('\n');
  if (parts == null || parts.length < 2) return '';
  return parts.skip(1).join('\n');
}

final locationState = LocationState();
