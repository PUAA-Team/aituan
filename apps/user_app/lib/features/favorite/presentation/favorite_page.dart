import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/section_header.dart';
import '../../home/data/backend_app_repository.dart';

class FavoritePage extends StatefulWidget {
  const FavoritePage({super.key});

  @override
  State<FavoritePage> createState() => _FavoritePageState();
}

class _FavoritePageState extends State<FavoritePage> {
  static const _tabs = [
    ('', '全部'),
    ('store', '店铺'),
    ('item', '商品'),
    ('service', '服务'),
  ];

  bool _loading = true;
  Object? _error;
  String _type = '';
  List<FavoriteEntry> _favorites = const [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('我的收藏')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          const SectionHeader(title: '收藏列表'),
          const SizedBox(height: 8),
          _TypeTabs(value: _type, tabs: _tabs, onChanged: _switchType),
          const SizedBox(height: 8),
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else if (_favorites.isEmpty)
            AppCard(child: Text(_emptyText))
          else
            for (final favorite in _favorites)
              _FavoriteCard(
                favorite: favorite,
                onTap: () => _openFavorite(favorite),
                onDelete: () => _deleteFavorite(favorite),
              ),
        ],
      ),
    ),
  );

  String get _emptyText =>
      _type == 'service' ? '服务收藏依赖后续服务模型扩展，当前暂无数据' : '暂无收藏';

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final favorites = await backendRepository.fetchFavorites(
        favoriteType: _type,
      );
      if (!mounted) return;
      setState(() {
        _favorites = favorites;
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

  Future<void> _switchType(String type) async {
    if (_type == type) return;
    setState(() => _type = type);
    await _load();
  }

  Future<void> _deleteFavorite(FavoriteEntry favorite) async {
    final id = int.tryParse(favorite.targetId);
    if (id == null) {
      showAppSnackBar(context, '收藏目标信息不完整');
      return;
    }
    try {
      await backendRepository.deleteFavorite(
        favoriteType: favorite.favoriteType,
        targetId: id,
      );
      if (!mounted) return;
      setState(() {
        _favorites = _favorites
            .where(
              (entry) =>
                  entry.favoriteType != favorite.favoriteType ||
                  entry.targetId != favorite.targetId,
            )
            .toList();
      });
      showAppSnackBar(context, '已取消收藏');
    } catch (error) {
      if (!mounted) return;
      showAppSnackBar(context, '取消收藏失败：$error');
    }
  }

  Future<void> _openFavorite(FavoriteEntry favorite) async {
    final id = int.tryParse(favorite.targetId);
    if (id == null) {
      showAppSnackBar(context, '收藏目标信息不完整');
      return;
    }
    final type = favorite.favoriteType.toLowerCase();
    try {
      if (_looksLikeMerchant(type)) {
        final merchant = await backendRepository.fetchStore(id);
        if (!mounted) return;
        await Navigator.pushNamed(
          context,
          Routes.merchantDetail,
          arguments: MerchantArgs(type: merchant.type, merchant: merchant),
        );
        if (mounted) _load();
        return;
      }
      final detail = await backendRepository.fetchItem(id);
      if (!mounted) return;
      await Navigator.pushNamed(
        context,
        Routes.itemDetail,
        arguments: ItemArgs(detail.item),
      );
      if (mounted) _load();
    } catch (_) {
      try {
        final merchant = await backendRepository.fetchStore(id);
        if (!mounted) return;
        await Navigator.pushNamed(
          context,
          Routes.merchantDetail,
          arguments: MerchantArgs(type: merchant.type, merchant: merchant),
        );
        if (mounted) _load();
      } catch (error) {
        if (!mounted) return;
        showAppSnackBar(context, '打开收藏失败：$error');
      }
    }
  }

  bool _looksLikeMerchant(String type) =>
      type.contains('merchant') ||
      type.contains('store') ||
      type.contains('shop') ||
      type.contains('merchant_store');
}

class _TypeTabs extends StatelessWidget {
  const _TypeTabs({
    required this.value,
    required this.tabs,
    required this.onChanged,
  });

  final String value;
  final List<(String, String)> tabs;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) => SingleChildScrollView(
    scrollDirection: Axis.horizontal,
    child: Row(
      children: [
        for (final tab in tabs)
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              label: Text(tab.$2),
              selected: value == tab.$1,
              onSelected: (_) => onChanged(tab.$1),
            ),
          ),
      ],
    ),
  );
}

class _FavoriteCard extends StatelessWidget {
  const _FavoriteCard({
    required this.favorite,
    required this.onTap,
    required this.onDelete,
  });

  final FavoriteEntry favorite;
  final VoidCallback onTap;
  final VoidCallback onDelete;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onTap,
    child: Row(
      children: [
        MockThumb(
          size: 72,
          icon: _looksLikeStore ? Icons.storefront : Icons.local_activity,
          label: _typeLabel,
          imageUrl: favorite.coverUrl,
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                favorite.targetName,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 4),
              Text(
                favorite.subtitle ?? '收藏于爱团',
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 8),
              BrandTag(_typeLabel, emphasis: true),
            ],
          ),
        ),
        IconButton(
          tooltip: '取消收藏',
          onPressed: onDelete,
          icon: const Icon(Icons.favorite, color: AppColors.brand),
        ),
      ],
    ),
  );

  bool get _looksLikeStore =>
      favorite.favoriteType.toLowerCase().contains('store');

  String get _typeLabel => switch (favorite.favoriteType.toLowerCase()) {
    'store' || 'merchant_store' => '店铺',
    'item' => '商品',
    'service' => '服务',
    _ => favorite.favoriteType,
  };
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
        Text('收藏加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
