import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';

class CheckoutPage extends StatelessWidget {
  const CheckoutPage({super.key, required this.args});

  final CheckoutArgs args;

  @override
  Widget build(BuildContext context) {
    final isTakeaway = args.kind == OrderKind.takeaway;
    final payable = args.amount + (isTakeaway ? 4 : 0) - 8;
    return Scaffold(
      appBar: AppBar(title: const Text('确认订单')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  isTakeaway ? '收货地址' : '使用信息',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 8),
                Text(
                  isTakeaway ? '当前定位 · 示例收货地址' : '支付成功后生成券码，到店出示即可核销',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          AppCard(
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    args.title,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                PriceText(args.amount, size: 18),
              ],
            ),
          ),
          const AppCard(
            child: Row(
              children: [
                Text('优惠券'),
                Spacer(),
                Text(
                  '已优惠 ￥8',
                  style: TextStyle(
                    color: AppColors.brand,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
          const AppCard(
            child: Row(
              children: [
                Icon(Icons.payments_outlined, color: AppColors.brand),
                SizedBox(width: 8),
                Text('模拟支付', style: TextStyle(fontWeight: FontWeight.w700)),
                Spacer(),
                Icon(Icons.check_circle, color: AppColors.brand),
              ],
            ),
          ),
          AppCard(
            child: Column(
              children: [
                _FeeRow('商品金额', args.amount),
                if (isTakeaway) const _StaticFeeRow('配送费', '￥4.0'),
                const _StaticFeeRow('优惠', '-￥8.0'),
                const Divider(),
                Row(
                  children: [
                    const Text(
                      '实付',
                      style: TextStyle(fontWeight: FontWeight.w700),
                    ),
                    const Spacer(),
                    PriceText(payable, size: 22),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 90),
        ],
      ),
      bottomNavigationBar: AppBottomActionBar(
        primaryText: '提交并支付',
        onPrimary: () => _pay(context),
        price: payable,
        note: '模拟支付',
      ),
    );
  }

  void _pay(BuildContext context) {
    final status = args.kind == OrderKind.takeaway
        ? OrderStatus.pending
        : OrderStatus.unused;
    Navigator.pushReplacementNamed(
      context,
      Routes.orderDetail,
      arguments: OrderDetailArgs(kind: args.kind, status: status),
    );
  }
}

class _FeeRow extends StatelessWidget {
  const _FeeRow(this.label, this.value);

  final String label;
  final double value;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      children: [Text(label), const Spacer(), PriceText(value, size: 16)],
    ),
  );
}

class _StaticFeeRow extends StatelessWidget {
  const _StaticFeeRow(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(children: [Text(label), const Spacer(), Text(value)]),
  );
}
