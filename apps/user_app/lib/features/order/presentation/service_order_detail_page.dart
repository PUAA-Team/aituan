import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../home/data/mock_data.dart';
import 'service_order_detail_widgets.dart';

class ServiceOrderDetailPage extends StatelessWidget {
  const ServiceOrderDetailPage({super.key, required this.status});

  final OrderStatus status;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('订单详情')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        ServiceOrderStatusCard(status: status, desc: _desc),
        if (status != OrderStatus.unpaid)
          ServiceVoucherCard(used: status == OrderStatus.used),
        _storeCard(),
        _goodsCard(),
        const AppCard(
          child: Column(
            children: [
              ServiceKv('使用时间', '11:00-21:00'),
              ServiceKv('退款规则', '未使用可退'),
              ServiceKv('预约要求', '高峰建议电话确认'),
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
    OrderStatus.unused => '支付成功，请到店出示二维码或券码完成核销。',
    _ => '券码已由商家核销，可以发布评价。',
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
        kind: OrderKind.service,
        title: '江南小馆双人套餐',
        amount: 98,
      ),
    ),
    _ => () {
      final merchant = merchantById('m3');
      Navigator.pushNamed(
        context,
        Routes.merchantDetail,
        arguments: MerchantArgs(type: merchant.type, merchant: merchant),
      );
    },
  };
}

Widget _storeCard() => const AppCard(
  child: Row(
    children: [
      Icon(Icons.storefront, color: AppColors.brand),
      SizedBox(width: 8),
      Expanded(
        child: Text('江南小馆', style: TextStyle(fontWeight: FontWeight.w700)),
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
      Expanded(child: Text('江南小馆双人套餐')),
      PriceText(98, size: 18),
    ],
  ),
);
