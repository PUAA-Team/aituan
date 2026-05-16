import 'package:aituan_user_app/app/app.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets('shows splash page', (tester) async {
    await tester.pumpWidget(const AituanApp());
    expect(find.text('爱团'), findsOneWidget);
    expect(find.text('游客进入'), findsOneWidget);
  });
}
