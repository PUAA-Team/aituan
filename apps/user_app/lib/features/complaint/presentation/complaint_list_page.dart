import 'package:flutter/material.dart';

import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../data/complaint_repository.dart';

class ComplaintListPage extends StatefulWidget {
  const ComplaintListPage({super.key});

  @override
  State<ComplaintListPage> createState() => _ComplaintListPageState();
}

class _ComplaintListPageState extends State<ComplaintListPage> {
  Future<List<ComplaintSummary>>? _future;
  String? _status;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    setState(() => _future = complaintRepository.fetchMy(status: _status));
  }

  Future<void> _openSubmit() async {
    await Navigator.pushNamed(context, Routes.complaintSubmit);
    if (mounted) _load();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('投诉与建议'),
      actions: [
        PopupMenuButton<String?>(
          initialValue: _status,
          icon: const Icon(Icons.filter_list),
          onSelected: (value) {
            _status = value;
            _load();
          },
          itemBuilder: (_) => const [
            PopupMenuItem(value: null, child: Text('全部')),
            PopupMenuItem(value: 'pending', child: Text('待受理')),
            PopupMenuItem(value: 'processing', child: Text('处理中')),
            PopupMenuItem(value: 'resolved', child: Text('已处理')),
            PopupMenuItem(value: 'closed', child: Text('已关闭')),
          ],
        ),
      ],
    ),
    body: RefreshIndicator(
      onRefresh: () async => _load(),
      child: FutureBuilder<List<ComplaintSummary>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return Center(child: Text('加载失败：${snapshot.error}'));
          }
          final list = snapshot.data ?? const [];
          if (list.isEmpty) {
            return ListView(
              padding: const EdgeInsets.all(12),
              children: const [
                AppCard(child: Text('暂无投诉记录。需要平台协助时，可提交新的投诉与建议。')),
              ],
            );
          }
          return ListView.builder(
            padding: const EdgeInsets.all(12),
            itemCount: list.length,
            itemBuilder: (_, index) =>
                _ComplaintCard(item: list[index], onReturned: _load),
          );
        },
      ),
    ),
    floatingActionButton: FloatingActionButton.extended(
      onPressed: _openSubmit,
      icon: const Icon(Icons.add),
      label: const Text('提交投诉'),
    ),
  );
}

class _ComplaintCard extends StatelessWidget {
  const _ComplaintCard({required this.item, required this.onReturned});

  final ComplaintSummary item;
  final VoidCallback onReturned;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: () async {
      await Navigator.pushNamed(
        context,
        Routes.complaintDetail,
        arguments: item.id,
      );
      onReturned();
    },
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Text(
                item.title,
                style: Theme.of(context).textTheme.titleMedium,
              ),
            ),
            _StatusBadge(status: item.status),
          ],
        ),
        const SizedBox(height: 6),
        Text('工单号：${item.ticketNo}'),
        if (item.orderNo != null && item.orderNo!.isNotEmpty)
          Text('关联订单：${item.orderNo}'),
        const SizedBox(height: 6),
        Text(
          '进度：${_statusText(item.status)}',
          style: Theme.of(context).textTheme.bodySmall,
        ),
        const SizedBox(height: 4),
        Text('查看进度、结果和补充意见 ›', style: Theme.of(context).textTheme.labelSmall),
      ],
    ),
  );
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
    decoration: BoxDecoration(
      color: Colors.red.withValues(alpha: 0.08),
      borderRadius: BorderRadius.circular(6),
    ),
    child: Text(_statusText(status), style: const TextStyle(color: Colors.red)),
  );
}

String _statusText(String status) => switch (status) {
  'pending' => '待受理',
  'accepted' => '处理中',
  'resolved' => '已处理',
  'closed' => '已关闭',
  _ => status,
};
