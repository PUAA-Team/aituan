class MessageItem {
  const MessageItem({
    required this.id,
    required this.type,
    required this.title,
    required this.content,
    required this.badge,
    required this.time,
    required this.unread,
    this.createdAt = '',
    this.relatedOrderId,
    this.relatedTargetType,
    this.relatedTargetId,
  });

  final int id;
  final String type;
  final String title;
  final String content;
  final String badge;
  final String time;
  final bool unread;
  final String createdAt;
  final int? relatedOrderId;
  final String? relatedTargetType;
  final int? relatedTargetId;

  bool get isSupportSession => relatedTargetType == 'support_session';

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
    createdAt: _text(json['createdAt']),
    relatedOrderId: (json['relatedOrderId'] as num?)?.toInt(),
    relatedTargetType: _nullableText(json['relatedTargetType']),
    relatedTargetId: (json['relatedTargetId'] as num?)?.toInt(),
  );

  MessageItem copyWith({bool? unread}) => MessageItem(
    id: id,
    type: type,
    title: title,
    content: content,
    badge: badge,
    time: time,
    unread: unread ?? this.unread,
    createdAt: createdAt,
    relatedOrderId: relatedOrderId,
    relatedTargetType: relatedTargetType,
    relatedTargetId: relatedTargetId,
  );
}

String _text(dynamic value) => value?.toString().trim() ?? '';

String? _nullableText(dynamic value) {
  final text = value?.toString().trim();
  return text == null || text.isEmpty ? null : text;
}

String _time(dynamic value) {
  final time = DateTime.tryParse(value?.toString() ?? '')?.toLocal();
  if (time == null) return '';
  final hour = time.hour.toString().padLeft(2, '0');
  final minute = time.minute.toString().padLeft(2, '0');
  return '$hour:$minute';
}
