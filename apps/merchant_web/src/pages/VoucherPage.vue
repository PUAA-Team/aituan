<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchVouchers, lookupVoucher, redeemVoucher } from '../api';
import type { MerchantStore, OpsVoucher, VoucherLookup } from '../types';

const props = defineProps<{
  store: MerchantStore;
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const voucherCode = ref('');
const lookup = ref<VoucherLookup | null>(null);
const statusFilter = ref('');
const keyword = ref('');
const vouchers = ref<OpsVoucher[]>([]);
const loading = ref(false);

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'unused', label: '未核销' },
  { value: 'used', label: '已核销' },
];

watch(() => props.refreshKey, load, { immediate: true });
watch(() => props.store.id, load);

async function load() {
  try {
    loading.value = true;
    const page = await fetchVouchers({
      status: statusFilter.value,
      keyword: keyword.value,
    });
    vouchers.value = page.list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function doLookup() {
  const code = voucherCode.value.trim();
  if (!code) return;
  try {
    lookup.value = await lookupVoucher(code);
  } catch (error) {
    lookup.value = null;
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function doRedeem(code: string) {
  try {
    await redeemVoucher(code);
    emit('notice', `券码 ${code} 已核销`);
    lookup.value = null;
    voucherCode.value = '';
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function statusLabel(status: string) {
  return status === 'used' ? '已核销' : status === 'unused' ? '未核销' : status;
}
</script>

<template>
  <section class="page-grid">
    <article class="panel-card">
      <div class="panel-toolbar"><h2>券码扫码与核销</h2></div>
      <form class="voucher-form" @submit.prevent="doLookup">
        <input v-model="voucherCode" placeholder="输入用户出示的券码（先查后销）" />
        <button type="submit" class="secondary-btn">查询券码</button>
      </form>
      <div v-if="lookup" class="result-box">
        <div class="result-grid">
          <span><strong>{{ lookup.orderTitle }}</strong></span>
          <span>订单 {{ lookup.orderNo }}</span>
          <span>{{ lookup.storeName }} · {{ lookup.businessType }}</span>
          <span>{{ money(lookup.payableAmount) }}</span>
          <span>状态：{{ statusLabel(lookup.status) }}</span>
          <span v-if="lookup.effectiveTo">有效期至 {{ timeText(lookup.effectiveTo) }}</span>
        </div>
        <div v-if="lookup.usageRulesSnapshot" class="result-rule">{{ lookup.usageRulesSnapshot }}</div>
        <div class="result-actions">
          <button
            class="primary-btn"
            :disabled="lookup.status === 'used'"
            @click="doRedeem(lookup.voucherCode)"
          >{{ lookup.status === 'used' ? '券码已核销' : '确认核销' }}</button>
        </div>
      </div>
    </article>

    <article class="panel-card">
      <div class="panel-toolbar">
        <h2>券码记录</h2>
        <div class="toolbar-actions">
          <select v-model="statusFilter" @change="load">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <input v-model="keyword" class="search-input" placeholder="按券码/订单号/标题搜索" @keyup.enter="load" />
          <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '加载中' : '刷新' }}</button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>券码</th>
            <th>订单</th>
            <th>状态</th>
            <th>金额</th>
            <th>有效期</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in vouchers" :key="row.voucherCode">
            <td>{{ row.voucherCode }}</td>
            <td>
              <div>{{ row.orderTitle }}</div>
              <small>{{ row.orderNo }}</small>
            </td>
            <td>{{ statusLabel(row.status) }}</td>
            <td>{{ money(row.payableAmount) }}</td>
            <td>{{ timeText(row.effectiveTo) }}</td>
            <td>
              <button
                class="text-btn"
                :disabled="row.status === 'used'"
                @click="doRedeem(row.voucherCode)"
              >{{ row.status === 'used' ? '已核销' : '核销' }}</button>
            </td>
          </tr>
          <tr v-if="vouchers.length === 0">
            <td colspan="6" class="empty-row">暂无券码记录</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<style scoped>
.voucher-form {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.voucher-form input {
  flex: 1;
}
.result-box {
  border: 1px solid var(--line, #e7ebf0);
  border-radius: 12px;
  padding: 16px;
  background: #fffdf6;
}
.result-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 16px;
  margin-bottom: 8px;
}
.result-rule {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}
.result-actions {
  text-align: right;
}
.empty-row {
  text-align: center;
  color: #999;
  padding: 18px 0;
}
</style>
