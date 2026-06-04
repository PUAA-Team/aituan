import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../../core/widgets/app_card.dart';
import '../../home/data/backend_app_repository.dart';
import '../data/review_repository.dart';

class ReviewDetailPage extends StatefulWidget {
  const ReviewDetailPage({super.key, required this.reviewId});

  final int reviewId;

  @override
  State<ReviewDetailPage> createState() => _ReviewDetailPageState();
}

class _ReviewDetailPageState extends State<ReviewDetailPage> {
  ReviewSummary? _review;
  bool _loading = true;
  bool _helpfulBusy = false;
  bool _reporting = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final r = await reviewRepository.fetchDetail(widget.reviewId);
      if (!mounted) return;
      setState(() {
        _review = r;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = '$e';
        _loading = false;
      });
    }
  }

  Future<void> _toggleHelpful() async {
    if (_helpfulBusy) return;
    setState(() => _helpfulBusy = true);
    try {
      final (helpful, count) = await reviewRepository.toggleHelpful(
        widget.reviewId,
      );
      if (!mounted) return;
      setState(() {
        if (_review != null) {
          _review = ReviewSummary(
            id: _review!.id,
            orderId: _review!.orderId,
            orderTitle: _review!.orderTitle,
            storeName: _review!.storeName,
            userMaskedNickname: _review!.userMaskedNickname,
            rating: _review!.rating,
            content: _review!.content,
            labels: _review!.labels,
            imageUrls: _review!.imageUrls,
            helpfulCount: count,
            reportedCount: _review!.reportedCount,
            helpfulByMe: helpful,
            status: _review!.status,
            replied: _review!.replied,
            replyContent: _review!.replyContent,
            repliedAt: _review!.repliedAt,
            createdAt: _review!.createdAt,
          );
        }
      });
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('操作失败：$e')));
    } finally {
      if (mounted) setState(() => _helpfulBusy = false);
    }
  }

  Future<void> _report() async {
    if (_reporting) return;
    final result = await showDialog<_ReportDraft>(
      context: context,
      builder: (_) => const _ReportDialog(),
    );
    if (result == null) return;
    if (!mounted) return;
    setState(() => _reporting = true);
    try {
      await reviewRepository.report(
        widget.reviewId,
        result.reason,
        detail: result.detail,
        evidenceUrls: result.evidenceUrls,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('已提交举报，我们将尽快审核')));
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('举报失败：$e')));
    } finally {
      if (mounted) setState(() => _reporting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('评价详情')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : (_error != null
                ? Center(child: Text(_error!))
                : _buildBody(_review!)),
    );
  }

  Widget _buildBody(ReviewSummary r) {
    return ListView(
      padding: const EdgeInsets.all(12),
      children: [
        AppCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(r.storeName, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 4),
              Text(r.orderTitle, style: Theme.of(context).textTheme.bodySmall),
              const SizedBox(height: 8),
              Row(
                children: [
                  for (var i = 0; i < 5; i++)
                    Icon(
                      i < r.rating ? Icons.star : Icons.star_border,
                      size: 18,
                      color: Colors.orange,
                    ),
                ],
              ),
              const SizedBox(height: 12),
              Text(r.content),
              if (r.imageUrls.isNotEmpty) ...[
                const SizedBox(height: 12),
                GridView.count(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  crossAxisCount: 3,
                  crossAxisSpacing: 6,
                  mainAxisSpacing: 6,
                  children: [
                    for (final url in r.imageUrls)
                      ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: _ReviewImage(url: url),
                      ),
                  ],
                ),
              ],
              if (r.labels.isNotEmpty) ...[
                const SizedBox(height: 12),
                Wrap(
                  spacing: 6,
                  runSpacing: 6,
                  children: r.labels
                      .map(
                        (l) => Chip(label: Text(l), padding: EdgeInsets.zero),
                      )
                      .toList(),
                ),
              ],
              const SizedBox(height: 8),
              Text(
                '发布时间：${r.createdAt}',
                style: Theme.of(context).textTheme.bodySmall,
              ),
            ],
          ),
        ),
        if (r.replyContent != null && r.replyContent!.isNotEmpty)
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('商家回复', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 6),
                Text(r.replyContent!),
                if (r.repliedAt != null) ...[
                  const SizedBox(height: 6),
                  Text(
                    '回复时间：${r.repliedAt}',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
              ],
            ),
          ),
        AppCard(
          child: Row(
            children: [
              TextButton.icon(
                icon: Icon(
                  r.helpfulByMe ? Icons.thumb_up : Icons.thumb_up_outlined,
                ),
                label: Text('有用 ${r.helpfulCount}'),
                onPressed: _helpfulBusy ? null : _toggleHelpful,
              ),
              const Spacer(),
              TextButton.icon(
                icon: const Icon(Icons.flag_outlined),
                label: Text(_reporting ? '提交中' : '举报'),
                onPressed: _reporting ? null : _report,
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ReportDraft {
  const _ReportDraft({
    required this.reason,
    required this.detail,
    required this.evidenceUrls,
  });

  final String reason;
  final String detail;
  final List<String> evidenceUrls;
}

class _ReportDialog extends StatefulWidget {
  const _ReportDialog();

  @override
  State<_ReportDialog> createState() => _ReportDialogState();
}

class _ReportDialogState extends State<_ReportDialog> {
  static const _reasons = ['广告/营销', '辱骂攻击', '虚假内容', '违法违规', '其他'];
  static const _maxImages = 3;

  String _reason = _reasons.first;
  final _detailController = TextEditingController();
  final List<String> _imageUrls = [];
  bool _uploading = false;

  @override
  void dispose() {
    _detailController.dispose();
    super.dispose();
  }

  Future<void> _pickImage() async {
    if (_uploading || _imageUrls.length >= _maxImages) return;
    setState(() => _uploading = true);
    try {
      final picked = await ImagePicker().pickImage(
        source: ImageSource.gallery,
        maxWidth: 1280,
        imageQuality: 80,
      );
      if (picked == null) return;
      final url = await backendRepository.uploadCommonFile(
        picked.path,
        bizType: 'report',
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

  void _submit() {
    if (_uploading) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('图片上传中，请稍候')));
      return;
    }
    Navigator.pop(
      context,
      _ReportDraft(
        reason: _reason,
        detail: _detailController.text.trim(),
        evidenceUrls: List.of(_imageUrls),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('举报评价'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DropdownButtonFormField<String>(
              initialValue: _reason,
              decoration: const InputDecoration(labelText: '举报原因'),
              items: _reasons
                  .map(
                    (reason) =>
                        DropdownMenuItem(value: reason, child: Text(reason)),
                  )
                  .toList(),
              onChanged: (value) {
                if (value != null) setState(() => _reason = value);
              },
            ),
            const SizedBox(height: 10),
            TextField(
              controller: _detailController,
              maxLines: 3,
              maxLength: 200,
              decoration: const InputDecoration(
                labelText: '补充说明',
                hintText: '可填写问题说明或上传截图证据',
              ),
            ),
            const SizedBox(height: 6),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final url in _imageUrls)
                  InputChip(
                    label: const Text('已上传图片'),
                    onDeleted: () => setState(() => _imageUrls.remove(url)),
                  ),
                if (_imageUrls.length < _maxImages)
                  OutlinedButton.icon(
                    onPressed: _uploading ? null : _pickImage,
                    icon: const Icon(Icons.image_outlined),
                    label: Text(_uploading ? '上传中…' : '添加图片'),
                  ),
              ],
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('取消'),
        ),
        TextButton(
          onPressed: _uploading ? null : _submit,
          child: Text(_uploading ? '上传中…' : '提交'),
        ),
      ],
    );
  }
}

class _ReviewImage extends StatelessWidget {
  const _ReviewImage({required this.url});

  final String url;

  @override
  Widget build(BuildContext context) {
    final resolved = backendRepository.resolveAssetUrl(url);
    if (resolved == null) {
      return Container(color: Colors.grey.shade200);
    }
    return Image.network(
      resolved,
      fit: BoxFit.cover,
      errorBuilder: (_, _, _) => Container(
        color: Colors.grey.shade200,
        alignment: Alignment.center,
        child: const Icon(Icons.broken_image_outlined, color: Colors.grey),
      ),
    );
  }
}
