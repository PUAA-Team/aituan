<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchAuditLogs, fetchConfigs, updateConfig } from '../api';
import type { AdminConfig, AuditLog } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const configs = ref<AdminConfig[]>([]);
const audits = ref<AuditLog[]>([]);
const actionType = ref('');

watch(() => props.refreshKey, load, { immediate: true });

async function load() {
  await Promise.all([loadConfigs(), loadAudits()]);
}

async function loadConfigs() {
  try {
    configs.value = await fetchConfigs();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function loadAudits() {
  try {
    audits.value = (await fetchAuditLogs(actionType.value)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function saveConfig(item: AdminConfig) {
  try {
    configs.value = await updateConfig(item.configKey, item.configValue, item.remark);
    emit('notice', `${item.configKey} 已保存`);
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
      <div class="panel-toolbar"><h2>平台设置</h2></div>
      <div class="config-list">
        <form v-for="item in configs" :key="item.configKey" class="config-row" @submit.prevent="saveConfig(item)">
          <strong>{{ item.configKey }}</strong>
          <input v-model="item.configValue" />
          <input v-model="item.remark" placeholder="备注" />
          <button class="secondary-btn small">保存</button>
        </form>
        <div v-if="configs.length === 0" class="empty-card">暂无配置</div>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>审计日志</h2>
        <div class="toolbar-actions">
          <input v-model="actionType" class="search-input" placeholder="动作类型" @keyup.enter="loadAudits" />
          <button class="secondary-btn" @click="loadAudits">查询</button>
        </div>
      </div>
      <div class="audit-list">
        <div v-for="item in audits" :key="item.id" class="audit-row">
          <strong>{{ item.actionType }}</strong>
          <span>{{ item.actorType }} #{{ item.actorId }} → {{ item.targetType }} #{{ item.targetId }}</span>
          <small>{{ timeText(item.createdAt) }} {{ item.detail }}</small>
        </div>
        <div v-if="audits.length === 0" class="empty-card">暂无审计日志</div>
      </div>
    </section>
  </section>
</template>
