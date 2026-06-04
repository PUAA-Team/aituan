import 'package:flutter/material.dart';

import '../core/constants/route_constants.dart';
import '../features/address/presentation/address_edit_page.dart';
import '../features/address/presentation/address_list_page.dart';
import '../features/auth/presentation/login_page.dart';
import '../features/auth/presentation/splash_page.dart';
import '../features/checkout/presentation/checkout_page.dart';
import '../features/coupon/presentation/coupon_claim_page.dart';
import '../features/coupon/presentation/coupon_page.dart';
import '../features/coupon/presentation/coupon_selector_page.dart';
import '../features/complaint/presentation/complaint_list_page.dart';
import '../features/complaint/presentation/complaint_detail_page.dart';
import '../features/complaint/presentation/complaint_submit_page.dart';
import '../features/favorite/presentation/favorite_page.dart';
import '../features/home/data/mock_data.dart';
import '../features/home/presentation/module_page.dart';
import '../features/member/presentation/member_center_page.dart';
import '../features/merchant/presentation/item_detail_page.dart';
import '../features/merchant/presentation/service_merchant_page.dart';
import '../features/merchant/presentation/takeaway_merchant_page.dart';
import '../features/order/presentation/booking_detail_page.dart';
import '../features/order/presentation/delivery_tracking_page.dart';
import '../features/order/presentation/service_order_detail_page.dart';
import '../features/order/presentation/takeaway_order_detail_page.dart';
import '../features/order/presentation/voucher_detail_page.dart';
import '../features/profile/presentation/about_document_page.dart';
import '../features/profile/presentation/about_page.dart';
import '../features/profile/presentation/profile_edit_page.dart';
import '../features/profile/presentation/settings_page.dart';
import '../features/review/presentation/my_reviews_page.dart';
import '../features/review/presentation/review_detail_page.dart';
import '../features/review/presentation/review_publish_page.dart';
import '../features/search/presentation/search_page.dart';
import '../features/search/presentation/search_result_page.dart';
import '../features/support/presentation/support_chat_page.dart';
import '../features/support/presentation/support_sessions_page.dart';
import '../shared/enums/business_type.dart';
import '../shared/models/item_model.dart';
import '../shared/models/module_entry.dart';
import 'app_state.dart';
import 'main_shell.dart';
import 'route_args.dart';

class AppRouter {
  const AppRouter._();

  static final _protected = <String>{
    Routes.checkout,
    Routes.orders,
    Routes.orderDetail,
    Routes.deliveryTracking,
    Routes.message,
    Routes.favorite,
    Routes.addressList,
    Routes.addressEdit,
    Routes.profile,
    Routes.memberCenter,
    Routes.coupons,
    Routes.couponClaim,
    Routes.couponSelector,
    Routes.settings,
    Routes.profileEdit,
    Routes.reviewPublish,
    Routes.myReviews,
    Routes.reviewDetail,
    Routes.supportSessions,
    Routes.supportChat,
    Routes.complaintList,
    Routes.complaintDetail,
    Routes.complaintSubmit,
    Routes.voucherDetail,
    Routes.bookingDetail,
  };

  static Route<dynamic> onGenerateRoute(RouteSettings settings) {
    if (_protected.contains(settings.name) && !appState.isLoggedIn) {
      return _page(const LoginPage(showNotice: true), settings);
    }
    return switch (settings.name) {
      Routes.splash => _page(const SplashPage(), settings),
      Routes.login => _page(const LoginPage(), settings),
      Routes.main || Routes.home => _page(const MainShell(), settings),
      Routes.message => _page(const MainShell(initialIndex: 1), settings),
      Routes.orders => _page(const MainShell(initialIndex: 2), settings),
      Routes.profile => _page(const MainShell(initialIndex: 3), settings),
      Routes.module => _page(
        ModulePage(module: _module(settings.arguments)),
        settings,
      ),
      Routes.search => _page(const SearchPage(), settings),
      Routes.searchResult => _page(
        SearchResultPage(keyword: _searchKeyword(settings.arguments)),
        settings,
      ),
      Routes.merchantDetail => _merchant(settings),
      Routes.itemDetail => _page(
        ItemDetailPage(item: _item(settings.arguments)),
        settings,
      ),
      Routes.checkout => _page(
        CheckoutPage(args: _checkout(settings.arguments)),
        settings,
      ),
      Routes.orderDetail => _orderDetail(settings),
      Routes.deliveryTracking => _page(
        DeliveryTrackingPage(args: _orderDetailArgs(settings.arguments)),
        settings,
      ),
      Routes.favorite => _page(const FavoritePage(), settings),
      Routes.memberCenter => _page(const MemberCenterPage(), settings),
      Routes.coupons => _page(const CouponPage(), settings),
      Routes.couponClaim => _page(const CouponClaimPage(), settings),
      Routes.couponSelector => _page(
        CouponSelectorPage(args: _couponSelectorArgs(settings.arguments)),
        settings,
      ),
      Routes.addressList => _page(
        AddressListPage(args: _addressListArgs(settings.arguments)),
        settings,
      ),
      Routes.addressEdit => _page(
        AddressEditPage(args: _addressEditArgs(settings.arguments)),
        settings,
      ),
      Routes.settings => _page(const SettingsPage(), settings),
      Routes.profileEdit => _page(const ProfileEditPage(), settings),
      Routes.about => _page(const AboutPage(), settings),
      Routes.userAgreement => _page(
        const AboutDocumentPage(document: userAgreementDocument),
        settings,
      ),
      Routes.privacyPolicy => _page(
        const AboutDocumentPage(document: privacyPolicyDocument),
        settings,
      ),
      Routes.qualificationSupport => _page(
        const AboutDocumentPage(document: qualificationSupportDocument),
        settings,
      ),
      Routes.reviewPublish => _page(const ReviewPublishPage(), settings),
      Routes.myReviews => _page(const MyReviewsPage(), settings),
      Routes.reviewDetail => _page(
        ReviewDetailPage(reviewId: _intArg(settings.arguments)),
        settings,
      ),
      Routes.supportSessions => _page(
        SupportSessionsPage(
          launchArgs: settings.arguments is SupportLaunchArgs
              ? settings.arguments as SupportLaunchArgs
              : null,
        ),
        settings,
      ),
      Routes.supportChat => _page(
        SupportChatPage(sessionId: _intArg(settings.arguments)),
        settings,
      ),
      Routes.complaintList => _page(const ComplaintListPage(), settings),
      Routes.complaintDetail => _page(
        ComplaintDetailPage(complaintId: _intArg(settings.arguments)),
        settings,
      ),
      Routes.complaintSubmit => _page(
        ComplaintSubmitPage(
          args: settings.arguments is ComplaintSubmitArgs
              ? settings.arguments as ComplaintSubmitArgs
              : null,
        ),
        settings,
      ),
      Routes.voucherDetail => _page(
        VoucherDetailPage(args: _voucherArgs(settings.arguments)),
        settings,
      ),
      Routes.bookingDetail => _page(
        BookingDetailPage(args: _bookingArgs(settings.arguments)),
        settings,
      ),
      _ => _page(const MainShell(), settings),
    };
  }

  static int _intArg(Object? args) {
    if (args is int) return args;
    if (args is num) return args.toInt();
    if (args is String) return int.tryParse(args) ?? 0;
    return 0;
  }

  static MaterialPageRoute<dynamic> _merchant(RouteSettings settings) {
    final args = settings.arguments;
    final data = args is MerchantArgs
        ? args
        : MerchantArgs(type: BusinessType.takeaway, merchant: merchants.first);
    final merchant = data.merchant ?? merchantById(null);
    if (data.type.isTakeaway) {
      return _page(TakeawayMerchantPage(merchant: merchant), settings);
    }
    return _page(ServiceMerchantPage(merchant: merchant), settings);
  }

  static MaterialPageRoute<dynamic> _orderDetail(RouteSettings settings) {
    final data = _orderDetailArgs(settings.arguments);
    if (data.kind == OrderKind.takeaway) {
      return _page(TakeawayOrderDetailPage(args: data), settings);
    }
    return _page(ServiceOrderDetailPage(args: data), settings);
  }

  static OrderDetailArgs _orderDetailArgs(Object? args) =>
      args is OrderDetailArgs
      ? args
      : const OrderDetailArgs(
          kind: OrderKind.takeaway,
          status: OrderStatus.pending,
        );

  static MaterialPageRoute<dynamic> _page(
    Widget child,
    RouteSettings settings,
  ) => MaterialPageRoute(builder: (_) => child, settings: settings);

  static ModuleEntry _module(Object? args) =>
      args is ModuleEntry ? args : modules.first;

  static CheckoutArgs _checkout(Object? args) => args is CheckoutArgs
      ? args
      : const CheckoutArgs(
          kind: OrderKind.takeaway,
          title: '招牌中国汉堡',
          amount: 39.7,
        );

  static String _searchKeyword(Object? args) => switch (args) {
    SearchArgs(:final keyword) => keyword,
    String keyword => keyword,
    _ => '',
  };

  static AddressListArgs _addressListArgs(Object? args) =>
      args is AddressListArgs ? args : const AddressListArgs();

  static AddressEditArgs _addressEditArgs(Object? args) =>
      args is AddressEditArgs ? args : const AddressEditArgs();

  static ItemModel _item(Object? args) =>
      args is ItemArgs ? args.item : serviceItems.first;

  static VoucherDetailArgs _voucherArgs(Object? args) =>
      args is VoucherDetailArgs ? args : const VoucherDetailArgs(orderId: '');

  static BookingDetailArgs _bookingArgs(Object? args) =>
      args is BookingDetailArgs ? args : const BookingDetailArgs(orderId: '');

  static CouponSelectorArgs _couponSelectorArgs(Object? args) =>
      args is CouponSelectorArgs
      ? args
      : const CouponSelectorArgs(orderAmount: 0);
}
