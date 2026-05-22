<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchAuditLogs, fetchDashboard, fetchStats } from '../api';
import type { AuditLog, DashboardView, OrderStatusCount } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const dashboard = ref<DashboardView | null>(null);
const stats = ref<OrderStatusCount[]>([]);
const audits = ref<AuditLog[]>([]);

watch(() => props.refreshKey, load, { immediate: true });

async function load() {
  try {
    const [summary, statList, auditPage] = await Promise.all([
      fetchDashboard(),
      fetchStats(),
      fetchAuditLogs(),
    ]);
    dashboard.value = summary;
    stats.value = statList;
    audits.value = auditPage.list.slice(0, 8);
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
