<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { fetchCatalogItems, resolveAssetUrl, updateCatalogItemStatus } from '../api';
import type { CatalogItem } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const items = ref<CatalogItem[]>([]);
const filters = reactive({ storeId: undefined as number | undefined, businessType: '', status: '', keyword: '' });

const businessTypes = [
  { value: '', label: '全部业务' },
  { value: 'takeaway', label: '外卖' },
  { value: 'group_buy', label: '团购' },
  { value: 'hotel', label: '酒店' },
  { value: 'entertainment', label: '休闲娱乐' },
  { value: 'movie', label: '电影演出' },
  { value: 'beauty', label: '丽人医美' },
  { value: 'ticket', label: '景点门票' },
  { value: 'massage', label: '洗脚按摩' },
];

watch(() => props.refreshKey, load, { immediate: true });
watch([() => filters.businessType, () => filters.status], load);

async function load() {
  try {
    items.value = (await fetchCatalogItems(filters)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function toggleStatus(item: CatalogItem) {
  try {
    const next = item.status === 'on_sale' ? 'off_sale' : 'on_sale';
    const updated = await updateCatalogItemStatus(item.id, next);
    items.value = items.value.map((entry) => (entry.id === updated.id ? updated : entry));
    emit('notice', `${updated.title} 已${next === 'on_sale' ? '上架' : '下架'}`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}
</script>

<template>
  <section class="panel-card">
    <div class="panel-toolbar">
      <h2>商品治理</h2>
      <div class="toolbar-actions">
        <input v-model.number="filters.storeId" class="mini-input" type="number" min="1" placeholder="门店ID" @keyup.enter="load" />
        <select v-model="filters.businessType">
          <option v-for="item in businessTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
        <select v-model="filters.status">
          <option value="">全部状态</option>
          <option value="on_sale">上架中</option>
          <option value="off_sale">已下架</option>
        </select>
        <input v-model="filters.keyword" class="search-input" placeholder="商品名" @keyup.enter="load" />
        <button class="secondary-btn" @click="load">查询</button>
      </div>
    </div>
    <div class="catalog-grid">
      <article v-for="item in items" :key="item.id" class="catalog-card">
        <div class="thumb-box">
          <img v-if="item.coverUrl" :src="resolveAssetUrl(item.coverUrl)" :alt="item.title" />
          <span v-else>{{ item.categoryName || '商品' }}</span>
        </div>
        <div class="catalog-info">
          <div class="catalog-head">
            <span class="tag" :class="item.status">{{ item.status === 'on_sale' ? '上架中' : '已下架' }}</span>
            <strong>{{ money(item.price) }}</strong>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.storeName }} · {{ item.businessType }}</p>
          <small>{{ item.categoryName }} · 库存 {{ item.stock }} · 月售 {{ item.salesCount }}</small>
        </div>
        <div class="row-actions">
          <button class="secondary-btn small" @click="toggleStatus(item)">{{ item.status === 'on_sale' ? '下架' : '上架' }}</button>
        </div>
      </article>
      <article v-if="items.length === 0" class="empty-card">暂无商品</article>
    </div>
  </section>
</template>
