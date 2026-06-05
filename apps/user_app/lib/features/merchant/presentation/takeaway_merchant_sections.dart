import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../home/data/backend_app_repository.dart';
import '../../review/data/review_repository.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import 'merchant_category_widgets.dart';
import 'takeaway_merchant_widgets.dart';

class TakeawayMerchantTabs extends StatelessWidget {
  const TakeawayMerchantTabs({
    super.key,
    required this.value,
    required this.onChanged,
  });

  final int value;
  final ValueChanged<int> onChanged;

  @override
  Widget build(BuildContext context) => SegmentedButton<int>(
    segments: const [
      ButtonSegment(value: 0, label: Text('下单')),
      ButtonSegment(value: 1, label: Text('评价')),
      ButtonSegment(value: 2, label: Text('商家')),
    ],
    selected: {value},
    onSelectionChanged: (values) => onChanged(values.first),
  );
}

class TakeawayOrderPanel extends StatelessWidget {
  const TakeawayOrderPanel({
    super.key,
    required this.groups,
    required this.activeCategory,
    required this.cart,
    required this.onSelected,
    required this.onAdd,
    required this.onRemove,
  });

  final Map<String, List<ItemModel>> groups;
  final String activeCategory;
  final Map<String, int> cart;
  final ValueChanged<String> onSelected;
  final ValueChanged<ItemModel> onAdd;
  final ValueChanged<ItemModel> onRemove;

  @override
  Widget build(BuildContext context) => CategoryGroupedList(
    groups: groups,
    activeCategory: activeCategory,
    emptyText: '该商家暂未上架商品',
    headerAction: '可直接加购',
    onSelected: onSelected,
    itemBuilder: (_, item) => TakeawayFoodRow(
      item: item,
      count: cart[item.id] ?? 0,
      onAdd: () => onAdd(item),
      onRemove: () => onRemove(item),
    ),
  );
}

class TakeawayReviewPanel extends StatefulWidget {
  const TakeawayReviewPanel({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  State<TakeawayReviewPanel> createState() => _TakeawayReviewPanelState();
}

class _TakeawayReviewPanelState extends State<TakeawayReviewPanel> {
  late Future<List<ReviewSummary>> _future;

  @override
  void initState() {
    super.initState();
    _future = _load();
  }

  Future<List<ReviewSummary>> _load() async {
    final storeId = int.tryParse(widget.merchant.id);
    if (storeId == null) return const [];
    return reviewRepository.fetchStoreReviews(storeId);
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<List<ReviewSummary>>(
    future: _future,
    builder: (context, snapshot) {
      if (snapshot.connectionState != ConnectionState.done) {
        return const AppCard(child: Center(child: CircularProgressIndicator()));
      }
      final reviews = snapshot.data ?? const [];
      return Column(
        children: [
          AppCard(
            child: Text(
              _ratingLine(widget.merchant, reviews),
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
          if (snapshot.hasError)
            AppCard(
              child: Text(
                '评价加载失败：${snapshot.error}',
                style: const TextStyle(color: AppColors.textSub),
              ),
            )
          else if (reviews.isEmpty)
            const AppCard(
              child: Text(
                '暂无用户评价，完成订单后可以发布第一条评价。',
                style: TextStyle(color: AppColors.textSub),
              ),
            )
          else
            for (final review in reviews) _ReviewCard(review: review),
        ],
      );
    },
  );
}

String _ratingLine(MerchantModel merchant, List<ReviewSummary> reviews) {
  final rating = merchant.rating <= 0
      ? '暂无评分'
      : '${merchant.rating.toStringAsFixed(1)}分';
  if (reviews.isEmpty) return '$rating · 暂无评价';
  final labels = reviews.expand((review) => review.labels).take(3).join(' · ');
  return labels.isEmpty
      ? '$rating · ${reviews.length}条评价'
      : '$rating · $labels';
}

class _ReviewCard extends StatelessWidget {
  const _ReviewCard({required this.review});

  final ReviewSummary review;

  @override
  Widget build(BuildContext context) => AppCard(
    child: InkWell(
      onTap: () => Navigator.pushNamed(
        context,
        Routes.reviewDetail,
        arguments: review.id,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  review.userMaskedNickname ?? '爱团用户',
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
              Text(
                '${review.rating}星',
                style: const TextStyle(
                  color: Colors.orange,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            review.content,
            style: const TextStyle(color: AppColors.textSub),
          ),
          if (review.imageUrls.isNotEmpty) ...[
            const SizedBox(height: 8),
            _ReviewImageGrid(urls: review.imageUrls),
          ],
          if (review.labels.isNotEmpty) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: [
                for (final label in review.labels)
                  Chip(
                    label: Text(label),
                    visualDensity: VisualDensity.compact,
                  ),
              ],
            ),
          ],
          if (review.replyContent != null &&
              review.replyContent!.isNotEmpty) ...[
            const SizedBox(height: 8),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: AppColors.soft,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text('商家回复：${review.replyContent!}'),
            ),
          ],
          const SizedBox(height: 8),
          Row(
            children: [
              const Icon(Icons.thumb_up_alt_outlined, size: 16, color: AppColors.textSub),
              const SizedBox(width: 4),
              Text(
                '${review.helpfulCount} 人觉得有用',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                  color: AppColors.textSub,
                ),
              ),
            ],
          ),
        ],
      ),
    ),
  );
}

class _ReviewImageGrid extends StatelessWidget {
  const _ReviewImageGrid({required this.urls});

  final List<String> urls;

  @override
  Widget build(BuildContext context) => GridView.count(
    shrinkWrap: true,
    physics: const NeverScrollableScrollPhysics(),
    crossAxisCount: 3,
    crossAxisSpacing: 6,
    mainAxisSpacing: 6,
    children: [
      for (final url in urls.take(6))
        ClipRRect(
          borderRadius: BorderRadius.circular(6),
          child: _ReviewImage(url: url),
        ),
    ],
  );
}

class _ReviewImage extends StatelessWidget {
  const _ReviewImage({required this.url});

  final String url;

  @override
  Widget build(BuildContext context) {
    final resolved = backendRepository.resolveAssetUrl(url);
    if (resolved == null) {
      return Container(color: Colors.grey.shade200);
    }
    return Image.network(
      resolved,
      fit: BoxFit.cover,
      errorBuilder: (_, _, _) => Container(
        color: Colors.grey.shade200,
        alignment: Alignment.center,
        child: const Icon(Icons.broken_image_outlined, color: Colors.grey),
      ),
    );
  }
}

class TakeawayMerchantInfoPanel extends StatelessWidget {
  const TakeawayMerchantInfoPanel({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _InfoLine(icon: Icons.place_outlined, text: merchant.address),
        _InfoLine(icon: Icons.schedule, text: '营业时间 ${merchant.businessHours}'),
        _InfoLine(
          icon: Icons.delivery_dining,
          text: merchant.deliveryRule.deliveryText.isEmpty
              ? '由商家接单后安排配送，配送费以确认订单页为准'
              : merchant.deliveryRule.deliveryText,
        ),
        _InfoLine(
          icon: Icons.receipt_long_outlined,
          text:
              '起送￥${merchant.deliveryRule.startPrice.toStringAsFixed(0)} · 配送费￥${merchant.deliveryRule.deliveryFee.toStringAsFixed(0)}',
        ),
        const SizedBox(height: 8),
        Text(merchant.summary, style: Theme.of(context).textTheme.bodyMedium),
      ],
    ),
  );
}

class _InfoLine extends StatelessWidget {
  const _InfoLine({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: Row(
      children: [
        Icon(icon, size: 18, color: AppColors.textSub),
        const SizedBox(width: 6),
        Expanded(child: Text(text)),
      ],
    ),
  );
}
