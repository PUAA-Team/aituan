import '../shared/enums/business_type.dart';
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

class CheckoutArgs {
  const CheckoutArgs({
    required this.kind,
    required this.title,
    required this.amount,
  });

  final OrderKind kind;
  final String title;
  final double amount;
}

class OrderDetailArgs {
  const OrderDetailArgs({required this.kind, required this.status});

  final OrderKind kind;
  final OrderStatus status;
}
