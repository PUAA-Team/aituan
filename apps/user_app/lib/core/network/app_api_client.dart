import 'dart:async';

class AppApiClient {
  const AppApiClient({this.baseUrl = ''});

  final String baseUrl;

  Future<Map<String, dynamic>> get(String path) async {
    return Future.error(UnimplementedError('待后端接口接入：GET $path'));
  }

  Future<Map<String, dynamic>> post(
    String path,
    Map<String, dynamic> body,
  ) async {
    return Future.error(UnimplementedError('待后端接口接入：POST $path'));
  }
}
