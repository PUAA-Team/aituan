import 'package:flutter/material.dart';

import '../core/constants/route_constants.dart';
import '../features/location/application/location_scope.dart';
import 'app_state.dart';
import 'router.dart';
import 'theme.dart';

class AituanApp extends StatelessWidget {
  const AituanApp({super.key});

  @override
  Widget build(BuildContext context) {
    return AppScope(
      state: appState,
      child: LocationScope(
        child: MaterialApp(
          title: '爱团',
          debugShowCheckedModeBanner: false,
          theme: buildAituanTheme(),
          initialRoute: Routes.splash,
          onGenerateRoute: AppRouter.onGenerateRoute,
        ),
      ),
    );
  }
}
