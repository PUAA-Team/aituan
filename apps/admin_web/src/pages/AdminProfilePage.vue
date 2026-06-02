<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchAdminProfile } from '../api';
import type { AdminProfile } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const profile = ref<AdminProfile | null>(null);

watch(() => props.refreshKey, load, { immediate: true });

async function load() {
  try {
    profile.value = await fetchAdminProfile();
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
        <h2>管理员资料</h2>
        <span v-if="profile" class="tag" :class="profile.status">{{ profile.status }}</span>
      </div>

      <div v-if="profile" class="info-list profile-info-list">
        <div><strong>显示名称</strong><span>{{ profile.nickname || profile.accountNo }}</span></div>
        <div><strong>账号编号</strong><span>{{ profile.accountNo }}</span></div>
        <div><strong>账号类型</strong><span>{{ profile.accountType }}</span></div>
        <div><strong>账号状态</strong><span>{{ profile.status }}</span></div>
      </div>
      <div v-else class="empty-card">正在加载管理员资料</div>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>联系方式与登录信息</h2>
      </div>
      <div v-if="profile" class="info-list profile-info-list">
        <div><strong>手机号</strong><span>{{ profile.phone || '-' }}</span></div>
        <div><strong>邮箱</strong><span>{{ profile.email || '-' }}</span></div>
        <div><strong>创建时间</strong><span>{{ timeText(profile.createdAt) }}</span></div>
        <div><strong>最近登录</strong><span>{{ timeText(profile.lastLoginAt) }}</span></div>
      </div>
      <div v-else class="empty-card">暂无账号信息</div>
    </section>
  </section>
</template>
