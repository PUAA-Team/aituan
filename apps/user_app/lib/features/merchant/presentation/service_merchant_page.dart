import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/section_header.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import '../../home/data/backend_app_repository.dart';
import 'merchant_category_widgets.dart';
import 'service_merchant_widgets.dart';

class ServiceMerchantPage extends StatefulWidget {
  const ServiceMerchantPage({super.key, required this.merchant});

  final MerchantModel merchant;

  @override
  State<ServiceMerchantPage> createState() => _ServiceMerchantPageState();
}

class _ServiceMerchantPageState extends State<ServiceMerchantPage> {
  MerchantModel? _merchant;
  Object? _error;
  bool _loading = true;
  String? _selectedCategory;

  String? get _storeId =>
      int.tryParse(widget.merchant.id) == null ? null : widget.merchant.id;

  @override
  void initState() {
    super.initState();
    _merchant = widget.merchant;
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final merchant = _merchant ?? widget.merchant;
    final groups = groupItemsByCategory(merchant.items);
    final active = _activeCategory(groups);
    return Scaffold(
      appBar: AppBar(title: const Text('商家详情')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else ...[
            ServiceMerchantHero(merchant: merchant),
            SectionHeader(title: '${merchant.type.label}服务', action: '到店核销'),
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
        ],
      ),
    );
  }

  Future<void> _load() async {
    final storeId = _storeId;
    if (storeId == null) {
      setState(() => _loading = false);
      return;
    }
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final merchant = await backendRepository.fetchStore(int.parse(storeId));
      if (!mounted) return;
      setState(() {
        _merchant = merchant;
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

  String _activeCategory(Map<String, List<ItemModel>> groups) {
    if (groups.isEmpty) return '';
    final selected = _selectedCategory;
    return selected != null && groups.containsKey(selected)
        ? selected
        : groups.keys.first;
  }
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
        Text('商家加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
