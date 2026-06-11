import 'package:flutter/material.dart';

import '../../../app/app_state.dart';
import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../../../shared/models/message_item.dart';
import '../../home/data/backend_app_repository.dart';

class MessagePage extends StatefulWidget {
  const MessagePage({super.key});

  @override
  State<MessagePage> createState() => _MessagePageState();
}

class _MessagePageState extends State<MessagePage> {
  static const _groups = <_GroupTab>[
    _GroupTab(key: 'all', label: '全部', icon: Icons.inbox_outlined),
    _GroupTab(key: 'review', label: '评价', icon: Icons.rate_review_outlined),
    _GroupTab(key: 'support', label: '咨询', icon: Icons.support_agent),
    _GroupTab(key: 'complaint', label: '投诉', icon: Icons.report_outlined),
    _GroupTab(key: 'order', label: '订单', icon: Icons.receipt_long_outlined),
    _GroupTab(key: 'system', label: '系统', icon: Icons.notifications_none),
  ];

  bool _loading = true;
  Object? _error;
  List<MessageItem> _messages = const [];
  String _activeGroup = 'all';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  Widget build(BuildContext context) {
    final filtered = _filteredMessages;
    return SafeArea(
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
                  onPressed: _messages.any((message) => message.unread)
                      ? _markAllRead
                      : null,
                  child: const Text('全部已读'),
                ),
              ],
            ),
            const SizedBox(height: 12),
            _GroupTabsBar(
              groups: _groups,
              countOf: _countOf,
              active: _activeGroup,
              onTap: (g) => setState(() => _activeGroup = g),
            ),
            const SizedBox(height: 12),
            if (_loading)
              const AppCard(child: Center(child: CircularProgressIndicator()))
            else if (_error != null)
              _ErrorCard(message: _error.toString(), onRetry: _load)
            else if (filtered.isEmpty)
              const AppCard(child: Text('该分组下暂无消息'))
            else
              for (final item in filtered)
                _MessageCard(item: item, onTap: () => _openMessage(item)),
          ],
        ),
      ),
    );
  }

  List<MessageItem> get _filteredMessages {
    final messages = _activeGroup == 'all'
        ? _messages
        : _messages.where((m) => m.group == _activeGroup).toList();
    return [
      ...messages.where((message) => message.unread),
      ...messages.where((message) => !message.unread),
    ];
  }

  int _countOf(String groupKey) {
    if (groupKey == 'all') return _messages.length;
    return _messages.where((m) => m.group == groupKey).length;
  }

  Future<void> _load() async {
    try {
      setState(() {
        _loading = true;
        _error = null;
      });
      final messages = await backendRepository.fetchAllMessages();
      if (!mounted) return;
      _syncUnreadCount(messages);
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

  Future<void> _markAllRead() async {
    if (!_messages.any((message) => message.unread)) return;
    try {
      await backendRepository.markAllMessagesRead();
      if (!mounted) return;
      final nextMessages = [
        for (final item in _messages) item.copyWith(unread: false),
      ];
      _syncUnreadCount(nextMessages);
      setState(() => _messages = nextMessages);
    } catch (error) {
      if (!mounted) return;
      showAppSnackBar(context, '标记已读失败：$error');
    }
  }

  Future<void> _openMessage(MessageItem item) async {
    if (!await _markRead(item)) return;
    if (!mounted) return;
    final opened = await _openTarget(item);
    if (mounted && opened) await _load();
  }

  Future<bool> _markRead(MessageItem item) async {
    if (!item.unread) return true;
    try {
      await backendRepository.markMessageRead(item.id);
      if (!mounted) return false;
      final nextMessages = [
        for (final message in _messages)
          message.id == item.id ? message.copyWith(unread: false) : message,
      ];
      _syncUnreadCount(nextMessages);
      setState(() => _messages = nextMessages);
      return true;
    } catch (error) {
      if (mounted) showAppSnackBar(context, '标记已读失败：$error');
      return false;
    }
  }

  Future<bool> _openTarget(MessageItem item) async {
    final targetType = item.relatedTargetType ?? _fallbackTargetType(item);
    final targetId = item.relatedTargetId ?? item.relatedOrderId;
    if (targetType == null) {
      showAppSnackBar(context, '该消息暂无可跳转详情');
      return false;
    }
    switch (targetType) {
      case 'order':
        if (targetId == null) {
          showAppSnackBar(context, '该订单消息缺少订单信息');
          return false;
        }
        return _openOrder(targetId);
      case 'review':
        await Navigator.pushNamed(
          context,
          targetId == null ? Routes.myReviews : Routes.reviewDetail,
          arguments: targetId,
        );
        return true;
      case 'complaint':
        await Navigator.pushNamed(
          context,
          targetId == null ? Routes.complaintList : Routes.complaintDetail,
          arguments: targetId,
        );
        return true;
      case 'support':
      case 'support_session':
        await Navigator.pushNamed(
          context,
          targetId == null ? Routes.supportSessions : Routes.supportChat,
          arguments: targetId,
        );
        return true;
      default:
        showAppSnackBar(context, '该消息暂无可跳转详情');
        return false;
    }
  }

  String? _fallbackTargetType(MessageItem item) {
    if (item.relatedOrderId != null) return 'order';
    return switch (item.group) {
      'review' || 'complaint' || 'support' => item.group,
      _ => null,
    };
  }

  Future<bool> _openOrder(int orderId) async {
    try {
      final detail = await backendRepository.fetchOrderDetail('$orderId');
      if (!mounted) return false;
      await Navigator.pushNamed(
        context,
        Routes.orderDetail,
        arguments: OrderDetailArgs(
          kind: detail.kind,
          status: detail.status,
          orderId: detail.id.isEmpty ? '$orderId' : detail.id,
        ),
      );
      return true;
    } catch (error) {
      if (mounted) showAppSnackBar(context, '打开订单失败：$error');
      return false;
    }
  }

  void _syncUnreadCount(List<MessageItem> messages) {
    appState.updateProfile(
      unreadMessageCount: messages.where((message) => message.unread).length,
    );
  }
}

class _GroupTab {
  const _GroupTab({required this.key, required this.label, required this.icon});

  final String key;
  final String label;
  final IconData icon;
}

class _GroupTabsBar extends StatelessWidget {
  const _GroupTabsBar({
    required this.groups,
    required this.countOf,
    required this.active,
    required this.onTap,
  });

  final List<_GroupTab> groups;
  final int Function(String) countOf;
  final String active;
  final ValueChanged<String> onTap;

  @override
  Widget build(BuildContext context) => SingleChildScrollView(
    scrollDirection: Axis.horizontal,
    child: Row(
      children: [
        for (final g in groups)
          Padding(
            padding: const EdgeInsets.only(right: 8),
            child: ChoiceChip(
              avatar: Icon(g.icon, size: 16),
              label: Text('${g.label}（${countOf(g.key)}）'),
              selected: active == g.key,
              onSelected: (_) => onTap(g.key),
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
    backgroundColor: item.unread ? const Color(0xFFFFF7F5) : null,
    borderColor: item.unread ? const Color(0xFFFFD6CC) : null,
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 42,
          height: 42,
          decoration: BoxDecoration(
            color: AppColors.brandSoft,
            borderRadius: BorderRadius.circular(9),
            border: Border.all(color: AppColors.brandLine),
          ),
          child: Icon(_iconOf(item.group), color: AppColors.brand),
        ),
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
                  if (item.unread) ...[
                    Container(
                      margin: const EdgeInsets.only(right: 6),
                      width: 8,
                      height: 8,
                      decoration: const BoxDecoration(
                        color: Colors.red,
                        shape: BoxShape.circle,
                      ),
                    ),
                    Container(
                      margin: const EdgeInsets.only(right: 6),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 5,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.red.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        '未读',
                        style: TextStyle(color: Colors.red, fontSize: 11),
                      ),
                    ),
                  ],
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
              if (item.badge.isNotEmpty) ...[
                const SizedBox(height: 6),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 6,
                    vertical: 2,
                  ),
                  decoration: BoxDecoration(
                    color: AppColors.brandSoft,
                    borderRadius: BorderRadius.circular(4),
                  ),
                  child: Text(
                    item.badge,
                    style: const TextStyle(
                      color: AppColors.brand,
                      fontSize: 12,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ],
    ),
  );

  IconData _iconOf(String group) => switch (group) {
    'review' => Icons.rate_review_outlined,
    'support' => Icons.support_agent,
    'complaint' => Icons.report_outlined,
    'order' => Icons.receipt_long_outlined,
    _ => Icons.notifications_none,
  };
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
