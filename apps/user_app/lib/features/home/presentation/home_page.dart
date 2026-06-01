import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../shared/models/item_model.dart';
import '../../home/data/backend_app_repository.dart';
import '../../location/application/location_scope.dart';
import '../../location/application/location_state.dart';
import 'home_widgets.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  final _controller = ScrollController();
  LocationState? _location;
  double? _lastLatitude;
  double? _lastLongitude;
  HomeData? _data;
  Object? _error;
  bool _loading = true;
  int _visible = 6;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_loadMoreIfNeeded);
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadWithLocation());
  }

  @override
  void dispose() {
    _location?.removeListener(_reloadIfLocationChanged);
    _controller
      ..removeListener(_loadMoreIfNeeded)
      ..dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const SafeArea(child: Center(child: CircularProgressIndicator()));
    }
    if (_error != null) {
      return SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: AppCard(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('首页数据加载失败'),
                  const SizedBox(height: 8),
                  Text(
                    _error.toString(),
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 12),
                  FilledButton(onPressed: _loadHome, child: const Text('重试')),
                ],
              ),
            ),
          ),
        ),
      );
    }
    final data = _data!;
    final items = data.recommendations.take(_visible).toList();
    return SafeArea(
      child: RefreshIndicator(
        onRefresh: _loadHome,
        child: ListView(
          controller: _controller,
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
          children: [
            HomeHeroHeader(onLocationChanged: _loadHome),
            HomeModuleGrid(modules: data.modules),
            HomeRecommendSection(
              items: items,
              onTap: (item) => _openItem(context, item),
            ),
            if (_visible >= data.recommendations.length &&
                data.recommendations.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 6, bottom: 10),
                child: Text(
                  '已展示更多附近好店',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            if (data.recommendations.isEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text(
                  '暂无推荐内容',
                  textAlign: TextAlign.center,
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
          ],
        ),
      ),
    );
  }

  Future<void> _loadWithLocation() async {
    final location = LocationScope.of(context);
    _location = location;
    location.addListener(_reloadIfLocationChanged);
    if (!location.hasLocation && !location.loading) {
      await location.refresh();
    }
    _rememberLocation(location);
    if (!mounted) return;
    await _loadHome();
    if (!mounted) return;
    final locationError = location.error;
    if (locationError != null) {
      showAppSnackBar(context, '开发提示：定位失败，当前使用默认位置：$locationError');
    }
  }

  void _reloadIfLocationChanged() {
    final location = _location;
    if (!mounted || location == null || location.loading) return;
    final latitude = location.latitude;
    final longitude = location.longitude;
    if (latitude == null || longitude == null) return;
    if (_sameLocation(_lastLatitude, latitude) &&
        _sameLocation(_lastLongitude, longitude)) {
      return;
    }
    _rememberLocation(location);
    _loadHome();
  }

  void _rememberLocation(LocationState location) {
    _lastLatitude = location.latitude;
    _lastLongitude = location.longitude;
  }

  bool _sameLocation(double? oldValue, double newValue) =>
      oldValue != null && (oldValue - newValue).abs() < 0.00001;

  Future<void> _loadHome() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final data = await backendRepository.fetchHome();
      if (!mounted) return;
      setState(() {
        _data = data;
        _visible = math.min(6, data.recommendations.length);
        _loading = false;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        _error = error;
        _loading = false;
      });
    }
  }

  void _loadMoreIfNeeded() {
    final data = _data;
    if (!_controller.hasClients || data == null) return;
    if (_visible >= data.recommendations.length) return;
    if (_controller.position.extentAfter < 260) {
      setState(() {
        _visible = (_visible + 4).clamp(0, data.recommendations.length).toInt();
      });
    }
  }

  void _openItem(BuildContext context, ItemModel item) {
    Navigator.pushNamed(context, Routes.itemDetail, arguments: ItemArgs(item));
  }
}
