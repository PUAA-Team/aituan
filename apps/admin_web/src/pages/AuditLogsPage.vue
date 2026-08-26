<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { fetchAuditLogs } from '../api';
import type { AuditLog } from '../types';

const props = defineProps<{ refreshKey: number }>();
const emit = defineEmits<{ notice: [message: string] }>();

const loading = ref(false);
const logs = ref<AuditLog[]>([]);
const actionType = ref('');
const actorFilter = ref<'all' | 'user' | 'merchant' | 'admin'>('all');

onMounted(load);
watch(() => props.refreshKey, load);

async function load() {
  try {
    loading.value = true;
    const page = await fetchAuditLogs(actionType.value.trim());
    logs.value = actorFilter.value === 'all'
      ? page.list
      : page.list.filter(l => l.actorType === actorFilter.value);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

function actorBadge(type: string) {
  const map: Record<string, string> = { user: '用户', merchant: '商家', admin: '管理员' };
  return map[type] || type;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}
</script>

<template>
  <section class="panel-card">
    <div class="panel-toolbar">
      <h2>审计日志</h2>
      <div class="toolbar-actions">
        <select v-model="actorFilter" @change="load">
          <option value="all">全部操作人</option>
          <option value="user">用户</option>
          <option value="merchant">商家</option>
          <option value="admin">管理员</option>
        </select>
        <input v-model="actionType" class="search-input" placeholder="动作类型，如 review_audit" @keyup.enter="load" />
        <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '查询中' : '查询' }}</button>
      </div>
    </div>
    <table>
      <thead>
        <tr>
          <th>时间</th>
          <th>操作人</th>
          <th>动作</th>
          <th>对象</th>
          <th>详情</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in logs" :key="row.id">
          <td>{{ timeText(row.createdAt) }}</td>
          <td>{{ actorBadge(row.actorType) }} #{{ row.actorId }}</td>
          <td>{{ row.actionType }}</td>
          <td>{{ row.targetType }} #{{ row.targetId }}</td>
          <td>{{ row.detail }}</td>
        </tr>
        <tr v-if="logs.length === 0">
          <td colspan="5" class="empty-cell">暂无审计日志</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
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
  width: 140px;
}
.search-input {
  width: 220px;
}
.empty-cell {
  text-align: center;
  padding: 24px;
  color: #86909c;
}
</style>
