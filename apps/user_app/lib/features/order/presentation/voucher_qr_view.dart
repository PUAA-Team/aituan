import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../../core/constants/app_colors.dart';

class VoucherQrView extends StatelessWidget {
  const VoucherQrView({
    super.key,
    required this.data,
    this.size = 180,
    this.embeddedText,
  });

  final String data;
  final double size;
  final String? embeddedText;

  @override
  Widget build(BuildContext context) {
    final value = data.trim();
    if (value.isEmpty) {
      return Container(
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: AppColors.brandSoft,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppColors.brandLine),
        ),
        child: const Text('暂无二维码'),
      );
    }
    return QrImageView(
      data: value,
      version: QrVersions.auto,
      size: size,
      backgroundColor: Colors.white,
      embeddedImageStyle: embeddedText == null
          ? null
          : const QrEmbeddedImageStyle(size: Size(36, 36)),
      errorStateBuilder: (context, error) => Container(
        width: size,
        height: size,
        alignment: Alignment.center,
        child: Text('二维码生成失败', style: Theme.of(context).textTheme.bodySmall),
      ),
    );
  }
}
