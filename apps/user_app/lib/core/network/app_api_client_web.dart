import 'dart:convert';

import 'package:http/http.dart' as http;

class AppApiException implements Exception {
  const AppApiException(this.message, {this.statusCode});

  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}

typedef TokenProvider = String? Function();

class AppApiClient {
  AppApiClient({String? baseUrl, this.tokenProvider, http.Client? client})
    : baseUrl = _normalizeBaseUrl(baseUrl ?? _defaultBaseUrl()),
      _client = client ?? http.Client();

  final String baseUrl;
  final TokenProvider? tokenProvider;
  final http.Client _client;

  Future<Map<String, dynamic>> get(String path) => _send('GET', path);

  Future<Map<String, dynamic>> post(String path, Map<String, dynamic> body) =>
      _send('POST', path, body: body);

  Future<Map<String, dynamic>> put(String path, Map<String, dynamic> body) =>
      _send('PUT', path, body: body);

  Future<Map<String, dynamic>> patch(String path, Map<String, dynamic> body) =>
      _send('PATCH', path, body: body);

  Future<Map<String, dynamic>> delete(String path) => _send('DELETE', path);

  String resolveUrl(String path) => resolvePublicUrl(path, baseUrl: baseUrl);

  static String resolvePublicUrl(String path, {String? baseUrl}) {
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    final root = _normalizeBaseUrl(baseUrl ?? _defaultBaseUrl());
    return '$root${path.startsWith('/') ? path : '/$path'}';
  }

  Future<Map<String, dynamic>> postMultipart(
    String path, {
    required String fileField,
    required List<int> fileBytes,
    required String filename,
    required String contentType,
    Map<String, String> fields = const {},
  }) async {
    final boundary = 'aituan-${DateTime.now().microsecondsSinceEpoch}';
    final body = <int>[];
    for (final entry in fields.entries) {
      body.addAll(utf8.encode('--$boundary\r\n'));
      body.addAll(
        utf8.encode(
          'Content-Disposition: form-data; name="${entry.key}"\r\n\r\n${entry.value}\r\n',
        ),
      );
    }
    body.addAll(utf8.encode('--$boundary\r\n'));
    body.addAll(
      utf8.encode(
        'Content-Disposition: form-data; name="$fileField"; filename="$filename"\r\n',
      ),
    );
    body.addAll(utf8.encode('Content-Type: $contentType\r\n\r\n'));
    body.addAll(fileBytes);
    body.addAll(utf8.encode('\r\n--$boundary--\r\n'));

    final headers = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'multipart/form-data; boundary=$boundary',
    };
    final token = tokenProvider?.call();
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }
    final response = await _client.send(
      http.Request('POST', Uri.parse('$baseUrl$path'))
        ..headers.addAll(headers)
        ..bodyBytes = body,
    );
    return _handleResponse(await http.Response.fromStream(response));
  }

  Future<Map<String, dynamic>> _send(
    String method,
    String path, {
    Map<String, dynamic>? body,
  }) async {
    final headers = <String, String>{'Accept': 'application/json'};
    final token = tokenProvider?.call();
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }
    if (body != null) {
      headers['Content-Type'] = 'application/json';
    }
    final response = await _client.send(
      http.Request(method, Uri.parse('$baseUrl$path'))
        ..headers.addAll(headers)
        ..body = body == null ? '' : jsonEncode(body),
    );
    return _handleResponse(await http.Response.fromStream(response));
  }

  Future<Map<String, dynamic>> _handleResponse(http.Response response) async {
    final text = response.body;
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
    return configured.isNotEmpty ? configured : 'http://localhost:8080';
  }
}
