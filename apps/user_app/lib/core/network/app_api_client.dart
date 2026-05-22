import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

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

  String resolveUrl(String path) {
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return '$baseUrl${path.startsWith('/') ? path : '/$path'}';
  }

  Future<Map<String, dynamic>> postMultipart(
    String path, {
    required String fileField,
    required File file,
    Map<String, String> fields = const {},
  }) async {
    final uri = Uri.parse('$baseUrl$path');
    final boundary = 'aituan-${DateTime.now().microsecondsSinceEpoch}';
    final request = await _client.postUrl(uri);
    request.headers.set(HttpHeaders.acceptHeader, ContentType.json.mimeType);
    request.headers.contentType = ContentType(
      'multipart',
      'form-data',
      parameters: {'boundary': boundary},
    );
    final token = tokenProvider?.call();
    if (token != null && token.isNotEmpty) {
      request.headers.set(HttpHeaders.authorizationHeader, 'Bearer $token');
    }

    final body = BytesBuilder();
    for (final entry in fields.entries) {
      body.add(utf8.encode('--$boundary\r\n'));
      body.add(
        utf8.encode(
          'Content-Disposition: form-data; name="${entry.key}"\r\n\r\n${entry.value}\r\n',
        ),
      );
    }
    final filename = file.uri.pathSegments.isEmpty
        ? 'upload.jpg'
        : file.uri.pathSegments.last;
    body.add(utf8.encode('--$boundary\r\n'));
    body.add(
      utf8.encode(
        'Content-Disposition: form-data; name="$fileField"; filename="$filename"\r\n',
      ),
    );
    body.add(utf8.encode('Content-Type: ${_mimeType(filename)}\r\n\r\n'));
    body.add(await file.readAsBytes());
    body.add(utf8.encode('\r\n--$boundary--\r\n'));
    request.add(body.takeBytes());
    return _handleResponse(await request.close());
  }

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

    return _handleResponse(await request.close());
  }

  Future<Map<String, dynamic>> _handleResponse(
    HttpClientResponse response,
  ) async {
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

  String _mimeType(String filename) {
    final lower = filename.toLowerCase();
    if (lower.endsWith('.png')) return 'image/png';
    if (lower.endsWith('.webp')) return 'image/webp';
    return 'image/jpeg';
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
