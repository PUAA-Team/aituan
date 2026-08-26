import 'package:flutter/widgets.dart';

import 'location_state.dart';

class LocationScope extends InheritedNotifier<LocationState> {
  LocationScope({super.key, required super.child}) : super(notifier: locationState);

  static LocationState of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<LocationScope>();
    assert(scope != null, 'LocationScope not found');
    return scope!.notifier!;
  }
}
