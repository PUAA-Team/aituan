import 'package:aituan_user_app/app/app.dart';
import 'package:aituan_user_app/app/app_state.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    appState.logout();
  });

  testWidgets('游客从启动页进入首页并在受保护入口跳转登录', (tester) async {
    await tester.pumpWidget(const AituanApp());
    await pumpUntilFound(tester, find.text('游客进入'));

    expect(find.text('爱团'), findsWidgets);
    expect(find.text('游客进入'), findsOneWidget);

    await tester.tap(find.text('游客进入'));
    await pumpUntilFound(tester, find.text('首页'));

    expect(find.text('首页'), findsOneWidget);
    expect(find.byIcon(Icons.home), findsWidgets);

    await tester.tap(find.text('订单'));
    await pumpUntilFound(tester, find.text('爱团账号'));

    expect(find.text('爱团账号'), findsOneWidget);
    expect(find.text('欢迎回来'), findsOneWidget);
  });

  testWidgets('登录页支持注册和找回密码模式切换', (tester) async {
    await tester.pumpWidget(const AituanApp());
    await pumpUntilFound(tester, find.text('登录 / 注册'));

    await tester.tap(find.text('登录 / 注册'));
    await pumpUntilFound(tester, find.text('欢迎回来'));

    expect(find.text('欢迎回来'), findsOneWidget);

    await tester.tap(find.text('前往注册'));
    await pumpUntilFound(tester, find.text('创建爱团账号'));

    expect(find.text('创建爱团账号'), findsOneWidget);
    expect(find.text('邮箱验证码'), findsOneWidget);
    expect(find.text('返回登录'), findsOneWidget);

    await tester.tap(find.text('忘记密码'));
    await pumpUntilFound(tester, find.text('找回密码'));

    expect(find.text('找回密码'), findsOneWidget);
    expect(find.text('新密码'), findsOneWidget);

    await tester.tap(find.text('返回登录'));
    await pumpUntilFound(tester, find.text('欢迎回来'));

    expect(find.text('欢迎回来'), findsOneWidget);
  });
}

Future<void> pumpUntilFound(
  WidgetTester tester,
  Finder finder, {
  Duration step = const Duration(milliseconds: 250),
  int maxAttempts = 80,
}) async {
  for (var i = 0; i < maxAttempts; i++) {
    await tester.pump(step);
    if (finder.evaluate().isNotEmpty) return;
  }
  expect(finder, findsOneWidget);
}
