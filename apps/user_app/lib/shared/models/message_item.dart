class MessageItem {
  const MessageItem({
    required this.title,
    required this.content,
    required this.time,
  });

  final String title;
  final String content;
  final String time;

  factory MessageItem.fromApi(Map<String, dynamic> json) => MessageItem(
    title: _text(json['title']),
    content: _text(json['content']),
    time: _time(json['createdAt']),
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
