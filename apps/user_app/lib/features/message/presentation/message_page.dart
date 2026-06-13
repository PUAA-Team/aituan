import 'package:flutter/material.dart';

import '../../../app/route_args.dart';
import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/message_item.dart';
import '../../home/data/backend_app_repository.dart';
import '../../support/data/support_repository.dart';

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
    final filtered = _activeGroup == 'all'
        ? _messages
        : _messages.where((m) => m.group == _activeGroup).toList();
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
                  onPressed:
                      _messages.any(
                        (message) =>
                            message.unread && !message.isSupportSession,
                      )
                      ? _markAllRead
                      : null,
                  child: const Text('通知已读'),
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
      final results = await Future.wait<Object>([
        backendRepository.fetchMessages(),
        supportRepository.fetchSessions(),
      ]);
      final stationMessages = results[0] as List<MessageItem>;
      final supportSessions = results[1] as List<SupportSession>;
      if (!mounted) return;
      setState(() {
        _messages = _mergeMessages(stationMessages, supportSessions);
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
    try {
      await backendRepository.markAllMessagesRead();
      if (!mounted) return;
      setState(() {
        _messages = [
          for (final item in _messages)
            item.isSupportSession ? item : item.copyWith(unread: false),
        ];
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _error = error);
    }
  }

  Future<void> _openMessage(MessageItem item) async {
    final targetType =
        item.relatedTargetType ??
        (item.relatedOrderId == null ? null : 'order');
    final targetId = item.relatedTargetId ?? item.relatedOrderId;
    if (targetType == 'support_session' && targetId != null) {
      await Navigator.pushNamed(
        context,
        Routes.supportChat,
        arguments: targetId,
      );
      if (mounted) await _load();
      return;
    }
    if (item.unread) {
      try {
        await backendRepository.markMessageRead(item.id);
        if (mounted) {
          setState(() {
            _messages = [
              for (final message in _messages)
                message.id == item.id
                    ? message.copyWith(unread: false)
                    : message,
            ];
          });
        }
      } catch (_) {}
    }
    if (!mounted) return;
    if (targetType == 'order' && targetId != null) {
      await Navigator.pushNamed(
        context,
        Routes.orderDetail,
        arguments: OrderDetailArgs(
          kind: OrderKind.takeaway,
          status: OrderStatus.pending,
          orderId: '$targetId',
        ),
      );
      if (mounted) await _load();
    }
  }

  List<MessageItem> _mergeMessages(
    List<MessageItem> stationMessages,
    List<SupportSession> supportSessions,
  ) {
    final supportTargetIds = supportSessions.map((s) => s.id).toSet();
    final stationOnly = stationMessages.where((message) {
      return !(message.relatedTargetType == 'support_session' &&
          message.relatedTargetId != null &&
          supportTargetIds.contains(message.relatedTargetId));
    });
    final merged = <MessageItem>[
      ...stationOnly,
      for (final session in supportSessions) _supportSessionItem(session),
    ];
    merged.sort((a, b) => _sortTime(b).compareTo(_sortTime(a)));
    return merged;
  }

  MessageItem _supportSessionItem(SupportSession session) {
    final lastMessage = session.lastMessage?.trim();
    final content = lastMessage == null || lastMessage.isEmpty
        ? session.topic
        : lastMessage;
    final title = session.isPlatform
        ? '平台客服'
        : (session.storeName.trim().isEmpty ? '商家客服' : session.storeName);
    final source = session.isPlatform ? '平台客服' : '商家客服';
    final status = session.status == 'open' ? '进行中' : '已关闭';
    final badge = session.unreadCount > 0
        ? '$source · $status · 未读 ${session.unreadCount}'
        : '$source · $status';
    final createdAt = (session.lastMessageAt?.trim().isNotEmpty ?? false)
        ? session.lastMessageAt!.trim()
        : session.createdAt;
    return MessageItem(
      id: session.id,
      type: 'support',
      title: title,
      content: content,
      badge: badge,
      time: _displayTime(createdAt),
      unread: session.unreadCount > 0,
      createdAt: createdAt,
      relatedOrderId: session.relatedOrderId,
      relatedTargetType: 'support_session',
      relatedTargetId: session.id,
    );
  }

  DateTime _sortTime(MessageItem item) =>
      DateTime.tryParse(item.createdAt)?.toLocal() ??
      DateTime.fromMillisecondsSinceEpoch(0);

  String _displayTime(String value) {
    final time = DateTime.tryParse(value)?.toLocal();
    if (time == null) return '';
    final hour = time.hour.toString().padLeft(2, '0');
    final minute = time.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
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
                  if (item.unread)
                    Container(
                      margin: const EdgeInsets.only(right: 6),
                      width: 8,
                      height: 8,
                      decoration: const BoxDecoration(
                        color: Colors.red,
                        shape: BoxShape.circle,
                      ),
                    ),
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
