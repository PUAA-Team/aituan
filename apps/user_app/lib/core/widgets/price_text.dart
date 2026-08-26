import 'package:flutter/material.dart';

import '../constants/app_colors.dart';

class PriceText extends StatelessWidget {
  const PriceText(this.price, {super.key, this.size = 18});

  final double price;
  final double size;

  @override
  Widget build(BuildContext context) {
    final value = price % 1 == 0
        ? price.toInt().toString()
        : price.toStringAsFixed(1);
    return Text.rich(
      TextSpan(
        children: [
          TextSpan(
            text: '￥',
            style: TextStyle(fontSize: size * .58, fontWeight: FontWeight.w700),
          ),
          TextSpan(text: value),
        ],
      ),
      style: TextStyle(
        fontSize: size,
        fontWeight: FontWeight.w800,
        color: AppColors.brand,
        height: 1.1,
      ),
    );
  }
}
