import { expect, test } from '@playwright/test';
import { E2eApi } from '../support/api-client';
import {
  demo,
  loginAdminPage,
  loginMerchantPage,
  uniqueText,
} from '../support/scenarios';

interface SupportSession {
  id: number;
  sessionNo: string;
  status: string;
  serviceScope: string;
  assistantMode: string;
  platformInterventionStatus: string;
}

interface SupportMessage {
  id: number;
  senderType: string;
  content: string;
  messageKind: string;
}

interface SupportDetail {
  session: SupportSession;
  messages: SupportMessage[];
}

test.describe('UC11 客服、AI 与平台介入闭环', () => {
  test('平台 AI 降级接待后转平台人工回复', async ({ page, request }) => {
    const api = new E2eApi(request);
    const user = await api.loginUser();
    const marker = uniqueText('UC11-AI');

    const session = await api.post<SupportSession>('/api/app/support/sessions', {
      token: user.token,
      data: { storeId: null, topic: marker, relatedOrderId: null },
    });
    expect(session.serviceScope).toBe('platform');
    expect(session.assistantMode).toBe('ai');

    await api.post<SupportMessage>(`/api/app/support/sessions/${session.id}/messages`, {
      token: user.token,
      data: { content: `${marker} 请帮我查询优惠券和退款规则` },
    });
    const aiDetail = await api.get<SupportDetail>(`/api/app/support/sessions/${session.id}`, {
      token: user.token,
    });
    expect(aiDetail.session.assistantMode).toBe('ai');
    expect(aiDetail.messages).toEqual(expect.arrayContaining([
      expect.objectContaining({ senderType: 'platform', messageKind: 'auto_reply' }),
    ]));

    const handedOff = await api.post<SupportSession>(`/api/app/support/sessions/${session.id}/handoff`, {
      token: user.token,
    });
    expect(handedOff.assistantMode).toBe('human');

    const platformReply = `${marker} 平台人工回复`;
    await loginAdminPage(page);
    await page.getByRole('button', { name: /平台客服/ }).click();
    const adminRow = page.getByTestId(`admin-support-${session.id}`);
    await expect(adminRow).toBeVisible();
    await adminRow.click();
    await page.getByPlaceholder('输入平台人工回复…').fill(platformReply);
    await page.getByRole('button', { name: '发送', exact: true }).click();
    await expect(page.locator('.notice-bar')).toContainText('平台人工回复已发送');

    const finalDetail = await api.get<SupportDetail>(`/api/app/support/sessions/${session.id}`, {
      token: user.token,
    });
    expect(finalDetail.session.assistantMode).toBe('human');
    expect(finalDetail.messages).toContainEqual(expect.objectContaining({
      senderType: 'platform',
      content: platformReply,
      messageKind: 'text',
    }));
  });

  test('商家客服回复后申请平台介入并继续处理', async ({ page, request }) => {
    const api = new E2eApi(request);
    const user = await api.loginUser();
    const marker = uniqueText('UC11-MERCHANT');
    const userMessage = `${marker} 想确认包装备注`;
    const merchantReply = `${marker} 商家客服回复`;
    const platformReply = `${marker} 平台介入回复`;

    const session = await api.post<SupportSession>('/api/app/support/sessions', {
      token: user.token,
      data: { storeId: 1, topic: marker, relatedOrderId: 9001 },
    });
    await api.post<SupportMessage>(`/api/app/support/sessions/${session.id}/messages`, {
      token: user.token,
      data: { content: userMessage },
    });

    await loginMerchantPage(page, demo.takeawayMerchant);
    await page.getByRole('button', { name: /客服会话/ }).click();
    const merchantRow = page.getByTestId(`merchant-support-${session.id}`);
    await expect(merchantRow).toContainText(userMessage);
    await merchantRow.click();
    await page.getByPlaceholder('输入消息…').fill(merchantReply);
    await page.getByRole('button', { name: '发送', exact: true }).click();
    await expect(page.locator('.message-list')).toContainText(merchantReply);

    page.once('dialog', (dialog) => dialog.accept());
    await page.getByRole('button', { name: '平台介入', exact: true }).click();
    await expect(page.locator('.notice-bar')).toContainText('平台客服已介入');

    await loginAdminPage(page);
    await page.getByRole('button', { name: /平台客服/ }).click();
    const adminRow = page.getByTestId(`admin-support-${session.id}`);
    await expect(adminRow).toBeVisible();
    await adminRow.click();
    await page.getByPlaceholder('输入平台人工回复…').fill(platformReply);
    await page.getByRole('button', { name: '发送', exact: true }).click();
    await expect(page.locator('.notice-bar')).toContainText('平台人工回复已发送');

    const detail = await api.get<SupportDetail>(`/api/app/support/sessions/${session.id}`, {
      token: user.token,
    });
    expect(detail.session.serviceScope).toBe('platform');
    expect(detail.session.assistantMode).toBe('human');
    expect(detail.session.platformInterventionStatus).toBe('active');
    expect(detail.messages).toEqual(expect.arrayContaining([
      expect.objectContaining({ senderType: 'user', content: userMessage }),
      expect.objectContaining({ senderType: 'merchant', content: merchantReply }),
      expect.objectContaining({ senderType: 'platform', content: platformReply }),
    ]));
  });
});
