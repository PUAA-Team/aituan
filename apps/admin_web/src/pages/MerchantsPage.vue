<script setup lang="ts">
import { reactive, ref, watch } from 'vue';
import { fetchMerchants, fetchStores, resolveAssetUrl, updateMerchantStatus, updateStoreStatus, uploadStoreCover } from '../api';
import type { AdminMerchant, AdminStore } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const merchants = ref<AdminMerchant[]>([]);
const stores = ref<AdminStore[]>([]);
const keyword = ref('');
const filters = reactive({ businessType: '', status: '' });

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
watch([() => filters.businessType, () => filters.status], loadStores);

async function load() {
  await Promise.all([loadMerchants(), loadStores()]);
}

async function loadMerchants() {
  try {
    merchants.value = (await fetchMerchants(keyword.value)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function loadStores() {
  try {
    stores.value = (await fetchStores(filters)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function setMerchantStatus(merchant: AdminMerchant, status: string) {
  try {
    const updated = await updateMerchantStatus(merchant.merchantId, status);
    merchants.value = merchants.value.map((item) => (item.merchantId === updated.merchantId ? updated : item));
    emit('notice', `${updated.merchantName} 状态已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function setStoreStatus(store: AdminStore, status: string) {
  try {
    const updated = await updateStoreStatus(store.storeId, status);
    stores.value = stores.value.map((item) => (item.storeId === updated.storeId ? updated : item));
    emit('notice', `${updated.storeName} 状态已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function onCoverChange(store: AdminStore, event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  try {
    const updated = await uploadStoreCover(store.storeId, file);
    stores.value = stores.value.map((item) => (item.storeId === updated.storeId ? updated : item));
    emit('notice', `${updated.storeName} 图片已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    input.value = '';
  }
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="page-grid">
    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>商户管理</h2>
        <div class="toolbar-actions">
          <input v-model="keyword" class="search-input" placeholder="商户/联系人" @keyup.enter="loadMerchants" />
          <button class="secondary-btn" @click="loadMerchants">查询</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>商户</th><th>联系人</th><th>门店/商品</th><th>状态</th><th>入驻时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="merchant in merchants" :key="merchant.merchantId">
              <td><strong>{{ merchant.merchantName }}</strong><span>#{{ merchant.merchantId }} · {{ merchant.auditStatus }}</span></td>
              <td>{{ merchant.contactName }}<span>{{ merchant.contactPhone }}</span></td>
              <td>{{ merchant.storeCount }} 家门店<span>{{ merchant.itemCount }} 个商品</span></td>
              <td><span class="tag" :class="merchant.status">{{ merchant.status }}</span></td>
              <td>{{ timeText(merchant.settledAt) }}</td>
              <td>
                <div class="row-actions">
                  <button class="secondary-btn small" @click="setMerchantStatus(merchant, merchant.status === 'normal' ? 'disabled' : 'normal')">
                    {{ merchant.status === 'normal' ? '停用' : '启用' }}
                  </button>
                  <button class="secondary-btn small" @click="setMerchantStatus(merchant, 'blocked')">拉黑</button>
                </div>
              </td>
            </tr>
            <tr v-if="merchants.length === 0"><td colspan="6" class="empty-cell">暂无商户</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>门店治理</h2>
        <div class="toolbar-actions">
          <select v-model="filters.businessType">
            <option v-for="item in businessTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option value="open">营业中</option>
            <option value="closed">休息中</option>
          </select>
        </div>
      </div>
      <div class="store-grid">
        <article v-for="store in stores" :key="store.storeId" class="store-card">
          <div class="store-thumb">
            <img v-if="store.coverUrl" :src="resolveAssetUrl(store.coverUrl)" :alt="store.storeName" />
            <span v-else>{{ store.storeName.slice(0, 4) }}</span>
          </div>
          <div>
            <strong>{{ store.storeName }}</strong>
            <p>{{ store.merchantName }} · {{ store.businessType }}</p>
            <p>{{ store.address }}</p>
            <span class="tag" :class="store.status">{{ store.status === 'open' ? '营业中' : '休息中' }}</span>
          </div>
          <div class="row-actions">
            <button class="secondary-btn small" @click="setStoreStatus(store, store.status === 'open' ? 'closed' : 'open')">
              {{ store.status === 'open' ? '休息' : '营业' }}
            </button>
            <label class="file-btn small">
              换图
              <input type="file" accept="image/png,image/jpeg,image/webp" @change="onCoverChange(store, $event)" />
            </label>
          </div>
        </article>
        <article v-if="stores.length === 0" class="empty-card">暂无门店</article>
      </div>
    </section>
  </section>
</template>
