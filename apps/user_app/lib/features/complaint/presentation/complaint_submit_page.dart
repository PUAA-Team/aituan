import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/order_model.dart';
import '../../home/data/backend_app_repository.dart';
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
  final List<String> _imageUrls = [];
  List<OrderModel> _orders = const [];
  int? _selectedOrderId;
  String? _selectedOrderTitle;
  bool _submitting = false;
  bool _uploading = false;
  bool _loadingOrders = false;

  @override
  void initState() {
    super.initState();
    _selectedOrderId = widget.args?.orderId;
    _selectedOrderTitle = widget.args?.orderTitle;
    if (widget.args?.orderId == null) _loadOrders();
  }

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
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请完整填写标题与详细描述')));
      return;
    }
    setState(() => _submitting = true);
    try {
      await complaintRepository.submit(
        orderId: _selectedOrderId,
        category: _category,
        title: title,
        detail: detail,
        evidenceUrls: _imageUrls,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('投诉已提交，平台会尽快处理')));
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('提交失败：$e')));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _loadOrders() async {
    setState(() => _loadingOrders = true);
    try {
      final orders = await backendRepository.fetchOrders();
      if (!mounted) return;
      setState(() {
        _orders = orders;
        _loadingOrders = false;
      });
    } catch (_) {
      if (mounted) setState(() => _loadingOrders = false);
    }
  }

  Future<void> _chooseOrder() async {
    if (_orders.isEmpty) return;
    final selected = await showModalBottomSheet<OrderModel>(
      context: context,
      showDragHandle: true,
      builder: (_) => SafeArea(
        child: ListView(
          shrinkWrap: true,
          children: [
            ListTile(
              leading: const Icon(Icons.block),
              title: const Text('不关联订单'),
              onTap: () => Navigator.pop(context),
            ),
            for (final order in _orders)
              ListTile(
                title: Text(
                  order.title,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                subtitle: Text('${order.storeName} · ${order.id}'),
                trailing: Text(order.status.labelForKind(order.kind)),
                onTap: () => Navigator.pop(context, order),
              ),
          ],
        ),
      ),
    );
    setState(() {
      _selectedOrderId = selected == null ? null : int.tryParse(selected.id);
      _selectedOrderTitle = selected?.title;
    });
  }

  Future<void> _pickImage() async {
    if (_imageUrls.length >= 3 || _uploading) return;
    setState(() => _uploading = true);
    try {
      final picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        imageQuality: 82,
      );
      if (picked == null) return;
      final url = await backendRepository.uploadCommonFile(
        picked,
        bizType: 'complaint',
      );
      if (!mounted) return;
      setState(() => _imageUrls.add(url));
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('图片上传失败：$e')));
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('提交投诉')),
      body: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          AppCard(
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '关联订单',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 4),
                      Text(_selectedOrderTitle ?? '未选择订单'),
                    ],
                  ),
                ),
                if (widget.args?.orderId == null)
                  OutlinedButton(
                    onPressed: _loadingOrders ? null : _chooseOrder,
                    child: Text(_loadingOrders ? '加载中…' : '选择订单'),
                  ),
              ],
            ),
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
                      .map(
                        (e) => ChoiceChip(
                          label: Text(e.value),
                          selected: _category == e.key,
                          onSelected: (_) => setState(() => _category = e.key),
                        ),
                      )
                      .toList(),
                ),
              ],
            ),
          ),
          AppCard(
            child: TextField(
              controller: _titleController,
              maxLength: 60,
              decoration: const InputDecoration(
                labelText: '标题',
                hintText: '一句话概括问题',
              ),
            ),
          ),
          AppCard(
            child: TextField(
              controller: _detailController,
              maxLines: 6,
              maxLength: 500,
              decoration: const InputDecoration(
                labelText: '详细描述',
                hintText: '请描述问题发生时间、影响和期望处理结果',
              ),
            ),
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('补充图片', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                _EvidenceImageGrid(
                  urls: _imageUrls,
                  uploading: _uploading,
                  onAdd: _pickImage,
                  onRemove: (url) => setState(() => _imageUrls.remove(url)),
                ),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: AppBottomActionBar(
        primaryText: _submitting ? '提交中…' : '提交',
        onPrimary: _submitting || _uploading ? null : _submit,
      ),
    );
  }
}

class _EvidenceImageGrid extends StatelessWidget {
  const _EvidenceImageGrid({
    required this.urls,
    required this.uploading,
    required this.onAdd,
    required this.onRemove,
  });

  final List<String> urls;
  final bool uploading;
  final VoidCallback onAdd;
  final ValueChanged<String> onRemove;

  @override
  Widget build(BuildContext context) {
    final tiles = <Widget>[
      for (final url in urls)
        _EvidenceImageTile(url: url, onRemove: () => onRemove(url)),
      if (urls.length < 3)
        InkWell(
          onTap: uploading ? null : onAdd,
          borderRadius: BorderRadius.circular(8),
          child: Container(
            decoration: BoxDecoration(
              color: Colors.grey.shade100,
              border: Border.all(color: Colors.grey.shade300),
              borderRadius: BorderRadius.circular(8),
            ),
            alignment: Alignment.center,
            child: uploading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.add_a_photo_outlined, color: Colors.grey),
          ),
        ),
    ];
    return GridView.count(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisCount: 3,
      crossAxisSpacing: 8,
      mainAxisSpacing: 8,
      children: tiles,
    );
  }
}

class _EvidenceImageTile extends StatelessWidget {
  const _EvidenceImageTile({required this.url, required this.onRemove});

  final String url;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final resolved = backendRepository.resolveAssetUrl(url);
    return Stack(
      fit: StackFit.expand,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: resolved == null
              ? Container(color: Colors.grey.shade200)
              : Image.network(
                  resolved,
                  fit: BoxFit.cover,
                  errorBuilder: (_, _, _) => Container(
                    color: Colors.grey.shade200,
                    alignment: Alignment.center,
                    child: const Icon(Icons.broken_image_outlined, color: Colors.grey),
                  ),
                ),
        ),
        Positioned(
          top: 2,
          right: 2,
          child: InkWell(
            onTap: onRemove,
            child: const CircleAvatar(
              radius: 10,
              backgroundColor: Colors.black54,
              child: Icon(Icons.close, size: 12, color: Colors.white),
            ),
          ),
        ),
      ],
    );
  }
}
