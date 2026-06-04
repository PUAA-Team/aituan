<script setup lang="ts">
import { ref, watch } from 'vue';
import { adminRedeemVoucher, fetchVouchers, refundOrder } from '../api';
import type { OpsVoucher } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const statusFilter = ref('');
const keyword = ref('');
const vouchers = ref<OpsVoucher[]>([]);
const loading = ref(false);

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'unused', label: '未核销' },
  { value: 'used', label: '已核销' },
  { value: 'refunded', label: '已退款' },
];

watch(() => props.refreshKey, load, { immediate: true });

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

async function forceRedeem(row: OpsVoucher) {
  if (row.status !== 'unused') return;
  try {
    await adminRedeemVoucher(row.voucherCode);
    emit('notice', `券码 ${row.voucherCode} 已平台代为核销`);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function doRefund(row: OpsVoucher) {
  const reason = window.prompt('请输入退款原因', '平台券码退款');
  if (reason === null) return;
  try {
    await refundOrder(row.orderId, reason.trim() || '平台券码退款');
    emit('notice', `订单 ${row.orderNo} 已退款`);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function canRefund(row: OpsVoucher) {
  return row.status === 'unused' && row.refundableByStaff !== false;
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function statusLabel(status: string) {
  if (status === 'used') return '已核销';
  if (status === 'unused') return '未核销';
  if (status === 'refunded') return '已退款';
  return status;
}

function businessLabel(code: string) {
  const map: Record<string, string> = {
    group_buy: '团购',
    hotel: '酒店',
    entertainment: '休闲娱乐',
    movie: '电影演出',
    beauty: '丽人医美',
    ticket: '景点门票',
    massage: '洗脚按摩',
  };
  return map[code] || code;
}
</script>

<template>
  <section class="page-grid">
    <article class="panel-card">
      <div class="panel-toolbar">
        <h2>券码治理</h2>
        <div class="toolbar-actions">
          <select v-model="statusFilter" @change="load">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <input v-model="keyword" class="search-input" placeholder="按券码/订单号搜索" @keyup.enter="load" />
          <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '加载中' : '刷新' }}</button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>券码</th>
            <th>订单</th>
            <th>门店</th>
            <th>业务</th>
            <th>状态</th>
            <th>金额</th>
            <th>有效期</th>
            <th>核销时间</th>
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
            <td>{{ row.storeName }}</td>
            <td>{{ businessLabel(row.businessType) }}</td>
            <td>{{ statusLabel(row.status) }}</td>
            <td>{{ money(row.payableAmount) }}</td>
            <td>{{ timeText(row.effectiveTo) }}</td>
            <td>{{ timeText(row.verifiedAt) }}</td>
            <td>
              <button
                class="text-btn"
                :disabled="row.status !== 'unused'"
                @click="forceRedeem(row)"
              >{{ row.status === 'used' ? '已核销' : row.status === 'refunded' ? '已退款' : '平台核销' }}</button>
              <button
                v-if="canRefund(row)"
                class="text-btn"
                @click="doRefund(row)"
              >平台退款</button>
            </td>
          </tr>
          <tr v-if="vouchers.length === 0">
            <td colspan="9" class="empty-row">暂无券码记录</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<style scoped>
.empty-row {
  text-align: center;
  color: #999;
  padding: 18px 0;
}
</style>
