import 'package:flutter/material.dart';

import '../constants/app_colors.dart';
import '../constants/app_tokens.dart';

class AppSearchBox extends StatelessWidget {
  const AppSearchBox({
    super.key,
    required this.hint,
    this.controller,
    this.onTap,
    this.onSubmitted,
    this.autofocus = false,
    this.fillColor = AppColors.card,
    this.borderColor = AppColors.line,
    this.enabled = true,
  });

  final String hint;
  final TextEditingController? controller;
  final VoidCallback? onTap;
  final ValueChanged<String>? onSubmitted;
  final bool autofocus;
  final Color fillColor;
  final Color borderColor;
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    final editable = controller != null || onSubmitted != null || autofocus;
    final box = Container(
      height: AppTokens.searchHeight,
      padding: editable
          ? EdgeInsets.zero
          : const EdgeInsets.symmetric(horizontal: 14),
      decoration: BoxDecoration(
        color: fillColor,
        borderRadius: BorderRadius.circular(AppTokens.searchRadius),
        border: Border.all(color: borderColor),
      ),
      child: editable ? _TextInput(this) : _SearchHint(hint: hint),
    );
    if (onTap == null) return box;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        borderRadius: BorderRadius.circular(AppTokens.searchRadius),
        onTap: onTap,
        child: box,
      ),
    );
  }
}

class _SearchHint extends StatelessWidget {
  const _SearchHint({required this.hint});

  final String hint;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      const Icon(Icons.search, color: AppColors.textSub, size: 20),
      const SizedBox(width: 8),
      Expanded(
        child: Text(
          hint,
          style: const TextStyle(fontSize: 14, color: AppColors.textSub),
          overflow: TextOverflow.ellipsis,
        ),
      ),
    ],
  );
}

class _TextInput extends StatelessWidget {
  const _TextInput(this.box);

  final AppSearchBox box;

  @override
  Widget build(BuildContext context) => TextField(
    controller: box.controller,
    autofocus: box.autofocus,
    enabled: box.enabled,
    textInputAction: TextInputAction.search,
    onSubmitted: box.onSubmitted,
    decoration: InputDecoration(
      hintText: box.hint,
      prefixIcon: const Icon(Icons.search, size: 20),
      prefixIconConstraints: const BoxConstraints(minWidth: 40),
      filled: false,
      border: InputBorder.none,
      enabledBorder: InputBorder.none,
      focusedBorder: InputBorder.none,
      contentPadding: const EdgeInsets.fromLTRB(0, 10, 12, 10),
    ),
  );
}
