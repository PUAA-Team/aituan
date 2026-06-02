import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../core/widgets/cached_app_image.dart';
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
  bool _avatarBusy = false;

  @override
  void initState() {
    super.initState();
    _loadProfile();
  }

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    final profile = state.isLoggedIn ? _profile : null;
    final name = state.isLoggedIn
        ? (profile?.nickname ?? state.displayName)
        : '游客';
    final member = profile?.memberLevelName ?? state.memberLevelName;
    return SafeArea(
      child: RefreshIndicator(
        onRefresh: _loadProfile,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
          children: [
            AppCard(
              backgroundColor: AppColors.brandSoft,
              borderColor: AppColors.brandLine,
              onTap: state.isLoggedIn
                  ? () => _openAndRefresh(Routes.profileEdit)
                  : null,
              child: Row(
                children: [
                  _Avatar(
                    name: name,
                    avatarUrl: profile?.avatarUrl ?? state.avatarUrl,
                    busy: _avatarBusy,
                    onTap: state.isLoggedIn ? _pickAvatar : null,
                  ),
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
                          _memberLine(profile, member, state.isLoggedIn),
                          style: Theme.of(context).textTheme.bodySmall,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          state.isLoggedIn ? '点击查看和编辑个人资料' : '登录后可同步会员信息',
                          style: Theme.of(context).textTheme.labelSmall,
                        ),
                      ],
                    ),
                  ),
                  TextButton(
                    onPressed: state.isLoggedIn
                        ? () => _logout(state)
                        : () async {
                            await Navigator.pushNamed(context, Routes.login);
                            if (mounted) _loadProfile();
                          },
                    child: Text(state.isLoggedIn ? '退出' : '登录'),
                  ),
                ],
              ),
            ),
            if (_loading)
              const AppCard(child: Center(child: CircularProgressIndicator()))
            else if (_error != null)
              _ErrorCard(message: _error.toString(), onRetry: _loadProfile),
            _EntryGrid(profile: profile, onReturned: _loadProfile),
            AppCard(
              child: Column(
                children: [
                  _ToolRow(
                    icon: Icons.place_outlined,
                    title: '地址管理',
                    onTap: () => _openAndRefresh(Routes.addressList),
                  ),
                  const Divider(),
                  _ToolRow(
                    icon: Icons.rate_review_outlined,
                    title: '我的评价',
                    onTap: () => _openAndRefresh(Routes.myReviews),
                  ),
                  const Divider(),
                  _ToolRow(
                    icon: Icons.support_agent,
                    title: '客服咨询',
                    onTap: () => _openAndRefresh(Routes.supportSessions),
                  ),
                  const Divider(),
                  _ToolRow(
                    icon: Icons.report_outlined,
                    title: '投诉与建议',
                    onTap: () => _openAndRefresh(Routes.complaintSubmit),
                  ),
                  const Divider(),
                  const _ToolRow(
                    icon: Icons.workspace_premium_outlined,
                    title: '会员中心',
                  ),
                ],
              ),
            ),
            AppCard(
              child: Column(
                children: [
                  _StaticRow(
                    title: '设置',
                    onTap: () => Navigator.pushNamed(context, Routes.settings),
                  ),
                  const Divider(),
                  _StaticRow(
                    title: '关于爱团',
                    onTap: () => Navigator.pushNamed(context, Routes.about),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _loadProfile() async {
    if (!appState.isLoggedIn) {
      setState(() {
        _profile = null;
        _error = null;
        _loading = false;
      });
      return;
    }
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final profile = await backendRepository.fetchProfile();
      appState.updateProfile(
        displayName: profile.nickname,
        avatarUrl: profile.avatarUrl,
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
    if (!mounted) return;
    setState(() {
      _profile = null;
      _error = null;
      _loading = false;
    });
  }

  Future<void> _pickAvatar() async {
    try {
      final picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        maxWidth: 900,
        imageQuality: 85,
      );
      if (picked == null) return;
      setState(() => _avatarBusy = true);
      final profile = await backendRepository.uploadAvatar(picked);
      appState.updateProfile(
        displayName: profile.nickname,
        avatarUrl: profile.avatarUrl,
        phone: profile.phone,
        email: profile.email,
        memberLevelName: profile.memberLevelName,
        unreadMessageCount: profile.unreadMessageCount,
      );
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _avatarBusy = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _avatarBusy = false);
      showAppSnackBar(context, '头像上传失败：$error');
    }
  }

  Future<void> _openAndRefresh(String route) async {
    await Navigator.pushNamed(context, route);
    if (mounted) await _loadProfile();
  }

  String _memberLine(ProfileData? profile, String member, bool loggedIn) {
    if (!loggedIn) return '登录后查看会员权益和订单提醒';
    if (profile == null) return '$member · 爱团本地生活会员';
    return '$member · 成长值 ${profile.growthValue}';
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar({
    required this.name,
    required this.avatarUrl,
    required this.busy,
    required this.onTap,
  });

  final String name;
  final String? avatarUrl;
  final bool busy;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final fallback = Text(
      name.isEmpty ? '爱' : name.substring(0, 1),
      style: const TextStyle(
        color: Colors.white,
        fontSize: 24,
        fontWeight: FontWeight.w800,
      ),
    );
    return InkWell(
      onTap: busy ? null : onTap,
      borderRadius: BorderRadius.circular(16),
      child: Stack(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: Container(
              width: 58,
              height: 58,
              alignment: Alignment.center,
              color: AppColors.brand,
              child: CachedAppImage(
                imageUrl: avatarUrl,
                width: 58,
                height: 58,
                placeholder: fallback,
              ),
            ),
          ),
          if (busy)
            const Positioned.fill(
              child: ColoredBox(
                color: Color(0x66000000),
                child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
              ),
            ),
        ],
      ),
    );
  }
}

class _EntryGrid extends StatelessWidget {
  const _EntryGrid({required this.profile, required this.onReturned});

  final ProfileData? profile;
  final Future<void> Function() onReturned;

  @override
  Widget build(BuildContext context) {
    final entries = [
      ('订单', Icons.receipt_long, Routes.orders, profile?.orderCount),
      (
        '消息',
        Icons.chat_bubble_outline,
        Routes.message,
        profile?.unreadMessageCount,
      ),
      ('收藏', Icons.favorite_border, Routes.favorite, profile?.favoriteCount),
      ('地址', Icons.place_outlined, Routes.addressList, profile?.addressCount),
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
                    : () async {
                        await Navigator.pushNamed(context, entry.$3);
                        await onReturned();
                      },
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
  const _ToolRow({required this.icon, required this.title, this.onTap});

  final IconData icon;
  final String title;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) => ListTile(
    minLeadingWidth: 0,
    contentPadding: EdgeInsets.zero,
    visualDensity: VisualDensity.compact,
    leading: _IconBox(icon),
    title: Text(title, style: Theme.of(context).textTheme.titleSmall),
    trailing: const Icon(Icons.chevron_right, color: AppColors.textLight),
    onTap: onTap,
  );
}

class _StaticRow extends StatelessWidget {
  const _StaticRow({required this.title, required this.onTap});

  final String title;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    visualDensity: VisualDensity.compact,
    title: Text(title, style: Theme.of(context).textTheme.titleSmall),
    trailing: const Icon(Icons.chevron_right, color: AppColors.textLight),
    onTap: onTap,
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
