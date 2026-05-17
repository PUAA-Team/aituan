import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../home/data/backend_app_repository.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key});

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  ProfileData? _profile;
  Object? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final profile = _profile;
    final name = profile?.nickname ?? state.displayName;
    final member = profile?.memberLevelName ?? state.memberLevelName;
    return SafeArea(
      child: RefreshIndicator(
        onRefresh: _loadProfile,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
          children: [
            AppCard(
              backgroundColor: AppColors.brandSoft,
              borderColor: AppColors.brandLine,
              child: Row(
                children: [
                  _Avatar(name),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          name,
                          style: Theme.of(context).textTheme.titleLarge,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          _memberLine(profile, member),
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          _contactLine(profile, state),
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                      ],
                    ),
                  ),
                  TextButton(
                    onPressed: () => _logout(state),
                    child: const Text('退出'),
                  ),
                ],
              ),
            ),
            if (_loading)
              const AppCard(child: Center(child: CircularProgressIndicator()))
            else if (_error != null)
              _ErrorCard(message: _error.toString(), onRetry: _loadProfile),
            _EntryGrid(profile: profile),
            AppCard(
              child: Column(
                children: const [
                  _ToolRow(icon: Icons.place_outlined, title: '地址管理'),
                  Divider(),
                  _ToolRow(icon: Icons.rate_review_outlined, title: '我的评价'),
                  Divider(),
                  _ToolRow(icon: Icons.support_agent, title: '客服咨询'),
                  Divider(),
                  _ToolRow(
                    icon: Icons.workspace_premium_outlined,
                    title: '会员中心',
                  ),
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
      ),
    );
  }

  Future<void> _loadProfile() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final profile = await backendRepository.fetchProfile();
      appState.updateProfile(
        displayName: profile.nickname,
        phone: profile.phone,
        email: profile.email,
        memberLevelName: profile.memberLevelName,
        unreadMessageCount: profile.unreadMessageCount,
      );
      if (!mounted) return;
      setState(() {
        _profile = profile;
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

  Future<void> _logout(AppState state) async {
    try {
      await backendRepository.logout();
    } catch (_) {}
    state.logout();
  }

  String _memberLine(ProfileData? profile, String member) {
    if (profile == null) return '$member · 爱团本地生活会员';
    return '$member · 成长值 ${profile.growthValue}';
  }

  String _contactLine(ProfileData? profile, AppState state) {
    final phone = profile?.phone ?? state.phone;
    final email = profile?.email ?? state.email;
    if (phone != null && email != null) return '$phone · $email';
    return phone ?? email ?? '完善资料后可获得更多服务提醒';
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar(this.name);

  final String name;

  @override
  Widget build(BuildContext context) => Container(
    width: 58,
    height: 58,
    alignment: Alignment.center,
    decoration: BoxDecoration(
      color: AppColors.brand,
      borderRadius: BorderRadius.circular(16),
    ),
    child: Text(
      name.isEmpty ? '爱' : name.substring(0, 1),
      style: const TextStyle(
        color: Colors.white,
        fontSize: 24,
        fontWeight: FontWeight.w800,
      ),
    ),
  );
}

class _EntryGrid extends StatelessWidget {
  const _EntryGrid({required this.profile});

  final ProfileData? profile;

  @override
  Widget build(BuildContext context) {
    final entries = [
      ('订单', Icons.receipt_long, Routes.orders, null),
      (
        '消息',
        Icons.chat_bubble_outline,
        Routes.message,
        profile?.unreadMessageCount,
      ),
      ('收藏', Icons.favorite_border, Routes.favorite, profile?.favoriteCount),
      ('地址', Icons.place_outlined, '', profile?.addressCount),
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
                      if (entry.$4 != null)
                        Text(
                          '${entry.$4}',
                          style: Theme.of(context).textTheme.labelSmall,
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

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('资料加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
