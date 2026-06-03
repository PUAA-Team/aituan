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
import '../../merchant/presentation/takeaway_amount_utils.dart';

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
  String _tablewareOption = 'merchant_decide';
  int _tablewareCount = 1;
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
              if (isTakeaway)
                _TablewareCard(
                  option: _tablewareOption,
                  count: _tablewareCount,
                  previewText: preview?.tablewareText,
                  onTap: _openTablewareSelector,
                ),
              _CouponCard(
                coupon: _selectedCoupon,
                discountAmount: preview?.discountAmount ?? 0,
                enabled: _canUseBackend && preview != null,
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
                (preview?.deliverable ?? true) &&
                (preview?.minimumOrderMet ?? true)
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
        tablewareOption: _tablewareOption,
        tablewareCount: _tablewareOption == 'by_people'
            ? _tablewareCount
            : null,
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

  Future<void> _openTablewareSelector() async {
    final result = await showModalBottomSheet<_TablewareSelection>(
      context: context,
      isScrollControlled: true,
      builder: (_) =>
          _TablewareSheet(option: _tablewareOption, count: _tablewareCount),
    );
    if (result == null || !mounted) return;
    setState(() {
      _tablewareOption = result.option;
      _tablewareCount = result.count;
    });
    await _loadPreview();
  }

  Future<void> _openCouponSelector() async {
    final amount = _preview?.amount ?? widget.args.amount;
    final result = await Navigator.pushNamed(
      context,
      Routes.couponSelector,
      arguments: CouponSelectorArgs(
        orderAmount: amount,
        selectedCoupon: _selectedCoupon,
      ),
    );
    if (result is! CouponSelectorResult || !mounted) return;
    setState(() {
      _selectedCoupon = result.clear ? null : result.coupon;
    });
    await _loadPreview();
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
    if (preview != null && !preview.minimumOrderMet) {
      showAppSnackBar(
        context,
        '商品金额还差￥${takeawayMoneyText(preview.startPriceMissing)}起送',
      );
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
        tablewareOption: _tablewareOption,
        tablewareCount: _tablewareOption == 'by_people'
            ? _tablewareCount
            : null,
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
    padding: const EdgeInsets.symmetric(vertical: 6),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(item.itemName, maxLines: 2, overflow: TextOverflow.ellipsis),
              const SizedBox(height: 2),
              Text(
                '×${item.quantity}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ),
        const SizedBox(width: 10),
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
    padding: const EdgeInsets.symmetric(vertical: 6),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(line.title, maxLines: 2, overflow: TextOverflow.ellipsis),
              const SizedBox(height: 2),
              Text(
                '×${line.quantity}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ),
        const SizedBox(width: 10),
        PriceText(line.unitPrice * line.quantity, size: 16),
      ],
    ),
  );
}

class _CouponCard extends StatelessWidget {
  const _CouponCard({
    required this.coupon,
    required this.discountAmount,
    required this.enabled,
    required this.onTap,
  });

  final OrderCouponOption? coupon;
  final double discountAmount;
  final bool enabled;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final selected = coupon != null;
    final subtitle = selected
        ? '已选 ${coupon!.name}，预计优惠 ¥${discountAmount.toStringAsFixed(2)}'
        : '选择可用优惠券，后端会重新校验抵扣金额';
    return AppCard(
      onTap: enabled ? onTap : null,
      child: Row(
        children: [
          const Icon(Icons.confirmation_num_outlined, color: AppColors.brand),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('优惠券', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 4),
                Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
          if (selected)
            Text(
              '-¥${discountAmount.toStringAsFixed(2)}',
              style: const TextStyle(
                color: AppColors.brand,
                fontWeight: FontWeight.w800,
              ),
            ),
          const SizedBox(width: 6),
          const Icon(Icons.chevron_right, color: AppColors.textLight),
        ],
      ),
    );
  }
}

class _TablewareCard extends StatelessWidget {
  const _TablewareCard({
    required this.option,
    required this.count,
    required this.previewText,
    required this.onTap,
  });

  final String option;
  final int count;
  final String? previewText;
  final VoidCallback onTap;

  String get _text {
    final text = previewText;
    if (text != null && text.isNotEmpty) return text;
    return switch (option) {
      'none' => '无需餐具',
      'by_people' => '按 $count 人提供餐具',
      _ => '商家按餐量提供餐具',
    };
  }

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onTap,
    child: Row(
      children: [
        const Icon(Icons.restaurant_outlined, color: AppColors.brand),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('餐具数量', style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 4),
              Text(_text, style: Theme.of(context).textTheme.bodySmall),
            ],
          ),
        ),
        const Icon(Icons.chevron_right, color: AppColors.textLight),
      ],
    ),
  );
}

class _TablewareSelection {
  const _TablewareSelection(this.option, this.count);

  final String option;
  final int count;
}

class _TablewareSheet extends StatefulWidget {
  const _TablewareSheet({required this.option, required this.count});

  final String option;
  final int count;

  @override
  State<_TablewareSheet> createState() => _TablewareSheetState();
}

class _TablewareSheetState extends State<_TablewareSheet> {
  late String _option;
  late int _count;

  @override
  void initState() {
    super.initState();
    _option = widget.option;
    _count = widget.count < 1 ? 1 : widget.count;
  }

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Padding(
      padding: EdgeInsets.fromLTRB(
        16,
        16,
        16,
        16 + MediaQuery.of(context).viewInsets.bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('选择餐具数量', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 12),
          _optionTile('none', '无需餐具', '为减少浪费，本单不提供一次性餐具'),
          _optionTile('by_people', '按人数提供', '商家按选择人数准备餐具'),
          if (_option == 'by_people') _peopleCounter(),
          _optionTile('merchant_decide', '商家按餐量定', '由商家根据商品份数合理提供'),
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: () =>
                  Navigator.pop(context, _TablewareSelection(_option, _count)),
              child: const Text('确定'),
            ),
          ),
        ],
      ),
    ),
  );

  Widget _optionTile(String value, String title, String subtitle) => InkWell(
    onTap: () => setState(() => _option = value),
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            _option == value
                ? Icons.radio_button_checked
                : Icons.radio_button_unchecked,
            color: _option == value ? AppColors.brand : AppColors.textLight,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 2),
                Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
              ],
            ),
          ),
        ],
      ),
    ),
  );

  Widget _peopleCounter() => Padding(
    padding: const EdgeInsets.only(left: 6, right: 6, bottom: 8),
    child: Row(
      children: [
        const Text('用餐人数'),
        const Spacer(),
        IconButton(
          onPressed: _count > 1 ? () => setState(() => _count--) : null,
          icon: const Icon(Icons.remove_circle_outline),
        ),
        SizedBox(
          width: 36,
          child: Center(
            child: Text(
              '$_count',
              style: const TextStyle(fontWeight: FontWeight.w800),
            ),
          ),
        ),
        IconButton(
          onPressed: _count < 20 ? () => setState(() => _count++) : null,
          icon: const Icon(Icons.add_circle_outline),
        ),
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
        if ((preview?.packageFee ?? 0) > 0) _FeeRow('打包费', preview!.packageFee),
        if ((preview?.deliveryFee ?? 0) > 0)
          _FeeRow(
            (preview?.distanceExtraFee ?? 0) > 0 ? '配送费（含距离加价）' : '配送费',
            preview!.deliveryFee,
          ),
        if ((preview?.distanceExtraFee ?? 0) > 0)
          _InfoRow('距离加价', '￥${preview!.distanceExtraFee.toStringAsFixed(1)}'),
        if ((preview?.discountAmount ?? 0) > 0)
          _FeeRow('优惠', -preview!.discountAmount),
        if ((preview?.startPriceMissing ?? 0) > 0)
          _InfoRow(
            '起送差额',
            '商品金额还差￥${takeawayMoneyText(preview!.startPriceMissing)}，配送费和打包费不计入起送',
          ),
        if (preview != null && preview!.estimatedArrivalText != null)
          _InfoRow('预计送达', preview!.estimatedArrivalText!),
        if (preview != null && preview!.deliveryDistanceKm != null)
          _InfoRow(
            '配送距离',
            '${preview!.deliveryDistanceKm!.toStringAsFixed(2)}km',
          ),
        if (preview != null && !preview!.minimumOrderMet)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              '商品金额还差￥${takeawayMoneyText(preview!.startPriceMissing)}起送',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: AppColors.brand,
                fontWeight: FontWeight.w700,
              ),
            ),
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
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            value,
            textAlign: TextAlign.right,
            style: const TextStyle(fontWeight: FontWeight.w700),
          ),
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
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(child: Text(label)),
        const SizedBox(width: 12),
        PriceText(value, size: 16),
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
        Text('订单预览失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
