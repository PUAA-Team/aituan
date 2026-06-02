import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/address_model.dart';
import '../../coupon/data/coupon_repository.dart';
import '../../home/data/backend_app_repository.dart';

class CheckoutPage extends StatefulWidget {
  const CheckoutPage({super.key, required this.args});

  final CheckoutArgs args;

  @override
  State<CheckoutPage> createState() => _CheckoutPageState();
}

class _CheckoutPageState extends State<CheckoutPage> {
  final _remarkController = TextEditingController();
  CheckoutPreviewData? _preview;
  List<AddressData> _addresses = const [];
  String? _selectedAddressId;
  OrderCouponOption? _selectedCoupon;
  List<PaymentMethodData> _paymentMethods = const [];
  Object? _error;
  bool _loading = true;
  bool _submitting = false;

  BusinessType get _businessType =>
      widget.args.businessType ??
      (widget.args.kind == OrderKind.takeaway
          ? BusinessType.takeaway
          : BusinessType.groupBuy);

  AddressData? get _selectedAddress {
    for (final address in _addresses) {
      if (address.id == _selectedAddressId) return address;
    }
    return null;
  }

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
  void dispose() {
    _remarkController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isTakeaway = widget.args.kind == OrderKind.takeaway;
    final preview = _preview;
    final payable = preview?.payableAmount ?? widget.args.amount;
    return Scaffold(
      appBar: AppBar(title: const Text('确认订单')),
      body: RefreshIndicator(
        onRefresh: _loadPreview,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            if (isTakeaway)
              _AddressCard(
                address: _selectedAddress,
                loading: _loading,
                onTap: _openAddressSelector,
              )
            else
              AppCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '使用信息',
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '支付成功后生成券码，到店出示即可核销',
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
              _CouponCard(
                coupon: _selectedCoupon,
                amount: preview?.amount ?? widget.args.amount,
                onTap: _openCouponSelector,
              ),
              _RemarkCard(controller: _remarkController),
              _PaymentMethodCard(methods: _paymentMethods),
              _FeeCard(preview: preview, fallbackAmount: widget.args.amount),
            ],
            const SizedBox(height: 90),
          ],
        ),
      ),
      bottomNavigationBar: AppBottomActionBar(
        primaryText: _submitting ? '提交中' : '提交并支付',
        onPrimary:
            _canUseBackend &&
                !_loading &&
                _error == null &&
                !_submitting &&
                (preview?.deliverable ?? true)
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
      final isTakeaway = widget.args.kind == OrderKind.takeaway;
      final addresses = isTakeaway
          ? await backendRepository.fetchAddresses()
          : const <AddressData>[];
      final selected = _resolveSelectedAddress(addresses);
      final preview = await backendRepository.previewCheckout(
        storeId: widget.args.storeId,
        businessType: _businessType,
        addressId: selected?.id,
        lines: widget.args.lines,
        couponId: _selectedCoupon?.userCouponId,
      );
      final paymentMethods = await backendRepository.fetchPaymentMethods();
      if (!mounted) return;
      setState(() {
        _addresses = addresses;
        _selectedAddressId = selected?.id;
        _preview = preview;
        _paymentMethods = paymentMethods;
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

  AddressData? _resolveSelectedAddress(List<AddressData> addresses) {
    for (final address in addresses) {
      if (address.id == _selectedAddressId) return address;
    }
    for (final address in addresses) {
      if (address.isDefault) return address;
    }
    return addresses.isEmpty ? null : addresses.first;
  }

  Future<void> _openAddressSelector() async {
    final selected = await Navigator.pushNamed(
      context,
      Routes.addressList,
      arguments: AddressListArgs(
        selectMode: true,
        selectedAddressId: _selectedAddressId,
      ),
    );
    if (selected is AddressData) {
      setState(() => _selectedAddressId = selected.id);
    }
    if (mounted) await _loadPreview();
  }

  Future<void> _openCouponSelector() async {
    final coupon = await Navigator.pushNamed(
      context,
      Routes.couponSelector,
      arguments: _preview?.amount ?? widget.args.amount,
    );
    if (!mounted) return;
    if (coupon is OrderCouponOption) {
      setState(() => _selectedCoupon = coupon);
      await _loadPreview();
      if (mounted) showAppSnackBar(context, '已选择优惠券，已重新试算实付金额');
    }
  }

  Future<void> _pay() async {
    if (widget.args.kind == OrderKind.takeaway && _selectedAddress == null) {
      showAppSnackBar(context, '请先新增或选择收货地址');
      return;
    }
    final preview = _preview;
    if (preview != null && !preview.deliverable) {
      showAppSnackBar(context, preview.unavailableReason ?? '当前地址暂不可配送');
      return;
    }
    try {
      setState(() => _submitting = true);
      final order = await backendRepository.createOrder(
        storeId: widget.args.storeId,
        businessType: _businessType,
        addressId: _selectedAddress?.id,
        lines: widget.args.lines,
        remark: _remarkController.text.trim(),
        idempotencyKey: DateTime.now().microsecondsSinceEpoch.toString(),
        couponId: _selectedCoupon?.userCouponId,
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
      showAppSnackBar(context, '提交失败：$error');
    }
  }
}

class _AddressCard extends StatelessWidget {
  const _AddressCard({
    required this.address,
    required this.loading,
    required this.onTap,
  });

  final AddressData? address;
  final bool loading;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: loading ? null : onTap,
    borderColor: address == null ? AppColors.brandLine : AppColors.line,
    child: Row(
      children: [
        const Icon(Icons.place_outlined, color: AppColors.brand),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('收货地址', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 6),
              if (address == null)
                Text(
                  '新增或选择收货地址后继续下单',
                  style: Theme.of(context).textTheme.bodySmall,
                )
              else ...[
                Text('${address!.contactName} ${address!.contactPhone}'),
                const SizedBox(height: 4),
                Text(
                  address!.fullAddress,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ],
          ),
        ),
        const Icon(Icons.chevron_right, color: AppColors.textLight),
      ],
    ),
  );
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

class _CouponCard extends StatelessWidget {
  const _CouponCard({
    required this.coupon,
    required this.amount,
    required this.onTap,
  });

  final OrderCouponOption? coupon;
  final double amount;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onTap,
    child: Row(
      children: [
        const Icon(Icons.confirmation_number_outlined, color: AppColors.brand),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('优惠券', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 4),
              Text(
                coupon == null
                    ? '选择可用于 ¥${amount.toStringAsFixed(2)} 订单的优惠券'
                    : '${coupon!.name} · ${coupon!.discountDesc}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
              if (coupon != null)
                Text(
                  '已按当前订单重新试算抵扣',
                  style: Theme.of(
                    context,
                  ).textTheme.labelSmall?.copyWith(color: AppColors.textSub),
                ),
            ],
          ),
        ),
        const Icon(Icons.chevron_right, color: AppColors.textLight),
      ],
    ),
  );
}

class _RemarkCard extends StatelessWidget {
  const _RemarkCard({required this.controller});

  final TextEditingController controller;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('订单备注', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 10),
        TextField(
          controller: controller,
          maxLines: 2,
          decoration: const InputDecoration(
            hintText: '口味偏好、餐具数量、配送说明等',
            border: OutlineInputBorder(),
          ),
        ),
      ],
    ),
  );
}

class _PaymentMethodCard extends StatelessWidget {
  const _PaymentMethodCard({required this.methods});

  final List<PaymentMethodData> methods;

  @override
  Widget build(BuildContext context) {
    final display = methods.isEmpty
        ? const [PaymentMethodData(code: 'mock', name: '模拟支付', enabled: true)]
        : methods;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('支付方式', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 10),
          for (final method in display)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 5),
              child: Row(
                children: [
                  Icon(
                    method.enabled
                        ? Icons.check_circle
                        : Icons.radio_button_unchecked,
                    color: method.enabled
                        ? AppColors.brand
                        : AppColors.textLight,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    method.name,
                    style: TextStyle(
                      fontWeight: FontWeight.w700,
                      color: method.enabled
                          ? AppColors.textMain
                          : AppColors.textSub,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    method.enabled ? '当前可用' : '后续开放',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
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
        if (preview != null && preview!.estimatedArrivalText != null)
          _InfoRow('预计送达', preview!.estimatedArrivalText!),
        if (preview != null && preview!.deliveryDistanceKm != null)
          _InfoRow(
            '配送距离',
            '${preview!.deliveryDistanceKm!.toStringAsFixed(2)}km',
          ),
        if (preview != null && !preview!.deliverable)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              preview!.unavailableReason ?? '当前地址暂不可配送',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: AppColors.brand,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
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

class _InfoRow extends StatelessWidget {
  const _InfoRow(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      children: [
        Text(label, style: Theme.of(context).textTheme.bodySmall),
        const Spacer(),
        Text(value, style: const TextStyle(fontWeight: FontWeight.w700)),
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
