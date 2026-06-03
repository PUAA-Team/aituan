package com.aituan.coupon;

import java.math.BigDecimal;

// 优惠券抵扣试算结果，供成员C的交易(trade)模块跨包调用，故为 public
public record CouponCalcResult(boolean usable, BigDecimal discountAmount, String reason) {}
