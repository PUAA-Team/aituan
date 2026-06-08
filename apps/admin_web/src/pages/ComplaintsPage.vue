<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { complaintAction, fetchGovernanceComplaints, resolveAssetUrl } from '../api';
import type { AdminComplaintView } from '../types';

const props = defineProps<{ refreshKey: number }>();
const emit = defineEmits<{ notice: [message: string] }>();

const loading = ref(false);
const status = ref<'all' | 'pending' | 'processing' | 'resolved' | 'closed'>('all');
const category = ref<'all' | 'service' | 'quality' | 'delivery' | 'other'>('all');
const initialQuery = new URLSearchParams(window.location.search);
const orderNoKeyword = ref(initialQuery.get('orderNo')?.trim() || '');
const storeNameKeyword = ref(initialQuery.get('storeName')?.trim() || '');
const tickets = ref<AdminComplaintView[]>([]);
const active = ref<AdminComplaintView | null>(null);
const remark = ref('');
const submitting = ref(false);

const statusOptions: Array<{ value: typeof status.value; label: string }> = [
  { value: 'all', label: '全部状态' },
  { value: 'pending', label: '待处理' },
  { value: 'processing', label: '处理中' },
  { value: 'resolved', label: '已处理' },
  { value: 'closed', label: '已关闭' },
];

const categoryOptions: Array<{ value: typeof category.value; label: string }> = [
  { value: 'all', label: '全部分类' },
  { value: 'service', label: '服务态度' },
  { value: 'quality', label: '商品质量' },
  { value: 'delivery', label: '配送问题' },
  { value: 'other', label: '其他' },
];

onMounted(load);
watch(() => props.refreshKey, load);
watch([status, category], load);

async function load() {
  try {
    loading.value = true;
    const params: Parameters<typeof fetchGovernanceComplaints>[0] = { pageSize: 40 };
    if (status.value !== 'all') params.status = status.value;
    if (category.value !== 'all') params.category = category.value;
    if (orderNoKeyword.value.trim()) params.orderNo = orderNoKeyword.value.trim();
    if (storeNameKeyword.value.trim()) params.storeName = storeNameKeyword.value.trim();
    const page = await fetchGovernanceComplaints(params);
    tickets.value = page.list;
    if (active.value) {
      active.value = tickets.value.find(t => t.id === active.value!.id) || null;
    }
    if (!active.value && hasEntryFilter() && tickets.value.length > 0) {
      open(tickets.value[0]);
    }
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

function open(ticket: AdminComplaintView) {
  active.value = ticket;
  remark.value = '';
}

function hasEntryFilter() {
  return Boolean(orderNoKeyword.value.trim() || storeNameKeyword.value.trim());
}

async function clearEntryFilter() {
  orderNoKeyword.value = '';
  storeNameKeyword.value = '';
  active.value = null;
  await load();
}

async function run(action: 'accept' | 'resolve' | 'close') {
  if (!active.value) return;
  try {
    submitting.value = true;
    const updated = await complaintAction(active.value.id, action, remark.value);
    active.value = updated;
    emit('notice', `已${labelOf(action)}`);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    submitting.value = false;
  }
}

function labelOf(action: string) {
  const map: Record<string, string> = { accept: '受理', resolve: '处理完成', close: '关闭' };
  return map[action] || action;
}

function statusLabel(s: string) {
  return statusOptions.find(o => o.value === s)?.label || s;
}

function categoryLabel(c: string) {
  return categoryOptions.find(o => o.value === c)?.label || c;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function canRun(action: 'accept' | 'resolve' | 'close') {
  if (!active.value) return false;
  if (active.value.status === 'closed') return false;
  if (action === 'accept') return active.value.status === 'pending';
  if (action === 'resolve') return active.value.status === 'processing';
  if (action === 'close') return active.value.status === 'resolved';
  return false;
}
</script>

<template>
  <section class="page-grid two-col">
    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>投诉工单</h2>
        <div class="toolbar-actions">
          <select v-model="status">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <select v-model="category">
            <option v-for="opt in categoryOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <input v-model="orderNoKeyword" placeholder="订单号" @keyup.enter="load" />
          <input v-model="storeNameKeyword" placeholder="门店名" @keyup.enter="load" />
          <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '刷新中' : '刷新' }}</button>
          <button v-if="hasEntryFilter()" class="secondary-btn" :disabled="loading" @click="clearEntryFilter">清除定位</button>
        </div>
      </div>
      <table>
        <thead>
          <tr>
            <th>工单</th>
            <th>用户</th>
            <th>分类</th>
            <th>状态</th>
            <th>提交时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in tickets"
            :key="row.id"
            :class="{ active: active && active.id === row.id }"
            @click="open(row)"
          >
            <td>{{ row.title }}<span>{{ row.ticketNo }}</span></td>
            <td>{{ row.userMaskedNickname || '匿名' }}<span>{{ row.storeName || '-' }}</span></td>
            <td>{{ categoryLabel(row.category) }}</td>
            <td>{{ statusLabel(row.status) }}</td>
            <td>{{ timeText(row.createdAt) }}</td>
          </tr>
          <tr v-if="tickets.length === 0">
            <td colspan="5" class="empty-cell">暂无投诉工单</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar"><h2>工单处理</h2></div>
      <div v-if="!active" class="empty-card">在左侧选择工单进行处理</div>
      <div v-else class="detail">
        <div class="detail-head">
          <strong>{{ active.title }}</strong>
          <span class="tag">{{ statusLabel(active.status) }}</span>
        </div>
        <small>{{ active.ticketNo }} · {{ timeText(active.createdAt) }}</small>
        <p class="detail-content">{{ active.detail }}</p>
        <div class="kv">
          <span>分类</span><strong>{{ categoryLabel(active.category) }}</strong>
        </div>
        <div class="kv">
          <span>关联订单</span><strong>{{ active.orderNo || '-' }}</strong>
        </div>
        <div class="kv">
          <span>门店</span><strong>{{ active.storeName || '-' }}</strong>
        </div>
        <div class="kv">
          <span>受理人</span><strong>{{ active.acceptedBy || '-' }}（{{ timeText(active.acceptedAt) }}）</strong>
        </div>
        <div class="kv">
          <span>处理结果</span><strong>{{ active.resolvedBy || '-' }}（{{ timeText(active.resolvedAt) }}）</strong>
        </div>
        <div v-if="active.evidenceUrls && active.evidenceUrls.length" class="image-grid">
          <img v-for="url in active.evidenceUrls" :key="url" :src="resolveAssetUrl(url)" alt="证据" />
        </div>
        <label>备注
          <input v-model="remark" placeholder="处理意见，将写入工单日志" />
        </label>
        <div class="action-row">
          <button class="primary-btn" :disabled="submitting || !canRun('accept')" @click="run('accept')">受理</button>
          <button class="primary-btn" :disabled="submitting || !canRun('resolve')" @click="run('resolve')">处理完成</button>
          <button class="secondary-btn" :disabled="submitting || !canRun('close')" @click="run('close')">关闭</button>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page-grid.two-col {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(360px, 1fr);
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
  gap: 8px;
}
.toolbar-actions select {
  width: 140px;
}
.toolbar-actions input {
  width: 150px;
}
tbody tr {
  cursor: pointer;
}
tr.active {
  background: #fff5f6;
}
.empty-cell {
  text-align: center;
  padding: 24px;
  color: #86909c;
}
.detail {
  display: grid;
  gap: 10px;
}
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.detail-content {
  white-space: pre-wrap;
}
.kv {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.kv span {
  color: #86909c;
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
.action-row {
  display: flex;
  gap: 8px;
}
</style>
