import 'package:flutter/material.dart';

import '../../../../app/app_state.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/constants/app_tokens.dart';
import '../../../../core/constants/route_constants.dart';
import '../../../../core/widgets/app_search_box.dart';
import '../../../../core/widgets/app_toast.dart';
import '../../../location/application/location_scope.dart';

class HomeHeroHeader extends StatelessWidget {
  const HomeHeroHeader({super.key, this.onLocationChanged});

  final Future<void> Function()? onLocationChanged;

  @override
  Widget build(BuildContext context) => Container(
    margin: const EdgeInsets.only(bottom: AppTokens.cardGap),
    padding: const EdgeInsets.all(15),
    decoration: BoxDecoration(
      color: AppColors.brand,
      borderRadius: BorderRadius.circular(12),
    ),
    child: Stack(
      children: [
        const Positioned(right: -18, top: -20, child: _HeroDot(size: 72)),
        const Positioned(left: 170, bottom: -34, child: _HeroDot(size: 66)),
        Builder(
          builder: (context) {
            final unread = AppScope.of(context).unreadMessageCount;
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    _LocationButton(
                      label: LocationScope.of(context).label,
                      loading: LocationScope.of(context).loading,
                      onLocationChanged: onLocationChanged,
                    ),
                    const Spacer(),
                    _MessageButton(unread: unread),
                  ],
                ),
                const SizedBox(height: 12),
                AppSearchBox(
                  hint: '搜索外卖、团购、景点、洗脚',
                  fillColor: Colors.white,
                  borderColor: Colors.white,
                  onTap: () => Navigator.pushNamed(context, Routes.search),
                ),
              ],
            );
          },
        ),
      ],
    ),
  );
}

class _LocationButton extends StatelessWidget {
  const _LocationButton({
    required this.label,
    required this.loading,
    required this.onLocationChanged,
  });

  final String label;
  final bool loading;
  final Future<void> Function()? onLocationChanged;

  @override
  Widget build(BuildContext context) => InkWell(
    onTap: loading
        ? null
        : () async {
            final location = LocationScope.of(context);
            await location.refresh();
            if (!context.mounted) return;
            showAppSnackBar(context, '已更新位置');
            final callback = onLocationChanged;
            if (callback != null) {
              await callback();
            }
          },
    borderRadius: BorderRadius.circular(8),
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 3, horizontal: 2),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            loading ? Icons.sync : Icons.location_on,
            color: Colors.white,
            size: 18,
          ),
          const SizedBox(width: 3),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 15,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    ),
  );
}

class _MessageButton extends StatelessWidget {
  const _MessageButton({required this.unread});

  final int unread;

  @override
  Widget build(BuildContext context) => InkWell(
    onTap: () => Navigator.pushNamed(context, Routes.message),
    borderRadius: BorderRadius.circular(18),
    child: Padding(
      padding: const EdgeInsets.all(3),
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          const Icon(
            Icons.notifications_none,
            color: Colors.white,
            size: 22,
          ),
          if (unread > 0)
            Positioned(
              right: -5,
              top: -5,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  unread > 99 ? '99+' : '$unread',
                  style: const TextStyle(
                    color: AppColors.brand,
                    fontSize: 10,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ),
        ],
      ),
    ),
  );
}

class _HeroDot extends StatelessWidget {
  const _HeroDot({required this.size});

  final double size;

  @override
  Widget build(BuildContext context) => Container(
    width: size,
    height: size,
    decoration: BoxDecoration(
      shape: BoxShape.circle,
      color: Colors.white.withValues(alpha: .08),
    ),
  );
}
