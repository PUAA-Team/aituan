import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../core/widgets/cached_app_image.dart';
import '../../home/data/backend_app_repository.dart';

class ProfileEditPage extends StatefulWidget {
  const ProfileEditPage({super.key});

  @override
  State<ProfileEditPage> createState() => _ProfileEditPageState();
}

class _ProfileEditPageState extends State<ProfileEditPage> {
  final _nicknameController = TextEditingController();
  final _oldPasswordController = TextEditingController();
  final _newPasswordController = TextEditingController();
  ProfileData? _profile;
  bool _loading = true;
  bool _saving = false;
  bool _avatarBusy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _nicknameController.dispose();
    _oldPasswordController.dispose();
    _newPasswordController.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final profile = await backendRepository.fetchProfile();
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _nicknameController.text = profile.nickname;
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _loading = false);
      showAppSnackBar(context, '资料加载失败：$error');
    }
  }

  Future<void> _saveProfile() async {
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
      _syncProfile(profile);
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _saving = false;
      });
      showAppSnackBar(context, '资料已保存');
    } catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      showAppSnackBar(context, '保存失败：$error');
    }
  }

  Future<void> _changePassword() async {
    final oldPassword = _oldPasswordController.text;
    final newPassword = _newPasswordController.text;
    if (oldPassword.isEmpty || newPassword.length < 6) {
      showAppSnackBar(context, '请填写旧密码，新密码不少于 6 位');
      return;
    }
    try {
      setState(() => _saving = true);
      await backendRepository.changePassword(
        oldPassword: oldPassword,
        newPassword: newPassword,
      );
      if (!mounted) return;
      setState(() {
        _oldPasswordController.clear();
        _newPasswordController.clear();
        _saving = false;
      });
      showAppSnackBar(context, '密码已修改');
    } catch (error) {
      if (!mounted) return;
      setState(() => _saving = false);
      showAppSnackBar(context, '密码修改失败：$error');
    }
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
      final profile = await backendRepository.uploadAvatar(picked.path);
      _syncProfile(profile);
      if (!mounted) return;
      setState(() {
        _profile = profile;
        _avatarBusy = false;
      });
      showAppSnackBar(context, '头像已更新');
    } catch (error) {
      if (!mounted) return;
      setState(() => _avatarBusy = false);
      showAppSnackBar(context, '头像上传失败：$error');
    }
  }

  void _syncProfile(ProfileData profile) {
    appState.updateProfile(
      displayName: profile.nickname,
      avatarUrl: profile.avatarUrl,
      phone: profile.phone,
      email: profile.email,
      memberLevelName: profile.memberLevelName,
      unreadMessageCount: profile.unreadMessageCount,
    );
  }

  @override
  Widget build(BuildContext context) {
    final profile = _profile;
    return Scaffold(
      appBar: AppBar(title: const Text('编辑资料')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                AppCard(
                  child: Row(
                    children: [
                      _AvatarPreview(
                        name: profile?.nickname ?? '爱团用户',
                        avatarUrl: profile?.avatarUrl,
                        busy: _avatarBusy,
                        onTap: _avatarBusy ? null : _pickAvatar,
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '头像',
                              style: Theme.of(context).textTheme.titleMedium,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '点击头像更换',
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                AppCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        '基础资料',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _nicknameController,
                        decoration: const InputDecoration(labelText: '昵称'),
                      ),
                      const SizedBox(height: 12),
                      _ReadonlyProfileRow(label: '手机号', value: profile?.phone),
                      const Divider(height: 18),
                      _ReadonlyProfileRow(label: '邮箱', value: profile?.email),
                      const SizedBox(height: 12),
                      FilledButton(
                        onPressed: _saving ? null : _saveProfile,
                        child: Text(_saving ? '保存中' : '保存资料'),
                      ),
                    ],
                  ),
                ),
                AppCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Text(
                        '修改密码',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _oldPasswordController,
                        obscureText: true,
                        decoration: const InputDecoration(labelText: '旧密码'),
                      ),
                      const SizedBox(height: 10),
                      TextField(
                        controller: _newPasswordController,
                        obscureText: true,
                        decoration: const InputDecoration(labelText: '新密码'),
                      ),
                      const SizedBox(height: 12),
                      OutlinedButton(
                        onPressed: _saving ? null : _changePassword,
                        child: const Text('修改密码'),
                      ),
                    ],
                  ),
                ),
              ],
            ),
    );
  }
}

class _ReadonlyProfileRow extends StatelessWidget {
  const _ReadonlyProfileRow({required this.label, required this.value});

  final String label;
  final String? value;

  @override
  Widget build(BuildContext context) {
    final text = value == null || value!.isEmpty ? '未绑定' : value!;
    final bound = value != null && value!.isNotEmpty;
    return Row(
      children: [
        SizedBox(
          width: 64,
          child: Text(label, style: Theme.of(context).textTheme.bodyMedium),
        ),
        Expanded(
          child: Text(
            text,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
        ),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
          decoration: BoxDecoration(
            color: bound ? AppColors.brandSoft : AppColors.soft,
            borderRadius: BorderRadius.circular(999),
            border: Border.all(
              color: bound ? AppColors.brandLine : AppColors.line,
            ),
          ),
          child: Text(
            bound ? '已绑定' : '未绑定',
            style: TextStyle(
              color: bound ? AppColors.brand : AppColors.textSub,
              fontSize: 12,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ],
    );
  }
}

class _AvatarPreview extends StatelessWidget {
  const _AvatarPreview({
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
      name.isEmpty ? '爱' : name.characters.first,
      style: const TextStyle(
        color: Colors.white,
        fontSize: 28,
        fontWeight: FontWeight.w900,
      ),
    );
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Stack(
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: Container(
              width: 72,
              height: 72,
              alignment: Alignment.center,
              color: AppColors.brand,
              child: CachedAppImage(
                imageUrl: avatarUrl,
                fit: BoxFit.cover,
                width: 72,
                height: 72,
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
