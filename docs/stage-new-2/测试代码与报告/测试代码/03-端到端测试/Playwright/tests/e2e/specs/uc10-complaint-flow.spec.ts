import { expect, test } from '@playwright/test';
import { E2eApi } from '../support/api-client';
import {
  createTakeawayOrder,
  loginAdminPage,
  uniqueText,
} from '../support/scenarios';

interface ComplaintView {
  id: number;
  ticketNo: string;
  orderId: number;
  orderNo: string;
  title: string;
  status: string;
}

interface ComplaintDetail {
  complaint: ComplaintView;
  logs: Array<{
    action: string;
    remark?: string;
  }>;
}

test('UC10 用户投诉由平台受理、处理并关闭', async ({ page, request }) => {
  const api = new E2eApi(request);
  const user = await api.loginUser();
  const marker = uniqueText('UC10');
  const order = await createTakeawayOrder(api, user.token, marker);
  const title = `${marker} 投诉标题`;

  const complaint = await api.post<ComplaintView>('/api/app/complaints', {
    token: user.token,
    data: {
      orderId: order.id,
      category: 'service',
      title,
      detail: `${marker} 投诉详情，请平台跟进`,
      evidenceUrls: [],
    },
  });
  expect(complaint.status).toBe('pending');

  await loginAdminPage(page);
  await page.getByRole('button', { name: /投诉工单/ }).click();
  const row = page.getByTestId(`admin-complaint-${complaint.id}`);
  await expect(row).toContainText(title);
  await row.click();

  const remark = page.getByLabel('备注', { exact: true });
  const acceptRemark = `${marker} 已核对订单并受理`;
  await remark.fill(acceptRemark);
  await page.getByRole('button', { name: '受理', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('已受理');

  const resolveRemark = `${marker} 已完成处理`;
  await remark.fill(resolveRemark);
  await page.getByRole('button', { name: '处理完成', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('已处理完成');

  const closeRemark = `${marker} 用户确认后关闭`;
  await remark.fill(closeRemark);
  await page.getByRole('button', { name: '关闭', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('已关闭');

  const detail = await api.get<ComplaintDetail>(`/api/app/complaints/${complaint.id}`, {
    token: user.token,
  });
  expect(detail.complaint.status).toBe('closed');
  expect(detail.logs.map((log) => log.action)).toEqual(['submit', 'accept', 'resolve', 'close']);
  expect(detail.logs).toEqual(expect.arrayContaining([
    expect.objectContaining({ action: 'accept', remark: acceptRemark }),
    expect.objectContaining({ action: 'resolve', remark: resolveRemark }),
    expect.objectContaining({ action: 'close', remark: closeRemark }),
  ]));
});
