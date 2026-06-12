<script setup lang="ts">
import { ref, watch } from 'vue';
import {
  fetchAuditLogs,
  fetchDashboard,
  fetchGovernanceDashboard,
  fetchStats,
} from '../api';
import type {
  AdminGovernanceDashboardView,
  AuditLog,
  DashboardView,
  OrderStatusCount,
} from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const dashboard = ref<DashboardView | null>(null);
const governance = ref<AdminGovernanceDashboardView | null>(null);
const stats = ref<OrderStatusCount[]>([]);
const audits = ref<AuditLog[]>([]);

watch(() => props.refreshKey, load, { immediate: true });

async function load() {
  try {
    const [summary, statList, auditPage, gov] = await Promise.all([
      fetchDashboard(),
      fetchStats(),
      fetchAuditLogs(),
      fetchGovernanceDashboard(),
    ]);
    dashboard.value = summary;
    stats.value = statList;
    audits.value = auditPage.list.slice(0, 8);
    governance.value = gov;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}

function ratingLabel(key: string) {
  const map: Record<string, string> = { five: '5 星', four: '4 星', three: '3 星', two: '2 星', one: '1 星' };
  return map[key] || key;
}

function ratingPercent(key: string, total: number) {
  if (!governance.value || total === 0) return 0;
  const v = governance.value.ratingDistribution?.[key] || 0;
  return Math.round((v / total) * 100);
}

function ratingTotal() {
  if (!governance.value) return 0;
  return Object.values(governance.value.ratingDistribution || {}).reduce((a, b) => a + Number(b || 0), 0);
}
</script>

<template>
  <section class="page-grid">
    <div class="metric-row">
      <article class="metric-card"><span>今日订单</span><strong>{{ dashboard?.todayOrders || 0 }}</strong></article>
      <article class="metric-card"><span>今日交易额</span><strong>{{ money(dashboard?.todayAmount) }}</strong></article>
      <article class="metric-card warning"><span>异常订单</span><strong>{{ dashboard?.abnormalOrders || 0 }}</strong></article>
      <article class="metric-card"><span>配送中任务</span><strong>{{ dashboard?.deliveringTasks || 0 }}</strong></article>
      <article class="metric-card"><span>商户数</span><strong>{{ dashboard?.merchantCount || 0 }}</strong></article>
      <article class="metric-card"><span>用户数</span><strong>{{ dashboard?.userCount || 0 }}</strong></article>
      <article class="metric-card"><span>商品服务</span><strong>{{ dashboard?.itemCount || 0 }}</strong></article>
    </div>

    <div class="metric-row">
      <article class="metric-card"><span>待审核评价</span><strong>{{ governance?.pendingReviews || 0 }}</strong></article>
      <article class="metric-card warning"><span>被举报评价</span><strong>{{ governance?.reportedReviews || 0 }}</strong></article>
      <article class="metric-card warning"><span>待处理投诉</span><strong>{{ governance?.pendingComplaints || 0 }}</strong></article>
      <article class="metric-card"><span>进行中咨询</span><strong>{{ governance?.openSessions || 0 }}</strong></article>
    </div>

    <section class="split-grid">
      <article class="panel-card">
        <div class="panel-toolbar"><h2>订单履约分布</h2></div>
        <div class="status-list">
          <div v-for="item in stats" :key="item.status" class="status-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </div>
          <div v-if="stats.length === 0" class="empty-card">暂无订单统计</div>
        </div>
      </article>

      <article class="panel-card">
        <div class="panel-toolbar"><h2>评分分布</h2></div>
        <div class="rating-dist">
          <div v-for="key in ['five', 'four', 'three', 'two', 'one']" :key="key" class="rating-row">
            <span class="label">{{ ratingLabel(key) }}</span>
            <div class="bar-wrap">
              <div class="bar" :style="{ width: ratingPercent(key, ratingTotal()) + '%' }"></div>
            </div>
            <strong>{{ governance?.ratingDistribution?.[key] || 0 }}</strong>
          </div>
          <div v-if="ratingTotal() === 0" class="empty-card">暂无评价数据</div>
        </div>
      </article>

      <article class="panel-card">
        <div class="panel-toolbar"><h2>最近审计</h2></div>
        <div class="audit-list">
          <div v-for="item in audits" :key="item.id" class="audit-row">
            <strong>{{ item.actionType }}</strong>
            <span>{{ item.actorType }} #{{ item.actorId }} · {{ item.targetType }} #{{ item.targetId }}</span>
            <small>{{ timeText(item.createdAt) }} {{ item.detail }}</small>
          </div>
          <div v-if="audits.length === 0" class="empty-card">暂无审计记录</div>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.rating-dist {
  display: grid;
  gap: 8px;
}
.rating-row {
  display: grid;
  grid-template-columns: 50px 1fr 40px;
  align-items: center;
  gap: 8px;
}
.rating-row .label {
  color: #4e5969;
  font-size: 13px;
}
.bar-wrap {
  height: 12px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}
.bar {
  height: 100%;
  background: #e4002b;
  transition: width 0.2s;
}
.rating-row strong {
  text-align: right;
}
.split-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}
</style>
