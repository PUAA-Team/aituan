import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../home/data/backend_app_repository.dart';
import 'takeaway_fulfillment_text.dart';

class TakeawayOrderDetailPage extends StatefulWidget {
  const TakeawayOrderDetailPage({super.key, required this.args});

  final OrderDetailArgs args;

  @override
  State<TakeawayOrderDetailPage> createState() =>
      _TakeawayOrderDetailPageState();
}

class _TakeawayOrderDetailPageState extends State<TakeawayOrderDetailPage> {
  OrderDetailData? _detail;
  Object? _error;
  bool _loading = true;
  bool _paying = false;

  String? get _orderId => widget.args.orderId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    return Scaffold(
      appBar: AppBar(title: const Text('外卖订单详情')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            if (_orderId == null)
              const AppCard(child: Text('订单信息缺失，请从订单列表重新进入。'))
            else if (_loading)
              const AppCard(child: Center(child: CircularProgressIndicator()))
            else if (_error != null)
              _ErrorCard(message: _error.toString(), onRetry: _load)
            else if (detail != null) ...[
              _StatusCard(
                title: takeawayStatusLabel(
                  detail.status,
                  detail.fulfillmentStatus,
                ),
                desc: takeawayStatusDescription(
                  detail.status,
                  detail.fulfillmentStatus,
                ),
                tag: takeawayStatusTag(detail.status, detail.fulfillmentStatus),
                active: detail.status == OrderStatus.pending,
              ),
              if (detail.deliveryTimeline.isNotEmpty)
                _ProgressCard(nodes: detail.deliveryTimeline),
              _StoreCard(detail: detail),
              _GoodsCard(detail: detail),
              _FeeCard(detail: detail),
            ],
            const SizedBox(height: 80),
          ],
        ),
      ),
      bottomNavigationBar: detail == null
          ? null
          : AppBottomActionBar(
              primaryText: _primaryText(detail.status),
              onPrimary: _paying ? null : () => _primaryAction(detail),
              secondaryText: detail.deliveryTimeline.isEmpty ? '刷新状态' : '配送跟踪',
              onSecondary: detail.deliveryTimeline.isEmpty
                  ? _load
                  : () => _openDeliveryTracking(detail),
            ),
    );
  }

  Future<void> _load() async {
    final orderId = _orderId;
    if (orderId == null) {
      setState(() => _loading = false);
      return;
    }
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final detail = await backendRepository.fetchOrderDetail(orderId);
      if (!mounted) return;
      setState(() {
        _detail = detail;
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

  Future<void> _pay(OrderDetailData detail) async {
    try {
      setState(() => _paying = true);
      final paid = await backendRepository.payOrder(detail.id);
      if (!mounted) return;
      setState(() {
        _detail = paid;
        _paying = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _paying = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('支付失败：$error')));
    }
  }

  void _primaryAction(OrderDetailData detail) {
    if (detail.status == OrderStatus.unpaid) {
      _pay(detail);
      return;
    }
    if (detail.status == OrderStatus.used) {
      Navigator.pushNamed(
        context,
        Routes.reviewPublish,
        arguments: ReviewArgs(title: detail.title, orderId: detail.id),
      );
      return;
    }
    Navigator.pushNamed(
      context,
      Routes.searchResult,
      arguments: SearchArgs(detail.storeName),
    );
  }

  Future<void> _openDeliveryTracking(OrderDetailData detail) async {
    await Navigator.pushNamed(
      context,
      Routes.deliveryTracking,
      arguments: OrderDetailArgs(
        kind: OrderKind.takeaway,
        status: detail.status,
        orderId: detail.id,
      ),
    );
    await _load();
  }

  String _primaryText(OrderStatus status) => switch (status) {
    OrderStatus.unpaid => _paying ? '支付中' : '模拟支付',
    OrderStatus.used => '去评价',
    _ => '查看商家',
  };
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({
    required this.title,
    required this.desc,
    required this.tag,
    required this.active,
  });

  final String title;
  final String desc;
  final String tag;
  final bool active;

  @override
  Widget build(BuildContext context) => AppCard(
    backgroundColor: AppColors.brandSoft,
    borderColor: AppColors.brandLine,
    child: Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 6),
              Text(desc, style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
        BrandTag(tag, green: active, selected: true),
      ],
    ),
  );
}

class _ProgressCard extends StatelessWidget {
  const _ProgressCard({required this.nodes});

  final List<TimelineNodeData> nodes;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          '配送履约',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 8),
        for (final node in nodes)
          Padding(
            padding: const EdgeInsets.only(bottom: 4),
            child: Text(
              _timelineText(node),
              style: const TextStyle(color: AppColors.textMain),
            ),
          ),
      ],
    ),
  );

  String _timelineText(TimelineNodeData node) {
    final time = node.reachedAt == null
        ? ''
        : '${formatTimelineTime(node.reachedAt)} ';
    return '$time${node.text}';
  }
}

class _StoreCard extends StatelessWidget {
  const _StoreCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: () => Navigator.pushNamed(
      context,
      Routes.searchResult,
      arguments: SearchArgs(detail.storeName),
    ),
    child: Row(
      children: [
        const Icon(Icons.storefront, color: AppColors.brand),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            detail.storeName,
            style: const TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
        const Text('进店'),
      ],
    ),
  );
}

class _GoodsCard extends StatelessWidget {
  const _GoodsCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      children: [
        for (final item in detail.items)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              children: [
                MockThumb(size: 72, label: item.categoryName),
                const SizedBox(width: 10),
                Expanded(child: Text('${item.itemName} ×${item.quantity}')),
                PriceText(item.totalPrice, size: 18),
              ],
            ),
          ),
      ],
    ),
  );
}

class _FeeCard extends StatelessWidget {
  const _FeeCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      children: [
        _Kv('商品金额', _money(detail.amount)),
        if (detail.deliveryFee > 0) _Kv('配送费', _money(detail.deliveryFee)),
        if (detail.discountAmount > 0)
          _Kv('优惠', '-${_money(detail.discountAmount)}'),
        const Divider(),
        _Kv('实付', _money(detail.payableAmount), strong: true),
      ],
    ),
  );

  String _money(double value) =>
      '￥${value % 1 == 0 ? value.toInt() : value.toStringAsFixed(1)}';
}

class _Kv extends StatelessWidget {
  const _Kv(this.k, this.v, {this.strong = false});

  final String k;
  final String v;
  final bool strong;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      children: [
        Text(k),
        const Spacer(),
        Text(
          v,
          style: TextStyle(
            fontWeight: strong ? FontWeight.w800 : FontWeight.w400,
            color: strong ? AppColors.brand : AppColors.textMain,
          ),
        ),
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
        Text('订单详情加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
