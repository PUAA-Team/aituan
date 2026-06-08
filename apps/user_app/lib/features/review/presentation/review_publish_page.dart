import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/widgets/app_bottom_action_bar.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/cached_app_image.dart';
import '../../home/data/backend_app_repository.dart';
import '../data/review_repository.dart';

const _availableLabels = [
  '味道好',
  '出餐快',
  '服务好',
  '分量足',
  '性价比高',
  '环境好',
  '送达快',
  '会再来',
];
const _maxImages = 9;

class ReviewPublishPage extends StatefulWidget {
  const ReviewPublishPage({super.key});

  @override
  State<ReviewPublishPage> createState() => _ReviewPublishPageState();
}

class _ReviewPublishPageState extends State<ReviewPublishPage> {
  int _score = 5;
  final _controller = TextEditingController();
  final _selectedLabels = <String>{};
  final List<String> _imageUrls = [];
  bool _submitting = false;
  bool _uploading = false;
  ReviewArgs? _args;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final raw = ModalRoute.of(context)?.settings.arguments;
    _args = raw is ReviewArgs ? raw : null;
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _pickImage() async {
    if (_imageUrls.length >= _maxImages) return;
    final picked = await ImagePicker().pickImage(
      source: ImageSource.gallery,
      maxWidth: 1280,
      imageQuality: 80,
    );
    if (picked == null) return;
    setState(() => _uploading = true);
    try {
      final url = await backendRepository.uploadCommonFile(
        picked,
        bizType: 'review',
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

  Future<void> _submit() async {
    final orderId = _args?.orderId;
    if (orderId == null || orderId.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('订单信息缺失，无法发布评价')));
      return;
    }
    final content = _controller.text.trim();
    if (content.isEmpty) {
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('请填写评价内容')));
      return;
    }
    setState(() => _submitting = true);
    try {
      await reviewRepository.submit(
        orderId: orderId,
        rating: _score,
        content: content,
        labels: _selectedLabels.toList(),
        imageUrls: _imageUrls,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('评价已发布')));
      Navigator.pop(context, true);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('发布失败：$e')));
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = _args?.title ?? '我的订单';
    return Scaffold(
      appBar: AppBar(title: const Text('发布评价')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: Theme.of(context).textTheme.titleLarge),
                const SizedBox(height: 4),
                Text(
                  '订单已完成，发布评价后可帮助更多同学选择',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('本次体验评分', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                Row(
                  children: [
                    for (var i = 1; i <= 5; i += 1)
                      IconButton(
                        onPressed: () => setState(() => _score = i),
                        icon: Icon(
                          i <= _score ? Icons.star : Icons.star_border,
                          color: AppColors.brand,
                          size: 30,
                        ),
                      ),
                  ],
                ),
              ],
            ),
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('感受标签', style: Theme.of(context).textTheme.titleMedium),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: _availableLabels.map((label) {
                    final on = _selectedLabels.contains(label);
                    return ChoiceChip(
                      label: Text(label),
                      selected: on,
                      onSelected: (v) => setState(() {
                        if (v) {
                          _selectedLabels.add(label);
                        } else {
                          _selectedLabels.remove(label);
                        }
                      }),
                    );
                  }).toList(),
                ),
              ],
            ),
          ),
          AppCard(
            child: TextField(
              controller: _controller,
              maxLines: 5,
              maxLength: 500,
              decoration: const InputDecoration(hintText: '分享本次体验，帮助更多同学选择'),
            ),
          ),
          AppCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        '上传图片（${_imageUrls.length}/$_maxImages）',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ),
                    if (_uploading)
                      const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                  ],
                ),
                const SizedBox(height: 8),
                _ImageGrid(
                  urls: _imageUrls,
                  onAdd: _uploading || _imageUrls.length >= _maxImages
                      ? null
                      : _pickImage,
                  onRemove: (i) => setState(() => _imageUrls.removeAt(i)),
                ),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: AppBottomActionBar(
        primaryText: _submitting ? '提交中…' : '提交评价',
        onPrimary: _submitting || _uploading ? null : _submit,
      ),
    );
  }
}

class _ImageGrid extends StatelessWidget {
  const _ImageGrid({
    required this.urls,
    required this.onAdd,
    required this.onRemove,
  });

  final List<String> urls;
  final VoidCallback? onAdd;
  final ValueChanged<int> onRemove;

  @override
  Widget build(BuildContext context) {
    final tiles = <Widget>[
      for (var i = 0; i < urls.length; i += 1)
        _ImageTile(url: urls[i], onRemove: () => onRemove(i)),
      if (urls.length < _maxImages)
        InkWell(
          onTap: onAdd,
          borderRadius: BorderRadius.circular(8),
          child: Container(
            decoration: BoxDecoration(
              color: Colors.grey.shade100,
              border: Border.all(color: Colors.grey.shade300),
              borderRadius: BorderRadius.circular(8),
            ),
            alignment: Alignment.center,
            child: const Icon(Icons.add_a_photo_outlined, color: Colors.grey),
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

class _ImageTile extends StatelessWidget {
  const _ImageTile({required this.url, required this.onRemove});

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
          child: CachedAppImage(
            imageUrl: resolved,
            fit: BoxFit.cover,
            placeholder: Container(
              color: Colors.grey.shade200,
              alignment: Alignment.center,
              child: const Icon(
                Icons.broken_image_outlined,
                color: Colors.grey,
              ),
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
