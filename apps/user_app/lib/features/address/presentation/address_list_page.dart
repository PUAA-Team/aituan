import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../shared/models/address_model.dart';
import '../../home/data/backend_app_repository.dart';

class AddressListPage extends StatefulWidget {
  const AddressListPage({super.key, required this.args});

  final AddressListArgs args;

  @override
  State<AddressListPage> createState() => _AddressListPageState();
}

class _AddressListPageState extends State<AddressListPage> {
  List<AddressData> _addresses = const [];
  Object? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(widget.args.selectMode ? '选择收货地址' : '地址管理')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else if (_addresses.isEmpty)
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '暂无收货地址',
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '新增地址后，外卖结算会自动带入默认地址。',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ),
            )
          else
            for (final address in _addresses)
              _AddressCard(
                address: address,
                selected: address.id == widget.args.selectedAddressId,
                selectMode: widget.args.selectMode,
                onTap: () => _handleTap(address),
                onEdit: () => _openEdit(address),
                onDefault: address.isDefault
                    ? null
                    : () => _setDefault(address),
              ),
          const SizedBox(height: 90),
        ],
      ),
    ),
    floatingActionButton: FloatingActionButton.extended(
      onPressed: () => _openEdit(null),
      icon: const Icon(Icons.add),
      label: const Text('新增地址'),
    ),
  );

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final addresses = await backendRepository.fetchAddresses();
      if (!mounted) return;
      setState(() {
        _addresses = addresses;
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

  void _handleTap(AddressData address) {
    if (widget.args.selectMode) {
      Navigator.pop(context, address);
      return;
    }
    _openEdit(address);
  }

  Future<void> _openEdit(AddressData? address) async {
    final changed = await Navigator.pushNamed(
      context,
      Routes.addressEdit,
      arguments: AddressEditArgs(address: address),
    );
    if (changed == true && mounted) await _load();
  }

  Future<void> _setDefault(AddressData address) async {
    try {
      await backendRepository.setDefaultAddress(address.id);
      if (mounted) await _load();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('设置失败：$error')));
    }
  }
}

class _AddressCard extends StatelessWidget {
  const _AddressCard({
    required this.address,
    required this.selected,
    required this.selectMode,
    required this.onTap,
    required this.onEdit,
    required this.onDefault,
  });

  final AddressData address;
  final bool selected;
  final bool selectMode;
  final VoidCallback onTap;
  final VoidCallback onEdit;
  final VoidCallback? onDefault;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onTap,
    borderColor: selected ? AppColors.brand : AppColors.line,
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text(
              address.contactName,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(width: 8),
            Text(
              address.contactPhone,
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const Spacer(),
            if (address.isDefault) const BrandTag('默认', selected: true),
            if (selected)
              const Padding(
                padding: EdgeInsets.only(left: 6),
                child: Icon(
                  Icons.check_circle,
                  color: AppColors.brand,
                  size: 18,
                ),
              ),
          ],
        ),
        const SizedBox(height: 8),
        Text(address.fullAddress),
        if (address.deliveryNote.isNotEmpty) ...[
          const SizedBox(height: 4),
          Text(
            address.deliveryNote,
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
        const SizedBox(height: 10),
        Row(
          children: [
            BrandTag(address.tagName),
            const Spacer(),
            if (!selectMode)
              TextButton(onPressed: onEdit, child: const Text('编辑'))
            else
              TextButton(onPressed: onEdit, child: const Text('管理')),
            if (onDefault != null)
              TextButton(onPressed: onDefault, child: const Text('设为默认')),
          ],
        ),
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
        Text('地址加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
