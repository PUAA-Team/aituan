import 'package:aituan_user_app/app/app.dart';
import 'package:aituan_user_app/web/web_bootstrap_gate.dart';
import 'package:aituan_user_app/web/unsupported_web_app.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows unsupported web page with APK download action', (tester) async {
    tester.view.physicalSize = const Size(1200, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final opened = <String>[];
    await tester.pumpWidget(AituanUnsupportedWebApp(openUrl: opened.add));

    expect(find.text('暂时不支持电脑端用户服务'), findsOneWidget);
    expect(find.text('下载 Android APK'), findsOneWidget);
    expect(
      find.text('备用下载地址：/downloads/aituan-user-server-debug.apk'),
      findsOneWidget,
    );
    expect(find.byIcon(Icons.delivery_dining), findsOneWidget);
    expect(find.byIcon(Icons.confirmation_number_outlined), findsOneWidget);
    expect(find.byIcon(Icons.star_outline_rounded), findsOneWidget);

    await tester.tap(find.text('下载 Android APK'));
    expect(opened, contains(AituanUnsupportedWebApp.apkDownloadPath));
  });

  testWidgets('runs full app on narrow web viewport', (tester) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(AituanWebBootstrap(openUrl: (_) {}));

    expect(find.byType(AituanApp), findsOneWidget);
    expect(find.text('暂时不支持电脑端用户服务'), findsNothing);
  });

  testWidgets('shows unsupported page on desktop web viewport', (tester) async {
    tester.view.physicalSize = const Size(1200, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(AituanWebBootstrap(openUrl: (_) {}));

    expect(find.text('暂时不支持电脑端用户服务'), findsOneWidget);
    expect(find.byType(AituanApp), findsNothing);
  });
}
