import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../shared/enums/business_type.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../core/widgets/mock_thumb.dart';
import '../../../core/widgets/price_text.dart';
import '../../../features/complaint/presentation/complaint_submit_page.dart';
import '../../home/data/backend_app_repository.dart';
import 'service_order_detail_widgets.dart';

class ServiceOrderDetailPage extends StatefulWidget {
  const ServiceOrderDetailPage({super.key, required this.args});

  final OrderDetailArgs args;

  @override
  State<ServiceOrderDetailPage> createState() => _ServiceOrderDetailPageState();
}

class _ServiceOrderDetailPageState extends State<ServiceOrderDetailPage> {
  OrderDetailData? _detail;
  Object? _error;
  bool _loading = true;
  bool _paying = false;

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
      appBar: AppBar(
        title: const Text('订单详情'),
        actions: [
          if (detail != null)
            IconButton(
              tooltip: '投诉/反馈',
              icon: const Icon(Icons.report_outlined),
              onPressed: () => _openComplaint(detail),
            ),
        ],
      ),
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
              ServiceOrderStatusCard(
                status: detail.status,
                desc: _desc(detail),
              ),
              if (detail.status != OrderStatus.unpaid)
                ServiceVoucherCard(
                  voucher: detail.voucher,
                  used: detail.status == OrderStatus.used,
                ),
              if (detail.voucher != null && detail.status != OrderStatus.unpaid)
                _LinkCard(
                  icon: Icons.qr_code,
                  title: '查看券码完整详情',
                  subtitle: '大图二维码、券码号和使用规则',
                  onTap: () => Navigator.pushNamed(
                    context,
                    Routes.voucherDetail,
                    arguments: VoucherDetailArgs(orderId: detail.id),
                  ),
                ),
              if (_supportsBooking(detail) &&
                  detail.status != OrderStatus.unpaid)
                _LinkCard(
                  icon: Icons.event_available,
                  title: detail.booking == null ? '提交到店预约信息' : '查看预约详情',
                  subtitle: detail.booking == null
                      ? '提交联系人、日期和时段'
                      : (detail.booking!.isConfirmed
                            ? '商家已确认 ${detail.booking!.bookingDate ?? ''} ${detail.booking!.bookingTimeSlot ?? ''}'
                            : '等待商家确认'),
                  onTap: () => Navigator.pushNamed(
                    context,
                    Routes.bookingDetail,
                    arguments: BookingDetailArgs(orderId: detail.id),
                  ),
                ),
              _StoreCard(detail: detail),
              _GoodsCard(detail: detail),
              _RuleCard(detail: detail),
            ],
            const SizedBox(height: 80),
          ],
        ),
      ),
      bottomNavigationBar: detail == null
          ? null
          : AppBottomActionBar(
              primaryText: _primaryText(detail.status),
              onPrimary: _paying ? null : () => _primaryAction(detail),
              secondaryText: _secondaryText(detail.status),
              onSecondary: () => _secondaryAction(detail),
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

  Future<void> _pay(OrderDetailData detail) async {
    try {
      setState(() => _paying = true);
      final paid = await backendRepository.payOrder(detail.id);
      if (!mounted) return;
      setState(() {
        _detail = paid;
        _paying = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _paying = false);
      showAppSnackBar(context, '支付失败：$error');
    }
  }

  void _primaryAction(OrderDetailData detail) {
    if (detail.status == OrderStatus.unpaid) {
      _pay(detail);
      return;
    }
    if (detail.status == OrderStatus.used) {
      Navigator.pushNamed(
        context,
        Routes.reviewPublish,
        arguments: ReviewArgs(title: detail.title, orderId: detail.id),
      );
      return;
    }
    _openStore(detail);
  }

  void _secondaryAction(OrderDetailData detail) {
    if (detail.status == OrderStatus.unpaid ||
        detail.status == OrderStatus.used) {
      _openStore(detail);
      return;
    }
    _load();
  }

  void _openStore(OrderDetailData detail) {
    Navigator.pushNamed(
      context,
      Routes.searchResult,
      arguments: SearchArgs(detail.storeName),
    );
  }

  void _openComplaint(OrderDetailData detail) {
    final orderId = int.tryParse(detail.id);
    Navigator.pushNamed(
      context,
      Routes.complaintSubmit,
      arguments: ComplaintSubmitArgs(
        orderId: orderId,
        orderTitle: detail.title,
      ),
    );
  }

  String _primaryText(OrderStatus status) => switch (status) {
    OrderStatus.unpaid => _paying ? '支付中' : '模拟支付',
    OrderStatus.used => '去评价',
    _ => '查看商家',
  };

  String _secondaryText(OrderStatus status) => switch (status) {
    OrderStatus.unpaid || OrderStatus.used => '查看商家',
    _ => '刷新状态',
  };

  String _desc(OrderDetailData detail) => switch (detail.status) {
    OrderStatus.unpaid => '订单已创建，请在 15 分钟内完成支付。',
    OrderStatus.unused =>
      detail.voucherSummary?.isNotEmpty == true
          ? '支付成功，${detail.voucherSummary}。'
          : '支付成功，请到店出示二维码或券码完成核销。',
    _ => '券码已由商家核销，可以发布评价。',
  };

  bool _supportsBooking(OrderDetailData detail) {
    final type =
        (detail.items.isNotEmpty ? detail.items.first.businessType : '')
            .toLowerCase();
    // 酒店、休闲娱乐、丽人医美、洗脚按摩、电影演出需要预约/选场次
    return const {
      'hotel',
      'entertainment',
      'fun',
      'beauty',
      'massage',
      'movie',
    }.contains(type);
  }
}

class _LinkCard extends StatelessWidget {
  const _LinkCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onTap,
    child: Row(
      children: [
        Icon(icon, color: AppColors.brand),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontWeight: FontWeight.w700)),
              const SizedBox(height: 2),
              Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
        const Icon(Icons.chevron_right, color: AppColors.textSub),
      ],
    ),
  );
}

class _StoreCard extends StatelessWidget {
  const _StoreCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: () => Navigator.pushNamed(
      context,
      Routes.searchResult,
      arguments: SearchArgs(detail.storeName),
    ),
    child: Row(
      children: [
        const Icon(Icons.storefront, color: AppColors.brand),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            detail.storeName,
            style: const TextStyle(fontWeight: FontWeight.w700),
          ),
        ),
        const Text('进店'),
      ],
    ),
  );
}

class _GoodsCard extends StatelessWidget {
  const _GoodsCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      children: [
        for (final item in detail.items)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              children: [
                MockThumb(
                  size: 72,
                  label: item.categoryName,
                  imageUrl: item.coverUrl,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(item.itemName),
                      const SizedBox(height: 4),
                      Text(
                        item.subtitle,
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
                PriceText(item.totalPrice, size: 18),
              ],
            ),
          ),
      ],
    ),
  );
}

class _RuleCard extends StatelessWidget {
  const _RuleCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      children: [
        _Kv('有效期', _validityText(detail.voucher)),
        _Kv('退款规则', detail.status == OrderStatus.unused ? '未使用可退' : '以商家规则为准'),
        _Kv(
          '预约要求',
          detail.remark?.isNotEmpty == true ? detail.remark! : '高峰建议电话确认',
        ),
      ],
    ),
  );
}

String _validityText(VoucherData? voucher) {
  if (voucher == null) return '以商家规则为准';
  final from = voucher.effectiveFrom == null
      ? null
      : _dateText(voucher.effectiveFrom!);
  final to = voucher.effectiveTo == null
      ? null
      : _dateText(voucher.effectiveTo!);
  if (from != null && to != null) return '$from 至 $to';
  if (to != null) return '有效期至 $to';
  return '不限';
}

String _dateText(DateTime time) =>
    '${time.year}-${_two(time.month)}-${_two(time.day)}';

String _two(int value) => value < 10 ? '0$value' : '$value';

class _Kv extends StatelessWidget {
  const _Kv(this.k, this.v);

  final String k;
  final String v;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(width: 76, child: Text(k)),
        const SizedBox(width: 10),
        Expanded(child: Text(v, textAlign: TextAlign.right)),
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
        Text('订单详情加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
