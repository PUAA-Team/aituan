import 'package:flutter/material.dart';

import '../../../core/widgets/app_card.dart';
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
  bool _loading = true;
  bool _usingFallback = false;
  Object? _error;
  List<MerchantModel> _merchants = const [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('搜索结果')),
    body: _loading
        ? const Center(child: CircularProgressIndicator())
        : RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(16),
              children: [
                SearchResultBox(keyword: widget.keyword),
                const SizedBox(height: 10),
                const SearchCategoryRow(),
                const SizedBox(height: 8),
                SearchFilterRow(onLocationChanged: _load),
                const SizedBox(height: 12),
                if (_error != null)
                  _ErrorHint(message: _error.toString(), onRetry: _load),
                if (_usingFallback) const _EmptyResultHint(),
                for (final merchant in _merchants)
                  MerchantResultCard(merchant: merchant, onReturned: _load),
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
      var merchants = await backendRepository.searchStores(widget.keyword);
      var usingFallback = false;
      if (merchants.isEmpty && widget.keyword.trim().isNotEmpty) {
        merchants = await backendRepository.searchStores('');
        usingFallback = true;
      }
      if (!mounted) return;
      setState(() {
        _merchants = merchants;
        _usingFallback = usingFallback;
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
