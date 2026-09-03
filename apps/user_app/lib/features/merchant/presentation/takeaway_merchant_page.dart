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
import 'takeaway_amount_utils.dart';
import 'takeaway_catalog_fallback_notice.dart';
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
  TradeCartData? _cartData;
  bool _cartUpdating = false;

  List<ItemModel> get _items => (_merchant ?? widget.merchant).items;

  List<ItemModel> get _displayItems {
    final items = [..._items];
    final knownIds = items.map((item) => item.id).toSet();
    final merchant = _merchant ?? widget.merchant;
    for (final line in _cartData?.items ?? const <TradeCartLineData>[]) {
      final itemId = '${line.itemId}';
      if (knownIds.contains(itemId)) continue;
      items.add(
        ItemModel(
          id: itemId,
          title: line.itemName,
          subtitle: line.subtitle,
          type: BusinessType.takeaway,
          category: line.categoryName,
          price: line.unitPrice,
          oldPrice: null,
          tags: const [],
          storeId: merchant.id,
          storeName: merchant.name,
          stock: line.stock,
          saleStatus: line.status,
        ),
      );
    }
    return items;
  }

  bool get _catalogAvailable => _cartData?.catalogAvailable ?? true;

  double get _total =>
      _cartData?.amount ??
      _displayItems.fold(
        0,
        (sum, item) => sum + item.price * (_cart[item.id] ?? 0),
      );

  int get _count => _cart.values.fold(0, (a, b) => a + b);

  @override
  void initState() {
    super.initState();
    _merchant = widget.merchant;
    _refresh();
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
            key: const Key('refresh-merchant-cart'),
            tooltip: '刷新门店和购物车',
            icon: const Icon(Icons.refresh),
            onPressed: _refresh,
          ),
          IconButton(
            tooltip: '咨询商家',
            icon: const Icon(Icons.support_agent),
            onPressed: () => _openSupport(merchant),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _refresh,
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
            if (_tab == 0 && !_catalogAvailable)
              TakeawayCatalogFallbackNotice(
                notice: _cartData?.notice,
                onRefresh: _refresh,
              ),
            if (_tab == 0)
              TakeawayOrderPanel(
                groups: groups,
                activeCategory: active,
                cart: _cart,
                onSelected: (category) =>
                    setState(() => _selectedCategory = category),
                onAdd: _add,
                onRemove: _remove,
                catalogAvailable: _catalogAvailable,
              )
            else if (_tab == 1)
              TakeawayReviewPanel(merchant: merchant)
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
              catalogAvailable: _catalogAvailable,
            )
          : null,
    );
  }

  Future<void> _refresh() async {
    await Future.wait([_loadMerchant(), _loadCart()]);
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

  Future<void> _loadCart() async {
    if (!appState.isLoggedIn) return;
    final storeId = int.tryParse(widget.merchant.id);
    if (storeId == null) return;
    try {
      _applyCart(await backendRepository.fetchCart(storeId));
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

  Future<void> _add(ItemModel item) async {
    if (item.soldOut) {
      _showToast('该商品已售罄');
      return;
    }
    if (!AppScope.of(context).requireLogin(context)) return;
    if (!_catalogAvailable) {
      _showToast('商品服务暂不可用，当前只能查看快照、移除或清空商品');
      return;
    }
    final count = _cart[item.id] ?? 0;
    if (count >= item.stock) {
      _showToast('库存只剩${item.stock}份');
      return;
    }
    final storeId = int.tryParse(widget.merchant.id);
    final itemId = int.tryParse(item.id);
    if (storeId == null || itemId == null) return;
    await _mutateCart(
      () => backendRepository.addCartItem(storeId: storeId, itemId: itemId),
    );
  }

  Future<void> _remove(ItemModel item) async {
    if (!AppScope.of(context).requireLogin(context)) return;
    final storeId = int.tryParse(widget.merchant.id);
    final itemId = int.tryParse(item.id);
    final count = _cart[item.id] ?? 0;
    if (storeId == null || itemId == null || count <= 0) return;
    final removingDuringFallback = !_catalogAvailable;
    await _mutateCart(
      () => removingDuringFallback || count <= 1
          ? backendRepository.removeCartItem(storeId: storeId, itemId: itemId)
          : backendRepository.updateCartItem(
              storeId: storeId,
              itemId: itemId,
              quantity: count - 1,
            ),
      successMessage: removingDuringFallback ? '故障隔离生效：商品已成功移除' : null,
    );
  }

  Future<void> _clearCart() async {
    if (!AppScope.of(context).requireLogin(context)) return;
    final storeId = int.tryParse(widget.merchant.id);
    if (storeId == null) return;
    final clearingDuringFallback = !_catalogAvailable;
    await _mutateCart(
      () => backendRepository.clearCart(storeId),
      successMessage: clearingDuringFallback ? '故障隔离生效：购物车已清空' : null,
    );
  }

  void _openCart() {
    final merchant = _merchant ?? widget.merchant;
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (_) => TakeawayCartSheet(
        items: _displayItems,
        cart: _cart,
        deliveryFee: merchant.deliveryRule.deliveryFee,
        startPrice: merchant.deliveryRule.startPrice,
        onAdd: _add,
        onRemove: _remove,
        onClear: _clearCart,
        onSubmit: _submit,
        catalogAvailable: _catalogAvailable,
        notice: _cartData?.notice,
      ),
    );
  }

  void _submit() {
    final merchant = _merchant ?? widget.merchant;
    if (!_catalogAvailable) {
      _showToast('商品服务暂不可用，暂时无法结算，请稍后重新检测');
      return;
    }
    if (_total <= 0 || !AppScope.of(context).requireLogin(context)) return;
    final missing = takeawayStartMissing(
      _total,
      merchant.deliveryRule.startPrice,
    );
    if (missing > 0) {
      _showToast('商品金额还差￥${takeawayMoneyText(missing)}起送');
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
        lines: _displayItems
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

  Future<void> _mutateCart(
    Future<TradeCartData> Function() request, {
    String? successMessage,
  }) async {
    if (_cartUpdating) return;
    _cartUpdating = true;
    try {
      final cart = await request();
      _applyCart(cart);
      if (successMessage != null && mounted) _showToast(successMessage);
    } catch (error) {
      if (!mounted) return;
      _showToast(error.toString());
    } finally {
      _cartUpdating = false;
    }
  }

  void _applyCart(TradeCartData cart) {
    if (!mounted) return;
    setState(() {
      _cartData = cart;
      _cart
        ..clear()
        ..addEntries(
          cart.items.map((line) => MapEntry('${line.itemId}', line.quantity)),
        );
    });
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
