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

BusinessType businessTypeFromApi(String? code) {
  return switch ((code ?? '').toLowerCase()) {
    'takeaway' => BusinessType.takeaway,
    'group' || 'group_buy' || 'groupbuy' => BusinessType.groupBuy,
    'hotel' => BusinessType.hotel,
    'fun' || 'entertainment' => BusinessType.entertainment,
    'movie' => BusinessType.movie,
    'beauty' => BusinessType.beauty,
    'ticket' => BusinessType.ticket,
    'massage' => BusinessType.massage,
    _ => BusinessType.takeaway,
  };
}

String businessTypeApiCode(BusinessType type) {
  return switch (type) {
    BusinessType.takeaway => 'takeaway',
    BusinessType.groupBuy => 'group_buy',
    BusinessType.hotel => 'hotel',
    BusinessType.entertainment => 'entertainment',
    BusinessType.movie => 'movie',
    BusinessType.beauty => 'beauty',
    BusinessType.ticket => 'ticket',
    BusinessType.massage => 'massage',
  };
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

OrderKind orderKindFromApi(String? code) {
  return switch ((code ?? '').toLowerCase()) {
    'takeaway' => OrderKind.takeaway,
    'service' => OrderKind.service,
    _ => OrderKind.service,
  };
}

String orderKindApiCode(OrderKind kind) {
  return switch (kind) {
    OrderKind.takeaway => 'takeaway',
    OrderKind.service => 'service',
  };
}

enum OrderStatus { unpaid, pending, unused, used, cancelled, refunded }

OrderStatus orderStatusFromApi(String? code) {
  return switch ((code ?? '').toLowerCase()) {
    'unpaid' => OrderStatus.unpaid,
    'pending' => OrderStatus.pending,
    'unused' => OrderStatus.unused,
    'used' => OrderStatus.used,
    'cancelled' => OrderStatus.cancelled,
    'refunded' => OrderStatus.refunded,
    _ => OrderStatus.unpaid,
  };
}

String orderStatusApiCode(OrderStatus status) {
  return switch (status) {
    OrderStatus.unpaid => 'unpaid',
    OrderStatus.pending => 'pending',
    OrderStatus.unused => 'unused',
    OrderStatus.used => 'used',
    OrderStatus.cancelled => 'cancelled',
    OrderStatus.refunded => 'refunded',
  };
}

extension OrderStatusText on OrderStatus {
  String get label {
    return switch (this) {
      OrderStatus.unpaid => '未支付',
      OrderStatus.pending => '待完成',
      OrderStatus.unused => '未使用',
      OrderStatus.used => '已使用',
      OrderStatus.cancelled => '已取消',
      OrderStatus.refunded => '已退款',
    };
  }

  String labelForKind(OrderKind kind) {
    if (kind == OrderKind.takeaway) {
      return switch (this) {
        OrderStatus.unpaid => '待付款',
        OrderStatus.pending => '配送中',
        OrderStatus.unused || OrderStatus.used => '已完成',
        OrderStatus.cancelled => '已取消',
        OrderStatus.refunded => '已退款',
      };
    }
    return switch (this) {
      OrderStatus.unpaid => '待付款',
      OrderStatus.pending => '处理中',
      OrderStatus.unused => '待使用',
      OrderStatus.used => '已使用',
      OrderStatus.cancelled => '已取消',
      OrderStatus.refunded => '已退款',
    };
  }
}
