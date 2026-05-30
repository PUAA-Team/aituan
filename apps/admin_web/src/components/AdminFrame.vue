<script setup lang="ts">
import type { AdminPage } from '../types';

const props = defineProps<{
  activePage: AdminPage;
  title: string;
  loading: boolean;
  notice: string;
}>();

const emit = defineEmits<{
  navigate: [page: AdminPage];
  refresh: [];
  logout: [];
}>();

const navItems: Array<{ key: AdminPage; label: string; desc: string }> = [
  { key: 'dashboard', label: '平台总览', desc: '核心指标与风险概览' },
  { key: 'orders', label: '订单治理', desc: '订单详情与人工干预' },
  { key: 'merchants', label: '商户门店', desc: '商家、门店、图片治理' },
  { key: 'users', label: '用户管理', desc: '账号状态与用户画像' },
  { key: 'catalog', label: '商品治理', desc: '商品查询与上下架' },
  { key: 'delivery', label: '配送任务', desc: '自动推进与异常处理' },
  { key: 'vouchers', label: '券码治理', desc: '券码查询与平台核销' },
  { key: 'bookings', label: '预约治理', desc: '预约确认与到店时间' },
  { key: 'announcements', label: '公告运营', desc: '客户端公告发布' },
  { key: 'settings', label: '设置审计', desc: '平台配置与操作日志' },
];
</script>

<template>
  <section class="admin-layout">
    <aside class="admin-side">
      <div class="brand-block">
        <span>AITUAN ADMIN</span>
        <strong>平台治理后台</strong>
      </div>
      <nav class="side-nav">
        <button
          v-for="item in navItems"
          :key="item.key"
          :class="{ active: props.activePage === item.key }"
          @click="emit('navigate', item.key)"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.desc }}</small>
        </button>
      </nav>
      <div class="side-footer">
        <span>管理员控制台</span>
        <button class="text-btn" @click="emit('logout')">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="topbar">
        <div>
          <span class="section-kicker">OPERATION CENTER</span>
          <h1>{{ props.title }}</h1>
        </div>
        <button class="secondary-btn" :disabled="props.loading" @click="emit('refresh')">
          {{ props.loading ? '刷新中' : '刷新' }}
        </button>
      </header>
      <p v-if="props.notice" class="notice-bar">{{ props.notice }}</p>
      <slot />
    </main>
  </section>
</template>
