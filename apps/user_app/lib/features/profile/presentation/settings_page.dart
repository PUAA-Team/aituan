import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../home/data/backend_app_repository.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  bool _messageEnabled = true;
  bool _locationEnabled = true;
  bool _busy = false;

  @override
  Widget build(BuildContext context) {
    final state = AppScope.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('设置')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AppCard(
            child: Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: AppColors.brand,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    state.displayName.isEmpty ? '爱' : state.displayName.substring(0, 1),
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(state.displayName, style: Theme.of(context).textTheme.titleMedium),
                      const SizedBox(height: 4),
                      Text(_accountLine(state), style: Theme.of(context).textTheme.bodySmall),
                    ],
                  ),
                ),
              ],
            ),
          ),
          AppCard(
            child: Column(
              children: [
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  value: _messageEnabled,
                  activeThumbColor: AppColors.brand,
                  title: const Text('消息提醒'),
                  subtitle: const Text('接收订单、优惠和系统通知'),
                  onChanged: (value) => setState(() => _messageEnabled = value),
                ),
                const Divider(),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  value: _locationEnabled,
                  activeThumbColor: AppColors.brand,
                  title: const Text('位置服务'),
                  subtitle: const Text('用于展示附近商户距离和预计送达时间'),
                  onChanged: (value) => setState(() => _locationEnabled = value),
                ),
              ],
            ),
          ),
          AppCard(
            child: Column(
              children: [
                _ActionRow(
                  icon: Icons.cleaning_services_outlined,
                  title: '清理缓存',
                  subtitle: '释放临时图片和页面缓存',
                  onTap: () => showAppSnackBar(context, '缓存已清理'),
                ),
                const Divider(),
                _ActionRow(
                  icon: Icons.privacy_tip_outlined,
                  title: '隐私设置',
                  subtitle: '管理定位、图片和消息授权',
                  onTap: () => showAppSnackBar(context, '隐私设置将在后续版本开放更多选项'),
                ),
                const Divider(),
                _ActionRow(
                  icon: Icons.info_outline,
                  title: '关于爱团',
                  subtitle: '版本、协议和平台介绍',
                  onTap: () => Navigator.pushNamed(context, Routes.about),
                ),
              ],
            ),
          ),
          if (state.isLoggedIn)
            FilledButton(
              onPressed: _busy ? null : () => _logout(state),
              child: Text(_busy ? '退出中' : '退出登录'),
            ),
        ],
      ),
    );
  }

  Future<void> _logout(AppState state) async {
    try {
      setState(() => _busy = true);
      try {
        await backendRepository.logout();
      } catch (_) {}
      state.logout();
      if (!mounted) return;
      Navigator.pushNamedAndRemoveUntil(context, Routes.main, (_) => false);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String _accountLine(AppState state) {
    final contacts = [state.phone, state.email]
        .where((value) => value != null && value.isNotEmpty)
        .join(' · ');
    if (!state.isLoggedIn) return '未登录用户';
    return contacts.isEmpty ? state.memberLevelName : '${state.memberLevelName} · $contacts';
  }
}

class _ActionRow extends StatelessWidget {
  const _ActionRow({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => ListTile(
    contentPadding: EdgeInsets.zero,
    visualDensity: VisualDensity.compact,
    leading: Container(
      width: 34,
      height: 34,
      decoration: BoxDecoration(
        color: AppColors.soft,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Icon(icon, color: AppColors.brand, size: 19),
    ),
    title: Text(title, style: Theme.of(context).textTheme.titleSmall),
    subtitle: Text(subtitle),
    trailing: const Icon(Icons.chevron_right, color: AppColors.textLight),
    onTap: onTap,
  );
}
