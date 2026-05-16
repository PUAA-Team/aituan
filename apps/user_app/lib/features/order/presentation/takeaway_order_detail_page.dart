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
import '../../home/data/mock_data.dart';

class TakeawayOrderDetailPage extends StatelessWidget {
  const TakeawayOrderDetailPage({super.key, required this.status});

  final OrderStatus status;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('外卖订单详情')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _StatusCard(status: status, desc: _desc, tag: _tag),
        if (status == OrderStatus.pending) const _ProgressCard(),
        _storeCard(),
        _goodsCard(),
        const AppCard(
          child: Column(
            children: [
              _Kv('商品金额', '￥43.7'),
              _Kv('配送费', '￥4.0'),
              _Kv('优惠', '-￥8.0'),
              Divider(),
              _Kv('实付', '￥39.7', strong: true),
            ],
          ),
        ),
        const SizedBox(height: 80),
      ],
    ),
    bottomNavigationBar: AppBottomActionBar(
      primaryText: _primary,
      onPrimary: _primaryAction(context),
      secondaryText: '联系客服',
      onSecondary: () {},
    ),
  );

  String get _desc => switch (status) {
    OrderStatus.unpaid => '订单已创建，请在 15 分钟内完成支付。',
    OrderStatus.pending => '商家已接单，正在准备餐品，预计 35 分钟送达。',
    _ => '订单已完成，可对本次服务进行评价。',
  };

  String get _tag => switch (status) {
    OrderStatus.unpaid => '等待付款',
    OrderStatus.pending => '配送中',
    _ => '可评价',
  };

  String get _primary => switch (status) {
    OrderStatus.used => '去评价',
    OrderStatus.unpaid => '模拟支付',
    _ => '查看商家',
  };

  VoidCallback _primaryAction(BuildContext context) => switch (status) {
    OrderStatus.used => () => Navigator.pushNamed(
      context,
      Routes.reviewPublish,
    ),
    OrderStatus.unpaid => () => Navigator.pushNamed(
      context,
      Routes.checkout,
      arguments: const CheckoutArgs(
        kind: OrderKind.takeaway,
        title: '塔斯汀中国汉堡外卖',
        amount: 39.7,
      ),
    ),
    _ => () {
      final merchant = merchantById('m1');
      Navigator.pushNamed(
        context,
        Routes.merchantDetail,
        arguments: MerchantArgs(type: merchant.type, merchant: merchant),
      );
    },
  };
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({
    required this.status,
    required this.desc,
    required this.tag,
  });

  final OrderStatus status;
  final String desc;
  final String tag;

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
              Text(status.label, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 6),
              Text(desc, style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
        BrandTag(tag, green: status == OrderStatus.pending, selected: true),
      ],
    ),
  );
}

class _ProgressCard extends StatelessWidget {
  const _ProgressCard();

  @override
  Widget build(BuildContext context) => const AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '配送履约',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
        ),
        SizedBox(height: 8),
        Text('18:12 商家已接单', style: TextStyle(color: AppColors.textMain)),
        SizedBox(height: 4),
        Text('18:18 备餐中', style: TextStyle(color: AppColors.textMain)),
        SizedBox(height: 4),
        Text('预计 18:45 送达', style: TextStyle(color: AppColors.textSub)),
      ],
    ),
  );
}

Widget _storeCard() => const AppCard(
  child: Row(
    children: [
      Icon(Icons.storefront, color: AppColors.brand),
      SizedBox(width: 8),
      Expanded(
        child: Text('塔斯汀中国汉堡', style: TextStyle(fontWeight: FontWeight.w700)),
      ),
      Text('进店'),
    ],
  ),
);

Widget _goodsCard() => const AppCard(
  child: Row(
    children: [
      MockThumb(size: 72),
      SizedBox(width: 10),
      Expanded(child: Text('招牌中国汉堡 + 单人随心配')),
      PriceText(39.7, size: 18),
    ],
  ),
);

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
