import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/utils/validator.dart';
import '../../../core/widgets/app_card.dart';

enum AuthMode { login, register, reset }

class LoginPage extends StatefulWidget {
  const LoginPage({super.key, this.showNotice = false});

  final bool showNotice;

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  AuthMode _mode = AuthMode.login;
  final _account = TextEditingController(text: '18800001111');
  final _password = TextEditingController(text: 'aituan123');
  final _email = TextEditingController(text: 'user@example.com');

  @override
  void dispose() {
    _account.dispose();
    _password.dispose();
    _email.dispose();
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
                  '手机号可直接登录，邮箱验证码后续接入',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: 16),
                TextField(
                  controller: _account,
                  decoration: const InputDecoration(labelText: '手机号或邮箱'),
                ),
                const SizedBox(height: 10),
                if (_mode != AuthMode.login)
                  TextField(
                    controller: _email,
                    decoration: const InputDecoration(labelText: '邮箱验证码'),
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
                  onPressed: _submit,
                  child: Text(_mode == AuthMode.login ? '登录' : '确认'),
                ),
                const SizedBox(height: 10),
                if (_mode == AuthMode.reset)
                  Align(
                    alignment: Alignment.centerLeft,
                    child: TextButton(
                      onPressed: () => setState(() => _mode = AuthMode.login),
                      child: const Text('返回登录'),
                    ),
                  )
                else
                  Row(
                    children: [
                      TextButton(
                        onPressed: () => setState(
                          () => _mode = _mode == AuthMode.register
                              ? AuthMode.login
                              : AuthMode.register,
                        ),
                        child: Text(
                          _mode == AuthMode.register ? '返回登录' : '前往注册',
                        ),
                      ),
                      const Spacer(),
                      TextButton(
                        onPressed: () => setState(() => _mode = AuthMode.reset),
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

  void _submit() {
    if (!Validator.isAccount(_account.text.trim())) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请输入 11 位手机号或正确邮箱')));
      return;
    }
    AppScope.of(context).login();
    Navigator.pushNamedAndRemoveUntil(context, Routes.main, (_) => false);
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
        '继续下单、收藏、查看订单前需要登录。',
        style: TextStyle(color: AppColors.brandDeep),
      ),
    );
  }
}
