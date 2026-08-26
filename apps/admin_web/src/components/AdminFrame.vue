<script setup lang="ts">
import { computed } from 'vue';
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

type NavItem = { key: AdminPage; label: string; desc: string; icon: string };
type NavGroup = { title: string; detail: string; items: NavItem[] };

const navGroups: NavGroup[] = [
  {
    title: '总览',
    detail: '平台运行与风险',
    items: [
      { key: 'dashboard', label: '平台总览', desc: '核心指标与风险概览', icon: '总' },
    ],
  },
  {
    title: '交易履约',
    detail: '订单、预约、券码、配送',
    items: [
      { key: 'orders', label: '订单治理', desc: '订单详情与人工干预', icon: '单' },
      { key: 'bookings', label: '预约治理', desc: '预约确认与到店时间', icon: '约' },
      { key: 'vouchers', label: '券码治理', desc: '券码查询与平台核销', icon: '券' },
      { key: 'delivery', label: '配送任务', desc: '自动推进与异常处理', icon: '配' },
    ],
  },
  {
    title: '商户与商品',
    detail: '门店、商品与服务治理',
    items: [
      { key: 'merchants', label: '商户门店', desc: '商家、门店、图片治理', icon: '店' },
      { key: 'catalog', label: '商品治理', desc: '商品查询与上下架', icon: '品' },
    ],
  },
  {
    title: '用户资产',
    detail: '账号、会员与优惠券',
    items: [
      { key: 'users', label: '用户管理', desc: '账号状态与用户画像', icon: '客' },
      { key: 'memberLevels', label: '会员等级', desc: '成长值与会员权益', icon: '会' },
      { key: 'couponTemplates', label: '优惠券模板', desc: '领券与结算抵扣', icon: '惠' },
    ],
  },
  {
    title: '内容治理',
    detail: '评价、投诉、客服、审计',
    items: [
      { key: 'reviews', label: '评价审核', desc: '违规评价屏蔽与恢复', icon: '评' },
      { key: 'complaints', label: '投诉工单', desc: '受理、处理、关闭', icon: '诉' },
      { key: 'support', label: '平台客服', desc: 'AI 转人工与平台介入', icon: '服' },
      { key: 'audit', label: '审计日志', desc: '全量操作回溯', icon: '审' },
    ],
  },
  {
    title: '系统',
    detail: '账号资料与平台参数',
    items: [
      { key: 'profile', label: '管理员资料', desc: '当前后台账号信息', icon: '管' },
      { key: 'settings', label: '平台设置', desc: '运行参数配置', icon: '设' },
    ],
  },
];

const currentItem = computed(() => navGroups.flatMap((group) => group.items).find((item) => item.key === props.activePage));
</script>

<template>
  <section class="admin-layout">
    <aside class="admin-side">
      <div class="brand-block">
        <span class="brand-eyebrow">AITUAN ADMIN</span>
        <strong>平台治理后台</strong>
        <small>按治理场景重构导航，主内容独立滚动</small>
      </div>

      <div class="side-scroll">
        <nav class="side-nav" aria-label="后台功能导航">
          <section v-for="group in navGroups" :key="group.title" class="nav-group">
            <p class="nav-group-title">
              <span>{{ group.title }}</span>
              <small>{{ group.detail }}</small>
            </p>
            <button
              v-for="item in group.items"
              :key="item.key"
              class="nav-item"
              :class="{ active: props.activePage === item.key }"
              @click="emit('navigate', item.key)"
            >
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-copy">
                <strong>{{ item.label }}</strong>
                <small>{{ item.desc }}</small>
              </span>
            </button>
          </section>
        </nav>
      </div>

      <div class="side-footer">
        <span>管理员控制台</span>
        <button class="text-btn" @click="emit('logout')">退出登录</button>
      </div>
    </aside>

    <main class="admin-main">
      <header class="topbar">
        <div class="topbar-title">
          <span class="section-kicker">OPERATION CENTER</span>
          <h1>{{ props.title }}</h1>
          <p>{{ currentItem?.desc || '平台运营工作台' }}</p>
        </div>
        <div class="topbar-actions">
          <span class="tag normal">Stage7 重构</span>
          <button class="secondary-btn" :disabled="props.loading" @click="emit('refresh')">
            {{ props.loading ? '刷新中' : '刷新' }}
          </button>
        </div>
      </header>
      <p v-if="props.notice" class="notice-bar">{{ props.notice }}</p>
      <slot />
    </main>
  </section>
</template>
