import 'package:flutter/material.dart';
import 'package:web/web.dart' as web;

import '../web/unsupported_web_app.dart';

Future<void> bootstrap() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(AituanUnsupportedWebApp(openUrl: _openUrl));
}

void _openUrl(String url) {
  web.window.location.href = url;
}
