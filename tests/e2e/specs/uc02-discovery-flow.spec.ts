import { expect, test } from '@playwright/test';
import { newApi, type StoreCard } from '../support/api-client.js';
import { expectFlutterRole, expectFlutterText, openUserWeb } from '../support/flutter-web.js';

test.describe('UC02 用户搜索筛选商家/商品并查看详情', () => {
  test('游客从用户端 Web 搜索并筛选商家，打开商家详情和商品详情', async ({ page }) => {
    const api = newApi();
    const home = await api.home();
    expect(home.modules?.length).toBeGreaterThan(0);
    expect(home.recommendations?.list?.length).toBeGreaterThan(0);

    const search = await api.search('塔斯汀');
    expect(search.list.some((s: StoreCard) => s.name === '塔斯汀中国汉堡')).toBe(true);

    const filtered = await api.search('塔', '&businessType=takeaway');
    expect(filtered.list.every((s: StoreCard) => !s.businessType || s.businessType === 'takeaway')).toBe(true);

    const store = await api.storeDetail(1);
    expect(store.store?.name).toContain('塔斯汀');
    expect(store.itemGroups?.length).toBeGreaterThan(0);
    expect(store.itemGroups?.some((g) => g.items && g.items.length > 0)).toBe(true);

    await openUserWeb(page);
    await expectFlutterText(page, '爱团');
    await page.getByText('游客进入', { exact: true }).first().click();

    await page.getByText('搜索外卖、团购、景点、洗脚', { exact: false }).first().click();
    await page.getByRole('textbox', { name: /汉堡|洗脚|双人套餐/ }).first().fill('塔斯汀');
    await page.getByRole('button', { name: '搜索', exact: true }).click();
    await expectFlutterRole(page, 'group', /塔斯汀中国汉堡/);

    await page.getByRole('group', { name: /塔斯汀中国汉堡/ }).click();
    await expectFlutterText(page, '藤椒鸡腿堡');
  });
});
