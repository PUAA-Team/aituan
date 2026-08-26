import 'package:flutter/foundation.dart';

import '../../../shared/models/address_model.dart';
import 'location_service.dart';

class LocationState extends ChangeNotifier {
  static const _debugErrors = bool.fromEnvironment('LOCATION_DEBUG_ERRORS');

  int _refreshSeq = 0;
  UserLocation? _current;
  UserLocation? _browseLocation;
  Object? _error;
  String _debugNote = '';
  bool _loading = false;

  UserLocation? get current => _current;
  UserLocation? get browseLocation => _browseLocation;
  UserLocation? get effectiveLocation => _browseLocation ?? _current;
  Object? get error => _error;
  String get debugNote => _debugNote;
  bool get debugErrors => _debugErrors;
  bool get loading => _loading;
  bool get usingAddress => _browseLocation != null;
  bool get hasLocation => effectiveLocation != null;
  double? get latitude => effectiveLocation?.latitude;
  double? get longitude => effectiveLocation?.longitude;
  String get label {
    if (_browseLocation != null) {
      return _browseLocation!.label.split('\n').first;
    }
    if (_loading) return '定位中';
    final current = _current;
    if (current != null) return current.label.split('\n').first;
    return '当前位置';
  }

  Future<void> selectCurrentLocation() async {
    _browseLocation = null;
    await refresh();
  }

  void selectAddressLocation(AddressData address) {
    final latitude = address.latitude;
    final longitude = address.longitude;
    if (latitude == null || longitude == null) return;
    _browseLocation = UserLocation(
      latitude: latitude,
      longitude: longitude,
      label: address.detailAddress.isNotEmpty
          ? address.detailAddress
          : address.fullAddress,
      formattedAddress: address.fullAddress,
      province: address.province,
      city: address.city,
      district: address.district,
    );
    _error = null;
    notifyListeners();
  }

  void clearBrowseLocation() {
    if (_browseLocation == null) return;
    _browseLocation = null;
    notifyListeners();
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
