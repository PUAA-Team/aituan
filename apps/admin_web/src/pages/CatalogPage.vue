<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import {
  createCatalogCategory,
  createCatalogItem,
  fetchCatalogCategories,
  fetchCatalogItems,
  fetchStores,
  resolveAssetUrl,
  updateCatalogItem,
  updateCatalogItemStatus,
  uploadCatalogItemCover,
} from '../api';
import type { AdminStore, CatalogCategory, CatalogItem, CatalogItemForm } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const items = ref<CatalogItem[]>([]);
const stores = ref<AdminStore[]>([]);
const categories = ref<CatalogCategory[]>([]);
const filters = reactive({ storeId: undefined as number | undefined, businessType: '', status: '', keyword: '' });
const editingId = ref<number | null>(null);
const editing = ref<CatalogItemForm | null>(null);
const coverFile = ref<File | null>(null);
const newCategoryName = ref('');

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

const selectedStore = computed(() => stores.value.find((store) => store.storeId === filters.storeId));
const selectedBusinessType = computed(() => filters.businessType || selectedStore.value?.businessType || 'takeaway');
const itemNoun = computed(() => selectedBusinessType.value === 'takeaway' ? '商品' : '服务/套餐');
const selectableBusinessTypes = computed(() => businessTypes.filter((item) => item.value));

watch(() => props.refreshKey, load, { immediate: true });
watch([() => filters.businessType, () => filters.status], loadItems);
watch(() => filters.storeId, () => {
  if (selectedStore.value && !filters.businessType) filters.businessType = selectedStore.value.businessType;
  load();
});

async function load() {
  await Promise.all([loadStores(), loadItems(), loadCategories()]);
}

async function loadStores() {
  try {
    stores.value = (await fetchStores()).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function loadItems() {
  try {
    items.value = (await fetchCatalogItems(filters)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function loadCategories() {
  try {
    categories.value = await fetchCatalogCategories({ storeId: filters.storeId, businessType: selectedBusinessType.value });
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function openCreate() {
  if (!filters.storeId) {
    emit('notice', '请先输入门店 ID 再新增商品或服务');
    return;
  }
  editingId.value = null;
  coverFile.value = null;
  editing.value = {
    storeId: filters.storeId,
    businessType: selectedBusinessType.value,
    title: '',
    subtitle: '',
    price: 1,
    originalPrice: undefined,
    stock: 100,
    status: 'on_sale',
    coverUrl: '',
    tagText: '',
  };
}

function openEdit(item: CatalogItem) {
  editingId.value = item.id;
  coverFile.value = null;
  filters.storeId = item.storeId;
  filters.businessType = item.businessType;
  editing.value = {
    storeId: item.storeId,
    businessType: item.businessType,
    categoryId: item.categoryId,
    title: item.title,
    subtitle: item.subtitle,
    price: item.price,
    originalPrice: item.originalPrice,
    stock: item.stock,
    status: item.status,
    coverUrl: item.coverUrl || '',
    tagText: item.tagText || '',
  };
}

async function saveItem() {
  if (!editing.value || !editing.value.storeId) return;
  try {
    const saved = editingId.value
      ? await updateCatalogItem(editingId.value, editing.value)
      : await createCatalogItem(editing.value);
    const finalItem = coverFile.value ? await uploadCatalogItemCover(saved.id, coverFile.value) : saved;
    items.value = items.value.some((item) => item.id === finalItem.id)
      ? items.value.map((item) => (item.id === finalItem.id ? finalItem : item))
      : [finalItem, ...items.value];
    editing.value = null;
    emit('notice', `${finalItem.title} 已保存`);
    await loadCategories();
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

async function addCategory() {
  const name = newCategoryName.value.trim();
  if (!name || !filters.storeId) return;
  try {
    const category = await createCatalogCategory({
      storeId: filters.storeId,
      businessType: selectedBusinessType.value,
      categoryName: name,
      sortOrder: categories.value.length + 1,
      status: 'normal',
    });
    categories.value = [...categories.value, category];
    newCategoryName.value = '';
    emit('notice', '分类已新增');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  coverFile.value = input.files?.[0] || null;
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}
</script>

<template>
  <section class="panel-card">
    <div class="panel-toolbar">
      <h2>商品与服务治理</h2>
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
        <input v-model="filters.keyword" class="search-input" placeholder="商品/服务名" @keyup.enter="loadItems" />
        <button class="secondary-btn" @click="loadItems">查询</button>
        <button class="primary-btn" @click="openCreate">新增{{ itemNoun }}</button>
      </div>
    </div>

    <div class="category-line">
      <span v-for="category in categories" :key="category.id" class="tag">{{ category.categoryName }}</span>
      <input v-model="newCategoryName" placeholder="新增分类名称" @keyup.enter="addCategory" />
      <button class="secondary-btn small" @click="addCategory">添加分类</button>
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
          <button class="secondary-btn small" @click="openEdit(item)">编辑</button>
          <button class="primary-btn small" @click="toggleStatus(item)">{{ item.status === 'on_sale' ? '下架' : '上架' }}</button>
        </div>
      </article>
      <article v-if="items.length === 0" class="empty-card">暂无商品</article>
    </div>
  </section>

  <PanelModal v-if="editing" :title="editingId ? `编辑${itemNoun}` : `新增${itemNoun}`" @close="editing = null">
    <form class="modal-form" @submit.prevent="saveItem">
      <div class="form-grid three">
        <label>
          门店ID
          <input v-model.number="editing.storeId" type="number" min="1" required />
        </label>
        <label>
          业务类型
          <select v-model="editing.businessType">
            <option v-for="item in selectableBusinessTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label>
          分类
          <select v-model.number="editing.categoryId">
            <option :value="undefined">默认分类</option>
            <option v-for="category in categories" :key="category.id" :value="category.id">{{ category.categoryName }}</option>
          </select>
        </label>
      </div>
      <label>名称<input v-model="editing.title" required /></label>
      <label>说明<input v-model="editing.subtitle" /></label>
      <div class="form-grid four">
        <label>价格<input v-model.number="editing.price" type="number" min="0.1" step="0.1" required /></label>
        <label>原价<input v-model.number="editing.originalPrice" type="number" min="0" step="0.1" /></label>
        <label>库存<input v-model.number="editing.stock" type="number" min="0" /></label>
        <label>
          状态
          <select v-model="editing.status">
            <option value="on_sale">上架中</option>
            <option value="off_sale">已下架</option>
          </select>
        </label>
      </div>
      <label>标签<input v-model="editing.tagText" placeholder="热销,新品" /></label>
      <label>封面地址<input v-model="editing.coverUrl" /></label>
      <label>上传封面<input type="file" accept="image/png,image/jpeg,image/webp" @change="onFileChange" /></label>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="editing = null">取消</button>
        <button class="primary-btn">保存</button>
      </div>
    </form>
  </PanelModal>
</template>
