import '../../../app/app_state.dart';
import '../../../app/route_args.dart';
import '../../../core/network/app_api_client.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/item_model.dart';
import '../../../shared/models/merchant_model.dart';
import '../../../shared/models/message_item.dart';
import '../../../shared/models/module_entry.dart';
import '../../../shared/models/order_model.dart';

final backendRepository = BackendAppRepository();

class BackendAppRepository {
  BackendAppRepository({AppApiClient? client})
    : _client = client ?? AppApiClient(tokenProvider: () => appState.token);

  final AppApiClient _client;

  Future<AuthSession> login(String account, String password) async {
    final json = await _post('/api/open/auth/user/login/password', {
      'account': account,
      'password': password,
    });
    return AuthSession.fromApi(_map(json['data']));
  }

  Future<String> sendEmailCode(String email, String scene) async {
    final json = await _post('/api/open/auth/email-code', {
      'email': email,
      'scene': scene,
    });
    return _string(_map(json['data'])['code']);
  }

  Future<AuthSession> register({
    required String phone,
    required String email,
    required String emailCode,
    required String password,
  }) async {
    final json = await _post('/api/open/auth/user/register', {
      'phone': phone,
      'email': email,
      'emailCode': emailCode,
      'password': password,
    });
    return AuthSession.fromApi(_map(json['data']));
  }

  Future<void> resetPassword({
    required String email,
    required String emailCode,
    required String newPassword,
  }) async {
    await _post('/api/open/auth/user/password/reset', {
      'email': email,
      'emailCode': emailCode,
      'newPassword': newPassword,
    });
  }

  Future<HomeData> fetchHome() async {
    final json = await _get('/api/app/discovery/home');
    final data = _map(json['data']);
    final recommendations = _pageItems(data['recommendations']);
    return HomeData(
      modules: _modules(data['modules']),
      recommendations: recommendations,
      unreadMessageCount: _int(data['unreadMessageCount']),
    );
  }

  Future<List<ItemModel>> fetchRecommendations() async {
    final json = await _get(
      '/api/app/discovery/recommendations?page=1&pageSize=12',
    );
    return _pageItems(_map(json['data']));
  }

  Future<ModuleData> fetchModule(String moduleCode) async {
    final json = await _get('/api/app/discovery/modules/$moduleCode');
    final data = _map(json['data']);
    return ModuleData(
      moduleCode: _string(data['moduleCode'], fallback: moduleCode),
      businessType: businessTypeFromApi(_string(data['businessType'])),
      merchants: _merchants(data['stores']),
      featuredItems: _pageItems(_map(data['featuredItems'])),
    );
  }

  Future<List<MerchantModel>> searchStores(String keyword) async {
    final json = await _get(
      '/api/app/discovery/stores/search?keyword=${Uri.encodeQueryComponent(keyword)}&page=1&pageSize=12',
    );
    final page = _map(json['data']);
    return _merchants(page['list']);
  }

  Future<MerchantModel> fetchStore(int storeId) async =>
      _merchantFromStoreDetail(
        _map((await _get('/api/app/discovery/stores/$storeId'))['data']),
      );

  Future<ItemDetailData> fetchItem(int itemId) async {
    final json = await _get('/api/app/discovery/items/$itemId');
    final data = _map(json['data']);
    return ItemDetailData(
      item: ItemModel.fromApi(_map(data['item'])),
      merchant: _merchantFromStoreCard(_map(data['store']), items: const []),
      categories: _categories(data['categories']),
      itemGroups: _itemGroups(data['itemGroups']),
    );
  }

  Future<List<OrderModel>> fetchOrders({String? displayStatus}) async {
    final query = displayStatus == null || displayStatus.isEmpty
        ? '/api/app/trade/orders?page=1&pageSize=20'
        : '/api/app/trade/orders?displayStatus=${Uri.encodeQueryComponent(displayStatus)}&page=1&pageSize=20';
    final json = await _get(query);
    final page = _map(json['data']);
    return _orders(page['list']);
  }

  Future<OrderDetailData> fetchOrderDetail(String orderId) async {
    final json = await _get('/api/app/trade/orders/$orderId');
    return OrderDetailData.fromApi(_map(json['data']));
  }

  Future<DeliveryTimelineData> fetchDeliveryTimeline(String orderId) async {
    final json = await _get('/api/app/trade/orders/$orderId/delivery/timeline');
    return DeliveryTimelineData.fromApi(_map(json['data']));
  }

  Future<List<PaymentMethodData>> fetchPaymentMethods() async {
    final json = await _get('/api/app/trade/payment-methods');
    return _list(
      json['data'],
    ).map((entry) => PaymentMethodData.fromApi(_map(entry))).toList();
  }

  Future<CheckoutPreviewData> previewCheckout({
    required String storeId,
    required BusinessType businessType,
    required String? addressId,
    required List<CheckoutLineArg> lines,
    String remark = '',
  }) async {
    final json = await _post('/api/app/trade/checkout/preview', {
      'storeId': int.parse(storeId),
      'businessType': businessTypeApiCode(businessType),
      'addressId': addressId == null || addressId.isEmpty
          ? null
          : int.tryParse(addressId),
      'items': [
        for (final line in lines)
          {'itemId': int.parse(line.itemId), 'quantity': line.quantity},
      ],
      'remark': remark,
    });
    return CheckoutPreviewData.fromApi(_map(json['data']));
  }

  Future<OrderDetailData> createOrder({
    required String storeId,
    required BusinessType businessType,
    required String? addressId,
    required List<CheckoutLineArg> lines,
    required String remark,
    required String idempotencyKey,
  }) async {
    final json = await _post('/api/app/trade/orders', {
      'storeId': int.parse(storeId),
      'businessType': businessTypeApiCode(businessType),
      'addressId': addressId == null || addressId.isEmpty
          ? null
          : int.tryParse(addressId),
      'items': [
        for (final line in lines)
          {'itemId': int.parse(line.itemId), 'quantity': line.quantity},
      ],
      'remark': remark,
      'idempotencyKey': idempotencyKey,
    });
    return OrderDetailData.fromApi(_map(json['data']));
  }

  Future<OrderDetailData> payOrder(String orderId) async {
    final json = await _post('/api/app/trade/orders/$orderId/pay', {
      'paymentMode': 'mock',
    });
    return OrderDetailData.fromApi(_map(json['data']));
  }

  Future<List<MessageItem>> fetchMessages() async {
    final json = await _get('/api/app/message/station?page=1&pageSize=20');
    final page = _map(json['data']);
    return _list(
      page['list'],
    ).map((entry) => MessageItem.fromApi(_map(entry))).toList();
  }

  Future<ProfileData> fetchProfile() async {
    final json = await _get('/api/app/account/profile');
    return ProfileData.fromApi(_map(json['data']));
  }

  Future<List<FavoriteEntry>> fetchFavorites() async {
    final json = await _get('/api/app/account/favorites?page=1&pageSize=20');
    final page = _map(json['data']);
    return _list(
      page['list'],
    ).map((entry) => FavoriteEntry.fromApi(_map(entry))).toList();
  }

  Future<void> logout() async {
    await _post('/api/open/auth/logout', <String, dynamic>{});
  }

  Future<Map<String, dynamic>> _get(String path) => _client.get(path);

  Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) =>
      _client.post(path, body);
}

class AuthSession {
  const AuthSession({
    required this.token,
    required this.nickname,
    required this.phone,
    required this.email,
    required this.memberLevelName,
    required this.unreadMessageCount,
  });

  final String token;
  final String nickname;
  final String? phone;
  final String? email;
  final String memberLevelName;
  final int unreadMessageCount;

  factory AuthSession.fromApi(Map<String, dynamic> json) => AuthSession(
    token: _string(json['token']),
    nickname: _string(_map(json['profile'])['nickname'], fallback: '爱团用户'),
    phone: _nullableString(_map(json['profile'])['phone']),
    email: _nullableString(_map(json['profile'])['email']),
    memberLevelName: _string(
      _map(json['profile'])['memberLevelName'],
      fallback: '普通会员',
    ),
    unreadMessageCount: _int(_map(json['profile'])['unreadMessageCount']),
  );
}

class HomeData {
  const HomeData({
    required this.modules,
    required this.recommendations,
    required this.unreadMessageCount,
  });

  final List<ModuleEntry> modules;
  final List<ItemModel> recommendations;
  final int unreadMessageCount;
}

class ModuleData {
  const ModuleData({
    required this.moduleCode,
    required this.businessType,
    required this.merchants,
    required this.featuredItems,
  });

  final String moduleCode;
  final BusinessType businessType;
  final List<MerchantModel> merchants;
  final List<ItemModel> featuredItems;
}

class ItemDetailData {
  const ItemDetailData({
    required this.item,
    required this.merchant,
    required this.categories,
    required this.itemGroups,
  });

  final ItemModel item;
  final MerchantModel merchant;
  final List<CategoryData> categories;
  final List<ItemGroupData> itemGroups;
}

class CategoryData {
  const CategoryData({
    required this.id,
    required this.name,
    required this.sortOrder,
  });

  final String id;
  final String name;
  final int sortOrder;
}

class ItemGroupData {
  const ItemGroupData({
    required this.categoryId,
    required this.categoryName,
    required this.items,
  });

  final String categoryId;
  final String categoryName;
  final List<ItemModel> items;
}

class ProfileData {
  const ProfileData({
    required this.nickname,
    required this.phone,
    required this.email,
    required this.memberLevelName,
    required this.growthValue,
    required this.addressCount,
    required this.favoriteCount,
    required this.unreadMessageCount,
  });

  final String nickname;
  final String? phone;
  final String? email;
  final String memberLevelName;
  final int growthValue;
  final int addressCount;
  final int favoriteCount;
  final int unreadMessageCount;

  factory ProfileData.fromApi(Map<String, dynamic> json) => ProfileData(
    nickname: _string(json['nickname'], fallback: '爱团用户'),
    phone: _nullableString(json['phone']),
    email: _nullableString(json['email']),
    memberLevelName: _string(json['memberLevelName'], fallback: '普通会员'),
    growthValue: _int(json['growthValue']),
    addressCount: _int(json['addressCount']),
    favoriteCount: _int(json['favoriteCount']),
    unreadMessageCount: _int(json['unreadMessageCount']),
  );
}

class CheckoutPreviewData {
  const CheckoutPreviewData({
    required this.storeId,
    required this.storeName,
    required this.businessType,
    required this.addressSnapshot,
    required this.deliveryFee,
    required this.amount,
    required this.payableAmount,
    required this.discountAmount,
    required this.items,
    required this.note,
  });

  final int storeId;
  final String storeName;
  final String businessType;
  final String? addressSnapshot;
  final double deliveryFee;
  final double amount;
  final double payableAmount;
  final double discountAmount;
  final List<CheckoutLineItemData> items;
  final String? note;

  factory CheckoutPreviewData.fromApi(Map<String, dynamic> json) =>
      CheckoutPreviewData(
        storeId: _int(json['storeId']),
        storeName: _string(json['storeName']),
        businessType: _string(json['businessType']),
        addressSnapshot: _nullableString(json['addressSnapshot']),
        deliveryFee: _double(json['deliveryFee']),
        amount: _double(json['amount']),
        payableAmount: _double(json['payableAmount']),
        discountAmount: _double(json['discountAmount']),
        items: _list(
          json['items'],
        ).map((entry) => CheckoutLineItemData.fromApi(_map(entry))).toList(),
        note: _nullableString(json['note']),
      );
}

class CheckoutLineItemData {
  const CheckoutLineItemData({
    required this.itemId,
    required this.itemName,
    required this.subtitle,
    required this.quantity,
    required this.unitPrice,
    required this.totalPrice,
    required this.categoryName,
  });

  final String itemId;
  final String itemName;
  final String subtitle;
  final int quantity;
  final double unitPrice;
  final double totalPrice;
  final String categoryName;

  factory CheckoutLineItemData.fromApi(Map<String, dynamic> json) =>
      CheckoutLineItemData(
        itemId: _string(json['itemId']),
        itemName: _string(json['itemName']),
        subtitle: _string(json['subtitle']),
        quantity: _int(json['quantity']),
        unitPrice: _double(json['unitPrice']),
        totalPrice: _double(json['totalPrice']),
        categoryName: _string(json['categoryName']),
      );
}

class OrderDetailData {
  const OrderDetailData({
    required this.id,
    required this.orderNo,
    required this.kind,
    required this.status,
    required this.paymentStatus,
    required this.fulfillmentStatus,
    required this.paymentMethod,
    required this.storeName,
    required this.title,
    required this.amount,
    required this.deliveryFee,
    required this.discountAmount,
    required this.payableAmount,
    required this.addressSnapshot,
    required this.voucherSummary,
    required this.remark,
    required this.createdAt,
    required this.paidAt,
    required this.completedAt,
    required this.items,
    required this.deliveryTimeline,
    required this.voucher,
  });

  final String id;
  final String orderNo;
  final OrderKind kind;
  final OrderStatus status;
  final String paymentStatus;
  final String fulfillmentStatus;
  final String? paymentMethod;
  final String storeName;
  final String title;
  final double amount;
  final double deliveryFee;
  final double discountAmount;
  final double payableAmount;
  final String? addressSnapshot;
  final String? voucherSummary;
  final String? remark;
  final DateTime? createdAt;
  final DateTime? paidAt;
  final DateTime? completedAt;
  final List<OrderLineItemData> items;
  final List<TimelineNodeData> deliveryTimeline;
  final VoucherData? voucher;

  factory OrderDetailData.fromApi(Map<String, dynamic> json) => OrderDetailData(
    id: _string(json['id']),
    orderNo: _string(json['orderNo']),
    kind: orderKindFromApi(_string(json['orderKind'])),
    status: orderStatusFromApi(_string(json['displayStatus'])),
    paymentStatus: _string(json['paymentStatus']),
    fulfillmentStatus: _string(json['fulfillmentStatus']),
    paymentMethod: _nullableString(json['paymentMethod']),
    storeName: _string(json['storeName']),
    title: _string(json['title']),
    amount: _double(json['amount']),
    deliveryFee: _double(json['deliveryFee']),
    discountAmount: _double(json['discountAmount']),
    payableAmount: _double(json['payableAmount']),
    addressSnapshot: _nullableString(json['addressSnapshot']),
    voucherSummary: _nullableString(json['voucherSummary']),
    remark: _nullableString(json['remark']),
    createdAt: _dateTime(json['createdAt']),
    paidAt: _dateTime(json['paidAt']),
    completedAt: _dateTime(json['completedAt']),
    items: _list(
      json['items'],
    ).map((entry) => OrderLineItemData.fromApi(_map(entry))).toList(),
    deliveryTimeline: _timeline(_map(json['deliveryTimeline'])),
    voucher: json['voucher'] == null
        ? null
        : VoucherData.fromApi(_map(json['voucher'])),
  );
}

class OrderLineItemData {
  const OrderLineItemData({
    required this.itemId,
    required this.itemName,
    required this.subtitle,
    required this.businessType,
    required this.categoryName,
    required this.quantity,
    required this.unitPrice,
    required this.totalPrice,
    required this.coverUrl,
  });

  final String itemId;
  final String itemName;
  final String subtitle;
  final String businessType;
  final String categoryName;
  final int quantity;
  final double unitPrice;
  final double totalPrice;
  final String coverUrl;

  factory OrderLineItemData.fromApi(Map<String, dynamic> json) =>
      OrderLineItemData(
        itemId: _string(json['itemId']),
        itemName: _string(json['itemName']),
        subtitle: _string(json['subtitle']),
        businessType: _string(json['businessType']),
        categoryName: _string(json['categoryName']),
        quantity: _int(json['quantity']),
        unitPrice: _double(json['unitPrice']),
        totalPrice: _double(json['totalPrice']),
        coverUrl: _nullableString(json['coverUrl']) ?? '',
      );
}

class DeliveryTimelineData {
  const DeliveryTimelineData({
    required this.orderNo,
    required this.currentStage,
    required this.nodes,
  });

  final String orderNo;
  final String currentStage;
  final List<TimelineNodeData> nodes;

  factory DeliveryTimelineData.fromApi(Map<String, dynamic> json) =>
      DeliveryTimelineData(
        orderNo: _string(json['orderNo']),
        currentStage: _string(json['currentStage']),
        nodes: _timeline(json),
      );
}

class TimelineNodeData {
  const TimelineNodeData({
    required this.code,
    required this.text,
    required this.reachedAt,
  });

  final String code;
  final String text;
  final DateTime? reachedAt;

  factory TimelineNodeData.fromApi(Map<String, dynamic> json) =>
      TimelineNodeData(
        code: _string(json['code']),
        text: _string(json['text']),
        reachedAt: _dateTime(json['reachedAt']),
      );
}

class VoucherData {
  const VoucherData({
    required this.voucherCode,
    required this.qrPayload,
    required this.status,
    required this.effectiveFrom,
    required this.effectiveTo,
  });

  final String voucherCode;
  final String qrPayload;
  final String status;
  final DateTime? effectiveFrom;
  final DateTime? effectiveTo;

  factory VoucherData.fromApi(Map<String, dynamic> json) => VoucherData(
    voucherCode: _string(json['voucherCode']),
    qrPayload: _string(json['qrPayload']),
    status: _string(json['status']),
    effectiveFrom: _dateTime(json['effectiveFrom']),
    effectiveTo: _dateTime(json['effectiveTo']),
  );
}

class PaymentMethodData {
  const PaymentMethodData({
    required this.code,
    required this.name,
    required this.enabled,
  });

  final String code;
  final String name;
  final bool enabled;

  factory PaymentMethodData.fromApi(Map<String, dynamic> json) =>
      PaymentMethodData(
        code: _string(json['code']),
        name: _string(json['name']),
        enabled: _bool(json['enabled']),
      );
}

class FavoriteEntry {
  const FavoriteEntry({
    required this.favoriteType,
    required this.targetId,
    required this.targetName,
    required this.coverUrl,
    required this.subtitle,
  });

  final String favoriteType;
  final String targetId;
  final String targetName;
  final String? coverUrl;
  final String? subtitle;

  factory FavoriteEntry.fromApi(Map<String, dynamic> json) => FavoriteEntry(
    favoriteType: _string(json['favoriteType']),
    targetId: _string(json['targetId']),
    targetName: _string(json['targetName']),
    coverUrl: _nullableString(json['coverUrl']),
    subtitle: _nullableString(json['subtitle']),
  );
}

List<ModuleEntry> _modules(dynamic value) => _list(value)
    .map(
      (entry) => ModuleEntry(
        code: _string(_map(entry)['code']),
        title: _string(_map(entry)['name']),
        type: businessTypeFromApi(_string(_map(entry)['businessType'])),
      ),
    )
    .toList();

List<ItemModel> _pageItems(dynamic value) => _list(
  _map(value)['list'],
).map((entry) => ItemModel.fromApi(_map(entry))).toList();

List<ItemModel> _items(dynamic value) =>
    _list(value).map((entry) => ItemModel.fromApi(_map(entry))).toList();

List<MerchantModel> _merchants(dynamic value) => _list(value)
    .map(
      (entry) => _merchantFromStoreCard(
        _map(entry),
        items: _items(_map(entry)['matchedItems']),
      ),
    )
    .toList();

MerchantModel _merchantFromStoreCard(
  Map<String, dynamic> json, {
  List<ItemModel> items = const [],
}) => MerchantModel(
  id: _string(json['id']),
  name: _string(json['name']),
  type: businessTypeFromApi(_string(json['businessType'])),
  distance: _string(json['distanceText']),
  rating: _double(json['rating']),
  summary: _string(json['summary']),
  address: _string(json['address']),
  tags: _strings(json['tags']),
  items: items,
);

MerchantModel _merchantFromStoreDetail(Map<String, dynamic> json) {
  final store = _map(json['store']);
  final itemGroups = _list(json['itemGroups']);
  final items = <ItemModel>[];
  for (final group in itemGroups) {
    items.addAll(_items(_map(group)['items']));
  }
  return _merchantFromStoreCard(
    store,
    items: items.isEmpty ? _items(store['matchedItems']) : items,
  );
}

List<OrderModel> _orders(dynamic value) => _list(value).map((entry) {
  final json = _map(entry);
  final kind = orderKindFromApi(_string(json['orderKind']));
  final type = _string(json['businessType']).isEmpty
      ? (kind == OrderKind.takeaway
            ? BusinessType.takeaway
            : BusinessType.groupBuy)
      : businessTypeFromApi(_string(json['businessType']));
  return OrderModel(
    id: _string(json['id']),
    title: _string(json['title']),
    storeName: _string(json['storeName']),
    kind: kind,
    status: orderStatusFromApi(_string(json['displayStatus'])),
    businessType: type,
    amount: _double(json['amount']),
    desc: _string(json['title']),
  );
}).toList();

List<CategoryData> _categories(dynamic value) => _list(value).map((entry) {
  final json = _map(entry);
  return CategoryData(
    id: _string(json['id']),
    name: _string(json['name']),
    sortOrder: _int(json['sortOrder']),
  );
}).toList();

List<ItemGroupData> _itemGroups(dynamic value) => _list(value).map((entry) {
  final json = _map(entry);
  return ItemGroupData(
    categoryId: _string(json['categoryId']),
    categoryName: _string(json['categoryName']),
    items: _items(json['items']),
  );
}).toList();

List<TimelineNodeData> _timeline(Map<String, dynamic> json) => _list(
  json['nodes'],
).map((entry) => TimelineNodeData.fromApi(_map(entry))).toList();

List<String> _strings(dynamic value) {
  if (value is List) {
    return value
        .map((entry) => entry.toString())
        .where((entry) => entry.isNotEmpty)
        .toList();
  }
  if (value == null) return const [];
  return value
      .toString()
      .split(',')
      .map((entry) => entry.trim())
      .where((entry) => entry.isNotEmpty)
      .toList();
}

Map<String, dynamic> _map(dynamic value) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return value.cast<String, dynamic>();
  return <String, dynamic>{};
}

List<dynamic> _list(dynamic value) {
  if (value is List) return value;
  return const [];
}

String _string(dynamic value, {String fallback = ''}) {
  final text = value?.toString().trim();
  return (text == null || text.isEmpty) ? fallback : text;
}

String? _nullableString(dynamic value) {
  final text = value?.toString().trim();
  return (text == null || text.isEmpty) ? null : text;
}

int _int(dynamic value) =>
    value is num ? value.toInt() : int.tryParse(value?.toString() ?? '') ?? 0;

double _double(dynamic value) => value is num
    ? value.toDouble()
    : double.tryParse(value?.toString() ?? '') ?? 0;

bool _bool(dynamic value) =>
    value is bool ? value : value?.toString() == 'true';

DateTime? _dateTime(dynamic value) {
  if (value == null) return null;
  return DateTime.tryParse(value.toString())?.toLocal();
}
