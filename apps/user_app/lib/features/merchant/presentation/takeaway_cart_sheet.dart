import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/models/item_model.dart';
import 'takeaway_amount_utils.dart';
import 'takeaway_quantity_stepper.dart';

class TakeawayCartSheet extends StatefulWidget {
  const TakeawayCartSheet({
    super.key,
    required this.items,
    required this.cart,
    required this.deliveryFee,
    required this.startPrice,
    required this.onAdd,
    required this.onRemove,
    required this.onClear,
    required this.onSubmit,
  });

  final List<ItemModel> items;
  final Map<String, int> cart;
  final double deliveryFee;
  final double startPrice;
  final ValueChanged<ItemModel> onAdd;
  final ValueChanged<ItemModel> onRemove;
  final VoidCallback onClear;
  final VoidCallback onSubmit;

  @override
  State<TakeawayCartSheet> createState() => _TakeawayCartSheetState();
}

class _TakeawayCartSheetState extends State<TakeawayCartSheet> {
  double get _total => widget.items.fold(
    0,
    (sum, item) => sum + item.price * (widget.cart[item.id] ?? 0),
  );

  double get _missing => takeawayStartMissing(_total, widget.startPrice);

  @override
  Widget build(BuildContext context) {
    final selectedItems = widget.items
        .where((item) => (widget.cart[item.id] ?? 0) > 0)
        .toList();
    final deliveryFee = _total > 0 ? widget.deliveryFee : 0.0;
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(18, 14, 18, 18),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Text(
                  '购物车',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900),
                ),
                const Spacer(),
                if (selectedItems.isNotEmpty)
                  TextButton(
                    onPressed: () {
                      widget.onClear();
                      setState(() {});
                    },
                    child: const Text('清空'),
                  ),
              ],
            ),
            if (selectedItems.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 18),
                child: Text('还没有选择商品'),
              )
            else
              Flexible(
                child: ListView.separated(
                  shrinkWrap: true,
                  itemCount: selectedItems.length,
                  separatorBuilder: (_, index) => const Divider(height: 18),
                  itemBuilder: (_, index) => _CartLine(
                    item: selectedItems[index],
                    count: widget.cart[selectedItems[index].id] ?? 0,
                    onAdd: () {
                      widget.onAdd(selectedItems[index]);
                      setState(() {});
                    },
                    onRemove: () {
                      widget.onRemove(selectedItems[index]);
                      setState(() {});
                    },
                  ),
                ),
              ),
            const Divider(),
            _SummaryRow(label: '商品金额', value: _total),
            _SummaryRow(label: '配送费', value: deliveryFee),
            if (_missing > 0)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  '还差￥${takeawayMoneyText(_missing)}起送',
                  style: const TextStyle(
                    color: AppColors.brand,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            const SizedBox(height: 10),
            Row(
              children: [
                const Text('预计实付'),
                const Spacer(),
                PriceText(_total + deliveryFee, size: 20),
              ],
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton(
                onPressed: _total > 0 && _missing <= 0
                    ? () {
                        Navigator.of(context).pop();
                        widget.onSubmit();
                      }
                    : null,
                child: Text(
                  _missing > 0 ? '差￥${takeawayMoneyText(_missing)}起送' : '去确认订单',
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _CartLine extends StatelessWidget {
  const _CartLine({
    required this.item,
    required this.count,
    required this.onAdd,
    required this.onRemove,
  });

  final ItemModel item;
  final int count;
  final VoidCallback onAdd;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final canAdd = !item.soldOut && count < item.stock;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                item.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 4),
              PriceText(item.price, size: 16),
            ],
          ),
        ),
        const SizedBox(width: 8),
        TakeawayQuantityStepper(
          count: count,
          canAdd: canAdd,
          onAdd: onAdd,
          onRemove: onRemove,
        ),
      ],
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow({required this.label, required this.value});

  final String label;
  final double value;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 3),
    child: Row(
      children: [Text(label), const Spacer(), PriceText(value, size: 16)],
    ),
  );
}
