import { expect, test } from '@playwright/test';
import { newApi, type AddressView } from '../support/api-client.js';
import {
  expectFlutterRole,
  expectFlutterText,
  loginViaFlutterWeb,
  openUserWeb,
} from '../support/flutter-web.js';

test.describe('UC03 用户维护个人资料、地址和收藏', () => {
  test('登录后维护资料、地址和收藏，并在用户端展示', async ({ page }) => {
    const api = newApi();
    const { phone, password } = await api.createUser('uc03');
    const nickname = `E2E用户${Date.now() % 100000}`;

    await api.updateProfile(nickname, '');
    const address = await api.createAddress({
      contactName: 'E2E 收货人',
      detailAddress: '端到端测试路 8 号',
    });
    await api.saveFavorite('store', 1, '塔斯汀中国汉堡', '现烤汉堡');

    const profile = await api.profile();
    expect(profile.nickname).toBe(nickname);
    const addresses = await api.addresses();
    expect(addresses.some((a: AddressView) => a.id === address.id)).toBe(true);
    const favorites = await api.favorites();
    expect(
      favorites.list.some((f: { favoriteType: string; targetId: number }) => f.favoriteType === 'store' && f.targetId === 1),
    ).toBe(true);

    await openUserWeb(page);
    await loginViaFlutterWeb(page, phone, password);

    await page.getByRole('tab').nth(3).click();
    await expectFlutterRole(page, 'group', nickname);

    await page.getByText('地址管理', { exact: true }).first().click();
    await expectFlutterRole(page, 'group', /端到端测试路 8 号/);

    await page.getByRole('button', { name: 'Back' }).click();
    await expectFlutterRole(page, 'group', nickname);
    await page.getByRole('button', { name: /收藏/ }).click();
    await expectFlutterRole(page, 'button', /塔斯汀中国汉堡/);

    await api.deleteAddress(address.id);
    await api.deleteFavorite('store', 1);
  });
});
