import '../../../shared/enums/business_type.dart';
import 'mock_item_factory.dart';

final tustinItems = [
  mockItem(
    't1',
    '招牌中国汉堡',
    '现做热卖 · 约 35 分钟送达',
    BusinessType.takeaway,
    '汉堡',
    18.8,
    null,
    ['外卖', '热销'],
    'm1',
  ),
  mockItem('t2', '单人随心配', '汉堡+小食+饮品', BusinessType.takeaway, '套餐', 24.9, 32, [
    '套餐',
  ], 'm1'),
  mockItem(
    't5',
    '香辣鸡腿堡',
    '微辣多汁 · 搭配可乐更划算',
    BusinessType.takeaway,
    '汉堡',
    19.9,
    26,
    ['新品'],
    'm1',
  ),
  mockItem(
    't6',
    '脆皮鸡米花',
    '外酥里嫩 · 追剧小食',
    BusinessType.takeaway,
    '小食',
    12.8,
    null,
    ['小食'],
    'm1',
  ),
  mockItem(
    't7',
    '双人分享套餐',
    '双堡+双饮+小食拼盘',
    BusinessType.takeaway,
    '套餐',
    49.9,
    66,
    ['双人'],
    'm1',
  ),
];

final riceItems = [
  mockItem(
    't3',
    '鸡排饭工作餐',
    '米饭套餐 · 午晚餐优选',
    BusinessType.takeaway,
    '简餐',
    21.9,
    null,
    ['快送'],
    'm2',
  ),
  mockItem('t4', '照烧鸡腿饭', '热卖便当 · 配例汤', BusinessType.takeaway, '简餐', 23.9, 29, [
    '午餐',
  ], 'm2'),
  mockItem(
    't8',
    '酸汤肥牛饭',
    '酸爽开胃 · 热汤拌饭',
    BusinessType.takeaway,
    '简餐',
    28.8,
    35,
    ['新品'],
    'm2',
  ),
  mockItem(
    't9',
    '鸡排小食拼盘',
    '鸡排块+薯角+饮品',
    BusinessType.takeaway,
    '小食',
    18.8,
    null,
    ['加购'],
    'm2',
  ),
];

final takeawayItems = [...tustinItems, ...riceItems];
