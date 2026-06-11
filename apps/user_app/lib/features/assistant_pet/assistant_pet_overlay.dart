import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../app/app_navigator.dart';
import '../../app/app_state.dart';
import '../../core/constants/route_constants.dart';

class AssistantPetOverlay extends StatefulWidget {
  const AssistantPetOverlay({super.key, required this.child});

  final Widget child;

  @override
  State<AssistantPetOverlay> createState() => _AssistantPetOverlayState();
}

class _AssistantPetOverlayState extends State<AssistantPetOverlay>
    with SingleTickerProviderStateMixin {
  static const _size = 70.0;
  static const _peek = 28.0;
  static const _margin = 10.0;

  late final AnimationController _breathController;
  Offset? _position;
  bool _leftEdge = false;
  bool _expanded = false;
  bool _dragging = false;
  bool _hasValidPosition = false;
  bool _wasDragged = false;

  @override
  void initState() {
    super.initState();
    _breathController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1450),
      lowerBound: 0,
      upperBound: 1,
    )..repeat(reverse: true);
  }

  @override
  void dispose() {
    _breathController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: appState,
      builder: (context, _) {
        return Stack(
          clipBehavior: Clip.hardEdge,
          children: [
            widget.child,
            if (appState.isLoggedIn)
              Positioned.fill(
                child: LayoutBuilder(
                  builder: (context, constraints) {
                    final bounds = Size(
                      constraints.maxWidth,
                      constraints.maxHeight,
                    );
                    if (bounds.width < _size + _peek ||
                        bounds.height < _size * 2) {
                      return const SizedBox.shrink();
                    }
                    final position = _resolvedPosition(bounds);
                    return Stack(
                      clipBehavior: Clip.hardEdge,
                      children: [
                        AnimatedPositioned(
                          duration: _dragging
                              ? Duration.zero
                              : const Duration(milliseconds: 320),
                          curve: Curves.easeOutCubic,
                          left: position.dx,
                          top: position.dy,
                          width: _size,
                          height: _size,
                          child: SizedBox.square(
                            dimension: _size,
                            child: _AssistantPetButton(
                              expanded: _expanded || _dragging,
                              leftEdge: _leftEdge,
                              breathController: _breathController,
                              onTap: _openAssistant,
                              onHoverChanged: (value) {
                                if (_dragging || !mounted) return;
                                setState(() => _expanded = value);
                              },
                              onPanStart: () {
                                setState(() {
                                  _dragging = true;
                                  _wasDragged = true;
                                  _expanded = true;
                                  _position = _fullyVisible(position, bounds);
                                });
                              },
                              onPanUpdate: (details) {
                                setState(() {
                                  final current =
                                      _position ??
                                      _fullyVisible(position, bounds);
                                  _position = _clamp(
                                    current + details.delta,
                                    bounds,
                                    expanded: true,
                                  );
                                });
                              },
                              onPanEnd: () => _dockToNearestEdge(bounds),
                            ),
                          ),
                        ),
                      ],
                    );
                  },
                ),
              ),
          ],
        );
      },
    );
  }

  Offset _resolvedPosition(Size bounds) {
    if (!_wasDragged && !_dragging) {
      _position = Offset(bounds.width - _peek, bounds.height * 0.62);
      _leftEdge = false;
      _hasValidPosition = true;
    }
    if (!_hasValidPosition || _position == null) {
      _position = Offset(bounds.width - _peek, bounds.height * 0.62);
      _leftEdge = false;
      _hasValidPosition = true;
    }
    return _clamp(_position!, bounds, expanded: _expanded || _dragging);
  }

  Offset _fullyVisible(Offset position, Size bounds) {
    final x = position.dx < 0
        ? _margin
        : math.min(position.dx, bounds.width - _size - _margin);
    return _clamp(Offset(x, position.dy), bounds, expanded: true);
  }

  Offset _clamp(Offset position, Size bounds, {required bool expanded}) {
    final minX = expanded ? _margin : -(_size - _peek);
    final maxX = expanded
        ? bounds.width - _size - _margin
        : bounds.width - _peek;
    final minY = MediaQuery.paddingOf(context).top + _margin;
    final maxY =
        bounds.height - _size - MediaQuery.paddingOf(context).bottom - _margin;
    return Offset(
      position.dx.clamp(minX, math.max(minX, maxX)),
      position.dy.clamp(minY, math.max(minY, maxY)),
    );
  }

  void _dockToNearestEdge(Size bounds) {
    final current =
        _position ??
        Offset(bounds.width - _size - _margin, bounds.height * 0.58);
    final centerX = current.dx + _size / 2;
    setState(() {
      _dragging = false;
      _expanded = false;
      _leftEdge = centerX < bounds.width / 2;
      _position = Offset(
        _leftEdge ? -(_size - _peek) : bounds.width - _peek,
        current.dy,
      );
    });
  }

  void _openAssistant() {
    if (_dragging) return;
    setState(() => _expanded = false);
    final navigator = appNavigatorKey.currentState;
    if (navigator == null) return;
    navigator.pushNamed(Routes.aiAssistant);
  }
}

class _AssistantPetButton extends StatelessWidget {
  const _AssistantPetButton({
    required this.expanded,
    required this.leftEdge,
    required this.breathController,
    required this.onTap,
    required this.onHoverChanged,
    required this.onPanStart,
    required this.onPanUpdate,
    required this.onPanEnd,
  });

  final bool expanded;
  final bool leftEdge;
  final AnimationController breathController;
  final VoidCallback onTap;
  final ValueChanged<bool> onHoverChanged;
  final VoidCallback onPanStart;
  final GestureDragUpdateCallback onPanUpdate;
  final VoidCallback onPanEnd;

  @override
  Widget build(BuildContext context) {
    return SizedBox.expand(
      child: MouseRegion(
        onEnter: (_) => onHoverChanged(true),
        onExit: (_) => onHoverChanged(false),
        child: GestureDetector(
          behavior: HitTestBehavior.opaque,
          onTap: onTap,
          onPanStart: (_) => onPanStart(),
          onPanUpdate: onPanUpdate,
          onPanEnd: (_) => onPanEnd(),
          child: AnimatedBuilder(
            animation: breathController,
            builder: (context, child) {
              final scale = expanded
                  ? 1.06
                  : 0.96 + breathController.value * 0.045;
              final tilt = leftEdge ? -0.035 : 0.035;
              return Transform.rotate(
                angle: expanded ? 0 : tilt,
                child: Transform.scale(scale: scale, child: child),
              );
            },
            child: Semantics(
              button: true,
              label: '小爱同学 AI 助手',
              child: SizedBox.expand(
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: Colors.white,
                    border: Border.all(
                      color: const Color(0xFFFFB7C7),
                      width: 2,
                    ),
                    boxShadow: const [
                      BoxShadow(
                        color: Color(0x33000000),
                        blurRadius: 14,
                        offset: Offset(0, 6),
                      ),
                    ],
                  ),
                  child: ClipOval(
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        Transform.scale(
                          scale: 2.45,
                          alignment: const Alignment(0, -0.72),
                          child: Image.asset(
                            'assets/assistant_pet/xiaoai_pet.png',
                            fit: BoxFit.cover,
                            alignment: Alignment.topCenter,
                          ),
                        ),
                        Positioned(
                          right: 9,
                          bottom: 8,
                          child: Container(
                            width: 14,
                            height: 14,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: const Color(0xFFE53958),
                              border: Border.all(color: Colors.white, width: 2),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
