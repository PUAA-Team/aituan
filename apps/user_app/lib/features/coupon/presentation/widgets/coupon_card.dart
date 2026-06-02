import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/widgets/app_card.dart';
import '../../data/coupon_repository.dart';

// 我的优惠券卡片（可用/已用/失效三态）
class UserCouponCard extends StatelessWidget {
  const UserCouponCard({super.key, required this.coupon});

  final UserCoupon coupon;

  @override
  Widget build(BuildContext context) {
    final available = coupon.status == 'unused';
    final (badgeText, badgeColor) = _statusBadge(coupon.status);
    return AppCard(
      child: Row(
        children: [
          _ValueBlock(
            discountDesc: coupon.discountDesc,
            thresholdDesc: coupon.thresholdDesc,
            color: available ? AppColors.brand : AppColors.textLight,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  coupon.name,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                const SizedBox(height: 4),
                Text(
                  _footer(coupon),
                  style: Theme.of(context).textTheme.labelSmall,
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          _Badge(text: badgeText, color: badgeColor),
        ],
      ),
    );
  }
}

// 可领取的优惠券卡片
class AvailableCouponCard extends StatelessWidget {
  const AvailableCouponCard({
    super.key,
    required this.coupon,
    required this.onClaim,
  });

  final AvailableCoupon coupon;
  final VoidCallback onClaim;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Row(
      children: [
        _ValueBlock(
          discountDesc: coupon.discountDesc,
          thresholdDesc: coupon.thresholdDesc,
          color: coupon.claimable ? AppColors.brand : AppColors.textLight,
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                coupon.name,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 4),
              Text(
                coupon.remaining == null
                    ? coupon.validDesc
                    : '${coupon.validDesc} · 剩 ${coupon.remaining} 张',
                style: Theme.of(context).textTheme.labelSmall,
              ),
            ],
          ),
        ),
        const SizedBox(width: 8),
        coupon.claimable
            ? FilledButton(onPressed: onClaim, child: const Text('领取'))
            : OutlinedButton(
                onPressed: null,
                child: Text(coupon.reason ?? '不可领取'),
              ),
      ],
    ),
  );
}

// 优惠券左侧面额块，两种卡片共用
class _ValueBlock extends StatelessWidget {
  const _ValueBlock({
    required this.discountDesc,
    required this.thresholdDesc,
    required this.color,
  });

  final String discountDesc;
  final String thresholdDesc;
  final Color color;

  @override
  Widget build(BuildContext context) => Container(
    width: 92,
    padding: const EdgeInsets.symmetric(vertical: 4),
    decoration: const BoxDecoration(
      border: Border(right: BorderSide(color: AppColors.line)),
    ),
    child: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          discountDesc,
          style: TextStyle(
            color: color,
            fontSize: 18,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          thresholdDesc,
          style: const TextStyle(color: AppColors.textSub, fontSize: 11),
        ),
      ],
    ),
  );
}

class _Badge extends StatelessWidget {
  const _Badge({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
    decoration: BoxDecoration(
      color: color.withValues(alpha: 0.12),
      borderRadius: BorderRadius.circular(6),
    ),
    child: Text(
      text,
      style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.w600),
    ),
  );
}

(String, Color) _statusBadge(String status) => switch (status) {
  'used' => ('已用', AppColors.textLight),
  'expired' => ('失效', AppColors.textLight),
  _ => ('可用', AppColors.brand),
};

String _footer(UserCoupon coupon) {
  if (coupon.status == 'used') {
    return coupon.usedAt == null ? '已使用' : '已于 ${_ymd(coupon.usedAt!)} 使用';
  }
  if (coupon.status == 'expired') {
    return coupon.expireAt == null ? '已过期' : '已于 ${_ymd(coupon.expireAt!)} 过期';
  }
  return coupon.expireAt == null ? '长期有效' : '有效期至 ${_ymd(coupon.expireAt!)}';
}

String _ymd(DateTime date) {
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '${date.year}-$month-$day';
}
