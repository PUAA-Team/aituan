import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/models/item_model.dart';

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

  double get _missing =>
      widget.startPrice > _total ? widget.startPrice - _total : 0;

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
                  '还差￥${_missing.toStringAsFixed(0)}起送',
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
                  _missing > 0 ? '差￥${_missing.toStringAsFixed(0)}起送' : '去确认订单',
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
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                item.title,
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 4),
              PriceText(item.price, size: 16),
            ],
          ),
        ),
        _RoundButton(icon: Icons.remove, onTap: onRemove),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8),
          child: Text(
            '$count',
            style: const TextStyle(fontWeight: FontWeight.w800),
          ),
        ),
        _RoundButton(
          icon: Icons.add,
          onTap: canAdd ? onAdd : null,
          filled: true,
        ),
      ],
    );
  }
}

class _RoundButton extends StatelessWidget {
  const _RoundButton({
    required this.icon,
    required this.onTap,
    this.filled = false,
  });

  final IconData icon;
  final VoidCallback? onTap;
  final bool filled;

  @override
  Widget build(BuildContext context) => InkWell(
    onTap: onTap,
    borderRadius: BorderRadius.circular(14),
    child: Opacity(
      opacity: onTap == null ? 0.45 : 1,
      child: Container(
        width: 28,
        height: 28,
        decoration: BoxDecoration(
          color: filled ? AppColors.brand : AppColors.soft,
          shape: BoxShape.circle,
          border: Border.all(color: AppColors.brandLine),
        ),
        child: Icon(
          icon,
          size: 18,
          color: filled ? Colors.white : AppColors.brand,
        ),
      ),
    ),
  );
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
