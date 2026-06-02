import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthStorage {
  const AuthStorage._();

  static const _storage = FlutterSecureStorage();
  static const _tokenKey = 'aituan_user_token';

  static Future<String?> readToken() async {
    try {
      return await _storage.read(key: _tokenKey);
    } catch (_) {
      return null;
    }
  }

  static Future<void> saveToken(String token) async {
    try {
      await _storage.write(key: _tokenKey, value: token);
    } catch (_) {}
  }

  static Future<void> clearToken() async {
    try {
      await _storage.delete(key: _tokenKey);
    } catch (_) {}
  }
}
