import { expect, test } from '@playwright/test';
import { newApi } from '../support/api-client.js';
import { clickMerchantNav, loginMerchantWeb } from '../support/vue-web.js';

test.describe('UC13 商家维护门店资料、履约规则和商品目录', () => {
  test('商家更新门店资料与履约规则，新增商品并在商家端下架', async ({ browser }) => {
    const api = newApi();
    await api.login('demo_merchant', '123456');

    await api.merchantStoreUpdate({
      storeName: '塔斯汀中国汉堡',
      summary: 'E2E 更新门店简介',
      address: '城市广场 1 层',
      businessHoursText: '09:30-22:30',
      tagText: '35分钟送达,配送费¥4,满减',
      contactPhone: '18810000001',
      announcement: 'E2E 门店公告',
      status: 'open',
      longitude: 116.3136,
      latitude: 39.9823,
    });
    expect((await api.merchantCurrentStore()).announcement).toBe('E2E 门店公告');

    await api.updateTakeawaySetting(1, 'manual');
    await api.updateDeliveryRule(1, {
      deliveryFee: 5,
      startPrice: 20,
      estimatedMinutes: 40,
      maxDeliveryDistanceKm: 5,
      packageFeeMode: 'none',
      packageFeeFixed: 0,
      packageFeePerItem: 0,
      distanceExtraThresholdKm: 0,
      distanceExtraFee: 0,
      distanceExtraStepKm: 1,
      deliveryText: 'E2E 配送规则',
    });

    const title = `E2E 测试商品-${Date.now()}`;
    const item = await api.createCatalogItem({
      storeId: 1,
      businessType: 'takeaway',
      categoryId: 101,
      title,
      subtitle: '端到端测试商品',
      price: 9.9,
      originalPrice: 12.9,
      stock: 10,
      status: 'on_sale',
      coverUrl: '',
      tagText: '测试',
    });
    expect(item.id).toBeGreaterThan(0);

    const page = await browser.newPage();
    await loginMerchantWeb(page, 'demo_merchant', '123456');
    await clickMerchantNav(page, '商品管理');
    const card = page.locator('.catalog-card', { hasText: title }).first();
    await expect(card).toBeVisible({ timeout: 30_000 });
    await card.getByRole('button', { name: '下架' }).click();
    await expect(page.getByText(`${title} 已下架`, { exact: false }).first()).toBeVisible({
      timeout: 30_000,
    });
    await page.close();

    expect((await api.catalogItem(item.id)).status).toBe('off_sale');
  });
});
