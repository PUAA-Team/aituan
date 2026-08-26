import 'package:aituan_user_app/core/widgets/app_bottom_action_bar.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('leading action does not stretch bottom bar vertically', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: const Text('主界面内容'),
          bottomNavigationBar: AppBottomActionBar(
            leading: OutlinedButton.icon(
              onPressed: () {},
              icon: const Icon(Icons.support_agent, size: 18),
              label: const Text('联系商家'),
            ),
            secondaryText: '申请退款',
            onSecondary: () {},
            primaryText: '查看商家',
            onPrimary: () {},
          ),
        ),
      ),
    );

    expect(find.text('主界面内容'), findsOneWidget);
    final barSize = tester.getSize(find.byType(AppBottomActionBar));
    expect(barSize.height, lessThan(120));
  });
}
