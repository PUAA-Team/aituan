import '../enums/business_type.dart';

class ModuleEntry {
  const ModuleEntry({
    required this.code,
    required this.title,
    required this.type,
  });

  final String code;
  final String title;
  final BusinessType type;
}
