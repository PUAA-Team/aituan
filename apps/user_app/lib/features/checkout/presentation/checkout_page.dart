import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../home/data/backend_app_repository.dart';

class CheckoutPage extends StatefulWidget {
  const CheckoutPage({super.key, required this.args});

  final CheckoutArgs args;

  @override
  State<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends State<CheckoutPage> {
  CheckoutPreviewData? _preview;
  Object? _error;
  bool _loading = true;
  bool _submitting = false;

  BusinessType get _businessType =>
      widget.args.businessType ??
      (widget.args.kind == OrderKind.takeaway
          ? BusinessType.takeaway
          : BusinessType.groupBuy);

  bool get _canUseBackend =>
      int.tryParse(widget.args.storeId) != null &&
      widget.args.lines.isNotEmpty &&
      widget.args.lines.every((line) => int.tryParse(line.itemId) != null);

  @override
  void initState() {
    super.initState();
    _loadPreview();
  }

  @override
  Widget build(BuildContext context) {
    final isTakeaway = widget.args.kind == OrderKind.takeaway;
    final preview = _preview;
    final payable = preview?.payableAmount ?? widget.args.amount;
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
                  preview?.addressSnapshot ??
                      (isTakeaway ? '使用默认收货地址' : '支付成功后生成券码，到店出示即可核销'),
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          if (!_canUseBackend)
            const AppCard(child: Text('订单信息不完整，请返回商家页重新选择商品。'))
          else if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _loadPreview)
          else ...[
            _GoodsCard(
              title: widget.args.title,
              preview: preview,
              args: widget.args,
            ),
            AppCard(
              child: Row(
                children: const [
                  Icon(Icons.payments_outlined, color: AppColors.brand),
                  SizedBox(width: 8),
                  Text('模拟支付', style: TextStyle(fontWeight: FontWeight.w700)),
                  Spacer(),
                  Icon(Icons.check_circle, color: AppColors.brand),
                ],
              ),
            ),
            _FeeCard(preview: preview, fallbackAmount: widget.args.amount),
          ],
          const SizedBox(height: 90),
        ],
      ),
      bottomNavigationBar: AppBottomActionBar(
        primaryText: _submitting ? '提交中' : '提交并支付',
        onPrimary: _canUseBackend && !_loading && _error == null && !_submitting
            ? _pay
            : null,
        price: payable,
        note: '模拟支付',
      ),
    );
  }

  Future<void> _loadPreview() async {
    if (!_canUseBackend) {
      setState(() => _loading = false);
      return;
    }
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final preview = await backendRepository.previewCheckout(
        storeId: widget.args.storeId,
        businessType: _businessType,
        addressId: null,
        lines: widget.args.lines,
      );
      if (!mounted) return;
      setState(() {
        _preview = preview;
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

  Future<void> _pay() async {
    try {
      setState(() => _submitting = true);
      final order = await backendRepository.createOrder(
        storeId: widget.args.storeId,
        businessType: _businessType,
        addressId: null,
        lines: widget.args.lines,
        remark: '',
        idempotencyKey: DateTime.now().microsecondsSinceEpoch.toString(),
      );
      final paid = await backendRepository.payOrder(order.id);
      if (!mounted) return;
      Navigator.pushReplacementNamed(
        context,
        Routes.orderDetail,
        arguments: OrderDetailArgs(
          kind: paid.kind,
          status: paid.status,
          orderId: paid.id,
        ),
      );
    } catch (error) {
      if (!mounted) return;
      setState(() => _submitting = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('提交失败：$error')));
    }
  }
}

class _GoodsCard extends StatelessWidget {
  const _GoodsCard({
    required this.title,
    required this.preview,
    required this.args,
  });

  final String title;
  final CheckoutPreviewData? preview;
  final CheckoutArgs args;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 10),
        if (preview != null)
          for (final item in preview!.items) _PreviewLine(item)
        else
          for (final line in args.lines) _ArgLine(line),
      ],
    ),
  );
}

class _PreviewLine extends StatelessWidget {
  const _PreviewLine(this.item);

  final CheckoutLineItemData item;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      children: [
        Expanded(child: Text('${item.itemName} ×${item.quantity}')),
        PriceText(item.totalPrice, size: 16),
      ],
    ),
  );
}

class _ArgLine extends StatelessWidget {
  const _ArgLine(this.line);

  final CheckoutLineArg line;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      children: [
        Expanded(child: Text('${line.title} ×${line.quantity}')),
        PriceText(line.unitPrice * line.quantity, size: 16),
      ],
    ),
  );
}

class _FeeCard extends StatelessWidget {
  const _FeeCard({required this.preview, required this.fallbackAmount});

  final CheckoutPreviewData? preview;
  final double fallbackAmount;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      children: [
        _FeeRow('商品金额', preview?.amount ?? fallbackAmount),
        if ((preview?.deliveryFee ?? 0) > 0)
          _FeeRow('配送费', preview!.deliveryFee),
        if ((preview?.discountAmount ?? 0) > 0)
          _FeeRow('优惠', -preview!.discountAmount),
        const Divider(),
        Row(
          children: [
            const Text('实付', style: TextStyle(fontWeight: FontWeight.w700)),
            const Spacer(),
            PriceText(preview?.payableAmount ?? fallbackAmount, size: 22),
          ],
        ),
      ],
    ),
  );
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

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('订单预览失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
