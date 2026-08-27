import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';
import { clickAdminNav, clickMerchantNav, loginAdminWeb, loginMerchantWeb } from '../support/vue-web.js';

test.describe('UC11 用户联系客服，商家与平台客服回复', () => {
  test('用户发起客服会话，商家回复，平台人工介入回复', async ({ browser }) => {
    const api = newApi();
    await api.login('demo_user', '123456');
    const question = `请问配送多久到-${Date.now()}`;
    const session = await api.createSupportSession(1, '商家客服咨询');
    await api.sendSupportMessage(session.id, question);

    const merchantPage = await browser.newPage();
    await loginMerchantWeb(merchantPage, 'demo_merchant', '123456');
    await clickMerchantNav(merchantPage, '客服会话');
    await merchantPage.locator('button.session-row').first().click();
    await merchantPage.getByPlaceholder('输入消息…').fill('正在为您配送，请稍候');
    await merchantPage.getByRole('button', { name: '发送' }).click();
    await expect(merchantPage.getByText('正在为您配送，请稍候').first()).toBeVisible({
      timeout: 30_000,
    });
    await merchantPage.close();

    const afterMerchant = await api.supportSessionDetail(session.id);
    expect(
      afterMerchant.messages.some(
        (m) => m.senderType === 'merchant' && m.content.includes('正在为您配送'),
      ),
    ).toBe(true);

    await api.requestPlatformIntervention(session.id);
    const adminPage = await browser.newPage();
    await loginAdminWeb(adminPage, 'demo_admin', '123456');
    await clickAdminNav(adminPage, '平台客服');
    await adminPage
      .locator('button.session-row', { hasText: '用户已申请平台客服介入' })
      .first()
      .click();
    await adminPage.getByPlaceholder('输入平台人工回复…').fill('平台客服已介入处理');
    await adminPage.getByRole('button', { name: '发送' }).click();
    await expect(adminPage.getByText('平台客服已介入处理').first()).toBeVisible({
      timeout: 30_000,
    });
    await adminPage.close();

    const finalDetail = await api.supportSessionDetail(session.id);
    expect(
      finalDetail.messages.some(
        (m) => m.senderType === 'platform' && m.content.includes('平台客服已介入处理'),
      ),
    ).toBe(true);
  });
});
