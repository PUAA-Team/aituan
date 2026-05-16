import '../../../shared/enums/business_type.dart';
import '../../../shared/models/message_item.dart';
import '../../../shared/models/order_model.dart';

final orders = [
  _order(
    'o1',
    '塔斯汀中国汉堡外卖',
    '塔斯汀中国汉堡',
    OrderKind.takeaway,
    OrderStatus.pending,
    BusinessType.takeaway,
    39.7,
    '商家已接单，预计 35 分钟送达',
  ),
  _order(
    'o2',
    '江南小馆双人套餐',
    '江南小馆',
    OrderKind.service,
    OrderStatus.unused,
    BusinessType.groupBuy,
    98,
    '到店出示券码即可核销',
  ),
  _order(
    'o3',
    '雅境洗脚按摩 60 分钟',
    '雅境洗脚按摩',
    OrderKind.service,
    OrderStatus.used,
    BusinessType.massage,
    128,
    '订单已核销，可发布评价',
  ),
];

const messages = [
  MessageItem(
    title: '订单配送中',
    content: '塔斯汀中国汉堡正在为您配送，预计 18:45 送达。',
    time: '18:20',
  ),
  MessageItem(title: '券码待使用', content: '江南小馆双人套餐已支付，请到店出示券码。', time: '17:42'),
  MessageItem(title: '爱团提醒', content: '今晚附近团购热度上升，火锅和电影票更受欢迎。', time: '16:10'),
];

OrderModel _order(
  String id,
  String title,
  String storeName,
  OrderKind kind,
  OrderStatus status,
  BusinessType businessType,
  double amount,
  String desc,
) => OrderModel(
  id: id,
  title: title,
  storeName: storeName,
  kind: kind,
  status: status,
  businessType: businessType,
  amount: amount,
  desc: desc,
);
