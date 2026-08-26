import 'package:flutter/material.dart';

import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../data/coupon_repository.dart';
import 'widgets/coupon_card.dart';

class CouponPage extends StatelessWidget {
  const CouponPage({super.key});

  @override
  Widget build(BuildContext context) => DefaultTabController(
    length: 3,
    child: Scaffold(
      appBar: AppBar(
        title: const Text('我的优惠券'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pushNamed(context, Routes.couponClaim),
            child: const Text('领券中心'),
          ),
        ],
        bottom: const TabBar(
          tabs: [
            Tab(text: '可用'),
            Tab(text: '已用'),
            Tab(text: '失效'),
          ],
        ),
      ),
      body: const TabBarView(
        children: [
          _CouponListTab(status: 'usable'),
          _CouponListTab(status: 'used'),
          _CouponListTab(status: 'expired'),
        ],
      ),
    ),
  );
}

// 单个状态下的我的券列表，切 Tab 保持已加载状态
class _CouponListTab extends StatefulWidget {
  const _CouponListTab({required this.status});

  final String status;

  @override
  State<_CouponListTab> createState() => _CouponListTabState();
}

class _CouponListTabState extends State<_CouponListTab>
    with AutomaticKeepAliveClientMixin {
  bool _loading = true;
  Object? _error;
  List<UserCoupon> _coupons = const [];

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    return RefreshIndicator(onRefresh: _load, child: _content());
  }

  Widget _content() {
    const padding = EdgeInsets.all(16);
    const physics = AlwaysScrollableScrollPhysics();
    if (_loading) {
      return ListView(
        physics: physics,
        padding: padding,
        children: const [
          AppCard(child: Center(child: CircularProgressIndicator())),
        ],
      );
    }
    if (_error != null) {
      return ListView(
        physics: physics,
        padding: padding,
        children: [_ErrorCard(message: _error.toString(), onRetry: _load)],
      );
    }
    if (_coupons.isEmpty) {
      return ListView(
        physics: physics,
        padding: padding,
        children: const [AppCard(child: Text('暂无优惠券'))],
      );
    }
    return ListView(
      physics: physics,
      padding: padding,
      children: [for (final coupon in _coupons) UserCouponCard(coupon: coupon)],
    );
  }

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final coupons = await couponRepository.fetchMyCoupons(widget.status);
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
