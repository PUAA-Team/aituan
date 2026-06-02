import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/storage/auth_storage.dart';
import '../../home/data/backend_app_repository.dart';

class SplashPage extends StatefulWidget {
  const SplashPage({super.key});

  @override
  State<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends State<SplashPage> {
  bool _checking = true;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _restoreLogin());
  }

  Future<void> _restoreLogin() async {
    final token = await AuthStorage.readToken();
    if (token == null || token.isEmpty) {
      if (mounted) setState(() => _checking = false);
      return;
    }

    final session = await backendRepository.checkToken(token);
    if (!mounted) return;
    if (session == null) {
      await AuthStorage.clearToken();
      appState.logout();
      if (mounted) setState(() => _checking = false);
      return;
    }

    AppScope.of(context).login(
      token: session.token,
      displayName: session.nickname,
      avatarUrl: session.avatarUrl,
      phone: session.phone,
      email: session.email,
      memberLevelName: session.memberLevelName,
      unreadMessageCount: session.unreadMessageCount,
    );
    Navigator.pushReplacementNamed(context, Routes.main);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.brand,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              Align(
                child: Container(
                  width: 86,
                  height: 86,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x1F000000),
                        blurRadius: 18,
                        offset: Offset(0, 8),
                      ),
                    ],
                  ),
                  child: const Text(
                    '爱',
                    style: TextStyle(
                      fontSize: 40,
                      fontWeight: FontWeight.w800,
                      color: AppColors.brand,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 18),
              const Text(
                '爱团',
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 34,
                  fontWeight: FontWeight.w800,
                  color: Colors.white,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                '吃喝玩乐，一键触达附近生活',
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Colors.white70,
                  fontSize: 14,
                  height: 1.4,
                ),
              ),
              const Spacer(),
              if (_checking)
                const Center(
                  child: CircularProgressIndicator(color: Colors.white),
                )
              else ...[
                FilledButton(
                  style: FilledButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: AppColors.brand,
                  ),
                  onPressed: () => Navigator.pushNamed(context, Routes.login),
                  child: const Text('登录 / 注册'),
                ),
                const SizedBox(height: 10),
                OutlinedButton(
                  style: OutlinedButton.styleFrom(
                    foregroundColor: Colors.white,
                    side: const BorderSide(color: Colors.white70),
                  ),
                  onPressed: () =>
                      Navigator.pushReplacementNamed(context, Routes.main),
                  child: const Text('游客进入'),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
