import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:aituan_user_app/features/merchant/presentation/takeaway_cart_sheet.dart';
import 'package:aituan_user_app/features/merchant/presentation/takeaway_catalog_fallback_notice.dart';
import 'package:aituan_user_app/shared/enums/business_type.dart';
import 'package:aituan_user_app/shared/models/item_model.dart';

void main() {
  testWidgets('故障提示明确说明正在展示备用快照且其他业务已隔离', (tester) async {
    var refreshCount = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: TakeawayCatalogFallbackNotice(
            onRefresh: () => refreshCount += 1,
          ),
        ),
      ),
    );

    expect(find.byKey(const Key('catalog-fallback-notice')), findsOneWidget);
    expect(find.text('商品服务异常 · 已启用备用结果'), findsOneWidget);
    expect(find.textContaining('最近一次购物车快照'), findsOneWidget);
    expect(find.textContaining('其他业务仍可正常访问'), findsOneWidget);

    await tester.tap(find.text('重新检测'));
    expect(refreshCount, 1);
  });

  testWidgets('故障购物车允许移除但禁用新增和结算', (tester) async {
    var removeCount = 0;
    final cart = <String, int>{'1002': 2};
    const item = ItemModel(
      id: '1002',
      title: '吮指原味鸡',
      subtitle: '两块装',
      type: BusinessType.takeaway,
      category: '炸鸡',
      price: 19.9,
      oldPrice: null,
      tags: [],
      storeId: '1',
      stock: 20,
    );

    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: TakeawayCartSheet(
            items: const [item],
            cart: cart,
            deliveryFee: 3,
            startPrice: 20,
            catalogAvailable: false,
            onAdd: (_) async {},
            onRemove: (_) async {
              removeCount += 1;
              cart.clear();
            },
            onClear: () async => cart.clear(),
            onSubmit: () {},
          ),
        ),
      ),
    );

    expect(find.text('移除商品'), findsOneWidget);
    expect(find.byIcon(Icons.add), findsNothing);
    final checkoutButton = tester.widget<FilledButton>(
      find.widgetWithText(FilledButton, '商品服务恢复后可结算'),
    );
    expect(checkoutButton.onPressed, isNull);

    await tester.tap(find.text('移除商品'));
    await tester.pump();
    expect(removeCount, 1);
    expect(find.text('还没有选择商品'), findsOneWidget);
  });
}
