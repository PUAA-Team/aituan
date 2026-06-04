import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/section_header.dart';
import '../../../shared/models/item_model.dart';

export 'merchant_item_cards.dart';

Map<String, List<ItemModel>> groupItemsByCategory(Iterable<ItemModel> items) {
  final groups = <String, List<ItemModel>>{};
  for (final item in items) {
    groups.putIfAbsent(item.category, () => []).add(item);
  }
  return groups;
}

typedef CategoryItemBuilder =
    Widget Function(BuildContext context, ItemModel item);

class CategoryGroupedList extends StatefulWidget {
  const CategoryGroupedList({
    super.key,
    required this.groups,
    required this.activeCategory,
    required this.emptyText,
    this.headerAction,
    required this.onSelected,
    required this.itemBuilder,
  });

  final Map<String, List<ItemModel>> groups;
  final String activeCategory;
  final String emptyText;
  final String? headerAction;
  final ValueChanged<String> onSelected;
  final CategoryItemBuilder itemBuilder;

  @override
  State<CategoryGroupedList> createState() => _CategoryGroupedListState();
}

class _CategoryGroupedListState extends State<CategoryGroupedList> {
  final _keys = <String, GlobalKey>{};

  @override
  Widget build(BuildContext context) {
    if (widget.groups.isEmpty) return AppCard(child: Text(widget.emptyText));
    final categories = widget.groups.keys.toList();
    _syncKeys(categories);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        CategoryRail(
          categories: categories,
          selected: widget.activeCategory,
          onSelected: _jumpTo,
        ),
        const SizedBox(width: 10),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final entry in widget.groups.entries)
                _CategorySection(
                  key: _keys[entry.key],
                  category: entry.key,
                  items: entry.value,
                  headerAction: widget.headerAction,
                  itemBuilder: widget.itemBuilder,
                ),
            ],
          ),
        ),
      ],
    );
  }

  void _syncKeys(List<String> categories) {
    final current = categories.toSet();
    _keys.removeWhere((category, _) => !current.contains(category));
    for (final category in categories) {
      _keys.putIfAbsent(category, () => GlobalKey());
    }
  }

  void _jumpTo(String category) {
    widget.onSelected(category);
    final targetContext = _keys[category]?.currentContext;
    if (targetContext == null) return;
    Scrollable.ensureVisible(
      targetContext,
      duration: const Duration(milliseconds: 260),
      curve: Curves.easeOutCubic,
      alignment: 0.05,
    );
  }
}

class _CategorySection extends StatelessWidget {
  const _CategorySection({
    super.key,
    required this.category,
    required this.items,
    required this.headerAction,
    required this.itemBuilder,
  });

  final String category;
  final List<ItemModel> items;
  final String? headerAction;
  final CategoryItemBuilder itemBuilder;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      SectionHeader(
        title: category,
        action: headerAction,
        padding: const EdgeInsets.fromLTRB(2, 0, 2, 8),
      ),
      for (final item in items) itemBuilder(context, item),
      const SizedBox(height: 6),
    ],
  );
}

class CategoryRail extends StatelessWidget {
  const CategoryRail({
    super.key,
    required this.categories,
    required this.selected,
    required this.onSelected,
  });

  final List<String> categories;
  final String selected;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) => SizedBox(
    width: 76,
    child: Column(
      children: [
        for (final category in categories)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: InkWell(
              borderRadius: BorderRadius.circular(8),
              onTap: () => onSelected(category),
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(vertical: 9),
                decoration: BoxDecoration(
                  color: selected == category
                      ? AppColors.brandSoft
                      : AppColors.soft,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(
                    color: selected == category
                        ? AppColors.brandLine
                        : AppColors.line,
                  ),
                ),
                alignment: Alignment.center,
                child: Text(
                  category,
                  textAlign: TextAlign.center,
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: selected == category
                        ? FontWeight.w800
                        : FontWeight.w600,
                    color: selected == category
                        ? AppColors.brand
                        : AppColors.textSub,
                  ),
                ),
              ),
            ),
          ),
      ],
    ),
  );
}
