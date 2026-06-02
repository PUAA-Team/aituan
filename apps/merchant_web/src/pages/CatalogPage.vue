<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import {
  createCatalogItem,
  createCategory,
  deleteCategory,
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

const businessTypeLabels: Record<string, string> = {
  takeaway: '外卖',
  group_buy: '团购',
  hotel: '酒店',
  entertainment: '休闲娱乐',
  movie: '电影演出',
  beauty: '丽人医美',
  ticket: '景点门票',
  massage: '洗脚按摩',
};

const currentBusinessType = computed(() => props.store.businessType);
const isTakeaway = computed(() => currentBusinessType.value === 'takeaway');
const businessTypeName = computed(() => businessTypeLabels[currentBusinessType.value] || currentBusinessType.value);
const itemNoun = computed(() => (isTakeaway.value ? '商品' : '服务/套餐'));
const catalogTitle = computed(() => (isTakeaway.value ? '商品管理' : '服务与套餐'));
const titleLabel = computed(() => (isTakeaway.value ? '商品名称' : '服务/套餐名称'));
const subtitleLabel = computed(() => (isTakeaway.value ? '副标题' : '使用规则'));
const imageLabel = computed(() => (isTakeaway.value ? '商品图片' : '服务图片'));
const stockLabel = computed(() => (isTakeaway.value ? '库存' : '可售份数'));

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'on_sale', label: '上架中' },
  { value: 'off_sale', label: '已下架' },
];

const selectedCategories = computed(() =>
  categories.value.filter((item) => item.businessType === currentBusinessType.value),
);

watch(() => props.refreshKey, load, { immediate: true });
watch(() => props.store.id, load);
watch(() => props.store.businessType, load);
watch(status, load);

async function load() {
  try {
    loading.value = true;
    const [itemList, categoryList] = await Promise.all([
      fetchCatalogItems({ businessType: currentBusinessType.value, status: status.value, keyword: keyword.value }),
      fetchCategories(currentBusinessType.value),
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
    businessType: currentBusinessType.value || props.store.businessType,
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
  editing.value.storeId = props.store.id;
  editing.value.businessType = currentBusinessType.value;
  try {
    loading.value = true;
    const saved = editingId.value
      ? await updateCatalogItem(editingId.value, editing.value)
      : await createCatalogItem(editing.value);
    if (coverFile.value) {
      try {
        const finalItem = await uploadItemCover(saved.id, coverFile.value);
        replaceItem(finalItem);
        editing.value = null;
        coverFile.value = null;
        emit('notice', editingId.value ? '商品已保存，图片已更新' : '商品已新增，图片已上传');
        await load();
      } catch (uploadError) {
        replaceItem(saved);
        emit('notice', `商品已保存，图片上传失败：${uploadError instanceof Error ? uploadError.message : String(uploadError)}`);
        return;
      }
    } else {
      replaceItem(saved);
      editing.value = null;
      emit('notice', editingId.value ? '商品已保存' : '商品已新增');
      await load();
    }
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
      businessType: currentBusinessType.value || props.store.businessType,
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

async function removeCategory(category: CatalogCategory) {
  if (items.value.some((item) => item.categoryId === category.id)) {
    emit('notice', '分类下存在商品，请先调整商品分类或删除商品');
    return;
  }
  if (!window.confirm(`确定删除分类「${category.categoryName}」吗？`)) return;
  try {
    loading.value = true;
    await deleteCategory(category.id);
    emit('notice', '分类已删除');
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
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
      <h2>{{ catalogTitle }}</h2>
      <div class="toolbar-actions">
        <span class="inline-field">
          业务类型
          <strong>{{ businessTypeName }}</strong>
        </span>
        <label class="inline-field">
          状态
          <select v-model="status">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <input v-model="keyword" class="search-input" :placeholder="`搜索${itemNoun}`" @keyup.enter="load" />
        <button class="secondary-btn" :disabled="loading" @click="load">查询</button>
        <button class="primary-btn" @click="openCreate">新增{{ itemNoun }}</button>
      </div>
    </div>

    <div class="category-line">
      <span v-for="category in selectedCategories" :key="category.id" class="tag category-tag">
        {{ category.categoryName }}
        <button type="button" title="删除分类" :disabled="loading" @click="removeCategory(category)">×</button>
      </span>
      <input v-model="newCategoryName" placeholder="新增分类名称" @keyup.enter="addCategory" />
      <button class="secondary-btn small" @click="addCategory">添加分类</button>
    </div>

    <div class="catalog-grid">
      <article v-for="item in items" :key="item.id" class="catalog-card">
        <div class="thumb-box">
          <img v-if="item.coverUrl" :src="resolveAssetUrl(item.coverUrl)" :alt="item.title" />
          <span v-else>{{ item.categoryName || itemNoun }}</span>
        </div>
        <div class="catalog-info">
          <div class="catalog-head">
            <span class="tag" :class="item.status">{{ itemStatusText(item.status) }}</span>
            <strong>{{ money(item.price) }}</strong>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.subtitle || item.tagText || '暂无副标题' }}</p>
          <small>{{ item.categoryName }} · {{ stockLabel }} {{ item.stock }} · 月售 {{ item.salesCount }}</small>
        </div>
        <div class="row-actions">
          <button class="secondary-btn small" @click="openEdit(item)">编辑</button>
          <button class="primary-btn small" @click="toggleStatus(item)">{{ item.status === 'on_sale' ? '下架' : '上架' }}</button>
        </div>
      </article>
      <article v-if="items.length === 0" class="empty-card">暂无{{ itemNoun }}</article>
    </div>
  </section>

  <PanelModal v-if="editing" :title="editingId ? `编辑${itemNoun}` : `新增${itemNoun}`" @close="editing = null">
    <form class="modal-form" @submit.prevent="saveItem">
      <div class="form-grid two">
        <label>
          {{ titleLabel }}
          <input v-model="editing.title" required />
        </label>
        <label>
          业务类型
          <input :value="businessTypeName" disabled />
        </label>
      </div>
      <label>
        {{ subtitleLabel }}
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
          {{ stockLabel }}
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
        {{ imageLabel }}
        <input type="file" accept="image/png,image/jpeg,image/webp" @change="onFileChange" />
      </label>
      <p class="form-hint">图片将在保存{{ itemNoun }}后上传；如果上传失败，基础信息会保留在弹窗中方便重试。</p>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="editing = null">取消</button>
        <button class="primary-btn" :disabled="loading">保存</button>
      </div>
    </form>
  </PanelModal>
</template>
