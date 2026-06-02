class MessageItem {
  const MessageItem({
    this.id = '',
    this.type = '',
    required this.title,
    required this.content,
    required this.time,
    this.badgeText = '',
    this.unread = false,
    this.relatedOrderId,
    this.relatedTargetType,
    this.relatedTargetId,
  });

  final String id;
  final String type;
  final String title;
  final String content;
  final String time;
  final String badgeText;
  final bool unread;
  final String? relatedOrderId;
  final String? relatedTargetType;
  final String? relatedTargetId;

  factory MessageItem.fromApi(Map<String, dynamic> json) => MessageItem(
    id: _text(json['id']),
    type: _text(json['type']),
    title: _text(json['title']),
    content: _text(json['content']),
    time: _time(json['createdAt']),
    badgeText: _text(json['badgeText']),
    unread: json['unread'] == true,
    relatedOrderId: _nullableText(json['relatedOrderId']),
    relatedTargetType: _nullableText(json['relatedTargetType']),
    relatedTargetId: _nullableText(json['relatedTargetId']),
  );

  MessageItem copyWith({bool? unread}) => MessageItem(
    id: id,
    type: type,
    title: title,
    content: content,
    time: time,
    badgeText: badgeText,
    unread: unread ?? this.unread,
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
