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

class ReviewArgs {
  const ReviewArgs({required this.title, this.orderId});

  final String title;
  final String? orderId;
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
