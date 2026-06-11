import 'package:flutter/material.dart';

import '../../../core/constants/app_colors.dart';
import '../../../core/constants/route_constants.dart';
import '../../../core/widgets/app_card.dart';
import '../../../core/widgets/app_toast.dart';
import '../data/assistant_repository.dart';

class AssistantPage extends StatefulWidget {
  const AssistantPage({super.key});

  @override
  State<AssistantPage> createState() => _AssistantPageState();
}

class _AssistantPageState extends State<AssistantPage> {
  final _controller = TextEditingController();
  final _scrollController = ScrollController();
  List<_ChatEntry> _entries = [
    _ChatEntry.assistant(
      '我是爱团助手，可以帮你查询订单、优惠券、评价投诉入口，并在需要时转到平台客服。',
      actions: const [
        AssistantAction(label: '查订单', message: '帮我看看最近订单', route: null),
        AssistantAction(label: '优惠券', message: '我有哪些优惠券', route: null),
        AssistantAction(
          label: '找客服',
          message: null,
          route: Routes.supportSessions,
        ),
      ],
    ),
  ];
  String? _conversationId;
  bool _sending = false;
  bool _loadingHistory = true;

  @override
  void initState() {
    super.initState();
    _loadHistory();
  }

  @override
  void dispose() {
    _controller.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.page,
      appBar: AppBar(
        title: const Text('爱团助手'),
        actions: [
          IconButton(
            tooltip: '平台客服',
            onPressed: () =>
                Navigator.pushNamed(context, Routes.supportSessions),
            icon: const Icon(Icons.support_agent),
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: _loadingHistory
                ? const Center(child: CircularProgressIndicator())
                : ListView.builder(
                    controller: _scrollController,
                    padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                    itemCount: _entries.length,
                    itemBuilder: (context, index) => _MessageBubble(
                      entry: _entries[index],
                      onAction: _handleAction,
                      onRoute: _openRoute,
                    ),
                  ),
          ),
          SafeArea(
            top: false,
            child: Container(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
              decoration: const BoxDecoration(
                color: AppColors.card,
                border: Border(top: BorderSide(color: AppColors.line)),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _controller,
                      enabled: !_sending && !_loadingHistory,
                      minLines: 1,
                      maxLines: 4,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => _send(),
                      decoration: InputDecoration(
                        hintText: _sending ? '助手正在整理...' : '输入订单、优惠券、投诉等问题',
                        filled: true,
                        fillColor: AppColors.soft,
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: 14,
                          vertical: 10,
                        ),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: BorderSide.none,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filled(
                    tooltip: '发送',
                    onPressed: (_sending || _loadingHistory) ? null : _send,
                    icon: _sending
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.send),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _loadHistory() async {
    try {
      final history = await assistantRepository.fetchCurrentConversation();
      if (!mounted) return;
      setState(() {
        _conversationId = history.conversationId;
        if (history.messages.isNotEmpty) {
          _entries = history.messages
              .map(
                (message) => _ChatEntry(
                  role: message.role,
                  content: message.content,
                  cards: message.cards,
                  actions: message.quickActions,
                  steps: message.steps,
                  modelUsed: message.modelUsed,
                ),
              )
              .toList();
        }
        _loadingHistory = false;
      });
      _scrollToBottom();
    } catch (_) {
      if (!mounted) return;
      setState(() => _loadingHistory = false);
    }
  }

  Future<void> _send([String? preset]) async {
    final text = (preset ?? _controller.text).trim();
    if (text.isEmpty || _sending) return;
    if (preset == null) _controller.clear();
    setState(() {
      _sending = true;
      _entries.add(_ChatEntry.user(text));
    });
    _scrollToBottom();
    try {
      final response = await assistantRepository.sendMessage(
        content: text,
        conversationId: _conversationId,
      );
      _conversationId = response.conversationId;
      if (!mounted) return;
      setState(() {
        _entries.add(
          _ChatEntry.assistant(
            response.reply,
            cards: response.cards,
            actions: response.quickActions,
            steps: response.steps,
            modelUsed: response.modelUsed,
          ),
        );
        _sending = false;
      });
      _scrollToBottom();
    } catch (error) {
      if (!mounted) return;
      setState(() => _sending = false);
      showAppSnackBar(context, '助手回复失败：$error');
    }
  }

  void _handleAction(AssistantAction action) {
    if (action.message != null && action.message!.trim().isNotEmpty) {
      _send(action.message);
      return;
    }
    _openRoute(action.route);
  }

  void _openRoute(String? route) {
    final target = _normalizeRoute(route);
    if (target == null) return;
    Navigator.pushNamed(context, target);
  }

  String? _normalizeRoute(String? route) {
    return switch (route) {
      '/orders' => Routes.orders,
      '/coupon/list' => Routes.coupons,
      '/coupon/claim' => Routes.couponClaim,
      '/complaint/submit' => Routes.complaintSubmit,
      '/complaint/list' => Routes.complaintList,
      '/review/my' => Routes.myReviews,
      '/review/detail' => Routes.myReviews,
      '/support/sessions' => Routes.supportSessions,
      '/order/detail' => Routes.orders,
      '/stores/detail' => Routes.search,
      '/items/detail' => Routes.search,
      '/search' => Routes.search,
      '/profile' => Routes.profile,
      '/favorites' => Routes.favorite,
      '/messages' => Routes.message,
      String value when value.startsWith('/') => value,
      _ => null,
    };
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) return;
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 240),
        curve: Curves.easeOut,
      );
    });
  }
}

class _ChatEntry {
  const _ChatEntry({
    required this.role,
    required this.content,
    this.cards = const [],
    this.actions = const [],
    this.steps = const [],
    this.modelUsed = false,
  });

  factory _ChatEntry.user(String content) =>
      _ChatEntry(role: 'user', content: content);

  factory _ChatEntry.assistant(
    String content, {
    List<AssistantCard> cards = const [],
    List<AssistantAction> actions = const [],
    List<AssistantStep> steps = const [],
    bool modelUsed = false,
  }) => _ChatEntry(
    role: 'assistant',
    content: content,
    cards: cards,
    actions: actions,
    steps: steps,
    modelUsed: modelUsed,
  );

  final String role;
  final String content;
  final List<AssistantCard> cards;
  final List<AssistantAction> actions;
  final List<AssistantStep> steps;
  final bool modelUsed;

  bool get isUser => role == 'user';
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({
    required this.entry,
    required this.onAction,
    required this.onRoute,
  });

  final _ChatEntry entry;
  final ValueChanged<AssistantAction> onAction;
  final ValueChanged<String?> onRoute;

  @override
  Widget build(BuildContext context) {
    final align = entry.isUser
        ? CrossAxisAlignment.end
        : CrossAxisAlignment.start;
    final bubbleColor = entry.isUser ? AppColors.brand : AppColors.card;
    final textColor = entry.isUser ? Colors.white : AppColors.textMain;
    return Column(
      crossAxisAlignment: align,
      children: [
        Container(
          constraints: const BoxConstraints(maxWidth: 340),
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            color: bubbleColor,
            borderRadius: BorderRadius.circular(8),
            border: entry.isUser ? null : Border.all(color: AppColors.line),
          ),
          child: Text(
            entry.content,
            style: TextStyle(color: textColor, height: 1.35),
          ),
        ),
        if (!entry.isUser && entry.steps.isNotEmpty)
          _AssistantStepsView(steps: entry.steps, modelUsed: entry.modelUsed),
        if (entry.cards.isNotEmpty)
          ...entry.cards.map(
            (card) => _AssistantCardView(
              card: card,
              onTap: () => onRoute(card.route),
            ),
          ),
        if (entry.actions.isNotEmpty)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: entry.actions
                  .map(
                    (action) => ActionChip(
                      label: Text(action.label),
                      avatar: const Icon(Icons.auto_awesome, size: 16),
                      onPressed: () => onAction(action),
                    ),
                  )
                  .toList(),
            ),
          ),
      ],
    );
  }
}

class _AssistantStepsView extends StatelessWidget {
  const _AssistantStepsView({required this.steps, required this.modelUsed});

  final List<AssistantStep> steps;
  final bool modelUsed;

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 360),
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: AppColors.soft,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppColors.line),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(
                  Icons.hub_outlined,
                  size: 15,
                  color: AppColors.textSub,
                ),
                const SizedBox(width: 5),
                Text(
                  modelUsed ? 'AI 已调用业务信息' : '本地助手已调用业务信息',
                  style: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: AppColors.textSub,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 6),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: steps
                  .map(
                    (step) => Chip(
                      visualDensity: VisualDensity.compact,
                      materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      avatar: const Icon(Icons.check_circle, size: 14),
                      label: Text(step.title),
                    ),
                  )
                  .toList(),
            ),
          ],
        ),
      ),
    );
  }
}

class _AssistantCardView extends StatelessWidget {
  const _AssistantCardView({required this.card, required this.onTap});

  final AssistantCard card;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 360),
      child: AppCard(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(12),
        backgroundColor: AppColors.brandSoft,
        borderColor: AppColors.brandLine,
        onTap: onTap,
        child: Row(
          children: [
            Icon(_iconFor(card.type), color: AppColors.brandStrong),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    card.title,
                    style: const TextStyle(
                      fontWeight: FontWeight.w700,
                      color: AppColors.textMain,
                    ),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    card.content,
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.textSub,
                    ),
                  ),
                ],
              ),
            ),
            if (card.actionLabel != null) ...[
              const SizedBox(width: 8),
              Text(
                card.actionLabel!,
                style: const TextStyle(
                  color: AppColors.brandStrong,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  IconData _iconFor(String type) {
    return switch (type) {
      'order' => Icons.receipt_long_outlined,
      'coupon' => Icons.confirmation_num_outlined,
      'complaint' => Icons.report_outlined,
      'review' => Icons.rate_review_outlined,
      'support' => Icons.support_agent,
      'store' => Icons.storefront_outlined,
      'item' => Icons.shopping_bag_outlined,
      _ => Icons.auto_awesome,
    };
  }
}
