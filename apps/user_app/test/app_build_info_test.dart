import 'dart:io';

import 'package:aituan_user_app/core/constants/app_build_info.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('app build info matches pubspec version', () {
    final pubspec = File('pubspec.yaml').readAsStringSync();
    final match = RegExp(
      r'^version:\s*([^\s]+)\s*$',
      multiLine: true,
    ).firstMatch(pubspec);

    expect(match, isNotNull);
    expect(match!.group(1), AppBuildInfo.fullVersion);
  });
}
