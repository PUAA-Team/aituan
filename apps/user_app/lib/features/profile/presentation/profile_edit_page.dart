import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../home/data/backend_app_repository.dart';

class ProfileEditPage extends StatefulWidget {
  const ProfileEditPage({super.key});

  @override
  State<ProfileEditPage> createState() => _ProfileEditPageState();
}

class _ProfileEditPageState extends State<ProfileEditPage> {
  final _nicknameController = TextEditingController();
  ProfileData? _profile;
  Object? _error;
  bool _loading = true;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('编辑资料')),
    body: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else
            _ProfileForm(
              controller: _nicknameController,
              profile: _profile,
              saving: _saving,
              onSubmit: _submit,
            ),
        ],
      ),
    ),
  );

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final profile = await backendRepository.fetchProfile();
      if (!mounted) return;
      _nicknameController.text = profile.nickname;
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

  Future<void> _submit() async {
    final nickname = _nicknameController.text.trim();
    if (nickname.isEmpty) {
      showAppSnackBar(context, '昵称不能为空');
      return;
    }
    try {
      setState(() => _saving = true);
      final profile = await backendRepository.updateProfile(
        nickname: nickname,
        avatarUrl: _profile?.avatarUrl,
      );
      appState.updateProfile(
        displayName: profile.nickname,
        avatarUrl: profile.avatarUrl,
        phone: profile.phone,
        email: profile.email,
        memberLevelName: profile.memberLevelName,
        unreadMessageCount: profile.unreadMessageCount,
      );
      if (!mounted) return;
      showAppSnackBar(context, '资料已保存');
      Navigator.pop(context, profile);
    } catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      showAppSnackBar(context, '保存失败：$error');
    }
  }
}

class _ProfileForm extends StatelessWidget {
  const _ProfileForm({
    required this.controller,
    required this.profile,
    required this.saving,
    required this.onSubmit,
  });

  final TextEditingController controller;
  final ProfileData? profile;
  final bool saving;
  final VoidCallback onSubmit;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('基础资料', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 12),
        TextField(
          controller: controller,
          maxLength: 20,
          decoration: const InputDecoration(
            labelText: '昵称',
            hintText: '请输入昵称',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 8),
        _InfoRow(label: '手机号', value: profile?.phone ?? '未绑定'),
        _InfoRow(label: '邮箱', value: profile?.email ?? '未绑定'),
        const SizedBox(height: 8),
        Text(
          '手机号和邮箱仅展示脱敏信息，当前阶段暂不支持在 APP 内换绑。',
          style: Theme.of(
            context,
          ).textTheme.bodySmall?.copyWith(color: AppColors.textSub),
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          child: FilledButton(
            onPressed: saving ? null : onSubmit,
            child: Text(saving ? '保存中' : '保存'),
          ),
        ),
      ],
    ),
  );
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 5),
    child: Row(
      children: [
        Text(label, style: Theme.of(context).textTheme.bodySmall),
        const Spacer(),
        Text(value, style: const TextStyle(fontWeight: FontWeight.w700)),
      ],
    ),
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
