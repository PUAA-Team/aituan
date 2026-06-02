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

    await tester.tap(find.text('下载 Android APK'));
    expect(opened, contains(AituanUnsupportedWebApp.apkDownloadPath));
  });

  testWidgets('uses compact copy on narrow browser viewport', (tester) async {
    tester.view.physicalSize = const Size(390, 844);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(AituanUnsupportedWebApp(openUrl: (_) {}));

    expect(find.text('请下载 App 使用完整服务'), findsOneWidget);
  });
}
