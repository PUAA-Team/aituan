import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_search_box.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../shared/models/merchant_model.dart';
import '../../location/presentation/location_picker_button.dart';
import '../../merchant/presentation/merchant_category_widgets.dart';

class SearchResultBox extends StatelessWidget {
  const SearchResultBox({super.key, required this.keyword});

  final String keyword;

  @override
  Widget build(BuildContext context) => AppSearchBox(
    hint: keyword.isEmpty ? '汉堡 / 洗脚 / 双人套餐' : keyword,
    onTap: () => Navigator.pushNamed(context, Routes.search),
  );
}

class SearchCategoryRow extends StatelessWidget {
  const SearchCategoryRow({super.key});

  @override
  Widget build(BuildContext context) {
    final labels = [
      '问小爱',
      '外卖',
      '团购',
      '酒店',
      '休闲娱乐',
      '电影演出',
      '丽人医美',
      '景点门票',
      '洗脚',
    ];
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final label in labels)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: BrandTag(
                label,
                solid: label == '问小爱',
                selected: label == '外卖',
              ),
            ),
        ],
      ),
    );
  }
}

class SearchFilterRow extends StatelessWidget {
  const SearchFilterRow({super.key, this.onLocationChanged});

  final Future<void> Function()? onLocationChanged;

  @override
  Widget build(BuildContext context) => SingleChildScrollView(
    scrollDirection: Axis.horizontal,
    child: Row(
      children: [
        LocationPickerButton(
          compact: true,
          onLocationChanged: onLocationChanged,
          foregroundColor: AppColors.textMain,
          backgroundColor: AppColors.card,
          borderColor: AppColors.line,
          borderRadius: 6,
          maxLabelWidth: 118,
          fontWeight: FontWeight.w600,
        ),
        const SizedBox(width: 8),
        const _FilterChip('排序方式'),
        const SizedBox(width: 8),
        const _FilterChip('筛选'),
      ],
    ),
  );
}

class MerchantResultCard extends StatelessWidget {
  const MerchantResultCard({
    super.key,
    required this.merchant,
    this.onReturned,
  });

  final MerchantModel merchant;
  final Future<void> Function()? onReturned;

  @override
  Widget build(BuildContext context) => MerchantAggregateCard(
    merchant: merchant,
    onMerchantTap: () => _openMerchant(context),
    onItemTap: (item) async {
      await Navigator.pushNamed(
        context,
        Routes.itemDetail,
        arguments: ItemArgs(item),
      );
      await onReturned?.call();
    },
  );

  Future<void> _openMerchant(BuildContext context) async {
    await Navigator.pushNamed(
      context,
      Routes.merchantDetail,
      arguments: MerchantArgs(type: merchant.type, merchant: merchant),
    );
    await onReturned?.call();
  }
}

class _FilterChip extends StatelessWidget {
  const _FilterChip(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 7),
    decoration: BoxDecoration(
      color: AppColors.card,
      border: Border.all(color: AppColors.line),
      borderRadius: BorderRadius.circular(6),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          text,
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: AppColors.textMain,
          ),
        ),
        const SizedBox(width: 2),
        const Icon(
          Icons.keyboard_arrow_down,
          size: 16,
          color: AppColors.textSub,
        ),
      ],
    ),
  );
}
