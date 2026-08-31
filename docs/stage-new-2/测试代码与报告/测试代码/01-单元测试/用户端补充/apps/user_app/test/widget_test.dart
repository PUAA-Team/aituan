import 'package:aituan_user_app/app/app.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows splash page while checking login state', (tester) async {
    await tester.pumpWidget(const AituanApp());

    expect(find.text('爱团'), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}
