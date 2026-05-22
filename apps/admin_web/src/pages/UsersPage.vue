<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchUsers, resolveAssetUrl, updateUserStatus } from '../api';
import type { AdminUser } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const keyword = ref('');
const users = ref<AdminUser[]>([]);

watch(() => props.refreshKey, load, { immediate: true });

async function load() {
  try {
    users.value = (await fetchUsers(keyword.value)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function setStatus(user: AdminUser, status: string) {
  try {
    const updated = await updateUserStatus(user.accountId, status);
    users.value = users.value.map((item) => (item.accountId === updated.accountId ? updated : item));
    emit('notice', `${updated.nickname} 状态已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="panel-card">
    <div class="panel-toolbar">
      <h2>用户管理</h2>
      <div class="toolbar-actions">
        <input v-model="keyword" class="search-input" placeholder="昵称/手机号/邮箱" @keyup.enter="load" />
        <button class="secondary-btn" @click="load">查询</button>
      </div>
    </div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>用户</th><th>联系方式</th><th>资产</th><th>状态</th><th>注册时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="user in users" :key="user.accountId">
            <td>
              <div class="user-cell">
                <img v-if="user.avatarUrl" :src="resolveAssetUrl(user.avatarUrl)" :alt="user.nickname" />
                <i v-else>{{ user.nickname?.slice(0, 1) || '用' }}</i>
                <strong>{{ user.nickname }}</strong>
              </div>
              <span>#{{ user.accountId }}</span>
            </td>
            <td>{{ user.phone || '-' }}<span>{{ user.email || '-' }}</span></td>
            <td>{{ user.orderCount }} 个订单<span>{{ user.addressCount }} 个地址</span></td>
            <td><span class="tag" :class="user.status">{{ user.status }}</span></td>
            <td>{{ timeText(user.createdAt) }}</td>
            <td>
              <div class="row-actions">
                <button class="secondary-btn small" @click="setStatus(user, user.status === 'normal' ? 'disabled' : 'normal')">
                  {{ user.status === 'normal' ? '停用' : '启用' }}
                </button>
                <button class="secondary-btn small" @click="setStatus(user, 'blocked')">拉黑</button>
              </div>
            </td>
          </tr>
          <tr v-if="users.length === 0"><td colspan="6" class="empty-cell">暂无用户</td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
