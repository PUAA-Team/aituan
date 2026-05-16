import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/section_header.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import '../../home/data/mock_data.dart';
import 'merchant_category_widgets.dart';
import 'service_merchant_widgets.dart';

class ServiceMerchantPage extends StatefulWidget {
  const ServiceMerchantPage({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  State<ServiceMerchantPage> createState() => _ServiceMerchantPageState();
}

class _ServiceMerchantPageState extends State<ServiceMerchantPage> {
  String? _selectedCategory;

  @override
  Widget build(BuildContext context) {
    final items = itemsForMerchant(widget.merchant);
    final groups = groupItemsByCategory(items);
    final active = _activeCategory(groups);
    return Scaffold(
      appBar: AppBar(title: const Text('商家详情')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          ServiceMerchantHero(merchant: widget.merchant),
          SectionHeader(
            title: '${widget.merchant.type.label}服务',
            action: '到店核销',
          ),
          ServiceCategoryPanel(
            groups: groups,
            activeCategory: active,
            onSelected: (value) => setState(() => _selectedCategory = value),
          ),
          const SectionHeader(title: '用户评价', action: '真实反馈'),
          const AppCard(
            child: Text(
              '环境干净，核销顺利，适合周末和朋友一起到店。',
              style: TextStyle(fontSize: 13, color: AppColors.textSub),
            ),
          ),
        ],
      ),
    );
  }

  String _activeCategory(Map<String, List<ItemModel>> groups) {
    if (groups.isEmpty) return '';
    final selected = _selectedCategory;
    return selected != null && groups.containsKey(selected)
        ? selected
        : groups.keys.first;
  }
}
