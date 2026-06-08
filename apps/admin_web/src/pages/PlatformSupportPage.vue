<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import {
  fetchPlatformSupportSessionDetail,
  fetchPlatformSupportSessions,
  sendPlatformSupportMessage,
} from '../api';
import type { AdminSupportMessageView, AdminSupportSessionView } from '../types';

const props = defineProps<{ refreshKey: number }>();
const emit = defineEmits<{ notice: [message: string] }>();

const loading = ref(false);
const status = ref<'all' | 'open' | 'closed'>('open');
const sessions = ref<AdminSupportSessionView[]>([]);
const active = ref<AdminSupportSessionView | null>(null);
const messages = ref<AdminSupportMessageView[]>([]);
const draft = ref('');
const sending = ref(false);

const statusOptions: Array<{ value: typeof status.value; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'open', label: '进行中' },
  { value: 'closed', label: '已关闭' },
];

onMounted(load);
watch(() => props.refreshKey, load);
watch(status, load);

async function load() {
  try {
    loading.value = true;
    const params: Parameters<typeof fetchPlatformSupportSessions>[0] = { pageSize: 50 };
    if (status.value !== 'all') params.status = status.value;
    const page = await fetchPlatformSupportSessions(params);
    sessions.value = page.list;
    if (active.value) {
      const refreshed = sessions.value.find(s => s.id === active.value!.id) || null;
      if (refreshed) {
        await openSession(refreshed);
      } else {
        active.value = null;
        messages.value = [];
      }
    }
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function openSession(session: AdminSupportSessionView) {
  try {
    const detail = await fetchPlatformSupportSessionDetail(session.id);
    active.value = detail.session;
    messages.value = detail.messages;
    draft.value = '';
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function send() {
  if (!active.value) return;
  const content = draft.value.trim();
  if (!content) return;
  try {
    sending.value = true;
    const msg = await sendPlatformSupportMessage(active.value.id, content);
    messages.value = [...messages.value, msg];
    draft.value = '';
    emit('notice', '平台人工回复已发送');
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    sending.value = false;
  }
}

function submitOnEnter(event: KeyboardEvent) {
  if (event.shiftKey || event.isComposing) return;
  event.preventDefault();
  void send();
}

function openComplaintEntry() {
  const query = new URLSearchParams({ page: 'complaints' });
  if (active.value?.relatedOrderNo) {
    query.set('orderNo', active.value.relatedOrderNo);
  }
  if (active.value?.storeName) {
    query.set('storeName', active.value.storeName);
  }
  window.open(`${window.location.origin}/?${query}`, '_blank');
}

function modeLabel(session: AdminSupportSessionView) {
  if (session.platformInterventionStatus === 'active') return '商家申请介入';
  return session.assistantMode === 'human' ? '用户转人工' : 'AI 接待';
}

function senderLabel(type: string) {
  const map: Record<string, string> = {
    user: '用户',
    merchant: '商家客服',
    platform: '平台客服',
    system: '系统消息',
  };
  return map[type] || type;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="page-grid two-col">
    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>平台客服队列</h2>
        <div class="toolbar-actions">
          <select v-model="status">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '刷新中' : '刷新' }}</button>
        </div>
      </div>
      <div class="session-list">
        <button
          v-for="row in sessions"
          :key="row.id"
          class="session-row"
          :class="{ active: active && active.id === row.id }"
          @click="openSession(row)"
        >
          <div class="row-head">
            <strong>{{ row.userMaskedNickname || '用户' }}</strong>
            <span class="status" :class="row.status">{{ row.status === 'open' ? '进行中' : '已关闭' }}</span>
          </div>
          <div class="row-topic">{{ row.storeName }} · {{ modeLabel(row) }}</div>
          <div class="row-foot">
            <small>{{ row.lastMessage || '暂无消息' }}</small>
            <small>{{ timeText(row.lastMessageAt || row.createdAt) }}</small>
          </div>
        </button>
        <div v-if="sessions.length === 0" class="empty-card">暂无平台客服会话</div>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>{{ active?.storeName || '人工处理' }}</h2>
        <div v-if="active" class="toolbar-actions">
          <button class="secondary-btn small" @click="openComplaintEntry">投诉工单</button>
          <span class="tag">{{ modeLabel(active) }}</span>
        </div>
      </div>
      <div v-if="!active" class="empty-card">在左侧选择需要处理的会话</div>
      <div v-else class="chat-area">
        <div class="meta-grid">
          <div><span>会话号</span><strong>{{ active.sessionNo }}</strong></div>
          <div><span>关联订单</span><strong>{{ active.relatedOrderNo || '-' }}</strong></div>
          <div><span>状态</span><strong>{{ active.status === 'open' ? '进行中' : '已关闭' }}</strong></div>
        </div>
        <div class="message-list">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="bubble"
            :class="msg.senderType"
          >
            <p>{{ msg.content }}</p>
            <small>{{ senderLabel(msg.senderType) }} · {{ timeText(msg.createdAt) }}</small>
          </div>
          <div v-if="messages.length === 0" class="empty-card">暂无消息</div>
        </div>
        <form class="composer" @submit.prevent="send">
          <textarea
            v-model="draft"
            rows="3"
            :disabled="active.status !== 'open'"
            :placeholder="active.status === 'open' ? '输入平台人工回复…' : '会话已关闭'"
            @keydown.enter="submitOnEnter"
          ></textarea>
          <button class="primary-btn" :disabled="sending || active.status !== 'open'">
            {{ sending ? '发送中' : '发送' }}
          </button>
        </form>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page-grid.two-col {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) minmax(0, 1.6fr);
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
  width: 120px;
}
.session-list {
  display: grid;
  gap: 8px;
}
.session-row {
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid #edf0f4;
  border-radius: 6px;
  background: #fff;
  text-align: left;
}
.session-row.active {
  border-color: #85a5ff;
  background: #f0f5ff;
}
.row-head,
.row-foot {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}
.row-topic,
.row-foot small {
  color: #86909c;
  font-size: 13px;
}
.status,
.tag {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  background: #e6f4ff;
  color: #0958d9;
}
.status.closed {
  color: #595959;
  background: #f0f0f0;
}
.chat-area {
  display: grid;
  gap: 10px;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}
.meta-grid div {
  display: grid;
  gap: 4px;
  padding: 8px;
  border: 1px solid #edf0f4;
  border-radius: 6px;
}
.meta-grid span {
  color: #86909c;
  font-size: 12px;
}
.message-list {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid #edf0f4;
  border-radius: 6px;
  min-height: 260px;
  max-height: 440px;
  overflow-y: auto;
}
.bubble {
  display: grid;
  gap: 4px;
  padding: 8px 10px;
  border-radius: 8px;
  max-width: 80%;
}
.bubble.user {
  background: #f0f0f0;
  justify-self: flex-start;
}
.bubble.platform {
  background: #e6f4ff;
  justify-self: flex-end;
}
.bubble.merchant {
  background: #fff5f6;
  justify-self: flex-end;
}
.bubble small {
  color: #86909c;
  font-size: 11px;
}
.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  align-items: end;
}
.composer textarea {
  border: 1px solid #d9dee7;
  border-radius: 4px;
  padding: 8px;
  resize: vertical;
  font: inherit;
}
</style>
