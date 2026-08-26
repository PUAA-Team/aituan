<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { fetchMerchantDashboard } from '../api';
import type { MerchantDashboardView } from '../types';

const props = defineProps<{ refreshKey: number }>();

const loading = ref(false);
const data = ref<MerchantDashboardView | null>(null);
const error = ref('');

onMounted(load);
watch(() => props.refreshKey, load);

async function load() {
  loading.value = true;
  error.value = '';
  try {
    data.value = await fetchMerchantDashboard();
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <section class="dashboard-page">
    <div v-if="loading" class="hint">加载中…</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <template v-else-if="data">
      <div class="kpis">
        <div class="kpi"><span class="label">今日订单</span><span class="value">{{ data.todayOrders }}</span></div>
        <div class="kpi"><span class="label">今日营业额</span><span class="value">¥{{ data.todayRevenue }}</span></div>
        <div class="kpi"><span class="label">待回复评价</span><span class="value">{{ data.pendingReviews }}</span></div>
        <div class="kpi"><span class="label">进行中咨询</span><span class="value">{{ data.openSessions }}</span></div>
        <div class="kpi"><span class="label">平均评分</span><span class="value">{{ data.averageRating.toFixed(1) }}</span></div>
      </div>

      <div class="card">
        <div class="card-title">近 7 天订单趋势</div>
        <div class="bars">
          <div v-for="(d, i) in data.weeklyOrders" :key="i" class="bar-col">
            <div class="bar" :style="{ height: barHeight(d.count, data.weeklyOrders) + 'px' }"></div>
            <div class="bar-num">{{ d.count }}</div>
            <div class="bar-label">{{ d.date.slice(5) }}</div>
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<script lang="ts">
function barHeight(count: number, all: { count: number }[]) {
  const max = Math.max(1, ...all.map(x => x.count));
  return Math.max(4, Math.round((count / max) * 120));
}
</script>

<style scoped>
.dashboard-page { padding: 16px; }
.kpis { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; margin-bottom: 16px; }
.kpi { background: white; border-radius: 8px; padding: 16px; display: flex; flex-direction: column; gap: 6px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.kpi .label { color: #888; font-size: 13px; }
.kpi .value { color: #d4380d; font-size: 22px; font-weight: 600; }
.card { background: white; border-radius: 8px; padding: 16px; box-shadow: 0 1px 2px rgba(0,0,0,0.05); }
.card-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; }
.bars { display: flex; align-items: flex-end; gap: 16px; height: 160px; }
.bar-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.bar { width: 24px; background: #fa8c16; border-radius: 4px 4px 0 0; }
.bar-num { font-size: 12px; color: #555; }
.bar-label { font-size: 11px; color: #999; }
.hint, .error { padding: 24px; color: #888; }
.error { color: #d4380d; }
</style>
