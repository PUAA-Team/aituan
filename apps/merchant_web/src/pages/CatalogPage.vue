<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import {
  createCatalogItem,
  createCategory,
  fetchCatalogItems,
  fetchCategories,
  resolveAssetUrl,
  updateCatalogItem,
  updateCatalogItemStatus,
  uploadItemCover,
} from '../api';
import type { CatalogCategory, CatalogItem, CatalogItemForm, MerchantStore } from '../types';

const props = defineProps<{
  store: MerchantStore;
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const loading = ref(false);
const status = ref('');
const keyword = ref('');
const items = ref<CatalogItem[]>([]);
const categories = ref<CatalogCategory[]>([]);
const editingId = ref<number | null>(null);
const editing = ref<CatalogItemForm | null>(null);
const coverFile = ref<File | null>(null);
const newCategoryName = ref('');

const businessTypeOptions = [
  { value: 'takeaway', label: '外卖' },
  { value: 'group_buy', label: '团购' },
  { value: 'hotel', label: '酒店' },
  { value: 'entertainment', label: '休闲娱乐' },
  { value: 'movie', label: '电影演出' },
  { value: 'beauty', label: '丽人医美' },
  { value: 'ticket', label: '景点门票' },
  { value: 'massage', label: '洗脚按摩' },
];

const filters = reactive({
  businessType: props.store.businessType,
});

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'on_sale', label: '上架中' },
  { value: 'off_sale', label: '已下架' },
];

const selectedCategories = computed(() =>
  categories.value.filter((item) => item.businessType === filters.businessType),
);

watch(() => props.refreshKey, load, { immediate: true });
watch(() => props.store.id, load);
watch([() => filters.businessType, status], load);

async function load() {
  try {
    loading.value = true;
    const [itemList, categoryList] = await Promise.all([
      fetchCatalogItems({ businessType: filters.businessType, status: status.value, keyword: keyword.value }),
      fetchCategories(filters.businessType),
    ]);
    items.value = itemList;
    categories.value = categoryList;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = null;
  coverFile.value = null;
  editing.value = {
    storeId: props.store.id,
    businessType: filters.businessType || props.store.businessType,
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
  if (!editing.value) return;
  try {
    loading.value = true;
    const saved = editingId.value
      ? await updateCatalogItem(editingId.value, editing.value)
      : await createCatalogItem(editing.value);
    const finalItem = coverFile.value ? await uploadItemCover(saved.id, coverFile.value) : saved;
    replaceItem(finalItem);
    editing.value = null;
    emit('notice', editingId.value ? '商品已保存' : '商品已新增');
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function toggleStatus(item: CatalogItem) {
  try {
    const nextStatus = item.status === 'on_sale' ? 'off_sale' : 'on_sale';
    const updated = await updateCatalogItemStatus(item.id, nextStatus);
    replaceItem(updated);
    emit('notice', `${updated.title} 已${nextStatus === 'on_sale' ? '上架' : '下架'}`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function addCategory() {
  const name = newCategoryName.value.trim();
  if (!name) return;
  try {
    const category = await createCategory({
      storeId: props.store.id,
      businessType: filters.businessType || props.store.businessType,
      categoryName: name,
      sortOrder: categories.value.length + 1,
      status: 'enabled',
    });
    categories.value = [...categories.value, category];
    newCategoryName.value = '';
    emit('notice', '分类已新增');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function replaceItem(item: CatalogItem) {
  const exists = items.value.some((entry) => entry.id === item.id);
  items.value = exists ? items.value.map((entry) => (entry.id === item.id ? item : entry)) : [item, ...items.value];
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  coverFile.value = input.files?.[0] || null;
}

function itemStatusText(value: string) {
  return value === 'on_sale' ? '上架中' : '已下架';
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}
</script>

<template>
  <section class="panel-card">
    <div class="panel-toolbar">
      <h2>商品与服务</h2>
      <div class="toolbar-actions">
        <label class="inline-field">
          业务类型
          <select v-model="filters.businessType">
            <option v-for="item in businessTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label class="inline-field">
          状态
          <select v-model="status">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <input v-model="keyword" class="search-input" placeholder="搜索商品/服务" @keyup.enter="load" />
        <button class="secondary-btn" :disabled="loading" @click="load">查询</button>
        <button class="primary-btn" @click="openCreate">新增商品</button>
      </div>
    </div>

    <div class="category-line">
      <span v-for="category in selectedCategories" :key="category.id" class="tag">{{ category.categoryName }}</span>
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
            <span class="tag" :class="item.status">{{ itemStatusText(item.status) }}</span>
            <strong>{{ money(item.price) }}</strong>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.subtitle || item.tagText || '暂无副标题' }}</p>
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

  <PanelModal v-if="editing" :title="editingId ? '编辑商品' : '新增商品'" @close="editing = null">
    <form class="modal-form" @submit.prevent="saveItem">
      <div class="form-grid two">
        <label>
          商品名称
          <input v-model="editing.title" required />
        </label>
        <label>
          业务类型
          <select v-model="editing.businessType">
            <option v-for="item in businessTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
      </div>
      <label>
        副标题
        <input v-model="editing.subtitle" />
      </label>
      <div class="form-grid four">
        <label>
          分类
          <select v-model.number="editing.categoryId">
            <option :value="undefined">默认分类</option>
            <option v-for="category in selectedCategories" :key="category.id" :value="category.id">{{ category.categoryName }}</option>
          </select>
        </label>
        <label>
          价格
          <input v-model.number="editing.price" type="number" min="0.1" step="0.1" />
        </label>
        <label>
          原价
          <input v-model.number="editing.originalPrice" type="number" min="0" step="0.1" />
        </label>
        <label>
          库存
          <input v-model.number="editing.stock" type="number" min="0" />
        </label>
      </div>
      <div class="form-grid two">
        <label>
          状态
          <select v-model="editing.status">
            <option value="on_sale">上架中</option>
            <option value="off_sale">已下架</option>
          </select>
        </label>
        <label>
          标签
          <input v-model="editing.tagText" placeholder="热销,新品" />
        </label>
      </div>
      <label>
        商品图片
        <input type="file" accept="image/png,image/jpeg,image/webp" @change="onFileChange" />
      </label>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="editing = null">取消</button>
        <button class="primary-btn" :disabled="loading">保存</button>
      </div>
    </form>
  </PanelModal>
</template>
