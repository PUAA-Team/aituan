import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/utils/validator.dart';
import '../../../core/widgets/app_card.dart';
import '../../home/data/backend_app_repository.dart';

enum AuthMode { login, register, reset }

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, this.showNotice = false});

  final bool showNotice;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  AuthMode _mode = AuthMode.login;
  bool _loading = false;
  final _account = TextEditingController(text: '18800001111');
  final _email = TextEditingController(text: 'user@example.com');
  final _code = TextEditingController();
  final _password = TextEditingController(text: '123456');

  @override
  void dispose() {
    _account.dispose();
    _email.dispose();
    _code.dispose();
    _password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final title = switch (_mode) {
      AuthMode.login => '欢迎回来',
      AuthMode.register => '创建爱团账号',
      AuthMode.reset => '找回密码',
    };
    return Scaffold(
      appBar: AppBar(title: const Text('爱团账号')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(18, 12, 18, 20),
        children: [
          if (widget.showNotice) const _Notice(),
          const SizedBox(height: 10),
          const _BrandHeader(),
          const SizedBox(height: 18),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(title, style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 4),
                Text(
                  '手机号或邮箱登录，注册和找回密码使用邮箱验证码',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 16),
                if (_mode != AuthMode.reset)
                  TextField(
                    controller: _account,
                    decoration: InputDecoration(
                      labelText: _mode == AuthMode.register ? '手机号' : '手机号或邮箱',
                    ),
                  ),
                if (_mode != AuthMode.reset) const SizedBox(height: 10),
                if (_mode != AuthMode.login)
                  TextField(
                    controller: _email,
                    decoration: const InputDecoration(labelText: '邮箱'),
                  ),
                if (_mode != AuthMode.login) const SizedBox(height: 10),
                if (_mode != AuthMode.login)
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _code,
                          decoration: const InputDecoration(labelText: '邮箱验证码'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      TextButton(
                        onPressed: _loading ? null : _sendCode,
                        child: const Text('获取验证码'),
                      ),
                    ],
                  ),
                if (_mode != AuthMode.login) const SizedBox(height: 10),
                TextField(
                  controller: _password,
                  obscureText: true,
                  decoration: InputDecoration(
                    labelText: _mode == AuthMode.reset ? '新密码' : '密码',
                  ),
                ),
                const SizedBox(height: 18),
                FilledButton(
                  onPressed: _loading ? null : _submit,
                  child: Text(
                    _loading
                        ? '请稍候'
                        : _mode == AuthMode.login
                        ? '登录'
                        : _mode == AuthMode.register
                        ? '注册'
                        : '确认',
                  ),
                ),
                const SizedBox(height: 10),
                if (_mode == AuthMode.reset)
                  Align(
                    alignment: Alignment.centerLeft,
                    child: TextButton(
                      onPressed: () => _setMode(AuthMode.login),
                      child: const Text('返回登录'),
                    ),
                  )
                else
                  Row(
                    children: [
                      TextButton(
                        onPressed: () => _setMode(
                          _mode == AuthMode.register
                              ? AuthMode.login
                              : AuthMode.register,
                        ),
                        child: Text(
                          _mode == AuthMode.register ? '返回登录' : '前往注册',
                        ),
                      ),
                      const Spacer(),
                      TextButton(
                        onPressed: () => _setMode(AuthMode.reset),
                        child: const Text('忘记密码'),
                      ),
                    ],
                  ),
              ],
            ),
          ),
          TextButton(
            onPressed: () => Navigator.pushNamedAndRemoveUntil(
              context,
              Routes.main,
              (_) => false,
            ),
            child: const Text('游客进入首页'),
          ),
        ],
      ),
    );
  }

  Future<void> _submit() async {
    final messenger = ScaffoldMessenger.of(context);
    try {
      setState(() => _loading = true);
      switch (_mode) {
        case AuthMode.login:
          if (!Validator.isAccount(_account.text.trim())) {
            messenger.showSnackBar(
              const SnackBar(content: Text('请输入 11 位手机号或正确邮箱')),
            );
            return;
          }
          final session = await backendRepository.login(
            _account.text.trim(),
            _password.text,
          );
          _applySession(session);
          if (!mounted) return;
          Navigator.pushNamedAndRemoveUntil(context, Routes.main, (_) => false);
          return;
        case AuthMode.register:
          if (!Validator.isPhone(_account.text.trim())) {
            messenger.showSnackBar(
              const SnackBar(content: Text('请填写 11 位手机号')),
            );
            return;
          }
          if (!Validator.isEmail(_email.text.trim())) {
            messenger.showSnackBar(const SnackBar(content: Text('请填写正确邮箱')));
            return;
          }
          if (_code.text.trim().isEmpty) {
            messenger.showSnackBar(const SnackBar(content: Text('请先获取邮箱验证码')));
            return;
          }
          final session = await backendRepository.register(
            phone: _account.text.trim(),
            email: _email.text.trim(),
            emailCode: _code.text.trim(),
            password: _password.text,
          );
          _applySession(session);
          if (!mounted) return;
          Navigator.pushNamedAndRemoveUntil(context, Routes.main, (_) => false);
          return;
        case AuthMode.reset:
          if (!Validator.isEmail(_email.text.trim())) {
            messenger.showSnackBar(const SnackBar(content: Text('请填写正确邮箱')));
            return;
          }
          if (_code.text.trim().isEmpty) {
            messenger.showSnackBar(const SnackBar(content: Text('请先获取邮箱验证码')));
            return;
          }
          await backendRepository.resetPassword(
            email: _email.text.trim(),
            emailCode: _code.text.trim(),
            newPassword: _password.text,
          );
          if (!mounted) return;
          messenger.showSnackBar(const SnackBar(content: Text('密码已更新，请重新登录')));
          _setMode(AuthMode.login);
          return;
      }
    } catch (error) {
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(error.toString())));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _sendCode() async {
    final messenger = ScaffoldMessenger.of(context);
    if (!Validator.isEmail(_email.text.trim())) {
      messenger.showSnackBar(const SnackBar(content: Text('请先填写正确邮箱')));
      return;
    }
    try {
      setState(() => _loading = true);
      final code = await backendRepository.sendEmailCode(
        _email.text.trim(),
        _mode == AuthMode.register ? 'register' : 'reset_password',
      );
      if (!mounted) return;
      _code.text = code;
      messenger.showSnackBar(SnackBar(content: Text('验证码已发送：$code')));
    } catch (error) {
      if (!mounted) return;
      messenger.showSnackBar(SnackBar(content: Text(error.toString())));
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _applySession(AuthSession session) {
    AppScope.of(context).login(
      token: session.token,
      displayName: session.nickname,
      avatarUrl: session.avatarUrl,
      phone: session.phone,
      email: session.email,
      memberLevelName: session.memberLevelName,
      unreadMessageCount: session.unreadMessageCount,
    );
  }

  void _setMode(AuthMode mode) {
    setState(() {
      _mode = mode;
      _code.clear();
      if (mode == AuthMode.register) {
        if (!Validator.isPhone(_account.text.trim())) {
          _account.text = '18800001111';
        }
        if (!Validator.isEmail(_email.text.trim())) {
          _email.text = 'user@example.com';
        }
      }
      if (mode == AuthMode.reset && !Validator.isEmail(_email.text.trim())) {
        _email.text = 'user@example.com';
      }
    });
  }
}

class _BrandHeader extends StatelessWidget {
  const _BrandHeader();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 68,
          height: 68,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: AppColors.brand,
            borderRadius: BorderRadius.circular(18),
          ),
          child: const Text(
            '爱',
            style: TextStyle(
              color: Colors.white,
              fontSize: 28,
              fontWeight: FontWeight.w800,
            ),
          ),
        ),
        const SizedBox(height: 9),
        Text('爱团', style: Theme.of(context).textTheme.headlineMedium),
      ],
    );
  }
}

class _Notice extends StatelessWidget {
  const _Notice();

  @override
  Widget build(BuildContext context) {
    return const AppCard(
      backgroundColor: AppColors.brandSoft,
      borderColor: AppColors.brandLine,
      child: Text(
        '登录后可以继续下单、收藏和查看订单。',
        style: TextStyle(color: AppColors.brandDeep),
      ),
    );
  }
}
