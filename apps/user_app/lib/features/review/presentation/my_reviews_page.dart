import 'package:flutter/material.dart';

import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../data/review_repository.dart';

class MyReviewsPage extends StatefulWidget {
  const MyReviewsPage({super.key});

  @override
  State<MyReviewsPage> createState() => _MyReviewsPageState();
}

class _MyReviewsPageState extends State<MyReviewsPage> {
  Future<List<ReviewSummary>>? _future;
  String? _statusFilter;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    setState(() {
      _future = reviewRepository.fetchMyReviews(status: _statusFilter);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('我的评价'),
        actions: [
          PopupMenuButton<String?>(
            initialValue: _statusFilter,
            onSelected: (v) {
              _statusFilter = v;
              _load();
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: null, child: Text('全部')),
              PopupMenuItem(value: 'published', child: Text('已发布')),
              PopupMenuItem(value: 'hidden', child: Text('已隐藏')),
            ],
            icon: const Icon(Icons.filter_list),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async => _load(),
        child: FutureBuilder<List<ReviewSummary>>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return _ErrorView(message: '加载失败：${snapshot.error}', onRetry: _load);
            }
            final list = snapshot.data ?? const [];
            if (list.isEmpty) {
              return const Center(child: Text('暂无评价'));
            }
            return ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: list.length,
              itemBuilder: (_, i) => _ReviewListItem(
                review: list[i],
                onTap: () async {
                  await Navigator.pushNamed(
                    context,
                    Routes.reviewDetail,
                    arguments: list[i].id,
                  );
                  _load();
                },
              ),
            );
          },
        ),
      ),
    );
  }
}

class _ReviewListItem extends StatelessWidget {
  const _ReviewListItem({required this.review, required this.onTap});

  final ReviewSummary review;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: InkWell(
        onTap: onTap,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(child: Text(review.storeName, style: Theme.of(context).textTheme.titleMedium)),
                _StatusBadge(status: review.status),
              ],
            ),
            const SizedBox(height: 4),
            Text(review.orderTitle, style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: 8),
            Row(
              children: [
                for (var i = 0; i < 5; i++)
                  Icon(i < review.rating ? Icons.star : Icons.star_border, size: 16, color: Colors.orange),
                const SizedBox(width: 8),
                Text(review.createdAt.substring(0, review.createdAt.length >= 10 ? 10 : review.createdAt.length)),
              ],
            ),
            const SizedBox(height: 6),
            Text(review.content, maxLines: 2, overflow: TextOverflow.ellipsis),
            if (review.replyContent != null && review.replyContent!.isNotEmpty) ...[
              const SizedBox(height: 6),
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text('商家回复：${review.replyContent!}', style: Theme.of(context).textTheme.bodySmall),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status});
  final String status;

  @override
  Widget build(BuildContext context) {
    final (label, color) = switch (status) {
      'hidden' => ('已隐藏', Colors.grey),
      _ => ('已发布', Colors.green),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: color.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(4)),
      child: Text(label, style: TextStyle(color: color, fontSize: 12)),
    );
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(message),
          const SizedBox(height: 12),
          OutlinedButton(onPressed: onRetry, child: const Text('重试')),
        ],
      ),
    );
  }
}
