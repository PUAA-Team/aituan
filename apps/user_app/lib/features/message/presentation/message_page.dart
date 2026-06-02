import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/message_item.dart';
import '../../home/data/backend_app_repository.dart';

class MessagePage extends StatefulWidget {
  const MessagePage({super.key});

  @override
  State<MessagePage> createState() => _MessagePageState();
}

class _MessagePageState extends State<MessagePage> {
  static const _tabs = [
    ('', '全部'),
    ('order', '订单'),
    ('system', '系统'),
    ('promotion', '互动'),
  ];

  bool _loading = true;
  bool _markingAll = false;
  Object? _error;
  String _type = '';
  List<MessageItem> _messages = const [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) => SafeArea(
    child: RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 20),
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  '消息',
                  style: Theme.of(context).textTheme.headlineMedium,
                ),
              ),
              TextButton(
                onPressed:
                    _markingAll || _messages.every((item) => !item.unread)
                    ? null
                    : _markAllRead,
                child: Text(_markingAll ? '处理中' : '全部已读'),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            '订单状态、优惠提醒和系统通知都会在这里展示',
            style: Theme.of(context).textTheme.bodySmall,
          ),
          const SizedBox(height: 12),
          _TypeTabs(value: _type, tabs: _tabs, onChanged: _switchType),
          const SizedBox(height: 12),
          if (_loading)
            const AppCard(child: Center(child: CircularProgressIndicator()))
          else if (_error != null)
            _ErrorCard(message: _error.toString(), onRetry: _load)
          else if (_messages.isEmpty)
            const AppCard(child: Text('暂无消息'))
          else
            for (final item in _messages)
              _MessageCard(item: item, onTap: () => _openMessage(item)),
        ],
      ),
    ),
  );

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final messages = await backendRepository.fetchMessages(type: _type);
      if (!mounted) return;
      setState(() {
        _messages = messages;
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

  Future<void> _switchType(String type) async {
    if (_type == type) return;
    setState(() => _type = type);
    await _load();
  }

  Future<void> _markAllRead() async {
    try {
      setState(() => _markingAll = true);
      await backendRepository.markAllMessagesRead();
      await _load();
      if (!mounted) return;
      showAppSnackBar(context, '已全部标记为已读');
    } catch (error) {
      if (!mounted) return;
      showAppSnackBar(context, '操作失败：$error');
    } finally {
      if (mounted) setState(() => _markingAll = false);
    }
  }

  Future<void> _openMessage(MessageItem item) async {
    if (item.unread && item.id.isNotEmpty) {
      try {
        await backendRepository.markMessageRead(item.id);
      } catch (_) {}
    }
    final targetType =
        item.relatedTargetType ??
        (item.relatedOrderId == null ? null : 'order');
    final targetId = item.relatedTargetId ?? item.relatedOrderId;
    if (!mounted) return;
    setState(() {
      _messages = [
        for (final message in _messages)
          message.id == item.id ? message.copyWith(unread: false) : message,
      ];
    });
    if (targetType == 'order' && targetId != null) {
      await Navigator.pushNamed(
        context,
        Routes.orderDetail,
        arguments: OrderDetailArgs(
          kind: _guessOrderKind(item),
          status: OrderStatus.pending,
          orderId: targetId,
        ),
      );
      if (mounted) await _load();
      return;
    }
    showAppSnackBar(context, '暂无可跳转的业务详情');
  }

  OrderKind _guessOrderKind(MessageItem item) {
    final text = '${item.title}${item.content}${item.badgeText}';
    return text.contains('外卖') || text.contains('配送')
        ? OrderKind.takeaway
        : OrderKind.service;
  }
}

class _TypeTabs extends StatelessWidget {
  const _TypeTabs({
    required this.value,
    required this.tabs,
    required this.onChanged,
  });

  final String value;
  final List<(String, String)> tabs;
  final ValueChanged<String> onChanged;

  @override
  Widget build(BuildContext context) => SingleChildScrollView(
    scrollDirection: Axis.horizontal,
    child: Row(
      children: [
        for (final tab in tabs)
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              label: Text(tab.$2),
              selected: value == tab.$1,
              onSelected: (_) => onChanged(tab.$1),
            ),
          ),
      ],
    ),
  );
}

class _MessageCard extends StatelessWidget {
  const _MessageCard({required this.item, required this.onTap});

  final MessageItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => AppCard(
    onTap: onTap,
    backgroundColor: item.unread ? AppColors.brandSoft : Colors.white,
    borderColor: item.unread ? AppColors.brandLine : AppColors.line,
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _MessageIcon(type: item.type, unread: item.unread),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      item.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleMedium,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    item.time,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: AppColors.textLight,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 5),
              Text(
                item.content,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.bodySmall,
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  if (item.badgeText.isNotEmpty) _Badge(text: item.badgeText),
                  if (item.unread) ...[
                    const SizedBox(width: 8),
                    const _Badge(text: '未读', emphasis: true),
                  ],
                  const Spacer(),
                  const Icon(
                    Icons.chevron_right,
                    size: 18,
                    color: AppColors.textLight,
                  ),
                ],
              ),
            ],
          ),
        ),
      ],
    ),
  );
}

class _MessageIcon extends StatelessWidget {
  const _MessageIcon({required this.type, required this.unread});

  final String type;
  final bool unread;

  @override
  Widget build(BuildContext context) => Container(
    width: 42,
    height: 42,
    decoration: BoxDecoration(
      color: unread ? AppColors.brand : AppColors.brandSoft,
      borderRadius: BorderRadius.circular(9),
      border: Border.all(color: AppColors.brandLine),
    ),
    child: Icon(_icon, color: unread ? Colors.white : AppColors.brand),
  );

  IconData get _icon => switch (type) {
    'order' => Icons.receipt_long_outlined,
    'system' => Icons.campaign_outlined,
    'promotion' => Icons.local_activity_outlined,
    _ => Icons.notifications_none,
  };
}

class _Badge extends StatelessWidget {
  const _Badge({required this.text, this.emphasis = false});

  final String text;
  final bool emphasis;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
    decoration: BoxDecoration(
      color: emphasis ? AppColors.brand : AppColors.soft,
      borderRadius: BorderRadius.circular(999),
    ),
    child: Text(
      text,
      style: Theme.of(context).textTheme.labelSmall?.copyWith(
        color: emphasis ? Colors.white : AppColors.textSub,
        fontWeight: FontWeight.w700,
      ),
    ),
  );
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => AppCard(
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('消息加载失败', style: Theme.of(context).textTheme.titleMedium),
        const SizedBox(height: 6),
        Text(message, style: Theme.of(context).textTheme.bodySmall),
        const SizedBox(height: 10),
        FilledButton(onPressed: onRetry, child: const Text('重试')),
      ],
    ),
  );
}
