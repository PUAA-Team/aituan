import 'package:flutter/material.dart';

import '../core/constants/route_constants.dart';

class AppState extends ChangeNotifier {
  bool _loggedIn = false;
  String? _token;
  String _displayName = '游客';
  String? _phone;
  String? _email;
  String _memberLevelName = '普通会员';
  int _unreadMessageCount = 0;

  bool get isLoggedIn => _loggedIn;
  String? get token => _token;
  String get displayName => _displayName;
  String? get phone => _phone;
  String? get email => _email;
  String get memberLevelName => _memberLevelName;
  int get unreadMessageCount => _unreadMessageCount;

  void login({
    required String token,
    required String displayName,
    String? phone,
    String? email,
    String? memberLevelName,
    int unreadMessageCount = 0,
  }) {
    _loggedIn = true;
    _token = token;
    _displayName = displayName;
    _phone = phone;
    _email = email;
    _memberLevelName = memberLevelName ?? '普通会员';
    _unreadMessageCount = unreadMessageCount;
    notifyListeners();
  }

  void logout() {
    _loggedIn = false;
    _token = null;
    _displayName = '游客';
    _phone = null;
    _email = null;
    _memberLevelName = '普通会员';
    _unreadMessageCount = 0;
    notifyListeners();
  }

  void updateProfile({
    String? displayName,
    String? phone,
    String? email,
    String? memberLevelName,
    int? unreadMessageCount,
  }) {
    if (!_loggedIn) return;
    if (displayName != null) _displayName = displayName;
    if (phone != null) _phone = phone;
    if (email != null) _email = email;
    if (memberLevelName != null) _memberLevelName = memberLevelName;
    if (unreadMessageCount != null) _unreadMessageCount = unreadMessageCount;
    notifyListeners();
  }

  bool requireLogin(BuildContext context) {
    if (_loggedIn) return true;
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('请先登录后继续操作')));
    Navigator.of(context).pushNamed(Routes.login);
    return false;
  }
}

final appState = AppState();

class AppScope extends InheritedNotifier<AppState> {
  const AppScope({super.key, required AppState state, required super.child})
    : super(notifier: state);

  static AppState of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AppScope>();
    assert(scope != null, 'AppScope not found');
    return scope!.notifier!;
  }
}
