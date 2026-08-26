import '../../../shared/enums/business_type.dart';
import 'mock_item_factory.dart';

final groupItems = [
  mockItem(
    's1',
    '江南小馆双人套餐',
    '周末可用 · 未使用可退',
    BusinessType.groupBuy,
    '双人餐',
    98,
    138,
    ['团购', '到店核销'],
    'm3',
  ),
  mockItem(
    's5',
    '江南小馆四人聚餐',
    '家庭聚会 · 多菜品组合',
    BusinessType.groupBuy,
    '多人餐',
    188,
    238,
    ['聚餐'],
    'm3',
  ),
  mockItem(
    's15',
    '江南招牌三人餐',
    '招牌热菜+主食+饮品',
    BusinessType.groupBuy,
    '多人餐',
    158,
    218,
    ['热卖'],
    'm3',
  ),
  mockItem(
    's16',
    '工作日单人餐',
    '午晚餐可用 · 快速核销',
    BusinessType.groupBuy,
    '单人餐',
    39.9,
    58,
    ['单人'],
    'm3',
  ),
];

final movieItems = [
  mockItem('s2', '光影夜场电影票', '今日可用 · 支持退', BusinessType.movie, '电影票', 45, 68, [
    '电影演出',
  ], 'm4'),
  mockItem(
    's6',
    '周末脱口秀单人票',
    '电子票入场 · 座位随机',
    BusinessType.movie,
    '演出票',
    88,
    128,
    ['演出'],
    'm4',
  ),
  mockItem('s17', '双人电影套票', '双人观影 · 含小食券', BusinessType.movie, '电影票', 86, 128, [
    '双人',
  ], 'm4'),
];

final entertainmentItems = [
  mockItem(
    's11',
    '星盒密室双人票',
    '剧情沉浸 · 需预约',
    BusinessType.entertainment,
    '密室',
    156,
    198,
    ['休闲娱乐'],
    'm8',
  ),
  mockItem(
    's12',
    '电玩小时畅玩券',
    '工作日通用 · 到店核销',
    BusinessType.entertainment,
    '电玩',
    59,
    89,
    ['电玩'],
    'm8',
  ),
];
