import 'package:flutter/material.dart';

import '../app/app.dart';

Future<void> bootstrap() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const AituanApp());
}
