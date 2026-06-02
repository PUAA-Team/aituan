import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../coupon/data/coupon_repository.dart';

class CouponSelectorPage extends StatefulWidget {
  const CouponSelectorPage({super.key, required this.orderAmount});

  final double orderAmount;

  @override
  State<CouponSelectorPage> createState() => _CouponSelectorPageState();
}

class _CouponSelectorPageState extends State<CouponSelectorPage> {
  bool _loading = true;
  Object? _error;
  List<OrderCouponOption> _coupons = const [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('选择优惠券')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          AppCard(
            backgroundColor: AppColors.brandSoft,
            borderColor: AppColors.brandLine,
            child: Text(
              '当前订单金额 ¥${widget.orderAmount.toStringAsFixed(2)}，选择优惠券后将重新试算优惠与实付金额。',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ),
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else if (_coupons.isEmpty)
            const AppCard(child: Text('暂无可用于当前订单的优惠券'))
          else
            for (final coupon in _coupons)
              _CouponOptionCard(coupon: coupon, onTap: () => _select(coupon)),
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
      final coupons = await couponRepository.fetchUsableForOrder(
        widget.orderAmount,
      );
      if (!mounted) return;
      setState(() {
        _coupons = coupons;
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

  void _select(OrderCouponOption coupon) {
    if (!coupon.usable) return;
    Navigator.pop(context, coupon);
  }
}

class _CouponOptionCard extends StatelessWidget {
  const _CouponOptionCard({required this.coupon, required this.onTap});

  final OrderCouponOption coupon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: coupon.usable ? onTap : null,
    backgroundColor: coupon.usable ? Colors.white : AppColors.soft,
    child: Row(
      children: [
        Container(
          width: 72,
          height: 72,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: coupon.usable ? AppColors.brand : AppColors.textLight,
            borderRadius: BorderRadius.circular(14),
          ),
          child: Text(
            coupon.discountDesc,
            textAlign: TextAlign.center,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(coupon.name, style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 4),
              Text(
                coupon.usable
                    ? '预计优惠 ¥${coupon.discountAmount.toStringAsFixed(2)}'
                    : (coupon.reason ?? '当前订单不可用'),
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ),
        Icon(
          coupon.usable ? Icons.chevron_right : Icons.block,
          color: AppColors.textLight,
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
        Text('优惠券加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
