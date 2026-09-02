package com.aituan.identity.coupon;

import com.aituan.common.exception.BusinessException;
import com.aituan.common.exception.ErrorCode;
import com.aituan.common.security.CurrentUserContext;
import com.aituan.identity.client.IdentityAuditClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
  private final CouponRepository couponRepository;
  private final IdentityAuditClient identityAuditClient;

  CouponService(CouponRepository couponRepository, IdentityAuditClient identityAuditClient) {
    this.couponRepository = couponRepository;
    this.identityAuditClient = identityAuditClient;
  }

  // ===== 用户端 =====

  // status: usable / used / expired
  @Transactional
  List<UserCouponView> myCoupons(String status) {
    long userId = CurrentUserContext.required().userId();
    refreshWeeklyMemberCoupons(userId);
    couponRepository.expireOverdue(userId);
    String dbStatus = switch (status == null ? "usable" : status) {
      case "used" -> "used";
      case "expired" -> "expired";
      default -> "unused";
    };
    return couponRepository.listUserCoupons(userId, dbStatus).stream().map(this::toUserCouponView).toList();
  }

  List<AvailableCouponView> availableTemplates() {
    long userId = CurrentUserContext.required().userId();
    return couponRepository.listClaimableTemplates().stream().map(t -> {
      long claimed = couponRepository.countUserCouponsByTemplate(userId, t.id());
      boolean claimable = claimed < t.perUserLimit();
      Integer remaining = t.totalQty() > 0 ? Math.max(0, t.totalQty() - t.issuedQty()) : null;
      return new AvailableCouponView(t.id(), t.name(), t.type(), t.faceValue(), t.thresholdAmount(),
          discountDesc(t.type(), t.faceValue()), thresholdDesc(t.thresholdAmount()), validDesc(t),
          remaining, claimable, claimable ? null : "已达领取上限");
    }).toList();
  }

  @Transactional
  void claim(long templateId) {
    long userId = CurrentUserContext.required().userId();
    CouponRepository.CouponTemplateRow t = couponRepository.findTemplateForUpdate(templateId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if ("member_weekly".equals(t.businessScope())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "会员周券会自动发放，无需手动领取");
    }
    if (!"enabled".equals(t.status())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "优惠券已下架");
    }
    if (t.validEnd() != null && t.validEnd().isBefore(LocalDateTime.now())) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "优惠券活动已结束");
    }
    if (t.totalQty() > 0 && t.issuedQty() >= t.totalQty()) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "优惠券已领完");
    }
    if (couponRepository.countUserCouponsByTemplate(userId, templateId) >= t.perUserLimit()) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "已达领取上限");
    }
    if (couponRepository.incrementIssuedIfAvailable(templateId) == 0) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "优惠券已领完");
    }
    couponRepository.insertUserCoupon(userId, t, computeExpireAt(t));
  }

  // 下单时可用券（按订单金额试算每张抵扣）
  List<OrderCouponOptionView> usableForOrder(BigDecimal orderAmount) {
    long userId = CurrentUserContext.required().userId();
    couponRepository.expireOverdue(userId);
    return couponRepository.listUserCoupons(userId, "unused").stream().map(uc -> {
      BigDecimal discount = computeDiscount(uc.typeSnapshot(), uc.faceValueSnapshot(), uc.thresholdSnapshot(), orderAmount);
      boolean usable = discount.signum() > 0;
      return new OrderCouponOptionView(uc.id(), uc.templateName(),
          discountDesc(uc.typeSnapshot(), uc.faceValueSnapshot()), usable ? discount : ZERO, usable,
          usable ? null : "未达门槛或不适用");
    }).toList();
  }

  // ===== 供其他业务模块跨包调用 =====

  @Transactional
  public void refreshWeeklyMemberCoupons(long userId) {
    couponRepository.expireOverdue(userId);
    String levelCode = couponRepository.findCurrentLevelCode(userId).orElse(null);
    if (levelCode == null) {
      return;
    }
    LocalDate weekStart = currentWeekStart();
    long batchId = getOrCreateWeeklyBatch(userId, weekStart, levelCode);
    for (CouponRepository.WeeklyCouponRuleRow rule : couponRepository.listWeeklyRules(levelCode)) {
      for (int seq = 1; seq <= rule.issueQuantity(); seq++) {
        if (!couponRepository.insertWeeklyIssue(batchId, rule.id(), seq)) {
          continue;
        }
        issueWeeklyCoupon(userId, batchId, rule, seq);
      }
    }
  }

  // 试算某券对订单金额的抵扣
  public CouponCalcResult calcDiscount(Long userId, Long userCouponId, BigDecimal orderAmount) {
    if (userId == null || userCouponId == null) {
      return new CouponCalcResult(false, ZERO, "未选择优惠券");
    }
    return couponRepository.findUserCoupon(userId, userCouponId)
        .map(uc -> {
          if (!"unused".equals(uc.status())) {
            return new CouponCalcResult(false, ZERO, "优惠券不可用");
          }
          if (uc.expireAt() != null && uc.expireAt().isBefore(LocalDateTime.now())) {
            return new CouponCalcResult(false, ZERO, "优惠券已过期");
          }
          BigDecimal discount = computeDiscount(uc.typeSnapshot(), uc.faceValueSnapshot(), uc.thresholdSnapshot(), orderAmount);
          if (discount.signum() <= 0) {
            return new CouponCalcResult(false, ZERO, "未达使用门槛");
          }
          return new CouponCalcResult(true, discount, null);
        })
        .orElse(new CouponCalcResult(false, ZERO, "优惠券不存在"));
  }

  // 下单成功后核销
  @Transactional
  public void redeem(Long userCouponId, Long orderId) {
    if (userCouponId == null) {
      return;
    }
    int rows = couponRepository.markUsed(userCouponId, orderId);
    if (rows == 0) {
      throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "优惠券已被使用或不可用");
    }
  }

  // 订单取消/超时关闭时回退
  @Transactional
  public void release(Long userCouponId) {
    if (userCouponId != null) {
      couponRepository.markUnused(userCouponId);
    }
  }

  @Transactional
  public void releaseByOrder(Long orderId) {
    if (orderId != null) {
      couponRepository.markUnusedByOrder(orderId);
    }
  }

  // ===== 后台 =====

  List<CouponTemplateView> listTemplates() {
    return couponRepository.listAllTemplates().stream().map(this::toTemplateView).toList();
  }

  @Transactional
  CouponTemplateView createTemplate(CouponTemplateUpsertRequest request) {
    Long actorId = CurrentUserContext.required().accountId();
    Long id = couponRepository.insertTemplate(request, normalizeQty(request.totalQty(), 0),
        normalizeQty(request.perUserLimit(), 1), normalizeScope(request.businessScope()), normalizeStatus(request.status()));
    identityAuditClient.publish(actorId, "coupon_template_create", "coupon_template", id, "新增优惠券模板:" + request.name());
    return couponRepository.findTemplate(id).map(this::toTemplateView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  @Transactional
  CouponTemplateView updateTemplate(long id, CouponTemplateUpsertRequest request) {
    Long actorId = CurrentUserContext.required().accountId();
    couponRepository.findTemplate(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    couponRepository.updateTemplate(id, request, normalizeQty(request.totalQty(), 0),
        normalizeQty(request.perUserLimit(), 1), normalizeScope(request.businessScope()), normalizeStatus(request.status()));
    identityAuditClient.publish(actorId, "coupon_template_update", "coupon_template", id, "更新优惠券模板:" + request.name());
    return couponRepository.findTemplate(id).map(this::toTemplateView)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
  }

  // ===== 内部计算与转换 =====

  private long getOrCreateWeeklyBatch(long userId, LocalDate weekStart, String levelCode) {
    Optional<Long> existing = couponRepository.findWeeklyBatch(userId, weekStart);
    if (existing.isPresent()) {
      return existing.get();
    }
    try {
      return couponRepository.insertWeeklyBatch(userId, weekStart, levelCode);
    } catch (DuplicateKeyException ignored) {
      return couponRepository.findWeeklyBatch(userId, weekStart).orElseThrow();
    }
  }

  private void issueWeeklyCoupon(long userId, long batchId, CouponRepository.WeeklyCouponRuleRow rule, int seq) {
    CouponRepository.CouponTemplateRow template = couponRepository.findTemplateForUpdate(rule.templateId()).orElse(null);
    if (template == null || !"enabled".equals(template.status())) {
      couponRepository.deleteWeeklyIssue(batchId, rule.id(), seq);
      return;
    }
    if (couponRepository.incrementIssuedIfAvailable(template.id()) == 0) {
      couponRepository.deleteWeeklyIssue(batchId, rule.id(), seq);
      return;
    }
    Long userCouponId = couponRepository.insertUserCoupon(userId, template, LocalDateTime.now().plusDays(7));
    couponRepository.attachWeeklyIssueCoupon(batchId, rule.id(), seq, userCouponId);
  }

  private LocalDate currentWeekStart() {
    LocalDate today = LocalDate.now();
    return today.minusDays(today.getDayOfWeek().getValue() - 1L);
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

  private LocalDateTime computeExpireAt(CouponRepository.CouponTemplateRow t) {
    if ("relative".equals(t.validKind())) {
      int days = t.validDays() == null ? 30 : t.validDays();
      return LocalDateTime.now().plusDays(days);
    }
    return t.validEnd() != null ? t.validEnd() : LocalDateTime.now().plusYears(1);
  }

  private UserCouponView toUserCouponView(CouponRepository.UserCouponRow r) {
    return new UserCouponView(r.id(), r.templateId(), r.templateName(), r.typeSnapshot(), r.faceValueSnapshot(),
        r.thresholdSnapshot(), r.status(), discountDesc(r.typeSnapshot(), r.faceValueSnapshot()),
        thresholdDesc(r.thresholdSnapshot()), r.claimedAt(), r.expireAt(), r.usedAt(), r.usedOrderId());
  }

  private CouponTemplateView toTemplateView(CouponRepository.CouponTemplateRow t) {
    return new CouponTemplateView(t.id(), t.name(), t.type(), t.faceValue(), t.thresholdAmount(), t.businessScope(),
        t.validKind(), t.validStart(), t.validEnd(), t.validDays(), t.totalQty(), t.issuedQty(), t.perUserLimit(), t.status());
  }

  private String discountDesc(String type, BigDecimal faceValue) {
    if (faceValue == null) {
      return "";
    }
    if ("discount".equals(type)) {
      return faceValue.multiply(BigDecimal.TEN).stripTrailingZeros().toPlainString() + "折";
    }
    return "减" + faceValue.stripTrailingZeros().toPlainString() + "元";
  }

  private String thresholdDesc(BigDecimal threshold) {
    if (threshold == null || threshold.signum() <= 0) {
      return "无门槛";
    }
    return "满" + threshold.stripTrailingZeros().toPlainString() + "可用";
  }

  private String validDesc(CouponRepository.CouponTemplateRow t) {
    if ("relative".equals(t.validKind())) {
      int days = t.validDays() == null ? 30 : t.validDays();
      return "领取后" + days + "天内有效";
    }
    return t.validEnd() == null ? "长期有效" : "有效期至" + t.validEnd().toLocalDate();
  }

  private int normalizeQty(Integer value, int defaultValue) {
    return value == null || value < 0 ? defaultValue : value;
  }

  private String normalizeScope(String scope) {
    return scope == null || scope.isBlank() ? "all" : scope.trim();
  }

  private String normalizeStatus(String status) {
    return status == null || status.isBlank() ? "enabled" : status.trim();
  }
}
