import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../shared/enums/business_type.dart';
import '../../home/data/backend_app_repository.dart';
import 'takeaway_fulfillment_text.dart';

class DeliveryTrackingPage extends StatefulWidget {
  const DeliveryTrackingPage({super.key, required this.args});

  final OrderDetailArgs args;

  @override
  State<DeliveryTrackingPage> createState() => _DeliveryTrackingPageState();
}

class _DeliveryTrackingPageState extends State<DeliveryTrackingPage> {
  OrderDetailData? _detail;
  Object? _error;
  bool _loading = true;

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
      appBar: AppBar(title: const Text('配送跟踪')),
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
              _TrackingHeader(detail: detail),
              const SizedBox(height: 12),
              _TimelineCard(detail: detail),
              const SizedBox(height: 12),
              _DeliveryInfoCard(detail: detail),
            ],
          ],
        ),
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
}

class _TrackingHeader extends StatelessWidget {
  const _TrackingHeader({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    backgroundColor: AppColors.brandSoft,
    borderColor: AppColors.brandLine,
    child: Row(
      children: [
        Container(
          width: 44,
          height: 44,
          decoration: BoxDecoration(
            color: AppColors.brand.withValues(alpha: 0.12),
            borderRadius: BorderRadius.circular(12),
          ),
          child: const Icon(Icons.delivery_dining, color: AppColors.brand),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                takeawayStatusLabel(detail.status, detail.fulfillmentStatus),
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 4),
              Text(
                takeawayStatusDescription(
                  detail.status,
                  detail.fulfillmentStatus,
                ),
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ),
        BrandTag(
          takeawayStatusTag(detail.status, detail.fulfillmentStatus),
          green: detail.status == OrderStatus.pending,
          selected: true,
        ),
      ],
    ),
  );
}

class _TimelineCard extends StatelessWidget {
  const _TimelineCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) {
    final nodes = detail.deliveryTimeline
        .where(
          (node) =>
              node.reachedAt != null || node.code == detail.fulfillmentStatus,
        )
        .toList();
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('履约时间线', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 12),
          if (nodes.isEmpty)
            const Text('支付后会生成配送时间线。')
          else
            for (var i = 0; i < nodes.length; i++)
              _TimelineRow(
                node: nodes[i],
                isLast: i == nodes.length - 1,
                isCurrent: nodes[i].code == detail.fulfillmentStatus,
              ),
        ],
      ),
    );
  }
}

class _TimelineRow extends StatelessWidget {
  const _TimelineRow({
    required this.node,
    required this.isLast,
    required this.isCurrent,
  });

  final TimelineNodeData node;
  final bool isLast;
  final bool isCurrent;

  @override
  Widget build(BuildContext context) {
    final reached = node.reachedAt != null;
    final color = reached ? AppColors.brand : AppColors.line;
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              Container(
                width: 18,
                height: 18,
                decoration: BoxDecoration(
                  color: reached ? AppColors.brand : Colors.white,
                  shape: BoxShape.circle,
                  border: Border.all(color: color, width: 2),
                ),
                child: reached
                    ? const Icon(Icons.check, size: 12, color: Colors.white)
                    : null,
              ),
              if (!isLast)
                Expanded(child: Container(width: 2, color: AppColors.line)),
            ],
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          node.text,
                          style: TextStyle(
                            fontWeight: isCurrent
                                ? FontWeight.w800
                                : FontWeight.w600,
                            color: reached
                                ? AppColors.textMain
                                : AppColors.textSub,
                          ),
                        ),
                      ),
                      Text(
                        formatTimelineTime(node.reachedAt),
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                  if (isCurrent) ...[
                    const SizedBox(height: 4),
                    const Text(
                      '当前进度',
                      style: TextStyle(color: AppColors.brand, fontSize: 12),
                    ),
                  ],
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _DeliveryInfoCard extends StatelessWidget {
  const _DeliveryInfoCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('订单信息', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 10),
        _InfoRow('订单编号', detail.orderNo),
        _InfoRow('商家', detail.storeName),
        if (detail.addressSnapshot != null)
          _InfoRow('收货地址', detail.addressSnapshot!),
        if (detail.remark != null) _InfoRow('备注', detail.remark!),
      ],
    ),
  );
}

class _InfoRow extends StatelessWidget {
  const _InfoRow(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 72,
          child: Text(label, style: const TextStyle(color: AppColors.textSub)),
        ),
        Expanded(child: Text(value)),
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
        Text('配送跟踪加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
