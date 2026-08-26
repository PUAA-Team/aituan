import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../shared/models/address_model.dart';
import '../application/location_scope.dart';

class LocationPickerButton extends StatelessWidget {
  const LocationPickerButton({
    super.key,
    this.onLocationChanged,
    this.foregroundColor = AppColors.brand,
    this.backgroundColor = AppColors.brandSoft,
    this.borderColor,
    this.borderRadius = 8,
    this.maxLabelWidth,
    this.fontWeight,
    this.compact = false,
  });

  final Future<void> Function()? onLocationChanged;
  final Color foregroundColor;
  final Color backgroundColor;
  final Color? borderColor;
  final double borderRadius;
  final double? maxLabelWidth;
  final FontWeight? fontWeight;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final location = LocationScope.of(context);
    return InkWell(
      onTap: location.loading ? null : () => _openPicker(context),
      borderRadius: BorderRadius.circular(borderRadius),
      child: Container(
        padding: EdgeInsets.symmetric(
          horizontal: compact ? 8 : 10,
          vertical: compact ? 5 : 7,
        ),
        decoration: BoxDecoration(
          color: backgroundColor,
          borderRadius: BorderRadius.circular(borderRadius),
          border: borderColor == null ? null : Border.all(color: borderColor!),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              location.loading
                  ? Icons.sync
                  : location.usingAddress
                  ? Icons.home_outlined
                  : Icons.location_on,
              color: foregroundColor,
              size: compact ? 16 : 18,
            ),
            const SizedBox(width: 4),
            ConstrainedBox(
              constraints: BoxConstraints(
                maxWidth: maxLabelWidth ?? (compact ? 150 : 190),
              ),
              child: Text(
                location.label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  color: foregroundColor,
                  fontSize: compact ? 13 : 15,
                  fontWeight: fontWeight ?? FontWeight.w800,
                ),
              ),
            ),
            const SizedBox(width: 2),
            Icon(Icons.expand_more, color: foregroundColor, size: 16),
          ],
        ),
      ),
    );
  }

  Future<void> _openPicker(BuildContext context) async {
    await showModalBottomSheet<void>(
      context: context,
      showDragHandle: true,
      builder: (sheetContext) => _LocationPickerSheet(
        onUseCurrent: () => _useCurrentLocation(context, sheetContext),
        onSelectAddress: () => _selectAddress(context, sheetContext),
      ),
    );
  }

  Future<void> _useCurrentLocation(
    BuildContext context,
    BuildContext sheetContext,
  ) async {
    Navigator.pop(sheetContext);
    final location = LocationScope.of(context);
    await location.selectCurrentLocation();
    if (!context.mounted) return;
    final error = location.error;
    showAppSnackBar(
      context,
      error == null ? '已切换为当前位置' : '定位失败，当前使用默认位置：$error',
    );
    await onLocationChanged?.call();
  }

  Future<void> _selectAddress(
    BuildContext context,
    BuildContext sheetContext,
  ) async {
    Navigator.pop(sheetContext);
    final state = AppScope.of(context);
    if (!state.requireLogin(context)) return;
    final result = await Navigator.pushNamed(
      context,
      Routes.addressList,
      arguments: const AddressListArgs(selectMode: true),
    );
    if (!context.mounted || result == null) return;
    final address = result is AddressData ? result : null;
    if (address == null) return;
    if (address.latitude == null || address.longitude == null) {
      showAppSnackBar(context, '该地址缺少定位坐标，请先编辑地址并保存定位');
      return;
    }
    LocationScope.of(context).selectAddressLocation(address);
    showAppSnackBar(context, '已切换到${address.detailAddress}周边');
    await onLocationChanged?.call();
  }
}

class _LocationPickerSheet extends StatelessWidget {
  const _LocationPickerSheet({
    required this.onUseCurrent,
    required this.onSelectAddress,
  });

  final VoidCallback onUseCurrent;
  final VoidCallback onSelectAddress;

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Padding(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 18),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('选择浏览位置', style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 6),
          Text(
            '首页推荐、搜索和附近商家会按所选位置排序。',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 12),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.my_location, color: AppColors.brand),
            title: const Text('使用当前位置'),
            subtitle: const Text('按手机定位或默认位置推荐'),
            onTap: onUseCurrent,
          ),
          const Divider(height: 8),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.home_outlined, color: AppColors.brand),
            title: const Text('选择收货地址'),
            subtitle: const Text('按某个收货地址周边推荐和搜索'),
            onTap: onSelectAddress,
          ),
        ],
      ),
    ),
  );
}
