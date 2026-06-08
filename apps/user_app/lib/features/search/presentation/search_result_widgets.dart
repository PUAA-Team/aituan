import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_search_box.dart';
import '../../../core/widgets/brand_tag.dart';
import '../../../shared/enums/business_type.dart';
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
  const SearchCategoryRow({
    super.key,
    required this.selected,
    required this.onSelected,
  });

  final BusinessType? selected;
  final ValueChanged<BusinessType?> onSelected;

  @override
  Widget build(BuildContext context) => SingleChildScrollView(
    scrollDirection: Axis.horizontal,
    child: Row(
      children: [
        Padding(
          padding: const EdgeInsets.only(right: 8),
          child: _SelectableTag(
            label: '全部',
            selected: selected == null,
            onTap: () => onSelected(null),
          ),
        ),
        for (final type in BusinessType.values)
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: _SelectableTag(
              label: type.label,
              selected: selected == type,
              onTap: () => onSelected(type),
            ),
          ),
      ],
    ),
  );
}

class SearchFilterRow extends StatelessWidget {
  const SearchFilterRow({
    super.key,
    required this.sort,
    required this.onSortChanged,
    this.onLocationChanged,
  });

  final String sort;
  final ValueChanged<String> onSortChanged;
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
        _FilterChip(sortLabel(sort), onTap: () => _showSortSheet(context)),
      ],
    ),
  );

  Future<void> _showSortSheet(BuildContext context) async {
    final selected = await showModalBottomSheet<String>(
      context: context,
      showDragHandle: true,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            for (final option in _sortOptions)
              ListTile(
                title: Text(option.label),
                trailing: option.value == sort
                    ? const Icon(Icons.check, color: AppColors.brand)
                    : null,
                onTap: () => Navigator.pop(context, option.value),
              ),
          ],
        ),
      ),
    );
    if (selected != null) {
      onSortChanged(selected);
    }
  }
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
  Widget build(BuildContext context) => Column(
    children: [
      if (merchant.recommendReason.isNotEmpty)
        Padding(
          padding: const EdgeInsets.only(bottom: 6),
          child: Align(
            alignment: Alignment.centerLeft,
            child: BrandTag(merchant.recommendReason, emphasis: true),
          ),
        ),
      MerchantAggregateCard(
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
      ),
    ],
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

class _SelectableTag extends StatelessWidget {
  const _SelectableTag({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => InkWell(
    borderRadius: BorderRadius.circular(999),
    onTap: onTap,
    child: BrandTag(label, selected: selected, solid: selected),
  );
}

class _FilterChip extends StatelessWidget {
  const _FilterChip(this.text, {required this.onTap});

  final String text;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => InkWell(
    borderRadius: BorderRadius.circular(6),
    onTap: onTap,
    child: Container(
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
    ),
  );
}

String sortLabel(String sort) => _sortOptions
    .firstWhere(
      (option) => option.value == sort,
      orElse: () => _sortOptions.first,
    )
    .label;

const _sortOptions = [
  _SortOption('default', '综合排序'),
  _SortOption('distance', '距离最近'),
  _SortOption('rating', '评分最高'),
  _SortOption('sales', '销量最高'),
  _SortOption('price_asc', '人均低价'),
];

class _SortOption {
  const _SortOption(this.value, this.label);

  final String value;
  final String label;
}
