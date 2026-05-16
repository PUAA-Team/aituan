enum BusinessType {
  takeaway,
  groupBuy,
  hotel,
  entertainment,
  movie,
  beauty,
  ticket,
  massage,
}

extension BusinessTypeText on BusinessType {
  String get label {
    return switch (this) {
      BusinessType.takeaway => '外卖',
      BusinessType.groupBuy => '团购',
      BusinessType.hotel => '酒店',
      BusinessType.entertainment => '休闲娱乐',
      BusinessType.movie => '电影演出',
      BusinessType.beauty => '丽人医美',
      BusinessType.ticket => '景点门票',
      BusinessType.massage => '洗脚',
    };
  }

  bool get isTakeaway => this == BusinessType.takeaway;
}

enum OrderKind { takeaway, service }

enum OrderStatus { unpaid, pending, unused, used }

extension OrderStatusText on OrderStatus {
  String get label {
    return switch (this) {
      OrderStatus.unpaid => '未支付',
      OrderStatus.pending => '待完成',
      OrderStatus.unused => '未使用',
      OrderStatus.used => '已使用',
    };
  }
}
