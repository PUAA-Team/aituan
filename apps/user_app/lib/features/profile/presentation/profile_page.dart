import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key});

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
        children: [
          AppCard(
            backgroundColor: AppColors.brandSoft,
            borderColor: AppColors.brandLine,
            child: Row(
              children: [
                Container(
                  width: 58,
                  height: 58,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: AppColors.brand,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: const Text(
                    '爱',
                    style: TextStyle(
                      color: Colors.white,
                      fontSize: 24,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        state.isLoggedIn ? '爱团用户' : '游客',
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '爱团本地生活会员',
                        style: Theme.of(context).textTheme.bodySmall,
                      ),
                    ],
                  ),
                ),
                TextButton(onPressed: state.logout, child: const Text('退出')),
              ],
            ),
          ),
          const _EntryGrid(),
          AppCard(
            child: Column(
              children: const [
                _ToolRow(icon: Icons.place_outlined, title: '地址管理'),
                Divider(),
                _ToolRow(icon: Icons.rate_review_outlined, title: '我的评价'),
                Divider(),
                _ToolRow(icon: Icons.support_agent, title: '客服咨询'),
                Divider(),
                _ToolRow(icon: Icons.workspace_premium_outlined, title: '会员中心'),
              ],
            ),
          ),
          const AppCard(
            child: Column(
              children: [_StaticRow('设置'), Divider(), _StaticRow('关于爱团')],
            ),
          ),
        ],
      ),
    );
  }
}

class _EntryGrid extends StatelessWidget {
  const _EntryGrid();

  @override
  Widget build(BuildContext context) {
    final entries = [
      ('订单', Icons.receipt_long, Routes.orders),
      ('消息', Icons.chat_bubble_outline, Routes.message),
      ('收藏', Icons.favorite_border, Routes.favorite),
      ('优惠券', Icons.confirmation_number_outlined, ''),
    ];
    return AppCard(
      child: Row(
        children: [
          for (final entry in entries)
            Expanded(
              child: InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: entry.$3.isEmpty
                    ? null
                    : () => Navigator.pushNamed(context, entry.$3),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 2),
                  child: Column(
                    children: [
                      Container(
                        width: 38,
                        height: 38,
                        decoration: BoxDecoration(
                          color: AppColors.brandSoft,
                          borderRadius: BorderRadius.circular(9),
                        ),
                        child: Icon(entry.$2, color: AppColors.brand, size: 21),
                      ),
                      const SizedBox(height: 7),
                      Text(
                        entry.$1,
                        style: Theme.of(context).textTheme.labelMedium,
                      ),
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _ToolRow extends StatelessWidget {
  const _ToolRow({required this.icon, required this.title});

  final IconData icon;
  final String title;

  @override
  Widget build(BuildContext context) => ListTile(
    minLeadingWidth: 0,
    contentPadding: EdgeInsets.zero,
    visualDensity: VisualDensity.compact,
    leading: _IconBox(icon),
    title: Text(title, style: Theme.of(context).textTheme.titleSmall),
    trailing: const Icon(Icons.chevron_right, color: AppColors.textLight),
  );
}

class _StaticRow extends StatelessWidget {
  const _StaticRow(this.title);

  final String title;

  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    visualDensity: VisualDensity.compact,
    title: Text(title, style: Theme.of(context).textTheme.titleSmall),
    trailing: const Icon(Icons.chevron_right, color: AppColors.textLight),
  );
}

class _IconBox extends StatelessWidget {
  const _IconBox(this.icon);

  final IconData icon;

  @override
  Widget build(BuildContext context) => Container(
    width: 32,
    height: 32,
    decoration: BoxDecoration(
      color: AppColors.soft,
      borderRadius: BorderRadius.circular(8),
    ),
    child: Icon(icon, color: AppColors.brand, size: 19),
  );
}
