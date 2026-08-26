import { expect, test } from '@playwright/test';
import type { PageResponse } from '../support/api-client';
import { E2eApi } from '../support/api-client';
import {
  completeTakeawayOrder,
  createTakeawayOrder,
  demo,
  loginAdminPage,
  loginMerchantPage,
  payOrder,
  uniqueText,
} from '../support/scenarios';

interface ReviewView {
  id: number;
  orderId: number;
  content: string;
  status: string;
  replied: boolean;
  replyContent?: string;
}

test('UC09 用户评价、商家回复与平台治理形成闭环', async ({ page, request }) => {
  const api = new E2eApi(request);
  const user = await api.loginUser();
  const merchant = await api.loginMerchant(demo.takeawayMerchant);
  const marker = uniqueText('UC09');
  const reviewContent = `${marker} 用户评价内容`;
  const replyContent = `${marker} 商家回复内容`;

  const created = await createTakeawayOrder(api, user.token, marker);
  await payOrder(api, user.token, created.id);
  const completed = await completeTakeawayOrder(api, merchant.token, created.id);
  expect(completed.displayStatus).toBe('used');
  expect(completed.fulfillmentStatus).toBe('completed');

  const review = await api.post<ReviewView>(`/api/app/interaction/orders/${created.id}/review`, {
    token: user.token,
    data: {
      rating: 5,
      content: reviewContent,
      labels: ['端到端测试', '服务满意'],
      imageUrls: [],
    },
  });
  expect(review.status).toBe('published');

  await loginMerchantPage(page, demo.takeawayMerchant);
  await page.getByRole('button', { name: /评价管理/ }).click();
  const merchantRow = page.getByTestId(`merchant-review-${review.id}`);
  await expect(merchantRow).toContainText(reviewContent);
  await merchantRow.click();
  await page.getByPlaceholder('感谢您的反馈…').fill(replyContent);
  await page.getByRole('button', { name: '发送回复', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('回复已发送');

  const replied = await api.get<ReviewView>(`/api/app/interaction/reviews/${review.id}`, {
    token: user.token,
  });
  expect(replied.replied).toBe(true);
  expect(replied.replyContent).toBe(replyContent);

  await loginAdminPage(page);
  await page.getByRole('button', { name: /评价审核/ }).click();
  const adminRow = page.getByTestId(`admin-review-${review.id}`);
  await expect(adminRow).toContainText(reviewContent);
  await adminRow.click();
  await page.getByLabel('审核备注（可选）').fill(`${marker} 屏蔽回归`);
  await page.getByRole('button', { name: '屏蔽', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('已屏蔽');

  const hidden = await api.get<ReviewView>(`/api/app/interaction/reviews/${review.id}`, {
    token: user.token,
  });
  expect(hidden.status).toBe('hidden');
  const hiddenPublic = await api.get<PageResponse<ReviewView>>('/api/app/interaction/stores/1/reviews', {
    token: user.token,
    params: { pageSize: 50 },
  });
  expect(hiddenPublic.list.some((item) => item.id === review.id)).toBe(false);

  await page.getByRole('button', { name: '恢复', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('已恢复');
  const restoredPublic = await api.get<PageResponse<ReviewView>>('/api/app/interaction/stores/1/reviews', {
    token: user.token,
    params: { pageSize: 50 },
  });
  expect(restoredPublic.list).toContainEqual(expect.objectContaining({ id: review.id, status: 'published' }));
});
