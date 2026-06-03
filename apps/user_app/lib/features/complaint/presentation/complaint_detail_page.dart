import 'package:flutter/material.dart';

import '../../../core/widgets/app_card.dart';
import '../data/complaint_repository.dart';

class ComplaintDetailPage extends StatefulWidget {
  const ComplaintDetailPage({super.key, required this.complaintId});

  final int complaintId;

  @override
  State<ComplaintDetailPage> createState() => _ComplaintDetailPageState();
}

class _ComplaintDetailPageState extends State<ComplaintDetailPage> {
  Future<ComplaintDetail>? _future;
  final _supplementController = TextEditingController();
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _supplementController.dispose();
    super.dispose();
  }

  void _load() {
    setState(
      () => _future = complaintRepository.fetchDetail(widget.complaintId),
    );
  }

  Future<void> _submitSupplement() async {
    final text = _supplementController.text.trim();
    if (text.isEmpty || _submitting) return;
    setState(() => _submitting = true);
    try {
      await complaintRepository.supplement(widget.complaintId, text);
      if (!mounted) return;
      _supplementController.clear();
      _load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('补充失败：$e')));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('投诉详情')),
    body: FutureBuilder<ComplaintDetail>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return Center(child: Text('加载失败：${snapshot.error}'));
        }
        final detail = snapshot.data!;
        final item = detail.complaint;
        final canSupplement = item.status != 'closed';
        return ListView(
          padding: const EdgeInsets.all(12),
          children: [
            AppCard(
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
                  const SizedBox(height: 8),
                  Text('工单号：${item.ticketNo}'),
                  if (item.orderNo != null && item.orderNo!.isNotEmpty)
                    Text('关联订单：${item.orderNo}'),
                  if (item.storeName != null && item.storeName!.isNotEmpty)
                    Text('关联商家：${item.storeName}'),
                  const SizedBox(height: 8),
                  Text(item.detail),
                  if (item.evidenceUrls.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Text('已上传图片：${item.evidenceUrls.length} 张'),
                  ],
                ],
              ),
            ),
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('处理进度', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  for (final log in detail.logs)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 10),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(
                            Icons.radio_button_checked,
                            size: 16,
                            color: Colors.red,
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(_actionText(log.action, log.operatorType)),
                                if (log.remark != null &&
                                    log.remark!.isNotEmpty)
                                  Text(
                                    log.remark!,
                                    style: Theme.of(
                                      context,
                                    ).textTheme.bodySmall,
                                  ),
                                Text(
                                  log.createdAt,
                                  style: Theme.of(context).textTheme.labelSmall,
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
            AppCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('补充意见', style: Theme.of(context).textTheme.titleMedium),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _supplementController,
                    maxLines: 3,
                    maxLength: 300,
                    enabled: canSupplement,
                    decoration: InputDecoration(
                      hintText: canSupplement ? '补充问题进展、诉求或新的说明' : '工单已关闭',
                    ),
                  ),
                  Align(
                    alignment: Alignment.centerRight,
                    child: FilledButton(
                      onPressed: canSupplement && !_submitting
                          ? _submitSupplement
                          : null,
                      child: Text(_submitting ? '提交中…' : '提交补充'),
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
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
  'processing' => '处理中',
  'accepted' => '处理中',
  'resolved' => '已处理',
  'closed' => '已关闭',
  _ => status,
};

String _actionText(String action, String operatorType) => switch (action) {
  'submit' => '已提交',
  'accept' => '平台已受理',
  'resolve' => '平台已处理',
  'close' => '工单已关闭',
  'supplement' => '用户补充意见',
  _ => operatorType == 'admin' ? '平台处理' : '状态更新',
};
