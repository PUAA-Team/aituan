import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../core/widgets/price_text.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../home/data/mock_data.dart';

class SearchHitItem extends StatelessWidget {
  const SearchHitItem({super.key, required this.item});

  final ItemModel item;

  @override
  Widget build(BuildContext context) => InkWell(
    borderRadius: BorderRadius.circular(8),
    onTap: () => item.type.isTakeaway
        ? Navigator.pushNamed(
            context,
            Routes.merchantDetail,
            arguments: MerchantArgs(
              type: item.type,
              merchant: merchantById(item.storeId),
            ),
          )
        : Navigator.pushNamed(
            context,
            Routes.itemDetail,
            arguments: ItemArgs(item),
          ),
    child: Container(
      margin: const EdgeInsets.only(top: 8),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 9),
      decoration: BoxDecoration(
        color: AppColors.soft,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppColors.line),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              item.title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: AppColors.textMain,
              ),
            ),
          ),
          const SizedBox(width: 8),
          BrandTag(item.type.label, emphasis: item.type.isTakeaway),
          const SizedBox(width: 8),
          PriceText(item.price, size: 16),
        ],
      ),
    ),
  );
}
