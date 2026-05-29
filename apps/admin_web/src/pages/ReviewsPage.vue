<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { auditGovernanceReview, fetchGovernanceReviews, resolveAssetUrl } from '../api';
import type { AdminReviewView } from '../types';

const props = defineProps<{ refreshKey: number }>();
const emit = defineEmits<{ notice: [message: string] }>();

const loading = ref(false);
const reviews = ref<AdminReviewView[]>([]);
const active = ref<AdminReviewView | null>(null);
const auditRemark = ref('');
const submitting = ref(false);
const filter = ref<'all' | 'published' | 'hidden' | 'reported'>('all');

const filterOptions: Array<{ value: typeof filter.value; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'published', label: '已发布' },
  { value: 'hidden', label: '已屏蔽' },
  { value: 'reported', label: '被举报' },
];

onMounted(load);
watch(() => props.refreshKey, load);
watch(filter, load);

async function load() {
  try {
    loading.value = true;
    const params: Parameters<typeof fetchGovernanceReviews>[0] = { pageSize: 40 };
    if (filter.value === 'published' || filter.value === 'hidden') params.status = filter.value;
    if (filter.value === 'reported') params.reported = true;
    const page = await fetchGovernanceReviews(params);
    reviews.value = page.list;
    if (active.value) {
      active.value = reviews.value.find(r => r.id === active.value!.id) || null;
    }
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

function openReview(review: AdminReviewView) {
  active.value = review;
  auditRemark.value = '';
}

async function audit(action: 'pass' | 'hide' | 'restore') {
  if (!active.value) return;
  try {
    submitting.value = true;
    const updated = await auditGovernanceReview(active.value.id, action, auditRemark.value);
    active.value = updated;
    emit('notice', `已${actionLabel(action)}`);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    submitting.value = false;
  }
}

function actionLabel(action: string) {
  const map: Record<string, string> = { pass: '通过', hide: '屏蔽', restore: '恢复' };
  return map[action] || action;
}

function statusBadge(status: string) {
  return status === 'hidden' ? '已屏蔽' : status === 'reported' ? '被举报' : '已发布';
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="page-grid two-col">
    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>评价审核</h2>
        <div class="toolbar-actions">
          <select v-model="filter">
            <option v-for="opt in filterOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '刷新中' : '刷新' }}</button>
        </div>
      </div>
      <table>
        <thead>
          <tr>
            <th>评分</th>
            <th>门店</th>
            <th>用户</th>
            <th>内容</th>
            <th>状态</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="row in reviews"
            :key="row.id"
            :class="{ active: active && active.id === row.id }"
            @click="openReview(row)"
          >
            <td>{{ '★'.repeat(row.rating) }}</td>
            <td>{{ row.storeName }}<span>{{ row.orderTitle }}</span></td>
            <td>{{ row.userMaskedNickname || '匿名' }}</td>
            <td>{{ row.content }}<span v-if="(row.reportedCount || 0) > 0">举报 {{ row.reportedCount }} 次</span></td>
            <td>{{ statusBadge(row.status) }}</td>
            <td>{{ timeText(row.createdAt) }}</td>
          </tr>
          <tr v-if="reviews.length === 0">
            <td colspan="6" class="empty-cell">暂无评价</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar"><h2>审核操作</h2></div>
      <div v-if="!active" class="empty-card">在左侧选择评价后执行审核</div>
      <div v-else class="detail">
        <div class="detail-head">
          <span class="rating">{{ '★'.repeat(active.rating) }}</span>
          <strong>{{ active.userMaskedNickname || '匿名' }}</strong>
          <small>{{ timeText(active.createdAt) }}</small>
        </div>
        <p class="detail-content">{{ active.content }}</p>
        <div v-if="active.labels && active.labels.length" class="tag-row">
          <span v-for="label in active.labels" :key="label" class="tag">{{ label }}</span>
        </div>
        <div v-if="active.imageUrls && active.imageUrls.length" class="image-grid">
          <img v-for="url in active.imageUrls" :key="url" :src="resolveAssetUrl(url)" alt="评价图片" />
        </div>
        <div v-if="active.reportReasons && active.reportReasons.length" class="report-card">
          <strong>举报原因</strong>
          <p v-for="(r, i) in active.reportReasons" :key="i">· {{ r }}</p>
        </div>
        <div v-if="active.replied" class="reply-card">
          <strong>商家回复</strong>
          <p>{{ active.replyContent }}</p>
          <small>{{ timeText(active.repliedAt) }}</small>
        </div>
        <label>审核备注（可选）
          <input v-model="auditRemark" placeholder="例如：广告，已屏蔽" />
        </label>
        <div class="action-row">
          <button class="secondary-btn" :disabled="submitting" @click="audit('pass')">标记通过</button>
          <button class="primary-btn" :disabled="submitting" @click="audit('hide')">屏蔽</button>
          <button class="secondary-btn" :disabled="submitting" @click="audit('restore')">恢复</button>
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
  gap: 12px;
}
.detail-head {
  display: flex;
  gap: 8px;
  align-items: center;
}
.detail-head .rating {
  color: #fa8c16;
  font-weight: 700;
}
.detail-head small {
  color: #86909c;
}
.detail-content {
  white-space: pre-wrap;
}
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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
.reply-card,
.report-card {
  display: grid;
  gap: 4px;
  padding: 10px;
  background: #f7f8fa;
  border-radius: 6px;
}
.report-card {
  background: #fff4f6;
}
.action-row {
  display: flex;
  gap: 8px;
}
</style>
