import '../features/coupon/data/coupon_repository.dart';
import '../shared/enums/business_type.dart';
import '../shared/models/address_model.dart';
import '../shared/models/item_model.dart';
import '../shared/models/merchant_model.dart';

class MerchantArgs {
  const MerchantArgs({required this.type, this.merchant});

  final BusinessType type;
  final MerchantModel? merchant;
}

class ItemArgs {
  const ItemArgs(this.item);

  final ItemModel item;
}

class SearchArgs {
  const SearchArgs(this.keyword);

  final String keyword;
}

class CheckoutLineArg {
  const CheckoutLineArg({
    required this.itemId,
    required this.quantity,
    required this.title,
    required this.subtitle,
    required this.unitPrice,
    this.categoryName = '',
  });

  final String itemId;
  final int quantity;
  final String title;
  final String subtitle;
  final double unitPrice;
  final String categoryName;
}

class CheckoutArgs {
  const CheckoutArgs({
    required this.kind,
    required this.title,
    required this.amount,
    this.storeId = '',
    this.businessType,
    this.lines = const [],
  });

  final OrderKind kind;
  final String title;
  final double amount;
  final String storeId;
  final BusinessType? businessType;
  final List<CheckoutLineArg> lines;
}

class OrderDetailArgs {
  const OrderDetailArgs({
    required this.kind,
    required this.status,
    this.orderId,
  });

  final OrderKind kind;
  final OrderStatus status;
  final String? orderId;
}

class CouponSelectorArgs {
  const CouponSelectorArgs({required this.orderAmount, this.selectedCoupon});

  final double orderAmount;
  final OrderCouponOption? selectedCoupon;
}

class CouponSelectorResult {
  const CouponSelectorResult.selected(this.coupon) : clear = false;
  const CouponSelectorResult.clear()
    : coupon = null,
      clear = true;

  final OrderCouponOption? coupon;
  final bool clear;
}

class ReviewArgs {
  const ReviewArgs({required this.title, this.orderId});

  final String title;
  final String? orderId;
}

/// 从商家页/订单页进入客服入口时使用：把目标门店与关联订单带过去，
/// 列表页可一键发起或定位到已有 open 会话。
class SupportLaunchArgs {
  const SupportLaunchArgs({
    this.storeId,
    this.storeName,
    this.relatedOrderId,
    this.topicHint,
  });

  final int? storeId;
  final String? storeName;
  final int? relatedOrderId;
  final String? topicHint;
}

class AddressListArgs {
  const AddressListArgs({this.selectMode = false, this.selectedAddressId});

  final bool selectMode;
  final String? selectedAddressId;
}

class AddressEditArgs {
  const AddressEditArgs({this.address});

  final AddressData? address;
}

class VoucherDetailArgs {
  const VoucherDetailArgs({required this.orderId});

  final String orderId;
}

class BookingDetailArgs {
  const BookingDetailArgs({required this.orderId});

  final String orderId;
}
