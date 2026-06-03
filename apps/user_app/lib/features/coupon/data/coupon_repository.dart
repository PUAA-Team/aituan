import '../../../app/app_state.dart';
import '../../../core/network/app_api_client.dart';
import '../../../core/network/json_codec.dart';

final couponRepository = CouponRepository();

// 优惠券数据访问：我的券（三态）、可领模板、领取
class CouponRepository {
  CouponRepository({AppApiClient? client})
    : _client = client ?? AppApiClient(tokenProvider: () => appState.token);

  final AppApiClient _client;

  // status: usable / used / expired
  Future<List<UserCoupon>> fetchMyCoupons(String status) async {
    final json = await _client.get('/api/app/account/coupons?status=$status');
    return jsonList(
      json['data'],
    ).map((entry) => UserCoupon.fromApi(jsonMap(entry))).toList();
  }

  Future<List<AvailableCoupon>> fetchAvailableCoupons() async {
    final json = await _client.get('/api/app/account/coupons/available');
    return jsonList(
      json['data'],
    ).map((entry) => AvailableCoupon.fromApi(jsonMap(entry))).toList();
  }

  Future<void> claimCoupon(int templateId) async {
    await _client.post(
      '/api/app/account/coupons/$templateId/claim',
      <String, dynamic>{},
    );
  }

  Future<List<OrderCouponOption>> fetchUsableForOrder(
    double orderAmount,
  ) async {
    final json = await _client.get(
      '/api/app/account/coupons/usable-for-order?orderAmount=$orderAmount',
    );
    return jsonList(
      json['data'],
    ).map((entry) => OrderCouponOption.fromApi(jsonMap(entry))).toList();
  }
}

// 我的优惠券
class UserCoupon {
  const UserCoupon({
    required this.id,
    required this.name,
    required this.type,
    required this.status,
    required this.discountDesc,
    required this.thresholdDesc,
    required this.expireAt,
    required this.usedAt,
  });

  final int id;
  final String name;
  final String type; // full_reduction / discount
  final String status; // unused / used / expired
  final String discountDesc; // "减5元" / "9折"
  final String thresholdDesc; // "满30可用" / "无门槛"
  final DateTime? expireAt;
  final DateTime? usedAt;

  factory UserCoupon.fromApi(Map<String, dynamic> json) => UserCoupon(
    id: jsonInt(json['id']),
    name: jsonString(json['name']),
    type: jsonString(json['type']),
    status: jsonString(json['status'], fallback: 'unused'),
    discountDesc: jsonString(json['discountDesc']),
    thresholdDesc: jsonString(json['thresholdDesc']),
    expireAt: jsonDateTime(json['expireAt']),
    usedAt: jsonDateTime(json['usedAt']),
  );
}

// 下单时可选择的优惠券
class OrderCouponOption {
  const OrderCouponOption({
    required this.userCouponId,
    required this.name,
    required this.discountDesc,
    required this.discountAmount,
    required this.usable,
    required this.reason,
  });

  final int userCouponId;
  final String name;
  final String discountDesc;
  final double discountAmount;
  final bool usable;
  final String? reason;

  factory OrderCouponOption.fromApi(Map<String, dynamic> json) =>
      OrderCouponOption(
        userCouponId: jsonInt(json['userCouponId']),
        name: jsonString(json['name']),
        discountDesc: jsonString(json['discountDesc']),
        discountAmount: jsonDouble(json['discountAmount']),
        usable: jsonBool(json['usable']),
        reason: jsonStringOrNull(json['reason']),
      );
}

// 可领取的优惠券
class AvailableCoupon {
  const AvailableCoupon({
    required this.templateId,
    required this.name,
    required this.type,
    required this.discountDesc,
    required this.thresholdDesc,
    required this.validDesc,
    required this.remaining,
    required this.claimable,
    required this.reason,
  });

  final int templateId;
  final String name;
  final String type;
  final String discountDesc;
  final String thresholdDesc;
  final String validDesc; // "有效期至…" / "领取后N天内有效"
  final int? remaining; // 为空表示不限量
  final bool claimable;
  final String? reason; // 不可领时的原因

  factory AvailableCoupon.fromApi(Map<String, dynamic> json) => AvailableCoupon(
    templateId: jsonInt(json['templateId']),
    name: jsonString(json['name']),
    type: jsonString(json['type']),
    discountDesc: jsonString(json['discountDesc']),
    thresholdDesc: jsonString(json['thresholdDesc']),
    validDesc: jsonString(json['validDesc']),
    remaining: jsonIntOrNull(json['remaining']),
    claimable: jsonBool(json['claimable']),
    reason: jsonStringOrNull(json['reason']),
  );
}
