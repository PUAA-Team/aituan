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
      await Navigator.pushNamed(context, Routes.supportChat, arguments: session.id);
      if (mounted) _load();
      return;
    }
    if (!mounted) return;
    final topic = await _askTopic(args);
    if (topic == null || topic.isEmpty || !mounted) return;
    try {
      final created = await supportRepository.createSession(
        storeId: storeId,
        topic: topic,
        relatedOrderId: args.relatedOrderId,
      );
      if (!mounted) return;
      await Navigator.pushNamed(context, Routes.supportChat, arguments: created.id);
      if (mounted) _load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('发起会话失败：$e')));
    }
  }

  Future<String?> _askTopic(SupportLaunchArgs args) async {
    final controller = TextEditingController(text: args.topicHint ?? '');
    final result = await showDialog<String>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(args.storeName == null ? '发起咨询' : '咨询${args.storeName}'),
        content: TextField(
          controller: controller,
          maxLength: 80,
          autofocus: true,
          decoration: const InputDecoration(
            hintText: '请输入您要咨询的内容，例如：菜品份量问题',
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('取消')),
          FilledButton(
            onPressed: () => Navigator.pop(context, controller.text.trim()),
            child: const Text('发起'),
          ),
        ],
      ),
    );
    return result;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('我的咨询'),
        actions: [
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
            if (snapshot.hasError) return Center(child: Text('加载失败：${snapshot.error}'));
            final list = snapshot.data ?? const [];
            if (list.isEmpty) {
              return const Center(child: Text('暂无咨询会话，可从商家详情页发起'));
            }
            return ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: list.length,
              itemBuilder: (_, i) => AppCard(
                child: InkWell(
                  onTap: () async {
                    await Navigator.pushNamed(context, Routes.supportChat, arguments: list[i].id);
                    _load();
                  },
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(children: [
                        Expanded(child: Text(list[i].storeName, style: Theme.of(context).textTheme.titleMedium)),
                        if (list[i].status == 'open')
                          const _Badge(text: '进行中', color: Colors.green)
                        else
                          const _Badge(text: '已关闭', color: Colors.grey),
                      ]),
                      const SizedBox(height: 4),
                      Text(list[i].topic, style: Theme.of(context).textTheme.bodySmall),
                      if (list[i].lastMessage != null) ...[
                        const SizedBox(height: 6),
                        Text(list[i].lastMessage!, maxLines: 1, overflow: TextOverflow.ellipsis),
                      ],
                      if (list[i].unreadCount > 0) ...[
                        const SizedBox(height: 6),
                        Text('未读 ${list[i].unreadCount}', style: const TextStyle(color: Colors.red)),
                      ],
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.text, required this.color});
  final String text;
  final Color color;
  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(color: color.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(4)),
        child: Text(text, style: TextStyle(color: color, fontSize: 12)),
      );
}
