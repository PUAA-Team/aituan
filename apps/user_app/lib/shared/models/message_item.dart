class MessageItem {
  const MessageItem({
    required this.id,
    required this.type,
    required this.title,
    required this.content,
    required this.badge,
    required this.time,
    required this.unread,
    this.relatedOrderId,
  });

  final int id;
  final String type;
  final String title;
  final String content;
  final String badge;
  final String time;
  final bool unread;
  final int? relatedOrderId;

  /// 把后端 type 归到用户端展示分组：评价 / 咨询 / 投诉 / 订单 / 系统。
  String get group => switch (type) {
    'review' => 'review',
    'support' => 'support',
    'complaint' => 'complaint',
    'order' => 'order',
    _ => 'system',
  };

  factory MessageItem.fromApi(Map<String, dynamic> json) => MessageItem(
    id: (json['id'] as num?)?.toInt() ?? 0,
    type: _text(json['type']),
    title: _text(json['title']),
    content: _text(json['content']),
    badge: _text(json['badgeText']),
    time: _time(json['createdAt']),
    unread: (json['unread'] as bool?) ?? false,
    relatedOrderId: (json['relatedOrderId'] as num?)?.toInt(),
  );
}

String _text(dynamic value) => value?.toString().trim() ?? '';

String _time(dynamic value) {
  final time = DateTime.tryParse(value?.toString() ?? '')?.toLocal();
  if (time == null) return '';
  final hour = time.hour.toString().padLeft(2, '0');
  final minute = time.minute.toString().padLeft(2, '0');
  return '$hour:$minute';
}
