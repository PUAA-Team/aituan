import 'package:flutter/material.dart';

import '../../../core/widgets/app_card.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/merchant_model.dart';
import '../../home/data/backend_app_repository.dart';
import 'search_result_widgets.dart';

class SearchResultPage extends StatefulWidget {
  const SearchResultPage({super.key, required this.keyword});

  final String keyword;

  @override
  State<SearchResultPage> createState() => _SearchResultPageState();
}

class _SearchResultPageState extends State<SearchResultPage> {
  final _controller = ScrollController();
  bool _loading = true;
  bool _loadingMore = false;
  bool _usingFallback = false;
  Object? _error;
  List<MerchantModel> _merchants = const [];
  String _sort = 'default';
  BusinessType? _businessType;
  int _page = 1;
  bool _hasNext = false;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_loadMoreIfNeeded);
    _load();
  }

  @override
  void dispose() {
    _controller
      ..removeListener(_loadMoreIfNeeded)
      ..dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('搜索结果')),
    body: _loading
        ? const Center(child: CircularProgressIndicator())
        : RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              controller: _controller,
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(16),
              children: [
                SearchResultBox(keyword: widget.keyword),
                const SizedBox(height: 10),
                SearchCategoryRow(
                  selected: _businessType,
                  onSelected: _changeBusinessType,
                ),
                const SizedBox(height: 8),
                SearchFilterRow(
                  sort: _sort,
                  onSortChanged: _changeSort,
                  onLocationChanged: _load,
                ),
                const SizedBox(height: 12),
                if (_error != null)
                  _ErrorHint(message: _error.toString(), onRetry: _load),
                if (_usingFallback) const _EmptyResultHint(),
                for (final merchant in _merchants)
                  MerchantResultCard(merchant: merchant, onReturned: _load),
                if (_loadingMore)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 12),
                    child: Center(
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  ),
                if (_merchants.isEmpty && _error == null) const _NoResult(),
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
      var page = await _fetchPage(1);
      var usingFallback = false;
      if (page.list.isEmpty && widget.keyword.trim().isNotEmpty) {
        page = await backendRepository.searchStores(
          '',
          page: 1,
          pageSize: 12,
          sort: _sort,
          businessType: _businessType,
        );
        usingFallback = true;
      }
      if (!mounted) return;
      setState(() {
        _page = 1;
        _merchants = page.list;
        _hasNext = usingFallback ? false : page.hasNext;
        _usingFallback = usingFallback;
        _loadingMore = false;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loadingMore = false;
        _loading = false;
      });
    }
  }

  Future<MerchantPageData> _fetchPage(int page) =>
      backendRepository.searchStores(
        widget.keyword,
        page: page,
        pageSize: 12,
        sort: _sort,
        businessType: _businessType,
      );

  void _changeBusinessType(BusinessType? type) {
    if (_businessType == type) return;
    setState(() => _businessType = type);
    _load();
  }

  void _changeSort(String sort) {
    if (_sort == sort) return;
    setState(() => _sort = sort);
    _load();
  }

  void _loadMoreIfNeeded() {
    if (!_controller.hasClients || _loading || _loadingMore || !_hasNext) {
      return;
    }
    if (_controller.position.extentAfter < 360) {
      _loadMore();
    }
  }

  Future<void> _loadMore() async {
    setState(() => _loadingMore = true);
    try {
      final nextPage = _page + 1;
      final page = await _fetchPage(nextPage);
      if (!mounted) return;
      setState(() {
        _page = nextPage;
        _merchants = [..._merchants, ...page.list];
        _hasNext = page.hasNext;
        _loadingMore = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loadingMore = false;
      });
    }
  }
}

class _EmptyResultHint extends StatelessWidget {
  const _EmptyResultHint();

  @override
  Widget build(BuildContext context) => AppCard(
    child: Text(
      '暂无完全匹配的结果，先看看附近热门商家。',
      style: Theme.of(context).textTheme.bodyMedium,
    ),
  );
}

class _NoResult extends StatelessWidget {
  const _NoResult();

  @override
  Widget build(BuildContext context) => AppCard(
    child: Text('暂无搜索结果', style: Theme.of(context).textTheme.bodyMedium),
  );
}

class _ErrorHint extends StatelessWidget {
  const _ErrorHint({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('搜索失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
