import 'package:image_picker/image_picker.dart';

import '../../../app/app_state.dart';
import '../../../app/route_args.dart';
import '../../location/application/location_state.dart';
import '../../../core/network/app_api_client.dart';
import '../../../core/storage/auth_storage.dart';
import '../../../shared/enums/business_type.dart';
import '../../../shared/models/address_model.dart';
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
    return _withProfileSnapshot(AuthSession.fromApi(_map(json['data'])));
  }

  Future<AuthSession?> checkToken(String token) async {
    await appState.restoreToken(token);
    try {
      final json = await _get('/api/open/auth/token/check');
      final data = _map(json['data']);
      if (data['valid'] != true || data['profile'] == null) return null;
      return _withProfileSnapshot(
        AuthSession.fromTokenProfile(token, _map(data['profile'])),
      );
    } catch (_) {
      return null;
    }
  }

  Future<AuthSession> _withProfileSnapshot(AuthSession session) async {
    if (session.token.isEmpty) return session;
    await appState.restoreToken(session.token);
    try {
      final profile = await fetchProfile();
      return session.copyWith(
        nickname: profile.nickname,
        avatarUrl: profile.avatarUrl,
        phone: profile.phone,
        email: profile.email,
        memberLevelName: profile.memberLevelName,
        unreadMessageCount: profile.unreadMessageCount,
      );
    } catch (_) {
      return session;
    }
  }

  Future<String> sendEmailCode(String email, String scene) async {
    final json = await _post('/api/open/auth/email-code', {
      'email': email,
      'scene': scene,
    });
    return _nullableString(_map(json['data'])['code']) ?? '';
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
    return _withProfileSnapshot(AuthSession.fromApi(_map(json['data'])));
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
    final json = await _get(_withLocation('/api/app/discovery/home'));
    final data = _map(json['data']);
    final recommendations = ItemPageData.fromApi(_map(data['recommendations']));
    return HomeData(
      modules: _modules(data['modules']),
      recommendations: recommendations.list,
      recommendationPage: recommendations,
      unreadMessageCount: _int(data['unreadMessageCount']),
    );
  }

  Future<ItemPageData> fetchRecommendations({
    int page = 1,
    int pageSize = 12,
    String sort = 'personalized',
  }) async {
    final json = await _get(
      _withLocation(
        '/api/app/discovery/recommendations?page=$page&pageSize=$pageSize&sort=${Uri.encodeQueryComponent(sort)}',
      ),
    );
    return ItemPageData.fromApi(_map(json['data']));
  }

  Future<ModuleData> fetchModule(String moduleCode) async {
    final json = await _get(
      _withLocation('/api/app/discovery/modules/$moduleCode'),
    );
    final data = _map(json['data']);
    return ModuleData(
      moduleCode: _string(data['moduleCode'], fallback: moduleCode),
      businessType: businessTypeFromApi(_string(data['businessType'])),
      merchants: _merchants(data['stores']),
      featuredItems: _pageItems(_map(data['featuredItems'])),
    );
  }

  Future<MerchantPageData> searchStores(
    String keyword, {
    int page = 1,
    int pageSize = 12,
    String sort = 'default',
    BusinessType? businessType,
  }) async {
    final businessTypeQuery = businessType == null
        ? ''
        : '&businessType=${Uri.encodeQueryComponent(businessTypeApiCode(businessType))}';
    final json = await _get(
      _withLocation(
        '/api/app/discovery/stores/search?keyword=${Uri.encodeQueryComponent(keyword)}&page=$page&pageSize=$pageSize&sort=${Uri.encodeQueryComponent(sort)}$businessTypeQuery',
      ),
    );
    return MerchantPageData.fromApi(_map(json['data']));
  }

  Future<MerchantModel> fetchStore(int storeId) async =>
      _merchantFromStoreDetail(
        _map(
          (await _get(
            _withLocation('/api/app/discovery/stores/$storeId'),
          ))['data'],
        ),
      );

  Future<TradeCartData> fetchCart(int storeId) async {
    final json = await _get('/api/app/trade/cart?storeId=$storeId');
    return TradeCartData.fromApi(_map(json['data']));
  }

  Future<TradeCartData> addCartItem({
    required int storeId,
    required int itemId,
  }) async {
    final json = await _post('/api/app/trade/cart/items', {
      'storeId': storeId,
      'itemId': itemId,
      'quantity': 1,
    });
    return TradeCartData.fromApi(_map(json['data']));
  }

  Future<TradeCartData> updateCartItem({
    required int storeId,
    required int itemId,
    required int quantity,
  }) async {
    final json = await _put('/api/app/trade/cart/items/$itemId', {
      'storeId': storeId,
      'quantity': quantity,
    });
    return TradeCartData.fromApi(_map(json['data']));
  }

  Future<TradeCartData> removeCartItem({
    required int storeId,
    required int itemId,
  }) async {
    final json = await _delete(
      '/api/app/trade/cart/items/$itemId?storeId=$storeId',
    );
    return TradeCartData.fromApi(_map(json['data']));
  }

  Future<TradeCartData> clearCart(int storeId) async {
    final json = await _delete('/api/app/trade/cart?storeId=$storeId');
    return TradeCartData.fromApi(_map(json['data']));
  }

  Future<ItemDetailData> fetchItem(int itemId) async {
    final json = await _get(_withLocation('/api/app/discovery/items/$itemId'));
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
    String tablewareOption = 'merchant_decide',
    int? tablewareCount,
    int? couponId,
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
      'tablewareOption': tablewareOption,
      'tablewareCount': tablewareCount,
      'couponId': couponId,
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
    String tablewareOption = 'merchant_decide',
    int? tablewareCount,
    int? couponId,
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
      'tablewareOption': tablewareOption,
      'tablewareCount': tablewareCount,
      'couponId': couponId,
    });
    return OrderDetailData.fromApi(_map(json['data']));
  }

  Future<OrderDetailData> payOrder(String orderId) async {
    final json = await _post('/api/app/trade/orders/$orderId/pay', {
      'paymentMode': 'mock',
    });
    final paid = OrderDetailData.fromApi(_map(json['data']));
    await refreshUnreadMessageCount();
    return paid;
  }

  Future<void> refreshUnreadMessageCount() async {
    final profile = await fetchProfile();
    appState.updateProfile(unreadMessageCount: profile.unreadMessageCount);
  }

  Future<OrderDetailData> cancelOrder(String orderId) async {
    final json = await _post('/api/app/trade/orders/$orderId/cancel', {
      'remark': '用户取消订单',
    });
    return OrderDetailData.fromApi(_map(json['data']));
  }

  Future<OrderDetailData> requestRefund(
    String orderId, {
    String reason = '用户申请退款',
  }) async {
    final json = await _post('/api/app/trade/orders/$orderId/refund', {
      'reason': reason,
    });
    return OrderDetailData.fromApi(_map(json['data']));
  }

  Future<OrderDetailData> remindOrder(String orderId) async {
    final json = await _post('/api/app/trade/orders/$orderId/remind', {
      'remark': '用户催单',
    });
    return OrderDetailData.fromApi(_map(json['data']));
  }

  Future<OrderDetailData> updateOrderDeliveryAddress({
    required String orderId,
    required String addressId,
  }) async {
    final json = await _put('/api/app/trade/orders/$orderId/delivery-address', {
      'addressId': int.parse(addressId),
    });
    return OrderDetailData.fromApi(_map(json['data']));
  }

  Future<BookingData?> fetchBooking(String orderId) async {
    final json = await _get('/api/app/trade/orders/$orderId/booking');
    final data = json['data'];
    if (data == null) return null;
    return BookingData.fromApi(_map(data));
  }

  Future<BookingData> upsertBooking({
    required String orderId,
    String? contactName,
    String? contactPhone,
    String? bookingDate,
    String? bookingTimeSlot,
    int guestCount = 1,
    String? remark,
  }) async {
    final json = await _post('/api/app/trade/orders/$orderId/booking', {
      'contactName': contactName,
      'contactPhone': contactPhone,
      'bookingDate': bookingDate,
      'bookingTimeSlot': bookingTimeSlot,
      'guestCount': guestCount,
      'remark': remark,
    });
    return BookingData.fromApi(_map(json['data']));
  }

  Future<List<MessageItem>> fetchMessages({
    String? type,
    int page = 1,
    int pageSize = 20,
  }) async => (await _fetchMessagePage(
    type: type,
    page: page,
    pageSize: pageSize,
  )).list;

  Future<List<MessageItem>> fetchAllMessages({String? type}) async {
    final messages = <MessageItem>[];
    var page = 1;
    while (true) {
      final data = await _fetchMessagePage(
        type: type,
        page: page,
        pageSize: 50,
      );
      messages.addAll(data.list);
      if (!data.hasNext || data.list.isEmpty) return messages;
      page += 1;
    }
  }

  Future<_MessagePageData> _fetchMessagePage({
    String? type,
    required int page,
    required int pageSize,
  }) async {
    final query = <String, String>{'page': '$page', 'pageSize': '$pageSize'};
    if (type != null && type.isNotEmpty) query['type'] = type;
    final path = Uri(
      path: '/api/app/message/station',
      queryParameters: query,
    ).toString();
    final json = await _get(path);
    return _MessagePageData.fromApi(_map(json['data']));
  }

  Future<void> markMessageRead(int messageId) async {
    await _client.patch('/api/app/message/station/$messageId/read', {});
  }

  Future<void> markMessagesRead(List<int> messageIds) async {
    await _client.patch('/api/app/message/station/batch-read', {
      'messageIds': messageIds,
    });
  }

  Future<void> markMessagesUnread(List<int> messageIds) async {
    await _client.patch('/api/app/message/station/batch-unread', {
      'messageIds': messageIds,
    });
  }

  Future<void> deleteMessages(List<int> messageIds) async {
    await _client.patch('/api/app/message/station/batch-delete', {
      'messageIds': messageIds,
    });
  }

  Future<void> markAllMessagesRead() async {
    await _client.patch('/api/app/message/station/read-all', {});
  }

  Future<ProfileData> fetchProfile() async {
    final json = await _get('/api/app/account/profile');
    return ProfileData.fromApi(_map(json['data']));
  }

  Future<ProfileData> updateProfile({
    required String nickname,
    String? avatarUrl,
  }) async {
    final json = await _put('/api/app/account/profile', {
      'nickname': nickname,
      'avatarUrl': avatarUrl,
    });
    return ProfileData.fromApi(_map(json['data']));
  }

  Future<ProfileData> uploadAvatar(XFile file) async {
    final upload = await _uploadFile(file);
    final json = await _client.postMultipart(
      '/api/app/account/avatar',
      fileField: 'file',
      fileBytes: upload.bytes,
      filename: upload.filename,
      contentType: upload.contentType,
    );
    return ProfileData.fromApi(_map(json['data']));
  }

  Future<void> changePassword({
    required String oldPassword,
    required String newPassword,
  }) async {
    await _put('/api/app/account/password', {
      'oldPassword': oldPassword,
      'newPassword': newPassword,
    });
  }

  /// 通用文件上传，返回可公开访问的 URL。评价/投诉等模块复用。
  Future<String> uploadCommonFile(XFile file, {required String bizType}) async {
    final upload = await _uploadFile(file);
    final json = await _client.postMultipart(
      '/api/common/files/upload',
      fileField: 'file',
      fileBytes: upload.bytes,
      filename: upload.filename,
      contentType: upload.contentType,
      fields: {'bizType': bizType},
    );
    final data = _map(json['data']);
    return _string(data['publicUrl']);
  }

  Future<List<AddressData>> fetchAddresses() async {
    final json = await _get('/api/app/account/addresses');
    return _list(
      json['data'],
    ).map((entry) => AddressData.fromApi(_map(entry))).toList();
  }

  Future<AddressData> createAddress(AddressFormData address) async {
    final json = await _post('/api/app/account/addresses', address.toApi());
    return AddressData.fromApi(_map(json['data']));
  }

  Future<AddressData> updateAddress(
    String addressId,
    AddressFormData address,
  ) async {
    final json = await _put(
      '/api/app/account/addresses/$addressId',
      address.toApi(),
    );
    return AddressData.fromApi(_map(json['data']));
  }

  Future<void> deleteAddress(String addressId) async {
    await _delete('/api/app/account/addresses/$addressId');
  }

  Future<AddressData> setDefaultAddress(String addressId) async {
    final json = await _post(
      '/api/app/account/addresses/$addressId/default',
      <String, dynamic>{},
    );
    return AddressData.fromApi(_map(json['data']));
  }

  String? resolveAssetUrl(String? path) {
    if (path == null || path.isEmpty) return null;
    return _client.resolveUrl(path);
  }

  Future<List<FavoriteEntry>> fetchFavorites({String? favoriteType}) async {
    final typeQuery = favoriteType == null || favoriteType.isEmpty
        ? ''
        : 'favoriteType=${Uri.encodeQueryComponent(favoriteType)}&';
    final json = await _get(
      '/api/app/account/favorites?${typeQuery}page=1&pageSize=20',
    );
    final page = _map(json['data']);
    return _list(
      page['list'],
    ).map((entry) => FavoriteEntry.fromApi(_map(entry))).toList();
  }

  Future<void> saveFavorite({
    required String favoriteType,
    required int targetId,
    required String targetName,
    String? coverUrl,
    String? subtitle,
  }) async {
    await _post('/api/app/account/favorites', {
      'favoriteType': favoriteType,
      'targetId': targetId,
      'targetName': targetName,
      'coverUrl': coverUrl,
      'subtitle': subtitle,
    });
  }

  Future<void> deleteFavorite({
    required String favoriteType,
    required int targetId,
  }) async {
    await _delete(
      '/api/app/account/favorites/${Uri.encodeComponent(favoriteType)}/$targetId',
    );
  }

  Future<void> logout() async {
    await AuthStorage.clearToken();
    await _post('/api/open/auth/logout', <String, dynamic>{});
  }

  Future<Map<String, dynamic>> _get(String path) => _client.get(path);

  String _withLocation(String path) {
    final latitude = locationState.latitude;
    final longitude = locationState.longitude;
    if (latitude == null || longitude == null) return path;
    final separator = path.contains('?') ? '&' : '?';
    return '$path${separator}latitude=$latitude&longitude=$longitude';
  }

  Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) =>
      _client.post(path, body);

  Future<Map<String, dynamic>> _put(String path, Map<String, dynamic> body) =>
      _client.put(path, body);

  Future<Map<String, dynamic>> _delete(String path) => _client.delete(path);
}

class TradeCartData {
  const TradeCartData({
    required this.storeId,
    required this.storeName,
    required this.amount,
    required this.items,
    required this.catalogAvailable,
    this.notice,
  });

  final int storeId;
  final String storeName;
  final double amount;
  final List<TradeCartLineData> items;
  final bool catalogAvailable;
  final String? notice;

  factory TradeCartData.fromApi(Map<String, dynamic> json) => TradeCartData(
    storeId: _int(json['storeId']),
    storeName: _string(json['storeName']),
    amount: _double(json['amount']),
    items: _list(
      json['items'],
    ).map((entry) => TradeCartLineData.fromApi(_map(entry))).toList(),
    catalogAvailable: json['catalogAvailable'] != false,
    notice: _nullableString(json['notice']),
  );
}

class TradeCartLineData {
  const TradeCartLineData({
    required this.itemId,
    required this.itemName,
    required this.subtitle,
    required this.categoryName,
    required this.unitPrice,
    required this.quantity,
    required this.totalPrice,
    required this.stock,
    required this.status,
    required this.soldOut,
  });

  final int itemId;
  final String itemName;
  final String subtitle;
  final String categoryName;
  final double unitPrice;
  final int quantity;
  final double totalPrice;
  final int stock;
  final String status;
  final bool soldOut;

  factory TradeCartLineData.fromApi(Map<String, dynamic> json) =>
      TradeCartLineData(
        itemId: _int(json['itemId']),
        itemName: _string(json['itemName']),
        subtitle: _string(json['subtitle']),
        categoryName: _string(json['categoryName']),
        unitPrice: _double(json['unitPrice']),
        quantity: _int(json['quantity']),
        totalPrice: _double(json['totalPrice']),
        stock: _int(json['stock']),
        status: _string(json['status'], fallback: 'on_sale'),
        soldOut: json['soldOut'] == true,
      );
}

class AuthSession {
  const AuthSession({
    required this.token,
    required this.nickname,
    required this.avatarUrl,
    required this.phone,
    required this.email,
    required this.memberLevelName,
    required this.unreadMessageCount,
  });

  final String token;
  final String nickname;
  final String? avatarUrl;
  final String? phone;
  final String? email;
  final String memberLevelName;
  final int unreadMessageCount;

  factory AuthSession.fromApi(Map<String, dynamic> json) {
    final profile = _map(json['profile']);
    return AuthSession.fromTokenProfile(_string(json['token']), profile);
  }

  factory AuthSession.fromTokenProfile(
    String token,
    Map<String, dynamic> profile,
  ) => AuthSession(
    token: token,
    nickname: _string(profile['nickname'], fallback: '爱团用户'),
    avatarUrl: _nullableString(profile['avatarUrl']),
    phone: _nullableString(profile['phone']),
    email: _nullableString(profile['email']),
    memberLevelName: _string(profile['memberLevelName'], fallback: '普通会员'),
    unreadMessageCount: _int(profile['unreadMessageCount']),
  );

  AuthSession copyWith({
    String? nickname,
    String? avatarUrl,
    String? phone,
    String? email,
    String? memberLevelName,
    int? unreadMessageCount,
  }) => AuthSession(
    token: token,
    nickname: nickname ?? this.nickname,
    avatarUrl: avatarUrl ?? this.avatarUrl,
    phone: phone ?? this.phone,
    email: email ?? this.email,
    memberLevelName: memberLevelName ?? this.memberLevelName,
    unreadMessageCount: unreadMessageCount ?? this.unreadMessageCount,
  );
}

class HomeData {
  const HomeData({
    required this.modules,
    required this.recommendations,
    required this.recommendationPage,
    required this.unreadMessageCount,
  });

  final List<ModuleEntry> modules;
  final List<ItemModel> recommendations;
  final ItemPageData recommendationPage;
  final int unreadMessageCount;
}

class ItemPageData {
  const ItemPageData({
    required this.list,
    required this.page,
    required this.pageSize,
    required this.total,
    required this.hasNext,
  });

  final List<ItemModel> list;
  final int page;
  final int pageSize;
  final int total;
  final bool hasNext;

  factory ItemPageData.fromApi(Map<String, dynamic> json) => ItemPageData(
    list: _pageItems(json),
    page: _int(json['page']),
    pageSize: _int(json['pageSize']),
    total: _int(json['total']),
    hasNext: _bool(json['hasNext']),
  );

  ItemPageData copyWith({List<ItemModel>? list}) => ItemPageData(
    list: list ?? this.list,
    page: page,
    pageSize: pageSize,
    total: total,
    hasNext: hasNext,
  );
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

class MerchantPageData {
  const MerchantPageData({
    required this.list,
    required this.page,
    required this.pageSize,
    required this.total,
    required this.hasNext,
  });

  final List<MerchantModel> list;
  final int page;
  final int pageSize;
  final int total;
  final bool hasNext;

  factory MerchantPageData.fromApi(Map<String, dynamic> json) =>
      MerchantPageData(
        list: _merchants(json['list']),
        page: _int(json['page']),
        pageSize: _int(json['pageSize']),
        total: _int(json['total']),
        hasNext: _bool(json['hasNext']),
      );
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

class _MessagePageData {
  const _MessagePageData({required this.list, required this.hasNext});

  final List<MessageItem> list;
  final bool hasNext;

  factory _MessagePageData.fromApi(Map<String, dynamic> json) =>
      _MessagePageData(
        list: _list(
          json['list'],
        ).map((entry) => MessageItem.fromApi(_map(entry))).toList(),
        hasNext: _bool(json['hasNext']),
      );
}

class ProfileData {
  const ProfileData({
    required this.nickname,
    required this.avatarUrl,
    required this.phone,
    required this.email,
    required this.memberLevelName,
    required this.growthValue,
    required this.addressCount,
    required this.favoriteCount,
    required this.orderCount,
    required this.unreadMessageCount,
  });

  final String nickname;
  final String? avatarUrl;
  final String? phone;
  final String? email;
  final String memberLevelName;
  final int growthValue;
  final int addressCount;
  final int favoriteCount;
  final int orderCount;
  final int unreadMessageCount;

  factory ProfileData.fromApi(Map<String, dynamic> json) => ProfileData(
    nickname: _string(json['nickname'], fallback: '爱团用户'),
    avatarUrl: _nullableString(json['avatarUrl']),
    phone: _nullableString(json['phone']),
    email: _nullableString(json['email']),
    memberLevelName: _string(json['memberLevelName'], fallback: '普通会员'),
    growthValue: _int(json['growthValue']),
    addressCount: _int(json['addressCount']),
    favoriteCount: _int(json['favoriteCount']),
    orderCount: _int(json['orderCount']),
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
    required this.packageFee,
    required this.distanceExtraFee,
    required this.amount,
    required this.payableAmount,
    required this.discountAmount,
    required this.startPrice,
    required this.startPriceMissing,
    required this.minimumOrderMet,
    required this.deliveryDistanceKm,
    required this.maxDeliveryDistanceKm,
    required this.estimatedDeliveryMinutes,
    required this.estimatedArrivalText,
    required this.deliverable,
    required this.unavailableReason,
    required this.tablewareOption,
    required this.tablewareCount,
    required this.tablewareText,
    required this.items,
    required this.note,
  });

  final int storeId;
  final String storeName;
  final String businessType;
  final String? addressSnapshot;
  final double deliveryFee;
  final double packageFee;
  final double distanceExtraFee;
  final double amount;
  final double payableAmount;
  final double discountAmount;
  final double startPrice;
  final double startPriceMissing;
  final bool minimumOrderMet;
  final double? deliveryDistanceKm;
  final double? maxDeliveryDistanceKm;
  final int? estimatedDeliveryMinutes;
  final String? estimatedArrivalText;
  final bool deliverable;
  final String? unavailableReason;
  final String tablewareOption;
  final int? tablewareCount;
  final String? tablewareText;
  final List<CheckoutLineItemData> items;
  final String? note;

  factory CheckoutPreviewData.fromApi(Map<String, dynamic> json) =>
      CheckoutPreviewData(
        storeId: _int(json['storeId']),
        storeName: _string(json['storeName']),
        businessType: _string(json['businessType']),
        addressSnapshot: _nullableString(json['addressSnapshot']),
        deliveryFee: _double(json['deliveryFee']),
        packageFee: _double(json['packageFee']),
        distanceExtraFee: _double(json['distanceExtraFee']),
        amount: _double(json['amount']),
        payableAmount: _double(json['payableAmount']),
        discountAmount: _double(json['discountAmount']),
        startPrice: _double(json['startPrice']),
        startPriceMissing: _double(json['startPriceMissing']),
        minimumOrderMet: json['minimumOrderMet'] == null
            ? true
            : _bool(json['minimumOrderMet']),
        deliveryDistanceKm: _nullableDouble(json['deliveryDistanceKm']),
        maxDeliveryDistanceKm: _nullableDouble(json['maxDeliveryDistanceKm']),
        estimatedDeliveryMinutes: _nullableInt(
          json['estimatedDeliveryMinutes'],
        ),
        estimatedArrivalText: _nullableString(json['estimatedArrivalText']),
        deliverable: json['deliverable'] == null
            ? true
            : _bool(json['deliverable']),
        unavailableReason: _nullableString(json['unavailableReason']),
        tablewareOption: _string(
          json['tablewareOption'],
          fallback: 'merchant_decide',
        ),
        tablewareCount: _nullableInt(json['tablewareCount']),
        tablewareText: _nullableString(json['tablewareText']),
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
    required this.storeId,
    required this.storeName,
    required this.title,
    required this.amount,
    required this.deliveryFee,
    required this.packageFee,
    required this.discountAmount,
    required this.payableAmount,
    required this.addressSnapshot,
    required this.deliveryDistanceKm,
    required this.estimatedArrivalText,
    required this.deliveryCompletionText,
    required this.voucherSummary,
    required this.tablewareOption,
    required this.tablewareCount,
    required this.tablewareText,
    required this.remark,
    required this.refundStatus,
    required this.refundAmount,
    required this.refundReason,
    required this.refundedAt,
    required this.refundableByUser,
    required this.refundableByStaff,
    required this.refundHint,
    required this.createdAt,
    required this.paidAt,
    required this.completedAt,
    required this.items,
    required this.deliveryTimeline,
    required this.voucher,
    required this.booking,
  });

  final String id;
  final String orderNo;
  final OrderKind kind;
  final OrderStatus status;
  final String paymentStatus;
  final String fulfillmentStatus;
  final String? paymentMethod;
  final int storeId;
  final String storeName;
  final String title;
  final double amount;
  final double deliveryFee;
  final double packageFee;
  final double discountAmount;
  final double payableAmount;
  final String? addressSnapshot;
  final double? deliveryDistanceKm;
  final String? estimatedArrivalText;
  final String? deliveryCompletionText;
  final String? voucherSummary;
  final String? tablewareOption;
  final int? tablewareCount;
  final String? tablewareText;
  final String? remark;
  final String refundStatus;
  final double refundAmount;
  final String? refundReason;
  final DateTime? refundedAt;
  final bool refundableByUser;
  final bool refundableByStaff;
  final String? refundHint;
  final DateTime? createdAt;
  final DateTime? paidAt;
  final DateTime? completedAt;
  final List<OrderLineItemData> items;
  final List<TimelineNodeData> deliveryTimeline;
  final VoucherData? voucher;
  final BookingData? booking;

  factory OrderDetailData.fromApi(Map<String, dynamic> json) => OrderDetailData(
    id: _string(json['id']),
    orderNo: _string(json['orderNo']),
    kind: orderKindFromApi(_string(json['orderKind'])),
    status: orderStatusFromApi(_string(json['displayStatus'])),
    paymentStatus: _string(json['paymentStatus']),
    fulfillmentStatus: _string(json['fulfillmentStatus']),
    paymentMethod: _nullableString(json['paymentMethod']),
    storeId: _int(json['storeId']),
    storeName: _string(json['storeName']),
    title: _string(json['title']),
    amount: _double(json['amount']),
    deliveryFee: _double(json['deliveryFee']),
    packageFee: _double(json['packageFee']),
    discountAmount: _double(json['discountAmount']),
    payableAmount: _double(json['payableAmount']),
    addressSnapshot: _nullableString(json['addressSnapshot']),
    deliveryDistanceKm: _nullableDouble(json['deliveryDistanceKm']),
    estimatedArrivalText: _nullableString(json['estimatedArrivalText']),
    deliveryCompletionText: _nullableString(json['deliveryCompletionText']),
    voucherSummary: _nullableString(json['voucherSummary']),
    tablewareOption: _nullableString(json['tablewareOption']),
    tablewareCount: _nullableInt(json['tablewareCount']),
    tablewareText: _nullableString(json['tablewareText']),
    remark: _nullableString(json['remark']),
    refundStatus: _string(json['refundStatus'], fallback: 'none'),
    refundAmount: _double(json['refundAmount']),
    refundReason: _nullableString(json['refundReason']),
    refundedAt: _dateTime(json['refundedAt']),
    refundableByUser: json['refundableByUser'] == true,
    refundableByStaff: json['refundableByStaff'] == true,
    refundHint: _nullableString(json['refundHint']),
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
    booking: json['booking'] == null
        ? null
        : BookingData.fromApi(_map(json['booking'])),
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

class BookingData {
  const BookingData({
    required this.orderId,
    required this.orderNo,
    required this.storeName,
    required this.businessType,
    required this.contactName,
    required this.contactPhone,
    required this.bookingDate,
    required this.bookingTimeSlot,
    required this.guestCount,
    required this.storeConfirmStatus,
    required this.storeConfirmRemark,
    required this.confirmedAt,
    required this.createdAt,
  });

  final String orderId;
  final String orderNo;
  final String storeName;
  final String businessType;
  final String? contactName;
  final String? contactPhone;
  final String? bookingDate;
  final String? bookingTimeSlot;
  final int guestCount;
  final String storeConfirmStatus;
  final String? storeConfirmRemark;
  final DateTime? confirmedAt;
  final DateTime? createdAt;

  bool get isConfirmed => storeConfirmStatus.toLowerCase() == 'confirmed';

  factory BookingData.fromApi(Map<String, dynamic> json) => BookingData(
    orderId: _string(json['orderId']),
    orderNo: _string(json['orderNo']),
    storeName: _string(json['storeName']),
    businessType: _string(json['businessType']),
    contactName: _nullableString(json['contactName']),
    contactPhone: _nullableString(json['contactPhone']),
    bookingDate: _nullableString(json['bookingDate']),
    bookingTimeSlot: _nullableString(json['bookingTimeSlot']),
    guestCount: _int(json['guestCount']) <= 0 ? 1 : _int(json['guestCount']),
    storeConfirmStatus: _string(
      json['storeConfirmStatus'],
      fallback: 'pending',
    ),
    storeConfirmRemark: _nullableString(json['storeConfirmRemark']),
    confirmedAt: _dateTime(json['confirmedAt']),
    createdAt: _dateTime(json['createdAt']),
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

Future<_UploadFile> _uploadFile(XFile file) async {
  final filename = _uploadFilename(file);
  return _UploadFile(
    bytes: await file.readAsBytes(),
    filename: filename,
    contentType: _imageContentType(file.mimeType, filename),
  );
}

String _uploadFilename(XFile file) {
  final name = file.name.trim();
  if (name.isNotEmpty) return name;
  final path = file.path.trim();
  if (path.isNotEmpty) {
    final normalized = path.replaceAll('\\', '/');
    final segments = normalized.split('/');
    final last = segments.isEmpty ? '' : segments.last.trim();
    if (last.isNotEmpty) return last;
  }
  return 'upload.jpg';
}

String _imageContentType(String? mimeType, String filename) {
  final normalized = mimeType?.trim().toLowerCase();
  if (normalized == 'image/png' ||
      normalized == 'image/webp' ||
      normalized == 'image/jpeg') {
    return normalized!;
  }
  final lower = filename.toLowerCase();
  if (lower.endsWith('.png')) return 'image/png';
  if (lower.endsWith('.webp')) return 'image/webp';
  return 'image/jpeg';
}

class _UploadFile {
  const _UploadFile({
    required this.bytes,
    required this.filename,
    required this.contentType,
  });

  final List<int> bytes;
  final String filename;
  final String contentType;
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
  DeliveryRuleModel deliveryRule = const DeliveryRuleModel(),
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
  coverUrl: _nullableString(json['coverUrl']),
  recommendReason: _string(json['recommendReason']),
  estimatedTimeText: _string(json['estimatedTimeText']),
  longitude: _nullableDouble(json['longitude']),
  latitude: _nullableDouble(json['latitude']),
  status: _string(json['status'], fallback: 'open'),
  businessHours: _string(json['businessHoursText'], fallback: '10:00-22:00'),
  monthlySales: _int(json['monthlySales']),
  avgPrice: _double(json['avgPrice']),
  deliveryRule: deliveryRule,
);

MerchantModel _merchantFromStoreDetail(Map<String, dynamic> json) {
  final store = _map(json['store']);
  final reviewSummary = _map(json['reviewSummary']);
  final itemGroups = _list(json['itemGroups']);
  final items = <ItemModel>[];
  for (final group in itemGroups) {
    items.addAll(_items(_map(group)['items']));
  }
  final merchant = _merchantFromStoreCard(
    store,
    items: items.isEmpty ? _items(store['matchedItems']) : items,
    deliveryRule: _deliveryRule(json['deliveryRule']),
  );
  return MerchantModel(
    id: merchant.id,
    name: merchant.name,
    type: merchant.type,
    distance: merchant.distance,
    rating: _double(reviewSummary['rating']),
    summary: merchant.summary,
    address: merchant.address,
    tags: merchant.tags,
    items: merchant.items,
    coverUrl: merchant.coverUrl,
    estimatedTimeText: merchant.estimatedTimeText,
    longitude: merchant.longitude,
    latitude: merchant.latitude,
    status: merchant.status,
    businessHours: merchant.businessHours,
    monthlySales: merchant.monthlySales,
    avgPrice: merchant.avgPrice,
    deliveryRule: merchant.deliveryRule,
  );
}

DeliveryRuleModel _deliveryRule(dynamic value) {
  final json = _map(value);
  if (json.isEmpty) return const DeliveryRuleModel();
  return DeliveryRuleModel(
    deliveryFee: _double(json['deliveryFee']),
    estimatedMinutes: _int(json['estimatedMinutes']),
    startPrice: _double(json['startPrice']),
    packageFeeFixed: _double(json['packageFeeFixed']),
    packageFeePerItem: _double(json['packageFeePerItem']),
    packageFeeMode: _string(json['packageFeeMode'], fallback: 'none'),
    distanceExtraThresholdKm: _double(json['distanceExtraThresholdKm']),
    distanceExtraFee: _double(json['distanceExtraFee']),
    distanceExtraStepKm: _double(json['distanceExtraStepKm']) <= 0
        ? 1
        : _double(json['distanceExtraStepKm']),
    deliveryText: _string(json['deliveryText']),
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
    fulfillmentStatus: _string(json['fulfillmentStatus']),
    refundStatus: _string(json['refundStatus'], fallback: 'none'),
    businessType: type,
    amount: _double(json['amount']),
    desc: _string(json['title']),
    coverUrl: _nullableString(json['coverUrl']),
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

int? _nullableInt(dynamic value) {
  if (value == null) return null;
  if (value is num) return value.toInt();
  return int.tryParse(value.toString());
}

double _double(dynamic value) => value is num
    ? value.toDouble()
    : double.tryParse(value?.toString() ?? '') ?? 0;

double? _nullableDouble(dynamic value) {
  if (value == null) return null;
  if (value is num) return value.toDouble();
  return double.tryParse(value.toString());
}

bool _bool(dynamic value) =>
    value is bool ? value : value?.toString() == 'true';

DateTime? _dateTime(dynamic value) {
  if (value == null) return null;
  return DateTime.tryParse(value.toString())?.toLocal();
}
