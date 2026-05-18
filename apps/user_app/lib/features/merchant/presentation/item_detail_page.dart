import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../core/widgets/section_header.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../home/data/backend_app_repository.dart';
import 'merchant_item_cards.dart';

class ItemDetailPage extends StatefulWidget {
  const ItemDetailPage({super.key, required this.item});

  final ItemModel item;

  @override
  State<ItemDetailPage> createState() => _ItemDetailPageState();
}

class _ItemDetailPageState extends State<ItemDetailPage> {
  ItemDetailData? _detail;
  Object? _error;
  bool _loading = true;
  bool _favorited = false;
  bool _favoriteBusy = false;

  bool get _canLoad => int.tryParse(widget.item.id) != null;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    final item = detail?.item ?? widget.item;
    final related = detail == null
        ? const <ItemModel>[]
        : detail.itemGroups
              .expand((group) => group.items)
              .where((entry) => entry.id != item.id)
              .take(8)
              .toList();
    return Scaffold(
      appBar: AppBar(title: const Text('商品/服务详情')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            if (!_canLoad)
              const AppCard(child: Text('商品信息不完整，请从搜索或商家页重新进入。'))
            else if (_loading)
              const AppCard(child: Center(child: CircularProgressIndicator()))
            else if (_error != null)
              _ErrorCard(message: _error.toString(), onRetry: _load)
            else if (detail != null) ...[
              MockThumb(
                width: double.infinity,
                height: 190,
                icon: businessIcon(item.type),
                label: item.category,
              ),
              const SizedBox(height: 10),
              _InfoCard(item: item, categories: detail.categories),
              _MerchantCard(detail: detail),
              const SectionHeader(title: '同店推荐', action: '更多选择'),
              if (related.isEmpty)
                const AppCard(child: Text('暂无更多推荐'))
              else
                MerchantItemCarousel(items: related, onTap: _openItem),
            ],
            const SizedBox(height: 80),
          ],
        ),
      ),
      bottomNavigationBar: detail == null
          ? null
          : AppBottomActionBar(
              primaryText: '立即购买',
              onPrimary: () => _buy(context, detail),
              secondaryText: _favoriteBusy
                  ? '处理中'
                  : (_favorited ? '取消收藏' : '收藏'),
              onSecondary: _favoriteBusy ? null : () => _toggleFavorite(detail),
              price: item.price,
            ),
    );
  }

  Future<void> _load() async {
    if (!_canLoad) {
      setState(() => _loading = false);
      return;
    }
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final detail = await backendRepository.fetchItem(
        int.parse(widget.item.id),
      );
      final favorited = await _loadFavoriteState(detail.item.id);
      if (!mounted) return;
      setState(() {
        _detail = detail;
        _favorited = favorited;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loading = false;
      });
    }
  }

  Future<bool> _loadFavoriteState(String itemId) async {
    if (!appState.isLoggedIn) return false;
    try {
      final favorites = await backendRepository.fetchFavorites(
        favoriteType: 'item',
      );
      return favorites.any(
        (favorite) =>
            favorite.favoriteType.toLowerCase() == 'item' &&
            favorite.targetId == itemId,
      );
    } catch (_) {
      return false;
    }
  }

  Future<void> _toggleFavorite(ItemDetailData detail) async {
    if (!AppScope.of(context).requireLogin(context)) return;
    final targetId = int.parse(detail.item.id);
    try {
      setState(() => _favoriteBusy = true);
      if (_favorited) {
        await backendRepository.deleteFavorite(
          favoriteType: 'item',
          targetId: targetId,
        );
      } else {
        await backendRepository.saveFavorite(
          favoriteType: 'item',
          targetId: targetId,
          targetName: detail.item.title,
          subtitle: detail.item.subtitle,
        );
      }
      if (!mounted) return;
      setState(() {
        _favorited = !_favorited;
        _favoriteBusy = false;
      });
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(_favorited ? '已加入收藏' : '已取消收藏')));
    } catch (error) {
      if (!mounted) return;
      setState(() => _favoriteBusy = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('收藏操作失败：$error')));
    }
  }

  void _openItem(ItemModel item) {
    Navigator.pushNamed(context, Routes.itemDetail, arguments: ItemArgs(item));
  }

  void _buy(BuildContext context, ItemDetailData detail) {
    if (!AppScope.of(context).requireLogin(context)) return;
    final item = detail.item;
    Navigator.pushNamed(
      context,
      Routes.checkout,
      arguments: CheckoutArgs(
        kind: item.type.isTakeaway ? OrderKind.takeaway : OrderKind.service,
        title: item.title,
        amount: item.price,
        storeId: detail.merchant.id,
        businessType: item.type,
        lines: [
          CheckoutLineArg(
            itemId: item.id,
            quantity: 1,
            title: item.title,
            subtitle: item.subtitle,
            unitPrice: item.price,
            categoryName: item.category,
          ),
        ],
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  const _InfoCard({required this.item, required this.categories});

  final ItemModel item;
  final List<CategoryData> categories;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        PriceText(item.price, size: 26),
        const SizedBox(height: 8),
        Text(item.title, style: Theme.of(context).textTheme.headlineMedium),
        const SizedBox(height: 8),
        Text(item.subtitle, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        Wrap(
          spacing: 6,
          runSpacing: 6,
          children: [
            for (final tag in item.tags) BrandTag(tag, emphasis: true),
            for (final category in categories) BrandTag(category.name),
          ],
        ),
      ],
    ),
  );
}

class _MerchantCard extends StatelessWidget {
  const _MerchantCard({required this.detail});

  final ItemDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: () => Navigator.pushNamed(
      context,
      Routes.merchantDetail,
      arguments: MerchantArgs(
        type: detail.merchant.type,
        merchant: detail.merchant,
      ),
    ),
    child: Row(
      children: [
        const Icon(Icons.storefront, color: AppColors.brand),
        const SizedBox(width: 8),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                detail.merchant.name,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 4),
              Text(
                detail.merchant.address,
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ),
        const Icon(Icons.chevron_right, color: AppColors.textSub),
      ],
    ),
  );
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('商品加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
