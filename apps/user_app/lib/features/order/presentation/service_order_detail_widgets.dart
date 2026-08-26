import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../shared/enums/business_type.dart';
import '../../home/data/backend_app_repository.dart';
import 'voucher_qr_view.dart';

class ServiceOrderStatusCard extends StatelessWidget {
  const ServiceOrderStatusCard({
    super.key,
    required this.status,
    required this.desc,
  });

  final OrderStatus status;
  final String desc;

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
              Text(
                status.labelForKind(OrderKind.service),
                style: Theme.of(context).textTheme.titleLarge,
              ),
              const SizedBox(height: 6),
              Text(desc, style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
        BrandTag(
          _tagText(status),
          green: status == OrderStatus.unused,
          selected: true,
        ),
      ],
    ),
  );

  String _tagText(OrderStatus status) => switch (status) {
    OrderStatus.unpaid => '待付款',
    OrderStatus.pending => '处理中',
    OrderStatus.unused => '待核销',
    OrderStatus.used => '可评价',
    OrderStatus.cancelled => '已取消',
    OrderStatus.refunded => '已退款',
  };
}

class ServiceVoucherCard extends StatelessWidget {
  const ServiceVoucherCard({
    super.key,
    required this.voucher,
    required this.used,
    this.refunded = false,
  });

  final VoucherData? voucher;
  final bool used;
  final bool refunded;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      children: [
        const Text(
          '核销凭证',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
        ),
        const SizedBox(height: 10),
        VoucherQrView(
          data: voucher?.qrPayload ?? voucher?.voucherCode ?? '',
          size: 150,
        ),
        const SizedBox(height: 10),
        Text(
          voucher?.voucherCode ?? '暂无券码',
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w800,
            letterSpacing: 1.6,
          ),
        ),
        if ((voucher?.qrPayload ?? '').isNotEmpty) ...[
          const SizedBox(height: 6),
          Text(
            voucher!.qrPayload,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontSize: 12, color: AppColors.textSub),
          ),
        ],
        Text(
          refunded ? '券码已退款失效' : (used ? '券码已核销' : '向商家出示二维码或券码号'),
          style: const TextStyle(fontSize: 13, color: AppColors.textSub),
        ),
      ],
    ),
  );
}

class ServiceKv extends StatelessWidget {
  const ServiceKv(this.k, this.v, {super.key});

  final String k;
  final String v;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(children: [Text(k), const Spacer(), Text(v)]),
  );
}
