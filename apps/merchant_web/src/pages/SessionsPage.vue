<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import {
  closeMerchantSession,
  createAutoReplyRule,
  deleteAutoReplyRule,
  fetchAutoReplyRules,
  fetchMerchantSessionDetail,
  fetchMerchantSessions,
  fetchSessionTemplates,
  requestPlatformIntervention,
  sendMerchantMessage,
  updateAutoReplyRule,
} from '../api';
import type { SupportAutoReplyRuleView, SupportMessageView, SupportSessionView } from '../types';

const props = defineProps<{ refreshKey: number }>();
const emit = defineEmits<{
  notice: [message: string];
  openComplaints: [filters: { orderNo?: string; storeName?: string }];
}>();

const loading = ref(false);
const filter = ref<'all' | 'open' | 'closed'>('all');
const sessions = ref<SupportSessionView[]>([]);
const activeSession = ref<SupportSessionView | null>(null);
const messages = ref<SupportMessageView[]>([]);
const draft = ref('');
const sending = ref(false);
const intervening = ref(false);
const templates = ref<string[]>([]);
const autoReplyRules = ref<SupportAutoReplyRuleView[]>([]);
const ruleForm = ref({ id: 0, keywords: '', replyContent: '', enabled: true });
const savingRule = ref(false);

const filterOptions: Array<{ value: typeof filter.value; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'open', label: '进行中' },
  { value: 'closed', label: '已关闭' },
];

onMounted(async () => {
  await Promise.all([load(), loadTemplates(), loadAutoReplyRules()]);
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

async function loadAutoReplyRules() {
  try {
    autoReplyRules.value = await fetchAutoReplyRules();
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

async function requestIntervention() {
  if (!activeSession.value) return;
  if (!confirm('确认申请平台客服介入？介入后本会话将进入平台人工处理队列')) return;
  try {
    intervening.value = true;
    const updated = await requestPlatformIntervention(activeSession.value.id);
    activeSession.value = updated;
    emit('notice', '平台客服已介入');
    await openSession(updated);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    intervening.value = false;
  }
}

function openComplaintEntry() {
  emit('openComplaints', {
    orderNo: activeSession.value?.relatedOrderNo,
    storeName: activeSession.value?.storeName,
  });
}

function useTemplate(text: string) {
  draft.value = draft.value ? `${draft.value} ${text}` : text;
}

function editRule(rule: SupportAutoReplyRuleView) {
  ruleForm.value = {
    id: rule.id,
    keywords: rule.keywords,
    replyContent: rule.replyContent,
    enabled: rule.enabled,
  };
}

function resetRuleForm() {
  ruleForm.value = { id: 0, keywords: '', replyContent: '', enabled: true };
}

async function saveRule() {
  const keywords = ruleForm.value.keywords.trim();
  const replyContent = ruleForm.value.replyContent.trim();
  if (!keywords || !replyContent) {
    emit('notice', '请填写触发关键词和回复内容');
    return;
  }
  try {
    savingRule.value = true;
    const payload = { keywords, replyContent, enabled: ruleForm.value.enabled };
    if (ruleForm.value.id > 0) {
      await updateAutoReplyRule(ruleForm.value.id, payload);
      emit('notice', '自动回复规则已更新');
    } else {
      await createAutoReplyRule(payload);
      emit('notice', '自动回复规则已新增');
    }
    resetRuleForm();
    await loadAutoReplyRules();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    savingRule.value = false;
  }
}

async function removeRule(rule: SupportAutoReplyRuleView) {
  if (!confirm(`删除规则「${rule.keywords}」？`)) return;
  try {
    await deleteAutoReplyRule(rule.id);
    await loadAutoReplyRules();
    emit('notice', '自动回复规则已删除');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
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
        <div v-if="activeSession" class="toolbar-actions">
          <button class="secondary-btn small" @click="openComplaintEntry">投诉工单</button>
          <button
            v-if="activeSession.status === 'open' && activeSession.platformInterventionStatus !== 'active'"
            class="secondary-btn small"
            :disabled="intervening"
            @click="requestIntervention"
          >{{ intervening ? '介入中' : '平台介入' }}</button>
          <button
            v-if="activeSession.status === 'open'"
            class="secondary-btn small"
            @click="closeSession"
          >关闭会话</button>
        </div>
      </div>
      <div v-if="!activeSession" class="empty-card">在左侧选择一个会话开始</div>
      <div v-else class="chat-area">
        <div class="message-list">
          <div
            v-if="activeSession.platformInterventionStatus === 'active'"
            class="intervention-banner"
          >
            平台客服已介入，后续由平台人工协助处理
          </div>
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
          <div class="rule-head">
            <span class="tip">关键词自动回复</span>
            <button type="button" class="secondary-btn small" @click="resetRuleForm">新建规则</button>
          </div>
          <form class="rule-form" @submit.prevent="saveRule">
            <input v-model="ruleForm.keywords" placeholder="关键词，用逗号分隔，例如：停车,排队" />
            <input v-model="ruleForm.replyContent" placeholder="自动回复内容" />
            <label class="check-line">
              <input v-model="ruleForm.enabled" type="checkbox" />
              启用
            </label>
            <button class="primary-btn small" :disabled="savingRule">{{ savingRule ? '保存中' : '保存规则' }}</button>
          </form>
          <div class="rule-list">
            <div v-for="rule in autoReplyRules" :key="rule.id" class="rule-row">
              <span>{{ rule.keywords }}：{{ rule.replyContent }}</span>
              <small>{{ rule.enabled ? '启用' : '停用' }}</small>
              <button type="button" class="secondary-btn small" @click="editRule(rule)">编辑</button>
              <button type="button" class="secondary-btn small" @click="removeRule(rule)">删除</button>
            </div>
            <div v-if="autoReplyRules.length === 0" class="empty-card">暂无自动回复规则</div>
          </div>
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
  border-radius: 6px;
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
  color: #237804;
  background: #f6ffed;
}
.status.closed {
  color: #595959;
  background: #f0f0f0;
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
  color: #d4380d;
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
  border-radius: 6px;
  min-height: 240px;
  max-height: 420px;
  overflow-y: auto;
}
.intervention-banner {
  justify-self: stretch;
  padding: 8px 10px;
  border: 1px solid #bae0ff;
  border-radius: 6px;
  background: #e6f4ff;
  color: #0958d9;
  font-size: 13px;
  text-align: center;
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
.bubble.merchant {
  background: #fff5f6;
  justify-self: flex-end;
}
.bubble.platform {
  background: #e6f4ff;
  justify-self: center;
}
.bubble.system {
  background: #e6f4ff;
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
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
}
.auto-rules .tip {
  color: #86909c;
  font-size: 12px;
}
.rule-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.rule-form {
  display: grid;
  grid-template-columns: minmax(140px, 0.8fr) minmax(180px, 1.2fr) auto auto;
  gap: 8px;
  align-items: center;
}
.rule-form input:not([type]),
.rule-form input[type='text'] {
  height: 34px;
}
.check-line {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  white-space: nowrap;
  color: #4e5969;
  font-size: 13px;
}
.rule-list {
  display: grid;
  gap: 6px;
}
.rule-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  gap: 8px;
  align-items: center;
  padding: 8px;
  border: 1px solid #d9dee7;
  border-radius: 6px;
  background: #fff;
}
.rule-row span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rule-row small {
  color: #86909c;
}
.templates .tip {
  color: #86909c;
  font-size: 12px;
}
.template-chip {
  padding: 4px 8px;
  border: 1px solid #d9dee7;
  border-radius: 12px;
  background: #fff;
  font-size: 12px;
  height: 24px;
  min-height: 24px;
}
@media (max-width: 900px) {
  .rule-form,
  .rule-row {
    grid-template-columns: 1fr;
  }
  .rule-row span {
    white-space: normal;
  }
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
