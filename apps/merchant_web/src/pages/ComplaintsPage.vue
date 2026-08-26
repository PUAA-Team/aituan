<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { fetchMerchantComplaintDetail, fetchMerchantComplaints, resolveAssetUrl } from '../api';
import type { ComplaintDetailView, ComplaintView } from '../types';

const props = defineProps<{
  refreshKey: number;
  initialFilters?: { orderNo?: string; storeName?: string };
}>();
const emit = defineEmits<{ notice: [message: string] }>();

const loading = ref(false);
const complaints = ref<ComplaintView[]>([]);
const active = ref<ComplaintDetailView | null>(null);
const status = ref<'all' | 'pending' | 'processing' | 'resolved' | 'closed'>('all');
const orderNo = ref('');
const storeName = ref('');

const statusOptions = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '待处理' },
  { value: 'processing', label: '处理中' },
  { value: 'resolved', label: '已解决' },
  { value: 'closed', label: '已关闭' },
] as const;

onMounted(() => {
  applyInitialFilters();
  load();
});
watch(() => props.refreshKey, load);
watch(() => props.initialFilters, () => {
  applyInitialFilters();
  load();
}, { deep: true });

function applyInitialFilters() {
  orderNo.value = props.initialFilters?.orderNo || '';
  storeName.value = props.initialFilters?.storeName || '';
}

async function load() {
  try {
    loading.value = true;
    const page = await fetchMerchantComplaints({
      status: status.value === 'all' ? undefined : status.value,
      orderNo: orderNo.value.trim() || undefined,
      storeName: storeName.value.trim() || undefined,
      pageSize: 50,
    });
    complaints.value = page.list;
    if (active.value) {
      const stillVisible = complaints.value.some(item => item.id === active.value!.complaint.id);
      if (!stillVisible) active.value = null;
    }
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function openComplaint(row: ComplaintView) {
  try {
    active.value = await fetchMerchantComplaintDetail(row.id);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function clearFilters() {
  status.value = 'all';
  orderNo.value = '';
  storeName.value = '';
  load();
}

function statusText(value: string) {
  const map: Record<string, string> = {
    pending: '待处理',
    processing: '处理中',
    resolved: '已解决',
    closed: '已关闭',
  };
  return map[value] || value;
}

function categoryText(value: string) {
  const map: Record<string, string> = {
    service: '服务',
    quality: '质量',
    delivery: '配送',
    other: '其他',
  };
  return map[value] || value;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="page-grid two-col">
    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>投诉工单</h2>
        <div class="toolbar-actions">
          <select v-model="status" @change="load">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <input v-model="orderNo" placeholder="订单号" @keyup.enter="load" />
          <input v-model="storeName" placeholder="门店名" @keyup.enter="load" />
          <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '刷新中' : '筛选' }}</button>
          <button class="secondary-btn" @click="clearFilters">清空</button>
        </div>
      </div>

      <table>
        <thead>
          <tr>
            <th>工单</th>
            <th>订单/门店</th>
            <th>分类</th>
            <th>状态</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in complaints"
            :key="row.id"
            :class="{ active: active && active.complaint.id === row.id }"
            @click="openComplaint(row)"
          >
            <td>{{ row.ticketNo }}<span>{{ row.title }}</span></td>
            <td>{{ row.orderNo || '-' }}<span>{{ row.storeName || '-' }}</span></td>
            <td>{{ categoryText(row.category) }}</td>
            <td>{{ statusText(row.status) }}</td>
            <td>{{ timeText(row.createdAt) }}</td>
          </tr>
          <tr v-if="complaints.length === 0">
            <td colspan="5" class="empty-cell">暂无投诉工单</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar"><h2>工单详情</h2></div>
      <div v-if="!active" class="empty-card">在左侧选择工单查看详情</div>
      <div v-else class="detail">
        <div class="detail-head">
          <strong>{{ active.complaint.title }}</strong>
          <span class="status-pill" :class="active.complaint.status">{{ statusText(active.complaint.status) }}</span>
        </div>
        <p>{{ active.complaint.detail }}</p>
        <div class="meta-grid">
          <span>工单号：{{ active.complaint.ticketNo }}</span>
          <span>订单号：{{ active.complaint.orderNo || '-' }}</span>
          <span>门店：{{ active.complaint.storeName || '-' }}</span>
          <span>分类：{{ categoryText(active.complaint.category) }}</span>
        </div>
        <div v-if="active.complaint.evidenceUrls && active.complaint.evidenceUrls.length" class="image-grid">
          <img
            v-for="url in active.complaint.evidenceUrls"
            :key="url"
            :src="resolveAssetUrl(url)"
            alt="投诉证据"
          />
        </div>
        <div class="log-list">
          <strong>处理记录</strong>
          <div v-for="log in active.logs" :key="log.id" class="log-row">
            <span>{{ statusText(log.action) || log.action }}</span>
            <small>{{ log.operatorType }} · {{ timeText(log.createdAt) }}</small>
            <p v-if="log.remark">{{ log.remark }}</p>
          </div>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page-grid.two-col {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 1fr);
  gap: 16px;
}
.panel-card {
  padding: 16px;
}
.panel-toolbar {
  margin-bottom: 12px;
}
.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-actions select,
.toolbar-actions input {
  width: 140px;
}
tbody tr {
  cursor: pointer;
}
tbody tr.active {
  background: #fff5f6;
}
td span {
  display: block;
  margin-top: 3px;
  color: #86909c;
  font-size: 12px;
}
.empty-cell {
  text-align: center;
  padding: 24px;
  color: #86909c;
}
.detail {
  display: grid;
  gap: 12px;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: center;
}
.status-pill {
  padding: 3px 8px;
  border-radius: 999px;
  background: #f0f0f0;
  color: #595959;
  font-size: 12px;
}
.status-pill.pending {
  background: #fff7e6;
  color: #ad6800;
}
.status-pill.processing {
  background: #e6f4ff;
  color: #0958d9;
}
.status-pill.resolved {
  background: #f6ffed;
  color: #237804;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  color: #4e5969;
  font-size: 13px;
}
.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}
.image-grid img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 6px;
}
.log-list {
  display: grid;
  gap: 8px;
}
.log-row {
  display: grid;
  gap: 4px;
  padding: 8px;
  border-radius: 6px;
  background: #f7f8fa;
}
.log-row small {
  color: #86909c;
}
.log-row p {
  margin: 0;
}
@media (max-width: 900px) {
  .page-grid.two-col {
    grid-template-columns: 1fr;
  }
}
</style>
