<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import {
  closeMerchantSession,
  fetchMerchantSessionDetail,
  fetchMerchantSessions,
  fetchSessionTemplates,
  sendMerchantMessage,
} from '../api';
import type { SupportMessageView, SupportSessionView } from '../types';

const props = defineProps<{ refreshKey: number }>();
const emit = defineEmits<{ notice: [message: string] }>();

const loading = ref(false);
const filter = ref<'all' | 'open' | 'closed'>('all');
const sessions = ref<SupportSessionView[]>([]);
const activeSession = ref<SupportSessionView | null>(null);
const messages = ref<SupportMessageView[]>([]);
const draft = ref('');
const sending = ref(false);
const templates = ref<string[]>([]);
const autoReplyRules = [
  { keyword: '配送 / 多久 / 催 / 慢', reply: '自动回复催单与时效说明' },
  { keyword: '退款 / 退单 / 取消', reply: '自动提示补充订单状态' },
  { keyword: '发票 / 票据', reply: '自动提示补充抬头和联系方式' },
];

const filterOptions: Array<{ value: typeof filter.value; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'open', label: '进行中' },
  { value: 'closed', label: '已关闭' },
];

onMounted(async () => {
  await Promise.all([load(), loadTemplates()]);
});
watch(() => props.refreshKey, load);
watch(filter, load);

async function load() {
  try {
    loading.value = true;
    const params: Parameters<typeof fetchMerchantSessions>[0] = { pageSize: 50 };
    if (filter.value !== 'all') params.status = filter.value;
    const page = await fetchMerchantSessions(params);
    sessions.value = page.list;
    if (activeSession.value) {
      const refreshed = sessions.value.find(s => s.id === activeSession.value!.id) || null;
      if (refreshed) {
        await openSession(refreshed);
      } else {
        activeSession.value = null;
        messages.value = [];
      }
    }
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function loadTemplates() {
  try {
    templates.value = await fetchSessionTemplates();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function openSession(session: SupportSessionView) {
  try {
    const detail = await fetchMerchantSessionDetail(session.id);
    activeSession.value = detail.session;
    messages.value = detail.messages;
    draft.value = '';
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function send() {
  if (!activeSession.value) return;
  const content = draft.value.trim();
  if (!content) return;
  try {
    sending.value = true;
    const msg = await sendMerchantMessage(activeSession.value.id, content);
    messages.value = [...messages.value, msg];
    draft.value = '';
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    sending.value = false;
  }
}

async function closeSession() {
  if (!activeSession.value) return;
  if (!confirm('确认关闭本次会话？关闭后无法继续回复')) return;
  try {
    const closed = await closeMerchantSession(activeSession.value.id);
    activeSession.value = closed;
    emit('notice', '会话已关闭');
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function useTemplate(text: string) {
  draft.value = draft.value ? `${draft.value} ${text}` : text;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="page-grid two-col">
    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>客服会话</h2>
        <div class="toolbar-actions">
          <select v-model="filter">
            <option v-for="opt in filterOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '刷新中' : '刷新' }}</button>
        </div>
      </div>
      <div class="session-list">
        <button
          v-for="row in sessions"
          :key="row.id"
          class="session-row"
          :class="{ active: activeSession && activeSession.id === row.id }"
          @click="openSession(row)"
        >
          <div class="row-head">
            <strong>{{ row.userMaskedNickname || '用户' }}</strong>
            <span class="status" :class="row.status">{{ row.status === 'open' ? '进行中' : '已关闭' }}</span>
          </div>
          <div class="row-topic">{{ row.topic }}</div>
          <div class="row-foot">
            <small>{{ row.lastMessage || '暂无消息' }}</small>
            <small v-if="row.unreadCount > 0" class="unread">未读 {{ row.unreadCount }}</small>
          </div>
        </button>
        <div v-if="sessions.length === 0" class="empty-card">暂无会话</div>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>{{ activeSession?.userMaskedNickname || '消息面板' }}</h2>
        <button
          v-if="activeSession?.status === 'open'"
          class="secondary-btn small"
          @click="closeSession"
        >关闭会话</button>
      </div>
      <div v-if="!activeSession" class="empty-card">在左侧选择一个会话开始</div>
      <div v-else class="chat-area">
        <div class="message-list">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="bubble"
            :class="msg.senderType"
          >
            <p>{{ msg.content }}</p>
            <small>{{ timeText(msg.createdAt) }}</small>
          </div>
          <div v-if="messages.length === 0" class="empty-card">暂无消息</div>
        </div>
        <div v-if="templates.length" class="templates">
          <span class="tip">快捷回复</span>
          <button
            v-for="t in templates"
            :key="t"
            type="button"
            class="template-chip"
            :disabled="activeSession.status !== 'open'"
            @click="useTemplate(t)"
          >{{ t }}</button>
        </div>
        <div class="auto-rules">
          <span class="tip">关键词自动回复</span>
          <span v-for="rule in autoReplyRules" :key="rule.keyword" class="rule-chip">
            {{ rule.keyword }}：{{ rule.reply }}
          </span>
        </div>
        <form class="composer" @submit.prevent="send">
          <textarea
            v-model="draft"
            rows="3"
            :disabled="activeSession.status !== 'open'"
            :placeholder="activeSession.status === 'open' ? '输入消息…' : '会话已关闭'"
          ></textarea>
          <button
            class="primary-btn"
            :disabled="sending || activeSession.status !== 'open'"
          >{{ sending ? '发送中' : '发送' }}</button>
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
  border-radius: 4px;
  background: #fff;
  text-align: left;
}
.session-row.active {
  border-color: #f0b7c2;
  background: #fff5f6;
}
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.status {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
}
.status.open {
  color: #0f7a3d;
  border: 1px solid #bde7cf;
  background: #f1fbf5;
}
.status.closed {
  color: #4e5969;
  border: 1px solid #edf0f4;
  background: #f7f8fa;
}
.row-topic {
  color: #4e5969;
  font-size: 13px;
}
.row-foot {
  display: flex;
  justify-content: space-between;
}
.row-foot small {
  color: #86909c;
}
.row-foot small.unread {
  color: #e4002b;
  font-weight: 700;
}
.chat-area {
  display: grid;
  grid-template-rows: minmax(240px, 1fr) auto auto;
  gap: 10px;
}
.message-list {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid #edf0f4;
  border-radius: 4px;
  min-height: 240px;
  max-height: 420px;
  overflow-y: auto;
}
.bubble {
  display: grid;
  gap: 4px;
  padding: 8px 10px;
  border-radius: 4px;
  max-width: 80%;
}
.bubble.user {
  background: #f7f8fa;
  justify-self: flex-start;
}
.bubble.merchant {
  background: #fff5f6;
  justify-self: flex-end;
}
.bubble.system {
  background: #f5f6f8;
  justify-self: center;
}
.bubble small {
  color: #86909c;
  font-size: 11px;
}
.templates {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.auto-rules {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
.auto-rules .tip {
  color: #86909c;
  font-size: 12px;
}
.rule-chip {
  padding: 4px 8px;
  border: 1px solid #d9dee7;
  border-radius: 4px;
  background: #f7f8fa;
  color: #4e5969;
  font-size: 12px;
}
.templates .tip {
  color: #86909c;
  font-size: 12px;
}
.template-chip {
  padding: 4px 8px;
  border: 1px solid #d9dee7;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  height: 24px;
  min-height: 24px;
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
