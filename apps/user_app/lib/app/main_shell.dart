import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../core/constants/app_colors.dart';
import '../features/home/presentation/home_page.dart';
import '../features/message/presentation/message_page.dart';
import '../features/order/presentation/orders_page.dart';
import '../features/profile/presentation/profile_page.dart';
import 'app_state.dart';

class MainShell extends StatefulWidget {
  const MainShell({super.key, this.initialIndex = 0});

  final int initialIndex;

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  static const _exitInterval = Duration(seconds: 2);

  late int _index = widget.initialIndex;
  DateTime? _lastBackPressedAt;

  final _pages = const [HomePage(), MessagePage(), OrdersPage(), ProfilePage()];

  @override
  Widget build(BuildContext context) {
    final unreadMessageCount = AppScope.of(context).unreadMessageCount;
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (didPop) return;
        _handleBackPressed();
      },
      child: Scaffold(
        body: IndexedStack(index: _index, children: _pages),
        bottomNavigationBar: NavigationBar(
          selectedIndex: _index,
          indicatorColor: AppColors.brandSoft,
          onDestinationSelected: (value) {
            if (value > 0 && !AppScope.of(context).requireLogin(context)) {
              return;
            }
            setState(() => _index = value);
          },
          destinations: [
            const NavigationDestination(
              icon: Icon(Icons.home_outlined),
              selectedIcon: Icon(Icons.home),
              label: '首页',
            ),
            NavigationDestination(
              icon: _NavIconWithBadge(
                icon: Icons.chat_bubble_outline,
                count: unreadMessageCount,
              ),
              selectedIcon: _NavIconWithBadge(
                icon: Icons.chat_bubble,
                count: unreadMessageCount,
              ),
              label: '消息',
            ),
            const NavigationDestination(
              icon: Icon(Icons.receipt_long_outlined),
              selectedIcon: Icon(Icons.receipt_long),
              label: '订单',
            ),
            const NavigationDestination(
              icon: Icon(Icons.person_outline),
              selectedIcon: Icon(Icons.person),
              label: '我的',
            ),
          ],
        ),
      ),
    );
  }

  void _handleBackPressed() {
    if (_index != 0) {
      setState(() => _index = 0);
      _lastBackPressedAt = null;
      return;
    }

    final now = DateTime.now();
    final shouldExit =
        _lastBackPressedAt != null &&
        now.difference(_lastBackPressedAt!) < _exitInterval;
    if (shouldExit) {
      SystemNavigator.pop();
      return;
    }

    _lastBackPressedAt = now;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('再按一次退出爱团')));
  }
}

class _NavIconWithBadge extends StatelessWidget {
  const _NavIconWithBadge({required this.icon, required this.count});

  final IconData icon;
  final int count;

  @override
  Widget build(BuildContext context) => SizedBox(
    width: 28,
    height: 28,
    child: Stack(
      clipBehavior: Clip.none,
      children: [
        Center(child: Icon(icon)),
        if (count > 0)
          Positioned(
            right: -4,
            top: 0,
            child: Container(
              constraints: const BoxConstraints(minWidth: 16, minHeight: 16),
              padding: const EdgeInsets.symmetric(horizontal: 4),
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: Colors.red,
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                count > 99 ? '99+' : '$count',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 10,
                  height: 1,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
      ],
    ),
  );
}
