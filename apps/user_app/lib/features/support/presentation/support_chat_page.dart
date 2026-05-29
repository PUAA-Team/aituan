import 'package:flutter/material.dart';

import '../../../core/widgets/app_card.dart';
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
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('加载失败：$e')));
    }
  }

  Future<void> _send() async {
    final text = _controller.text.trim();
    if (text.isEmpty) return;
    setState(() => _sending = true);
    try {
      final msg = await supportRepository.sendMessage(widget.sessionId, text);
      if (!mounted) return;
      setState(() {
        _messages = [..._messages, msg];
        _controller.clear();
        _sending = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _sending = false);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('发送失败：$e')));
    }
  }

  Future<void> _close() async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('结束咨询'),
        content: const Text('确认关闭本次会话？关闭后无法再发送消息'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('取消')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('确定')),
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
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('关闭失败：$e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    final isOpen = _session?.status == 'open';
    return Scaffold(
      appBar: AppBar(
        title: Text(_session?.storeName ?? '咨询'),
        actions: [
          if (isOpen) TextButton(onPressed: _close, child: const Text('结束', style: TextStyle(color: Colors.white))),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                if (_session != null)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    child: AppCard(
                      padding: const EdgeInsets.all(8),
                      child: Text('主题：${_session!.topic}', style: Theme.of(context).textTheme.bodySmall),
                    ),
                  ),
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                    itemCount: _messages.length,
                    itemBuilder: (_, i) => _MessageBubble(message: _messages[i]),
                  ),
                ),
                SafeArea(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      border: Border(top: BorderSide(color: Color(0x11000000))),
                    ),
                    child: Row(
                      children: [
                        Expanded(
                          child: TextField(
                            controller: _controller,
                            decoration: InputDecoration(
                              hintText: isOpen ? '输入消息…' : '会话已关闭',
                              border: OutlineInputBorder(borderRadius: BorderRadius.circular(20), borderSide: BorderSide.none),
                              filled: true,
                              fillColor: Colors.grey.shade100,
                              contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
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
    final bgColor = isUser ? Colors.blue.shade50 : Colors.grey.shade200;
    return Align(
      alignment: align,
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 4),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
        constraints: const BoxConstraints(maxWidth: 280),
        decoration: BoxDecoration(color: bgColor, borderRadius: BorderRadius.circular(10)),
        child: Text(message.content),
      ),
    );
  }
}
