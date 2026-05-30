import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../home/data/backend_app_repository.dart';

/// Stage5-D 新增：非外卖券码完整详情页
/// 展示完整 QR 占位、券码大字号、有效期、核销状态与商家公示规则
class VoucherDetailPage extends StatefulWidget {
  const VoucherDetailPage({super.key, required this.args});

  final VoucherDetailArgs args;

  @override
  State<VoucherDetailPage> createState() => _VoucherDetailPageState();
}

class _VoucherDetailPageState extends State<VoucherDetailPage> {
  OrderDetailData? _detail;
  Object? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final detail = await backendRepository.fetchOrderDetail(widget.args.orderId);
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

  @override
  Widget build(BuildContext context) {
    final detail = _detail;
    final voucher = detail?.voucher;
    return Scaffold(
      appBar: AppBar(title: const Text('券码详情')),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            if (_loading)
              const AppCard(child: Center(child: CircularProgressIndicator()))
            else if (_error != null)
              AppCard(child: Text('加载失败：$_error'))
            else if (voucher == null)
              const AppCard(child: Text('该订单暂无券码，请联系商家。'))
            else ...[
              _QrCard(voucher: voucher),
              _RuleCard(detail: detail!, voucher: voucher),
              _StoreCard(detail: detail),
            ],
          ],
        ),
      ),
    );
  }
}

class _QrCard extends StatelessWidget {
  const _QrCard({required this.voucher});

  final VoucherData voucher;

  @override
  Widget build(BuildContext context) {
    final used = voucher.status.toLowerCase() == 'used';
    return AppCard(
      backgroundColor: AppColors.brandSoft,
      borderColor: AppColors.brandLine,
      child: Column(
        children: [
          BrandTag(
            used ? '券码已核销' : '待核销',
            green: !used,
            selected: true,
          ),
          const SizedBox(height: 14),
          Container(
            width: 200,
            height: 200,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.brandLine),
            ),
            child: Stack(
              alignment: Alignment.center,
              children: [
                CustomPaint(
                  size: const Size(180, 180),
                  painter: _PatternPainter(),
                ),
                Container(
                  width: 48,
                  height: 48,
                  alignment: Alignment.center,
                  color: Colors.white,
                  child: const Text(
                    'AT',
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: AppColors.brand,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Text(
            voucher.voucherCode,
            style: const TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.w800,
              letterSpacing: 2,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            voucher.qrPayload,
            style: const TextStyle(fontSize: 12, color: AppColors.textSub),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 6),
          Text(
            used ? '感谢使用爱团服务' : '到店时向店员出示二维码或券码号',
            style: const TextStyle(color: AppColors.textSub),
          ),
        ],
      ),
    );
  }
}

class _RuleCard extends StatelessWidget {
  const _RuleCard({required this.detail, required this.voucher});

  final OrderDetailData detail;
  final VoucherData voucher;

  @override
  Widget build(BuildContext context) {
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('券码信息', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          _Kv('订单编号', detail.orderNo),
          _Kv('订单标题', detail.title),
          _Kv('实付金额', '￥${detail.payableAmount.toStringAsFixed(1)}'),
          _Kv('有效期至', voucher.effectiveTo == null
              ? '不限'
              : _formatDate(voucher.effectiveTo!)),
          _Kv('核销状态', voucher.status.toLowerCase() == 'used' ? '已核销' : '未核销'),
        ],
      ),
    );
  }

  String _formatDate(DateTime time) {
    return '${time.year}-${_pad(time.month)}-${_pad(time.day)}';
  }

  String _pad(int v) => v < 10 ? '0$v' : '$v';
}

class _StoreCard extends StatelessWidget {
  const _StoreCard({required this.detail});

  final OrderDetailData detail;

  @override
  Widget build(BuildContext context) {
    return AppCard(
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
          if (detail.voucherSummary != null && detail.voucherSummary!.isNotEmpty)
            Text(
              detail.voucherSummary!,
              style: Theme.of(context).textTheme.bodySmall,
            ),
        ],
      ),
    );
  }
}

class _Kv extends StatelessWidget {
  const _Kv(this.k, this.v);

  final String k;
  final String v;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      children: [
        SizedBox(
          width: 80,
          child: Text(k, style: const TextStyle(color: AppColors.textSub)),
        ),
        Expanded(child: Text(v)),
      ],
    ),
  );
}

class _PatternPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()..color = const Color(0xFF1F1F1F);
    const cells = 18;
    final cell = size.width / cells;
    // 简易格子图样作为二维码占位（演示不接真实 QR 库）
    final pattern = [
      0x3E, 0x41, 0x5D, 0x5D, 0x41, 0x3E, 0x00, 0x12,
      0x1A, 0x3E, 0x14, 0x5D, 0x00, 0x3E, 0x41, 0x5D,
      0x5D, 0x41,
    ];
    for (var y = 0; y < cells; y++) {
      final row = pattern[y % pattern.length];
      for (var x = 0; x < cells; x++) {
        if ((row >> (x % 8)) & 1 == 1) {
          canvas.drawRect(
            Rect.fromLTWH(x * cell, y * cell, cell, cell),
            paint,
          );
        }
      }
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
