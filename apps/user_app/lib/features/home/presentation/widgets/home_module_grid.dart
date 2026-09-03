import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_tokens.dart';
import '../../../../core/constants/route_constants.dart';
import '../../../../core/widgets/app_card.dart';
import '../../../../shared/models/module_entry.dart';

class HomeModuleGrid extends StatelessWidget {
  const HomeModuleGrid({super.key, required this.modules});

  final List<ModuleEntry> modules;

  @override
  Widget build(BuildContext context) => AppCard(
    padding: const EdgeInsets.fromLTRB(14, 12, 14, 14),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text('生活服务', style: Theme.of(context).textTheme.titleMedium),
            const Spacer(),
            const Text(
              '附近生活',
              style: TextStyle(fontSize: 13, color: AppColors.textSub),
            ),
          ],
        ),
        const SizedBox(height: 12),
        GridView.count(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisCount: 4,
          mainAxisSpacing: 12,
          crossAxisSpacing: 10,
          childAspectRatio: .92,
          children: [
            for (final module in modules) _ModuleEntryTile(module: module),
          ],
        ),
      ],
    ),
  );
}

class _ModuleEntryTile extends StatelessWidget {
  const _ModuleEntryTile({required this.module});

  final ModuleEntry module;

  @override
  Widget build(BuildContext context) => InkWell(
    borderRadius: BorderRadius.circular(10),
    onTap: () => Navigator.pushNamed(context, Routes.module, arguments: module),
    child: Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: AppTokens.moduleIcon,
          height: AppTokens.moduleIcon,
          decoration: BoxDecoration(
            color: AppColors.brandSoft,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: AppColors.brandLine),
          ),
          child: Icon(_icon(module.code), color: AppColors.brand, size: 23),
        ),
        const SizedBox(height: 7),
        Text(
          _shortTitle(module.title),
          style: const TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: AppColors.textMain,
          ),
        ),
      ],
    ),
  );
}

IconData _icon(String code) => switch (code) {
  'takeaway' => Icons.delivery_dining,
  'hotel' => Icons.hotel,
  'movie' => Icons.movie,
  'massage' => Icons.spa,
  'beauty' => Icons.face_retouching_natural,
  'ticket' => Icons.confirmation_number,
  'fun' || 'entertainment' => Icons.sports_esports,
  _ => Icons.local_activity,
};

String _shortTitle(String title) => switch (title) {
  '休闲娱乐' => '娱乐',
  '电影演出' => '电影',
  '丽人医美' => '丽人',
  '景点门票' => '门票',
  _ => title,
};
