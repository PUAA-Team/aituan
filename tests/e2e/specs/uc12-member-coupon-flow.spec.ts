import { expect, test } from '@playwright/test';
import type { OrderDetail } from '../support/api-client';
import { E2eApi } from '../support/api-client';
import {
  createServiceOrder,
  demo,
  payOrder,
  uniqueText,
} from '../support/scenarios';

interface CouponTemplate {
  id: number;
  name: string;
  status: string;
}

interface UserCoupon {
  id: number;
  templateId: number;
  name: string;
  status: string;
  usedOrderId?: number;
}

interface CouponOption {
  userCouponId: number;
  name: string;
  discountAmount: number;
  usable: boolean;
}

interface MemberInfo {
  currentLevelCode: string;
  currentLevelName: string;
  growthValue: number;
}

test('UC12 查看会员、领取优惠券、下单抵扣并获得成长值', async ({ request }) => {
  const api = new E2eApi(request);
  const user = await api.loginUser();
  const admin = await api.loginAdmin();
  const merchant = await api.loginMerchant(demo.groupBuyMerchant);
  const marker = uniqueText('UC12');
  const couponName = `${marker} 满50减12`;

  const template = await api.post<CouponTemplate>('/api/admin/operation/coupon-templates', {
    token: admin.token,
    data: {
      name: couponName,
      type: 'full_reduction',
      faceValue: 12,
      thresholdAmount: 50,
      businessScope: 'all',
      validKind: 'relative',
      validDays: 7,
      totalQty: 10,
      perUserLimit: 1,
      status: 'enabled',
    },
  });
  expect(template.status).toBe('enabled');

  const memberBefore = await api.get<MemberInfo>('/api/app/account/member/info', { token: user.token });
  await api.post<null>(`/api/app/account/coupons/${template.id}/claim`, { token: user.token });

  const coupons = await api.get<UserCoupon[]>('/api/app/account/coupons', {
    token: user.token,
    params: { status: 'usable' },
  });
  const claimed = coupons.find((coupon) => coupon.templateId === template.id);
  expect(claimed).toEqual(expect.objectContaining({ name: couponName, status: 'unused' }));

  const options = await api.get<CouponOption[]>('/api/app/account/coupons/usable-for-order', {
    token: user.token,
    params: { orderAmount: 98 },
  });
  const usable = options.find((option) => option.userCouponId === claimed?.id);
  expect(usable).toEqual(expect.objectContaining({ usable: true, discountAmount: 12 }));

  const created = await createServiceOrder(api, user.token, marker, { couponId: claimed?.id });
  const paid = await payOrder(api, user.token, created.id);
  expect(paid.discountAmount).toBe(12);
  expect(paid.payableAmount).toBe(86);
  expect(paid.voucher?.status).toBe('unused');

  const usedCoupons = await api.get<UserCoupon[]>('/api/app/account/coupons', {
    token: user.token,
    params: { status: 'used' },
  });
  expect(usedCoupons).toContainEqual(expect.objectContaining({
    id: claimed?.id,
    status: 'used',
    usedOrderId: created.id,
  }));

  const completed = await api.post<OrderDetail>(
    `/api/merchant/trade/vouchers/${encodeURIComponent(paid.voucher!.voucherCode)}/redeem`,
    { token: merchant.token },
  );
  expect(completed.displayStatus).toBe('used');
  expect(completed.voucher?.status).toBe('used');

  const memberAfter = await api.get<MemberInfo>('/api/app/account/member/info', { token: user.token });
  expect(memberAfter.growthValue).toBe(memberBefore.growthValue + Math.floor(paid.payableAmount));
});
