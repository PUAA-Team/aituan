import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/order_model.dart';
import '../../home/data/mock_data.dart';

class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key});

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends State<OrdersPage> {
  OrderStatus? _filter;

  @override
  Widget build(BuildContext context) {
    final list = _filter == null
        ? orders
        : orders.where((order) => order.status == _filter).toList();
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('我的订单', style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 12),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _StatusChip(
                  label: '全部',
                  active: _filter == null,
                  onTap: () => setState(() => _filter = null),
                ),
                for (final status in OrderStatus.values)
                  _StatusChip(
                    label: status.label,
                    active: _filter == status,
                    onTap: () => setState(() => _filter = status),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          for (final order in list) _OrderCard(order: order),
        ],
      ),
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({
    required this.label,
    required this.active,
    required this.onTap,
  });

  final String label;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(right: 8),
    child: InkWell(
      borderRadius: BorderRadius.circular(6),
      onTap: onTap,
      child: BrandTag(label, selected: active),
    ),
  );
}

class _OrderCard extends StatelessWidget {
  const _OrderCard({required this.order});

  final OrderModel order;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: () => Navigator.pushNamed(
      context,
      Routes.orderDetail,
      arguments: OrderDetailArgs(kind: order.kind, status: order.status),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                order.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            BrandTag(
              order.status.label,
              emphasis: order.status == OrderStatus.unpaid,
              green: order.status == OrderStatus.unused,
            ),
          ],
        ),
        const SizedBox(height: 6),
        Text(
          order.desc,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: Theme.of(context).textTheme.bodySmall,
        ),
        const SizedBox(height: 10),
        Row(
          children: [
            BrandTag(order.businessType.label),
            const Spacer(),
            Text(
              _hint(order.status),
              style: const TextStyle(fontSize: 12, color: AppColors.textSub),
            ),
            const SizedBox(width: 8),
            PriceText(order.amount, size: 18),
          ],
        ),
      ],
    ),
  );

  String _hint(OrderStatus status) => switch (status) {
    OrderStatus.unpaid => '等待付款 ›',
    OrderStatus.pending => '正在进行 ›',
    OrderStatus.unused => '待核销 ›',
    OrderStatus.used => '去评价 ›',
  };
}
