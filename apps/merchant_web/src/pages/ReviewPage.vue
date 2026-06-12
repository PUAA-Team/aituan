<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import {
  fetchMerchantReviewDetail,
  fetchMerchantReviews,
  replyMerchantReview,
  resolveAssetUrl,
} from '../api';
import type { ReviewView } from '../types';

const props = defineProps<{ refreshKey: number }>();
const emit = defineEmits<{ notice: [message: string] }>();

const loading = ref(false);
const filter = ref<'all' | 'pending' | 'replied' | 'reported'>('all');
const reviews = ref<ReviewView[]>([]);
const active = ref<ReviewView | null>(null);
const replyText = ref('');
const submitting = ref(false);

const filterOptions: Array<{ value: typeof filter.value; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'pending', label: '待回复' },
  { value: 'replied', label: '已回复' },
  { value: 'reported', label: '被举报' },
];

onMounted(load);
watch(() => props.refreshKey, load);
watch(filter, load);

async function load() {
  try {
    loading.value = true;
    const params: Parameters<typeof fetchMerchantReviews>[0] = { pageSize: 50 };
    if (filter.value === 'pending') params.replied = false;
    if (filter.value === 'replied') params.replied = true;
    if (filter.value === 'reported') params.status = 'reported';
    const page = await fetchMerchantReviews(params);
    reviews.value = page.list;
    if (active.value) {
      const refreshed = reviews.value.find(r => r.id === active.value!.id) || null;
      active.value = refreshed;
    }
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function openReview(review: ReviewView) {
  try {
    active.value = await fetchMerchantReviewDetail(review.id);
    replyText.value = active.value?.replyContent || '';
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function submitReply() {
  if (!active.value) return;
  const content = replyText.value.trim();
  if (!content) {
    emit('notice', '请填写回复内容');
    return;
  }
  try {
    submitting.value = true;
    const updated = await replyMerchantReview(active.value.id, content);
    active.value = updated;
    emit('notice', '回复已发送');
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    submitting.value = false;
  }
}

function statusLabel(status: string) {
  return status === 'hidden' ? '已屏蔽' : '已发布';
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="page-grid two-col">
    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>评价列表</h2>
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
            <th>用户</th>
            <th>内容</th>
            <th>回复状态</th>
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
            <td>{{ row.userMaskedNickname || '匿名用户' }}<span>{{ row.orderTitle }}</span></td>
            <td>{{ row.content }}<span v-if="(row.reportedCount || 0) > 0">举报 {{ row.reportedCount }}</span></td>
            <td>{{ row.replied ? '已回复' : '待回复' }}<span>{{ statusLabel(row.status) }}</span></td>
            <td>{{ timeText(row.createdAt) }}</td>
          </tr>
          <tr v-if="reviews.length === 0">
            <td colspan="5" class="empty-cell">暂无评价</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar"><h2>评价详情</h2></div>
      <div v-if="!active" class="empty-card">在左侧选择一条评价查看与回复</div>
      <div v-else class="review-detail">
        <div class="review-head">
          <span class="rating">{{ '★'.repeat(active.rating) }}</span>
          <span>{{ active.userMaskedNickname || '匿名用户' }}</span>
          <small>{{ timeText(active.createdAt) }}</small>
        </div>
        <p class="review-content">{{ active.content }}</p>
        <div v-if="active.labels && active.labels.length" class="tag-row">
          <span v-for="label in active.labels" :key="label" class="tag">{{ label }}</span>
        </div>
        <div v-if="active.imageUrls && active.imageUrls.length" class="image-grid">
          <img
            v-for="url in active.imageUrls"
            :key="url"
            :src="resolveAssetUrl(url)"
            alt="评价图片"
          />
        </div>
        <div v-if="active.replied" class="reply-card">
          <strong>已回复</strong>
          <p>{{ active.replyContent }}</p>
          <small>{{ timeText(active.repliedAt) }}</small>
        </div>
        <form v-else class="reply-form" @submit.prevent="submitReply">
          <textarea v-model="replyText" rows="4" placeholder="感谢您的反馈…"></textarea>
          <div class="form-actions">
            <button class="primary-btn" :disabled="submitting">{{ submitting ? '发送中' : '发送回复' }}</button>
          </div>
        </form>
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
  align-items: center;
}
.toolbar-actions select {
  width: 120px;
}
tr.active {
  background: #fff5f6;
}
tbody tr {
  cursor: pointer;
}
.empty-cell {
  text-align: center;
  color: #86909c;
  padding: 24px;
}
.review-detail {
  display: grid;
  gap: 12px;
}
.review-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.review-head .rating {
  color: #e4002b;
  font-weight: 700;
}
.review-head small {
  color: #86909c;
}
.review-content {
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
  border-radius: 4px;
}
.reply-card {
  display: grid;
  gap: 4px;
  border: 1px solid #edf0f4;
  border-radius: 4px;
  padding: 10px;
  background: #f7f8fa;
}
.reply-card small {
  color: #86909c;
}
.reply-form textarea {
  width: 100%;
  min-height: 96px;
  border: 1px solid #d9dee7;
  border-radius: 4px;
  padding: 10px;
  resize: vertical;
  font: inherit;
}
.reply-form .form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
