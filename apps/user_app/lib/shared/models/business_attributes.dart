import '../enums/business_type.dart';

/// 解析 business_attributes 串（格式 `key:value;key:value`）为可读的键值对列表。
/// 用法约定见 stage5-d 文档：所有非外卖业务复用同一字段、按 [BusinessType] 渲染。
class BusinessAttributes {
  const BusinessAttributes(this.pairs);

  final List<MapEntry<String, String>> pairs;

  bool get isEmpty => pairs.isEmpty;

  /// 解析存储格式：`key:value;key:value`，支持中文 `：`，忽略空段。
  factory BusinessAttributes.parse(String raw) {
    if (raw.isEmpty) return const BusinessAttributes([]);
    final entries = <MapEntry<String, String>>[];
    for (final seg in raw.split(RegExp(r'[;；\n]'))) {
      final trimmed = seg.trim();
      if (trimmed.isEmpty) continue;
      final colon = trimmed.indexOf(RegExp(r'[:：]'));
      if (colon <= 0 || colon == trimmed.length - 1) continue;
      final key = trimmed.substring(0, colon).trim();
      final value = trimmed.substring(colon + 1).trim();
      if (key.isEmpty || value.isEmpty) continue;
      entries.add(MapEntry(key, value));
    }
    return BusinessAttributes(entries);
  }
}

/// 业务类型差异化展示的标题文案
String businessAttributesSectionTitle(BusinessType type) {
  return switch (type) {
    BusinessType.groupBuy => '套餐说明',
    BusinessType.hotel => '房型与入住',
    BusinessType.entertainment => '项目与场地',
    BusinessType.movie => '场次与入场',
    BusinessType.beauty => '项目与资质',
    BusinessType.ticket => '票种与入园',
    BusinessType.massage => '项目与到店',
    BusinessType.takeaway => '商品说明',
  };
}

/// 默认的兜底属性串（演示时若 seed 未填则退化到通用文案，不阻断展示）
List<MapEntry<String, String>> fallbackAttributes(BusinessType type) {
  return switch (type) {
    BusinessType.groupBuy => const [
      MapEntry('套餐内容', '商家公示菜单为准'),
      MapEntry('有效期', '购买后 90 天内可用'),
      MapEntry('适用门店', '本店通用'),
    ],
    BusinessType.hotel => const [
      MapEntry('房型', '舒适大床房'),
      MapEntry('入住时间', '14:00 起'),
      MapEntry('离店时间', '次日 12:00'),
    ],
    BusinessType.entertainment => const [
      MapEntry('人数', '2-4 人'),
      MapEntry('时长', '60-90 分钟'),
      MapEntry('场地说明', '到店领位'),
    ],
    BusinessType.movie => const [
      MapEntry('票种', '2D/3D 普通厅通兑'),
      MapEntry('使用规则', '特殊厅补差'),
      MapEntry('入场规则', '凭券码到柜台取票'),
    ],
    BusinessType.beauty => const [
      MapEntry('服务流程', '清洁 - 护理 - 舒缓'),
      MapEntry('适用人群', '一般肌肤'),
      MapEntry('资质说明', '持证技师上岗'),
    ],
    BusinessType.ticket => const [
      MapEntry('票种', '成人电子票'),
      MapEntry('入园日期', '购买后 30 天内任选一天'),
      MapEntry('开放时间', '09:00-18:00'),
    ],
    BusinessType.massage => const [
      MapEntry('项目时长', '60 分钟'),
      MapEntry('到店/上门', '到店'),
      MapEntry('注意事项', '到店前 30 分钟电话预约'),
    ],
    BusinessType.takeaway => const [],
  };
}
