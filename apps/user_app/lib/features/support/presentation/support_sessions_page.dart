import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../data/support_repository.dart';

class SupportSessionsPage extends StatefulWidget {
  const SupportSessionsPage({super.key, this.launchArgs});

  final SupportLaunchArgs? launchArgs;

  @override
  State<SupportSessionsPage> createState() => _SupportSessionsPageState();
}

class _SupportSessionsPageState extends State<SupportSessionsPage> {
  Future<List<SupportSession>>? _future;
  String? _statusFilter;
  String _typeFilter = 'all';
  bool _handledLaunch = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_handledLaunch) return;
    final args = widget.launchArgs;
    if (args == null || args.storeId == null) return;
    _handledLaunch = true;
    WidgetsBinding.instance.addPostFrameCallback((_) => _handleLaunch(args));
  }

  void _load() {
    setState(() {
      _future = supportRepository.fetchSessions(status: _statusFilter);
    });
  }

  Future<void> _handleLaunch(SupportLaunchArgs args) async {
    final storeId = args.storeId;
    if (storeId == null || !mounted) return;
    final sessions = await supportRepository.fetchSessions();
    if (!mounted) return;
    // 已有 open 会话直接打开，避免重复发起。
    final existing = sessions
        .where((s) => s.storeId == storeId && s.status == 'open')
        .toList();
    if (existing.isNotEmpty) {
      final session = existing.first;
      await Navigator.pushNamed(
        context,
        Routes.supportChat,
        arguments: session.id,
      );
      if (mounted && widget.launchArgs != null) {
        Navigator.pop(context);
      } else if (mounted) {
        _load();
      }
      return;
    }
    if (!mounted) return;
    try {
      final created = await supportRepository.createSession(
        storeId: storeId,
        topic: args.topicHint ?? '商家客服咨询',
        relatedOrderId: args.relatedOrderId,
      );
      if (!mounted) return;
      await Navigator.pushNamed(
        context,
        Routes.supportChat,
        arguments: created.id,
      );
      if (mounted && widget.launchArgs != null) {
        Navigator.pop(context);
      } else if (mounted) {
        _load();
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('发起会话失败：$e')));
    }
  }

  Future<void> _openPlatformSupport() async {
    final sessions = await supportRepository.fetchSessions();
    if (!mounted) return;
    final existing = sessions
        .where((s) => s.storeId == 0 && s.status == 'open')
        .toList();
    final session = existing.isNotEmpty
        ? existing.first
        : await supportRepository.createSession(topic: '平台客服');
    if (!mounted) return;
    await Navigator.pushNamed(
      context,
      Routes.supportChat,
      arguments: session.id,
    );
    if (mounted) _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('我的咨询'),
        actions: [
          IconButton(
            tooltip: '平台客服',
            onPressed: _openPlatformSupport,
            icon: const Icon(Icons.support_agent),
          ),
          PopupMenuButton<String?>(
            initialValue: _statusFilter,
            onSelected: (v) {
              _statusFilter = v;
              _load();
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: null, child: Text('全部')),
              PopupMenuItem(value: 'open', child: Text('进行中')),
              PopupMenuItem(value: 'closed', child: Text('已关闭')),
            ],
            icon: const Icon(Icons.filter_list),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async => _load(),
        child: FutureBuilder<List<SupportSession>>(
          future: _future,
          builder: (context, snapshot) {
            if (snapshot.connectionState != ConnectionState.done) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snapshot.hasError) {
              return Center(child: Text('加载失败：${snapshot.error}'));
            }
            final all = snapshot.data ?? const [];
            final list = all.where((s) {
              if (_typeFilter == 'platform') return s.storeId == 0;
              if (_typeFilter == 'merchant') return s.storeId != 0;
              return true;
            }).toList();
            if (list.isEmpty) {
              return ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  _TypeTabs(
                    active: _typeFilter,
                    onChanged: (v) => setState(() => _typeFilter = v),
                  ),
                  const SizedBox(height: 12),
                  const AppCard(child: Text('暂无咨询会话，可从商家详情页、订单页或平台客服入口发起')),
                ],
              );
            }
            return ListView(
              padding: const EdgeInsets.all(12),
              children: [
                _TypeTabs(
                  active: _typeFilter,
                  onChanged: (v) => setState(() => _typeFilter = v),
                ),
                const SizedBox(height: 12),
                for (final session in list)
                  AppCard(
                    child: InkWell(
                      onTap: () async {
                        await Navigator.pushNamed(
                          context,
                          Routes.supportChat,
                          arguments: session.id,
                        );
                        _load();
                      },
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  session.storeName,
                                  style: Theme.of(
                                    context,
                                  ).textTheme.titleMedium,
                                ),
                              ),
                              if (session.storeId == 0)
                                const _Badge(text: '平台', color: Colors.blue),
                              const SizedBox(width: 6),
                              if (session.status == 'open')
                                const _Badge(text: '进行中', color: Colors.green)
                              else
                                const _Badge(text: '已关闭', color: Colors.grey),
                            ],
                          ),
                          if (session.lastMessage != null) ...[
                            const SizedBox(height: 6),
                            Text(
                              session.lastMessage!,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                          if (session.unreadCount > 0) ...[
                            const SizedBox(height: 6),
                            Text(
                              '未读 ${session.unreadCount}',
                              style: const TextStyle(color: Colors.red),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),
              ],
            );
          },
        ),
      ),
    );
  }
}

class _TypeTabs extends StatelessWidget {
  const _TypeTabs({required this.active, required this.onChanged});
  final String active;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) => SegmentedButton<String>(
    segments: const [
      ButtonSegment(value: 'all', label: Text('全部')),
      ButtonSegment(value: 'merchant', label: Text('商家客服')),
      ButtonSegment(value: 'platform', label: Text('平台客服')),
    ],
    selected: {active},
    onSelectionChanged: (v) => onChanged(v.first),
  );
}

class _Badge extends StatelessWidget {
  const _Badge({required this.text, required this.color});
  final String text;
  final Color color;
  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
    decoration: BoxDecoration(
      color: color.withValues(alpha: 0.1),
      borderRadius: BorderRadius.circular(4),
    ),
    child: Text(text, style: TextStyle(color: color, fontSize: 12)),
  );
}
