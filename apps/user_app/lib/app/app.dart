import 'package:flutter/material.dart';

import '../core/constants/route_constants.dart';
import '../features/location/application/location_scope.dart';
import '../features/assistant_pet/assistant_pet_overlay.dart';
import 'app_navigator.dart';
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
          navigatorKey: appNavigatorKey,
          title: '爱团',
          debugShowCheckedModeBanner: false,
          theme: buildAituanTheme(),
          initialRoute: Routes.splash,
          onGenerateRoute: AppRouter.onGenerateRoute,
          builder: (context, child) => AssistantPetOverlay(
            child: child ?? const SizedBox.shrink(),
          ),
        ),
      ),
    );
  }
}
