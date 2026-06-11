import 'package:flutter/material.dart';

import '../../../core/constants/route_constants.dart';
import '../../complaint/presentation/complaint_submit_page.dart';
import '../data/support_repository.dart';

class SupportChatPage extends StatefulWidget {
  const SupportChatPage({super.key, required this.sessionId});

  final int sessionId;

  @override
  State<SupportChatPage> createState() => _SupportChatPageState();
}

class _SupportChatPageState extends State<SupportChatPage> {
  SupportSession? _session;
  List<SupportMessage> _messages = [];
  final _controller = TextEditingController();
  bool _loading = true;
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    try {
      final (s, m) = await supportRepository.fetchDetail(widget.sessionId);
      if (!mounted) return;
      setState(() {
        _session = s;
        _messages = m;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('加载失败：$e')));
    }
  }

  Future<void> _send() async {
    if (_sending) return;
    final text = _controller.text.trim();
    if (text.isEmpty) return;
    final optimisticUserMessage = SupportMessage(
      id: -DateTime.now().microsecondsSinceEpoch,
      senderType: 'user',
      content: text,
      createdAt: '',
    );
    final shouldShowAiThinking =
        (_session?.isPlatform ?? false) && (_session?.isAiMode ?? false);
    final thinkingMessage = SupportMessage(
      id: optimisticUserMessage.id - 1,
      senderType: 'platform',
      content: '平台 AI 正在整理订单、投诉和客服信息…',
      createdAt: '',
    );
    _controller.clear();
    setState(() {
      _sending = true;
      _messages = [
        ..._messages,
        optimisticUserMessage,
        if (shouldShowAiThinking) thinkingMessage,
      ];
    });
    try {
      final msg = await supportRepository.sendMessage(widget.sessionId, text);
      if (!mounted) return;
      final (session, messages) = await supportRepository.fetchDetail(
        widget.sessionId,
      );
      if (!mounted) return;
      setState(() {
        _session = session;
        _messages = messages.isEmpty ? [msg] : messages;
        _sending = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _sending = false);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('发送失败：$e')));
    }
  }

  Future<void> _handoff() async {
    try {
      await supportRepository.handoffToHuman(widget.sessionId);
      if (!mounted) return;
      await _load();
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(const SnackBar(content: Text('已转接平台人工客服')));
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('转人工失败：$e')));
    }
  }

  void _openComplaint() {
    final session = _session;
    Navigator.pushNamed(
      context,
      Routes.complaintSubmit,
      arguments: ComplaintSubmitArgs(
        orderId: session?.relatedOrderId,
        orderTitle: session?.relatedOrderNo ?? session?.storeName,
      ),
    );
  }

  Future<void> _close() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('结束咨询'),
        content: const Text('确认关闭本次会话？关闭后无法再发送消息'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('取消'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('确定'),
          ),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await supportRepository.closeSession(widget.sessionId, reason: '用户主动结束');
      if (!mounted) return;
      _load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('关闭失败：$e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final isOpen = _session?.status == 'open';
    final canHandoff =
        isOpen &&
        (_session?.isPlatform ?? false) &&
        (_session?.isAiMode ?? false);
    final actionButtonStyle = TextButton.styleFrom(
      foregroundColor: Theme.of(context).colorScheme.primary,
    );
    return Scaffold(
      appBar: AppBar(
        title: Text(_session?.storeName ?? '咨询'),
        actions: [
          IconButton(
            tooltip: '投诉',
            onPressed: _openComplaint,
            icon: const Icon(Icons.report_outlined),
          ),
          if (canHandoff)
            TextButton(
              style: actionButtonStyle,
              onPressed: _handoff,
              child: const Text('转人工'),
            ),
          if (isOpen)
            TextButton(
              style: actionButtonStyle,
              onPressed: _close,
              child: const Text('结束'),
            ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 4,
                    ),
                    itemCount: _messages.length,
                    itemBuilder: (_, i) =>
                        _MessageBubble(message: _messages[i]),
                  ),
                ),
                SafeArea(
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 6,
                    ),
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      border: Border(top: BorderSide(color: Color(0x11000000))),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _controller,
                            textInputAction: TextInputAction.send,
                            onSubmitted: (_) => _send(),
                            decoration: InputDecoration(
                              hintText: isOpen ? '输入消息…' : '会话已关闭',
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(20),
                                borderSide: BorderSide.none,
                              ),
                              filled: true,
                              fillColor: Colors.grey.shade100,
                              contentPadding: const EdgeInsets.symmetric(
                                horizontal: 14,
                                vertical: 8,
                              ),
                            ),
                            enabled: isOpen && !_sending,
                          ),
                        ),
                        const SizedBox(width: 8),
                        ElevatedButton(
                          onPressed: (isOpen && !_sending) ? _send : null,
                          child: Text(_sending ? '发送中…' : '发送'),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
    );
  }
}

class _MessageBubble extends StatelessWidget {
  const _MessageBubble({required this.message});
  final SupportMessage message;

  @override
  Widget build(BuildContext context) {
    final isUser = message.isUser;
    final align = isUser ? Alignment.centerRight : Alignment.centerLeft;
    final bgColor = isUser
        ? Colors.blue.shade50
        : message.isPlatform
        ? Colors.orange.shade50
        : Colors.grey.shade200;
    final label = isUser
        ? '我'
        : message.isPlatform
        ? '平台客服'
        : message.isMerchant
        ? '商家客服'
        : '系统消息';
    return Align(
      alignment: align,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        constraints: const BoxConstraints(maxWidth: 280),
        decoration: BoxDecoration(
          color: bgColor,
          borderRadius: BorderRadius.circular(10),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: TextStyle(
                fontSize: 11,
                color: isUser ? Colors.blueGrey : Colors.grey.shade700,
                fontWeight: FontWeight.w600,
              ),
            ),
            const SizedBox(height: 3),
            Text(message.content),
          ],
        ),
      ),
    );
  }
}
