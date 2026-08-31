import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';
import {
  expectFlutterRole,
  expectFlutterText,
  loginViaFlutterWeb,
  openUserWeb,
} from '../support/flutter-web.js';

test.describe('UC01 用户注册、登录并进入受保护业务', () => {
  test('注册新账号后通过用户端 Web 登录，并可访问受保护业务', async ({ page }) => {
    const api = newApi();
    const { phone, password } = await api.createUser('uc01');
    const profile = await api.profile();
    const nickname = profile.nickname ?? phone;

    const tokenCheck = await fetch(`${api.origin}/api/app/trade/orders`, {
      headers: { accept: 'application/json' },
    });
    expect(tokenCheck.status).toBe(401);

    await openUserWeb(page);
    await loginViaFlutterWeb(page, phone, password);
    await expectFlutterText(page, '外卖');

    await page.getByRole('tab').nth(2).click();
    await expectFlutterText(page, '我的订单');
    const hasNoOrders = await page.getByText('暂无订单', { exact: false }).count();
    if (hasNoOrders === 0) {
      await expectFlutterText(page, '待付款');
    }

    await page.getByRole('tab').nth(3).click();
    await expectFlutterRole(page, 'group', nickname);
    await expectFlutterRole(page, 'button', '退出');

    const orders = await api.orders();
    expect(orders.list).toBeInstanceOf(Array);
    const profileAgain = await api.profile();
    expect(profileAgain.nickname).toBe(nickname);
  });
});
