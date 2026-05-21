import 'dart:convert';
import 'dart:io';

class AppApiException implements Exception {
  const AppApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

typedef TokenProvider = String? Function();

class AppApiClient {
  AppApiClient({String? baseUrl, this.tokenProvider, HttpClient? client})
    : baseUrl = _normalizeBaseUrl(baseUrl ?? _defaultBaseUrl()),
      _client = client ?? HttpClient();

  final String baseUrl;
  final TokenProvider? tokenProvider;
  final HttpClient _client;

  Future<Map<String, dynamic>> get(String path) => _send('GET', path);

  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> body) =>
      _send('POST', path, body: body);

  Future<Map<String, dynamic>> put(String path, Map<String, dynamic> body) =>
      _send('PUT', path, body: body);

  Future<Map<String, dynamic>> patch(String path, Map<String, dynamic> body) =>
      _send('PATCH', path, body: body);

  Future<Map<String, dynamic>> delete(String path) => _send('DELETE', path);

  Future<Map<String, dynamic>> _send(
    String method,
    String path, {
    Map<String, dynamic>? body,
  }) async {
    final uri = Uri.parse('$baseUrl$path');
    final request = await _client.openUrl(method, uri);
    request.headers.set(HttpHeaders.acceptHeader, ContentType.json.mimeType);
    final token = tokenProvider?.call();
    if (token != null && token.isNotEmpty) {
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
    }
    if (body != null) {
      request.headers.contentType = ContentType.json;
      request.write(jsonEncode(body));
    }

    final response = await request.close();
    final text = await response.transform(utf8.decoder).join();
    final decoded = text.isEmpty
        ? <String, dynamic>{}
        : (jsonDecode(text) as Map<String, dynamic>);
    if (response.statusCode >= 400) {
      throw AppApiException(
        _message(decoded, text),
        statusCode: response.statusCode,
      );
    }
    final code = decoded['code'];
    if (code is num && code.toInt() != 0) {
      throw AppApiException(
        _message(decoded, text),
        statusCode: response.statusCode,
      );
    }
    return decoded;
  }

  String _message(Map<String, dynamic> json, String fallback) {
    final message = json['message']?.toString().trim();
    return (message == null || message.isEmpty) ? fallback : message;
  }

  static String _normalizeBaseUrl(String value) =>
      value.endsWith('/') ? value.substring(0, value.length - 1) : value;

  static String _defaultBaseUrl() {
    const configured = String.fromEnvironment('API_BASE_URL');
    if (configured.isNotEmpty) return configured;
    return Platform.isAndroid
        ? 'http://10.0.2.2:8080'
        : 'http://localhost:8080';
  }
}
