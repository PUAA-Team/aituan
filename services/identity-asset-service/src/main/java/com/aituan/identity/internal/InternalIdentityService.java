package com.aituan.identity.internal;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class InternalIdentityService {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private final InternalIdentityRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final String merchantDefaultPassword;

  InternalIdentityService(
      InternalIdentityRepository repository,
      PasswordEncoder passwordEncoder,
      @Value("${aituan.merchant.default-password:}") String merchantDefaultPassword) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.merchantDefaultPassword = merchantDefaultPassword;
  }

  UserSummaryView userSummary(long userId) {
    InternalIdentityRepository.UserSummaryRow row = repository.findUserSummary(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return new UserSummaryView(row.userId(), row.accountId(), row.nickname(), row.avatarUrl(), row.phone(), row.email(), row.status(), row.memberLevelName());
  }

  AddressSnapshotView addressSnapshot(long userId, long addressId) {
    return repository.findAddressSnapshot(userId, addressId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  UserHomeSummaryView homeSummary(long userId) {
    InternalIdentityRepository.UserSummaryRow row = repository.findUserSummary(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return new UserHomeSummaryView(row.userId(), row.nickname(), row.memberLevelName(), row.growthValue(),
        repository.countUnreadMessages(userId), repository.countUsableCoupons(userId));
  }

  List<PreferenceSignalView> preferenceSignals(long userId) {
    repository.findUserSummary(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    return repository.listPreferenceSignals(userId);
  }

  CouponQuoteView quote(CouponQuoteRequest request, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    if (request.couponId() == null) {
      return new CouponQuoteView(false, ZERO, "未选择优惠券");
    }
    return repository.findUserCoupon(request.userId(), request.couponId())
        .map(coupon -> quoteCoupon(coupon, request.orderAmount()))
        .orElse(new CouponQuoteView(false, ZERO, "优惠券不存在"));
  }

  @Transactional
  CouponCommandResult useCoupon(long couponId, CouponUseRequest request, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    InternalIdentityRepository.UserCouponRow coupon = repository.findUserCoupon(request.userId(), couponId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if ("used".equals(coupon.status()) && request.orderId().equals(coupon.usedOrderId())) {
      return new CouponCommandResult(true, "used", "重复请求已按原结果返回");
    }
    CouponQuoteView quoted = quoteCoupon(coupon, request.orderAmount());
    if (!quoted.usable()) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, quoted.reason());
    }
    int updated = repository.markCouponUsed(request.userId(), couponId, request.orderId());
    if (updated == 0) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "优惠券已被使用或不可用");
    }
    return new CouponCommandResult(true, "used", "优惠券已使用");
  }

  @Transactional
  CouponCommandResult releaseCoupon(long couponId, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    repository.releaseCoupon(couponId);
    return new CouponCommandResult(true, "unused", "优惠券已恢复");
  }

  @Transactional
  CouponCommandResult addGrowth(long userId, MemberGrowthRequest request, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    repository.findUserSummary(userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (!repository.insertGrowthLog(userId, request.sourceType().trim(), request.sourceId(), request.delta(), request.reason())) {
      return new CouponCommandResult(true, "unchanged", "重复成长值命令已忽略");
    }
    int growth = repository.changeGrowth(userId, request.delta());
    repository.currentLevel(growth).ifPresent(level -> repository.updateMemberLevelName(userId, level.levelName()));
    return new CouponCommandResult(true, "changed", "成长值已更新");
  }

  @Transactional
  CouponCommandResult publishMessage(MessagePublishRequest request, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    repository.findUserSummary(request.userId()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    repository.insertStationMessage(request, idempotencyKey);
    return new CouponCommandResult(true, "created", "站内消息已发布");
  }

  @Transactional
  MerchantAccountProvisionView provisionMerchantAccount(MerchantAccountProvisionRequest request, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    String loginName = request.loginName().trim();
    return repository.findAccountByLogin(loginName)
        .map(row -> new MerchantAccountProvisionView(
            true, row.accountId(), row.accountNo(), row.loginName(), row.status(), "商家账号已存在，按原结果返回"))
        .orElseGet(() -> {
          if (merchantDefaultPassword == null || merchantDefaultPassword.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "商家初始密码未配置");
          }
          long accountId = repository.insertMerchantAccount(
              loginName, null, null, passwordEncoder.encode(merchantDefaultPassword));
          InternalIdentityRepository.AccountRow created = repository.findAccountByLogin(loginName).orElseThrow();
          return new MerchantAccountProvisionView(
              true, accountId, created.accountNo(), created.loginName(), created.status(), "商家账号已创建");
        });
  }

  @Transactional
  CouponCommandResult deactivateMerchantAccount(long accountId, String idempotencyKey) {
    requireIdempotencyKey(idempotencyKey);
    repository.deactivateMerchantAccount(accountId);
    return new CouponCommandResult(true, "disabled", "商家账号已停用");
  }

  PlatformUserMetricsView userMetrics() {
    return repository.userMetrics();
  }

  private CouponQuoteView quoteCoupon(InternalIdentityRepository.UserCouponRow coupon, BigDecimal orderAmount) {
    if (!"unused".equals(coupon.status())) {
      return new CouponQuoteView(false, ZERO, "优惠券不可用");
    }
    if (coupon.expireAt() != null && coupon.expireAt().isBefore(LocalDateTime.now())) {
      return new CouponQuoteView(false, ZERO, "优惠券已过期");
    }
    BigDecimal discount = computeDiscount(coupon.type(), coupon.faceValue(), coupon.thresholdAmount(), orderAmount);
    if (discount.signum() <= 0) {
      return new CouponQuoteView(false, ZERO, "未达使用门槛");
    }
    return new CouponQuoteView(true, discount, null);
  }

  private BigDecimal computeDiscount(String type, BigDecimal faceValue, BigDecimal threshold, BigDecimal orderAmount) {
    if (orderAmount == null || faceValue == null) {
      return ZERO;
    }
    if ("full_reduction".equals(type)) {
      BigDecimal limit = threshold == null ? BigDecimal.ZERO : threshold;
      if (orderAmount.compareTo(limit) >= 0) {
        return faceValue.min(orderAmount).setScale(2, RoundingMode.HALF_UP);
      }
      return ZERO;
    }
    if ("discount".equals(type)) {
      return orderAmount.multiply(BigDecimal.ONE.subtract(faceValue)).setScale(2, RoundingMode.HALF_UP).max(ZERO);
    }
    return ZERO;
  }

  private void requireIdempotencyKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "内部写接口必须提供 Idempotency-Key");
    }
  }
}
