import 'package:flutter/material.dart';

import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../data/coupon_repository.dart';
import 'widgets/coupon_card.dart';

class CouponClaimPage extends StatefulWidget {
  const CouponClaimPage({super.key});

  @override
  State<CouponClaimPage> createState() => _CouponClaimPageState();
}

class _CouponClaimPageState extends State<CouponClaimPage> {
  bool _loading = true;
  bool _claiming = false;
  Object? _error;
  List<AvailableCoupon> _coupons = const [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('领券中心')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else if (_coupons.isEmpty)
            const AppCard(child: Text('暂无可领取的优惠券'))
          else
            for (final coupon in _coupons)
              AvailableCouponCard(
                coupon: coupon,
                onClaim: () => _claim(coupon.templateId),
              ),
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
      final coupons = await couponRepository.fetchAvailableCoupons();
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

  Future<void> _claim(int templateId) async {
    if (_claiming) return; // 防重复点击
    _claiming = true;
    try {
      await couponRepository.claimCoupon(templateId);
      if (!mounted) return;
      showAppSnackBar(context, '领取成功');
      await _load();
    } catch (error) {
      if (!mounted) return;
      showAppSnackBar(context, '领取失败：$error');
    } finally {
      _claiming = false;
    }
  }
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
