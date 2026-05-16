import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import '../../home/data/mock_data.dart';
import 'merchant_category_widgets.dart';
import 'takeaway_cart_sheet.dart';
import 'takeaway_merchant_sections.dart';
import 'takeaway_merchant_widgets.dart';

class TakeawayMerchantPage extends StatefulWidget {
  const TakeawayMerchantPage({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  State<TakeawayMerchantPage> createState() => _TakeawayMerchantPageState();
}

class _TakeawayMerchantPageState extends State<TakeawayMerchantPage> {
  final Map<String, int> _cart = {};
  int _tab = 0;
  String? _selectedCategory;

  List<ItemModel> get _items => itemsForMerchant(widget.merchant);

  double get _total =>
      _items.fold(0, (sum, item) => sum + item.price * (_cart[item.id] ?? 0));

  int get _count => _cart.values.fold(0, (a, b) => a + b);

  @override
  Widget build(BuildContext context) {
    final groups = groupItemsByCategory(_items);
    final active = _activeCategory(groups);
    return Scaffold(
      appBar: AppBar(title: const Text('外卖点单')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          TakeawayMerchantHeader(merchant: widget.merchant),
          TakeawayMerchantTabs(
            value: _tab,
            onChanged: (value) => setState(() => _tab = value),
          ),
          const SizedBox(height: 10),
          if (_tab == 0)
            TakeawayOrderPanel(
              groups: groups,
              activeCategory: active,
              cart: _cart,
              onSelected: (category) =>
                  setState(() => _selectedCategory = category),
              onAdd: _add,
              onRemove: _remove,
            )
          else if (_tab == 1)
            const TakeawayReviewPanel()
          else
            TakeawayMerchantInfoPanel(merchant: widget.merchant),
          const SizedBox(height: 90),
        ],
      ),
      bottomNavigationBar: _tab == 0
          ? TakeawayCartBar(
              total: _total,
              count: _count,
              onOpen: _openCart,
              onSubmit: _submit,
            )
          : null,
    );
  }

  String _activeCategory(Map<String, List<ItemModel>> groups) {
    if (groups.isEmpty) return '';
    final selected = _selectedCategory;
    return selected != null && groups.containsKey(selected)
        ? selected
        : groups.keys.first;
  }

  void _add(ItemModel item) =>
      setState(() => _cart[item.id] = (_cart[item.id] ?? 0) + 1);

  void _remove(ItemModel item) => setState(() {
    final count = (_cart[item.id] ?? 0) - 1;
    if (count <= 0) {
      _cart.remove(item.id);
    } else {
      _cart[item.id] = count;
    }
  });

  void _openCart() {
    showModalBottomSheet<void>(
      context: context,
      builder: (_) => TakeawayCartSheet(
        items: _items,
        cart: _cart,
        total: _total,
        onSubmit: _submit,
      ),
    );
  }

  void _submit() {
    if (_total <= 0 || !AppScope.of(context).requireLogin(context)) return;
    Navigator.pushNamed(
      context,
      Routes.checkout,
      arguments: CheckoutArgs(
        kind: OrderKind.takeaway,
        title: widget.merchant.name,
        amount: _total,
      ),
    );
  }
}
