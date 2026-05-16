import 'package:flutter/material.dart';

import '../../../core/widgets/price_text.dart';
import '../../../shared/models/item_model.dart';

class TakeawayCartSheet extends StatelessWidget {
  const TakeawayCartSheet({
    super.key,
    required this.items,
    required this.cart,
    required this.total,
    required this.onSubmit,
  });

  final List<ItemModel> items;
  final Map<String, int> cart;
  final double total;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) {
    final selectedItems = items
        .where((item) => (cart[item.id] ?? 0) > 0)
        .toList();
    final deliveryFee = total > 0 ? 4.0 : 0.0;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              '购物车',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900),
            ),
            if (selectedItems.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 18),
                child: Text('还没有选择商品'),
              ),
            for (final item in selectedItems)
              ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(item.title),
                trailing: Text('x${cart[item.id]}'),
              ),
            const Divider(),
            Row(
              children: [
                const Text('配送费'),
                const Spacer(),
                Text('￥${deliveryFee.toStringAsFixed(1)}'),
              ],
            ),
            Row(
              children: [
                const Text('预计实付'),
                const Spacer(),
                PriceText(total + deliveryFee, size: 18),
              ],
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: total > 0
                    ? () {
                        Navigator.of(context).pop();
                        onSubmit();
                      }
                    : null,
                child: const Text('去确认订单'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
