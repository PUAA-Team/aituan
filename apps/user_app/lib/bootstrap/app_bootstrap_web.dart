import 'package:flutter/material.dart';
import 'package:web/web.dart' as web;

import '../web/web_bootstrap_gate.dart';

Future<void> bootstrap() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(AituanWebBootstrap(openUrl: _openUrl));
}

void _openUrl(String url) {
  web.window.location.href = url;
}
