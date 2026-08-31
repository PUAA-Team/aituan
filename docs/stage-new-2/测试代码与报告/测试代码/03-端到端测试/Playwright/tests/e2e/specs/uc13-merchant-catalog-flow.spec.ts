import { expect, test } from '@playwright/test';
import { E2eApi } from '../support/api-client';
import {
  demo,
  loginMerchantPage,
  uniqueText,
} from '../support/scenarios';

interface MerchantStore {
  id: number;
  storeName: string;
  summary: string;
  businessHoursText: string;
  announcement: string;
}

interface TakeawaySetting {
  storeId: number;
  acceptMode: string;
}

interface DeliveryRule {
  storeId: number;
  deliveryFee: number;
  startPrice: number;
  estimatedMinutes: number;
  maxDeliveryDistanceKm: number;
}

interface CatalogItem {
  id: number;
  title: string;
  subtitle: string;
  price: number;
  stock: number;
  status: string;
  categoryName: string;
}

test('UC13 商家维护门店、履约规则和商品上下架', async ({ page, request }) => {
  const api = new E2eApi(request);
  const merchant = await api.loginMerchant(demo.takeawayMerchant);
  const marker = uniqueText('UC13');
  const summary = `${marker} 门店简介`;
  const announcement = `${marker} 门店公告`;
  const categoryName = `${marker} 分类`;
  const itemName = `${marker} 商品`;

  await loginMerchantPage(page, demo.takeawayMerchant);
  await page.getByRole('button', { name: /门店资料/ }).click();
  await page.getByLabel('门店简介', { exact: true }).fill(summary);
  await page.getByLabel('营业时间', { exact: true }).fill('08:30-22:30');
  await page.getByLabel('门店公告', { exact: true }).fill(announcement);
  await page.getByRole('button', { name: '保存门店资料', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('门店资料已保存');

  const store = await api.get<MerchantStore>('/api/merchant/stores/current', { token: merchant.token });
  expect(store).toEqual(expect.objectContaining({
    summary,
    businessHoursText: '08:30-22:30',
    announcement,
  }));

  await page.getByRole('button', { name: /履约设置/ }).click();
  await page.getByRole('button', { name: /自动接单/ }).click();
  await expect(page.locator('.notice-bar')).toContainText('已开启自动接单');
  await page.getByLabel('起送价', { exact: true }).fill('21.5');
  await page.getByLabel('配送费', { exact: true }).fill('4.5');
  await page.getByLabel('预计分钟', { exact: true }).fill('33');
  await page.getByLabel('可配送范围 km', { exact: true }).fill('8.8');
  await page.getByRole('button', { name: '保存规则', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('配送规则已保存');

  const setting = await api.get<TakeawaySetting>('/api/merchant/trade/stores/1/takeaway-setting', {
    token: merchant.token,
  });
  const rule = await api.get<DeliveryRule>('/api/merchant/trade/stores/1/delivery-rule', {
    token: merchant.token,
  });
  expect(setting.acceptMode).toBe('auto');
  expect(rule).toEqual(expect.objectContaining({
    deliveryFee: 4.5,
    startPrice: 21.5,
    estimatedMinutes: 33,
    maxDeliveryDistanceKm: 8.8,
  }));

  await page.getByRole('button', { name: /商品管理/ }).click();
  await page.getByPlaceholder('新增分类名称').fill(categoryName);
  await page.getByRole('button', { name: '添加分类', exact: true }).click();
  await expect(page.locator('.category-line')).toContainText(categoryName);

  await page.getByRole('button', { name: '新增商品', exact: true }).click();
  const createModal = page.locator('.modal-panel');
  await createModal.getByLabel('商品名称', { exact: true }).fill(itemName);
  await createModal.getByLabel('副标题', { exact: true }).fill(`${marker} 初始副标题`);
  await createModal.getByRole('combobox').first().selectOption({ label: categoryName });
  await createModal.getByLabel('价格', { exact: true }).fill('29.9');
  await createModal.getByLabel('库存', { exact: true }).fill('30');
  await createModal.getByLabel('标签', { exact: true }).fill('端到端,新品');
  await createModal.getByRole('button', { name: '保存', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('商品已新增');

  let items = await api.get<CatalogItem[]>('/api/merchant/catalog/items', {
    token: merchant.token,
    params: { businessType: 'takeaway', keyword: itemName },
  });
  const created = items.find((item) => item.title === itemName);
  expect(created).toBeDefined();

  const card = page.getByTestId(`merchant-catalog-${created!.id}`);
  await expect(card).toContainText(itemName);
  await card.getByRole('button', { name: '编辑', exact: true }).click();
  await page.getByLabel('副标题', { exact: true }).fill(`${marker} 已编辑副标题`);
  await page.getByLabel('价格', { exact: true }).fill('33.5');
  await page.getByLabel('库存', { exact: true }).fill('42');
  await page.getByRole('button', { name: '保存', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText('商品已保存');

  items = await api.get<CatalogItem[]>('/api/merchant/catalog/items', {
    token: merchant.token,
    params: { businessType: 'takeaway', keyword: itemName },
  });
  expect(items).toContainEqual(expect.objectContaining({
    id: created!.id,
    subtitle: `${marker} 已编辑副标题`,
    price: 33.5,
    stock: 42,
    status: 'on_sale',
  }));

  await page.getByTestId(`merchant-catalog-${created!.id}`).getByRole('button', { name: '下架', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText(`${itemName} 已下架`);
  items = await api.get<CatalogItem[]>('/api/merchant/catalog/items', {
    token: merchant.token,
    params: { businessType: 'takeaway', keyword: itemName },
  });
  expect(items.find((item) => item.id === created!.id)?.status).toBe('off_sale');

  await page.getByTestId(`merchant-catalog-${created!.id}`).getByRole('button', { name: '上架', exact: true }).click();
  await expect(page.locator('.notice-bar')).toContainText(`${itemName} 已上架`);
  items = await api.get<CatalogItem[]>('/api/merchant/catalog/items', {
    token: merchant.token,
    params: { businessType: 'takeaway', keyword: itemName },
  });
  expect(items.find((item) => item.id === created!.id)?.status).toBe('on_sale');
});
