import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
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
  bool _loading = true;
  Object? _error;
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
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else if (_favorites.isEmpty)
            const AppCard(child: Text('暂无收藏'))
          else
            for (final favorite in _favorites)
              _FavoriteCard(
                favorite: favorite,
                onTap: () => _openFavorite(favorite),
              ),
        ],
      ),
    ),
  );

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final favorites = await backendRepository.fetchFavorites();
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

  Future<void> _openFavorite(FavoriteEntry favorite) async {
    final id = int.tryParse(favorite.targetId);
    if (id == null) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('收藏目标信息不完整')));
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
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('打开收藏失败：$error')));
      }
    }
  }

  bool _looksLikeMerchant(String type) =>
      type.contains('merchant') ||
      type.contains('store') ||
      type.contains('shop') ||
      type.contains('merchant_store');
}

class _FavoriteCard extends StatelessWidget {
  const _FavoriteCard({required this.favorite, required this.onTap});

  final FavoriteEntry favorite;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onTap,
    child: Row(
      children: [
        MockThumb(
          size: 72,
          icon: favorite.favoriteType.toLowerCase().contains('store')
              ? Icons.storefront
              : Icons.local_activity,
          label: favorite.favoriteType,
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
            ],
          ),
        ),
        const SizedBox(width: 8),
        BrandTag(favorite.favoriteType, emphasis: true),
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
        Text('收藏加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
