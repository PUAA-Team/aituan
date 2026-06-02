import 'package:flutter/material.dart';

import '../app/app.dart';
import 'unsupported_web_app.dart';

const _desktopBreakpoint = 840.0;

class AituanWebBootstrap extends StatelessWidget {
  const AituanWebBootstrap({super.key, required this.openUrl});

  final WebUrlOpener openUrl;

  @override
  Widget build(BuildContext context) => MediaQuery.fromView(
    view: View.of(context),
    child: _AituanWebBootstrapBody(openUrl: openUrl),
  );
}

class _AituanWebBootstrapBody extends StatelessWidget {
  const _AituanWebBootstrapBody({required this.openUrl});

  final WebUrlOpener openUrl;

  @override
  Widget build(BuildContext context) {
    final isDesktop = MediaQuery.of(context).size.width >= _desktopBreakpoint;
    if (isDesktop) {
      return AituanUnsupportedWebApp(openUrl: openUrl);
    }
    return const AituanApp();
  }
}
