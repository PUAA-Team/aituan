import '../../../shared/enums/business_type.dart';
import 'mock_item_factory.dart';

final massageItems = [
  mockItem(
    's3',
    '雅境洗脚按摩 60 分钟',
    '需预约 · 到店核销',
    BusinessType.massage,
    '足疗',
    128,
    168,
    ['洗脚', '可预约'],
    'm5',
  ),
  mockItem(
    's7',
    '肩颈放松 45 分钟',
    '缓解疲劳 · 到店核销',
    BusinessType.massage,
    '按摩',
    96,
    129,
    ['放松'],
    'm5',
  ),
  mockItem(
    's18',
    '足疗养生 90 分钟',
    '热水泡脚+足底按摩',
    BusinessType.massage,
    '足疗',
    168,
    228,
    ['舒缓'],
    'm5',
  ),
  mockItem(
    's19',
    '采耳放松套餐',
    '到店预约 · 轻松解压',
    BusinessType.massage,
    '采耳',
    78,
    108,
    ['新品'],
    'm5',
  ),
];

final ticketItems = [
  mockItem('s4', '城市观景门票', '扫码入园 · 今日可订', BusinessType.ticket, '门票', 68, 88, [
    '景点门票',
  ], 'm6'),
  mockItem('s8', '亲子乐园联票', '一票多玩 · 周末可用', BusinessType.ticket, '联票', 118, 158, [
    '亲子',
  ], 'm6'),
];

final hotelItems = [
  mockItem(
    's9',
    '云栖酒店舒适大床房',
    '可取消 · 含双早',
    BusinessType.hotel,
    '大床房',
    268,
    338,
    ['酒店'],
    'm7',
  ),
  mockItem(
    's10',
    '云栖酒店商务双床房',
    '商旅优选 · 到店入住',
    BusinessType.hotel,
    '双床房',
    298,
    388,
    ['商务'],
    'm7',
  ),
  mockItem(
    's20',
    '云栖景观大床房',
    '高楼层景观 · 含早餐',
    BusinessType.hotel,
    '大床房',
    328,
    428,
    ['景观'],
    'm7',
  ),
  mockItem(
    's21',
    '午休钟点房 4 小时',
    '灵活入住 · 到店办理',
    BusinessType.hotel,
    '钟点房',
    128,
    168,
    ['短住'],
    'm7',
  ),
];

final beautyItems = [
  mockItem(
    's13',
    '轻颜皮肤清洁护理',
    '专业护理 · 到店预约',
    BusinessType.beauty,
    '护理',
    168,
    238,
    ['丽人医美'],
    'm9',
  ),
  mockItem(
    's14',
    '简约美甲单色款',
    '多色可选 · 到店核销',
    BusinessType.beauty,
    '美甲',
    89,
    128,
    ['美甲'],
    'm9',
  ),
];
