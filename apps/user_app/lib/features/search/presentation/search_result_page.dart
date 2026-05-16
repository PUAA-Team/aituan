import 'package:flutter/material.dart';

import '../../../core/widgets/app_card.dart';
import '../../home/data/mock_data.dart';
import 'search_result_widgets.dart';

class SearchResultPage extends StatelessWidget {
  const SearchResultPage({super.key, required this.keyword});

  final String keyword;

  @override
  Widget build(BuildContext context) {
    final results = searchMerchants(keyword);
    final displayMerchants = results.isEmpty ? merchants : results;
    return Scaffold(
      appBar: AppBar(title: const Text('搜索结果')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          SearchResultBox(keyword: keyword),
          const SizedBox(height: 10),
          const SearchCategoryRow(),
          const SizedBox(height: 8),
          const SearchFilterRow(),
          const SizedBox(height: 12),
          if (results.isEmpty && keyword.trim().isNotEmpty)
            const _EmptyResultHint(),
          for (final merchant in displayMerchants)
            MerchantResultCard(merchant: merchant),
        ],
      ),
    );
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
