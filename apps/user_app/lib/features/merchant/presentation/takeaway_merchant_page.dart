import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import '../../home/data/backend_app_repository.dart';
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
  MerchantModel? _merchant;

  List<ItemModel> get _items => (_merchant ?? widget.merchant).items;

  double get _total =>
      _items.fold(0, (sum, item) => sum + item.price * (_cart[item.id] ?? 0));

  int get _count => _cart.values.fold(0, (a, b) => a + b);

  @override
  void initState() {
    super.initState();
    _merchant = widget.merchant;
    _loadMerchant();
  }

  @override
  Widget build(BuildContext context) {
    final merchant = _merchant ?? widget.merchant;
    final groups = groupItemsByCategory(_items);
    final active = _activeCategory(groups);
    return Scaffold(
      appBar: AppBar(
        title: const Text('外卖点单'),
        actions: [
          IconButton(
            tooltip: '咨询商家',
            icon: const Icon(Icons.support_agent),
            onPressed: () => _openSupport(merchant),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadMerchant,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            TakeawayMerchantHeader(merchant: merchant),
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
              TakeawayMerchantInfoPanel(merchant: merchant),
            const SizedBox(height: 90),
          ],
        ),
      ),
      bottomNavigationBar: _tab == 0
          ? TakeawayCartBar(
              total: _total,
              count: _count,
              deliveryFee: merchant.deliveryRule.deliveryFee,
              startPrice: merchant.deliveryRule.startPrice,
              onOpen: _openCart,
              onSubmit: _submit,
            )
          : null,
    );
  }

  Future<void> _loadMerchant() async {
    final merchantId = int.tryParse(widget.merchant.id);
    if (merchantId == null) return;
    try {
      final merchant = await backendRepository.fetchStore(merchantId);
      if (!mounted) return;
      setState(() => _merchant = merchant);
    } catch (_) {
      if (!mounted) return;
    }
  }

  String _activeCategory(Map<String, List<ItemModel>> groups) {
    if (groups.isEmpty) return '';
    final selected = _selectedCategory;
    return selected != null && groups.containsKey(selected)
        ? selected
        : groups.keys.first;
  }

  void _add(ItemModel item) {
    if (item.soldOut) {
      _showToast('该商品已售罄');
      return;
    }
    final count = _cart[item.id] ?? 0;
    if (count >= item.stock) {
      _showToast('库存只剩${item.stock}份');
      return;
    }
    setState(() => _cart[item.id] = count + 1);
  }

  void _remove(ItemModel item) => setState(() {
    final count = (_cart[item.id] ?? 0) - 1;
    if (count <= 0) {
      _cart.remove(item.id);
    } else {
      _cart[item.id] = count;
    }
  });

  void _openCart() {
    final merchant = _merchant ?? widget.merchant;
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => TakeawayCartSheet(
        items: _items,
        cart: _cart,
        deliveryFee: merchant.deliveryRule.deliveryFee,
        startPrice: merchant.deliveryRule.startPrice,
        onAdd: _add,
        onRemove: _remove,
        onClear: () => setState(_cart.clear),
        onSubmit: _submit,
      ),
    );
  }

  void _submit() {
    final merchant = _merchant ?? widget.merchant;
    if (_total <= 0 || !AppScope.of(context).requireLogin(context)) return;
    if (_total < merchant.deliveryRule.startPrice) {
      _showToast(
        '还差￥${(merchant.deliveryRule.startPrice - _total).toStringAsFixed(0)}起送',
      );
      return;
    }
    Navigator.pushNamed(
      context,
      Routes.checkout,
      arguments: CheckoutArgs(
        kind: OrderKind.takeaway,
        title: merchant.name,
        amount: _total,
        storeId: merchant.id,
        businessType: merchant.type,
        lines: _items
            .where((item) => (_cart[item.id] ?? 0) > 0)
            .map(
              (item) => CheckoutLineArg(
                itemId: item.id,
                quantity: _cart[item.id] ?? 0,
                title: item.title,
                subtitle: item.subtitle,
                unitPrice: item.price,
                categoryName: item.category,
              ),
            )
            .toList(),
      ),
    );
  }

  void _showToast(String message) => showAppSnackBar(context, message);

  void _openSupport(MerchantModel merchant) {
    if (!AppScope.of(context).requireLogin(context)) return;
    final storeId = int.tryParse(merchant.id);
    Navigator.pushNamed(
      context,
      Routes.supportSessions,
      arguments: SupportLaunchArgs(
        storeId: storeId,
        storeName: merchant.name,
        topicHint: '关于「${merchant.name}」',
      ),
    );
  }
}
