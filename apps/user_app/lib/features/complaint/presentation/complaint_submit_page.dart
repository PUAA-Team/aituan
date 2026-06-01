import 'package:flutter/material.dart';

import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../data/complaint_repository.dart';

class ComplaintSubmitArgs {
  const ComplaintSubmitArgs({this.orderId, this.orderTitle});
  final int? orderId;
  final String? orderTitle;
}

const _categories = {
  'service': '服务态度',
  'quality': '商品质量',
  'delivery': '配送问题',
  'other': '其他',
};

class ComplaintSubmitPage extends StatefulWidget {
  const ComplaintSubmitPage({super.key, this.args});

  final ComplaintSubmitArgs? args;

  @override
  State<ComplaintSubmitPage> createState() => _ComplaintSubmitPageState();
}

class _ComplaintSubmitPageState extends State<ComplaintSubmitPage> {
  String _category = 'service';
  final _titleController = TextEditingController();
  final _detailController = TextEditingController();
  bool _submitting = false;

  @override
  void dispose() {
    _titleController.dispose();
    _detailController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    final title = _titleController.text.trim();
    final detail = _detailController.text.trim();
    if (title.isEmpty || detail.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('请完整填写标题与详细描述')),
      );
      return;
    }
    setState(() => _submitting = true);
    try {
      await complaintRepository.submit(
        orderId: widget.args?.orderId,
        category: _category,
        title: title,
        detail: detail,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('投诉已提交，平台会尽快处理')),
      );
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('提交失败：$e')),
      );
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('提交投诉')),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          if (widget.args?.orderTitle != null)
            AppCard(
              child: Text('关联订单：${widget.args!.orderTitle}'),
            ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('投诉分类', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _categories.entries
                      .map((e) => ChoiceChip(
                            label: Text(e.value),
                            selected: _category == e.key,
                            onSelected: (_) => setState(() => _category = e.key),
                          ))
                      .toList(),
                ),
              ],
            ),
          ),
          AppCard(
            child: TextField(
              controller: _titleController,
              maxLength: 60,
              decoration: const InputDecoration(labelText: '标题', hintText: '一句话概括问题'),
            ),
          ),
          AppCard(
            child: TextField(
              controller: _detailController,
              maxLines: 6,
              maxLength: 500,
              decoration: const InputDecoration(labelText: '详细描述', hintText: '请描述问题发生时间、影响和期望处理结果'),
            ),
          ),
        ],
      ),
      bottomNavigationBar: AppBottomActionBar(
        primaryText: _submitting ? '提交中…' : '提交',
        onPrimary: _submitting ? null : _submit,
      ),
    );
  }
}
