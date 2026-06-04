<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchOrderDetail, fetchOrders, fetchStats, refundOrder, runOrderAction } from '../api';
import type { OpsOrder, OrderDetail, OrderStatusCount } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const filter = ref('');
const abnormalRemark = ref('平台人工介入处理');
const orders = ref<OpsOrder[]>([]);
const stats = ref<OrderStatusCount[]>([]);
const selected = ref<OrderDetail | null>(null);

const statusOptions = [
  { value: '', label: '全部订单' },
  { value: 'merchant_pending', label: '待商家接单' },
  { value: 'accepted', label: '已接单' },
  { value: 'preparing', label: '备餐中' },
  { value: 'ready_for_delivery', label: '待配送' },
  { value: 'delivering', label: '配送中' },
  { value: 'delivered', label: '已送达' },
  { value: 'completed', label: '已完成' },
  { value: 'abnormal', label: '异常处理中' },
  { value: 'cancelled', label: '已取消' },
  { value: 'refunded', label: '已退款' },
];

watch(() => props.refreshKey, load, { immediate: true });
watch(filter, load);

async function load() {
  try {
    const [orderPage, statList] = await Promise.all([
      fetchOrders(filter.value),
      fetchStats(),
    ]);
    orders.value = orderPage.list;
    stats.value = statList;
    if (selected.value) selected.value = await fetchOrderDetail(selected.value.id);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function open(order: OpsOrder) {
  try {
    selected.value = await fetchOrderDetail(order.id);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function act(order: OpsOrder | OrderDetail, action: string) {
  try {
    selected.value = await runOrderAction(order.id, action, abnormalRemark.value);
    emit('notice', `${order.orderNo} 已更新`);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function doRefund(order: OpsOrder | OrderDetail) {
  const reason = window.prompt('请输入退款原因', '平台人工退款');
  if (reason === null) return;
  try {
    selected.value = await refundOrder(order.id, reason.trim() || '平台人工退款');
    emit('notice', `${order.orderNo} 已退款`);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function canRefund(order: OpsOrder | OrderDetail) {
  return order.paymentStatus === 'paid' && (order as OrderDetail).refundableByStaff !== false && order.displayStatus !== 'refunded' && order.refundStatus !== 'succeeded';
}

function stageText(stage: string, fallback = '') {
  return statusOptions.find((item) => item.value === stage)?.label || fallback || stage;
}

function orderStatusText(order: OpsOrder | OrderDetail) {
  if (order.displayStatus === 'refunded' || order.refundStatus === 'succeeded') return '已退款';
  const stage = 'currentStage' in order ? order.currentStage || order.fulfillmentStatus : order.deliveryTimeline?.currentStage || order.fulfillmentStatus;
  return stageText(stage, 'currentStageText' in order ? order.currentStageText : '');
}

function canAdvance(order: OpsOrder | OrderDetail) {
  if (order.displayStatus === 'refunded' || order.refundStatus === 'succeeded') return false;
  const stage = 'currentStage' in order ? order.currentStage || order.fulfillmentStatus : order.deliveryTimeline?.currentStage || order.fulfillmentStatus;
  return order.orderKind === 'takeaway' && !['completed', 'merchant_rejected', 'abnormal', 'cancelled', 'refunded'].includes(stage);
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
      <article v-for="item in stats" :key="item.status" class="metric-card"><span>{{ item.label }}</span><strong>{{ item.count }}</strong></article>
    </div>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>订单列表</h2>
        <div class="toolbar-actions">
          <select v-model="filter">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <input v-model="abnormalRemark" class="search-input" placeholder="异常备注" />
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>订单</th><th>门店</th><th>状态</th><th>金额</th><th>创建时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td class="mono">{{ order.orderNo }}<span>{{ order.title }}</span></td>
              <td>{{ order.storeName }}</td>
              <td><span class="tag">{{ orderStatusText(order) }}</span></td>
              <td class="amount">{{ money(order.amount) }}</td>
              <td>{{ timeText(order.createdAt) }}</td>
              <td>
                <div class="row-actions">
                  <button class="secondary-btn small" @click="open(order)">详情</button>
                  <button v-if="canAdvance(order)" class="primary-btn small" @click="act(order, 'delivery/advance')">推进</button>
                  <button v-if="canRefund(order)" class="secondary-btn small" @click="doRefund(order)">退款</button>
                  <button class="secondary-btn small" @click="act(order, 'abnormal')">异常</button>
                </div>
              </td>
            </tr>
            <tr v-if="orders.length === 0"><td colspan="6" class="empty-cell">暂无订单</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <aside v-if="selected" class="drawer-card">
      <div class="panel-toolbar">
        <div><span class="mono">{{ selected.orderNo }}</span><h2>{{ selected.title }}</h2></div>
        <button class="text-btn" @click="selected = null">收起</button>
      </div>
      <div class="info-list">
        <span>{{ selected.storeName }}</span>
        <span>{{ selected.addressSnapshot || '暂无收货地址' }}</span>
        <span>状态：{{ orderStatusText(selected) }}</span>
        <span v-if="selected.refundStatus && selected.refundStatus !== 'none'">退款状态：{{ selected.refundStatus }} {{ money(selected.refundAmount) }}</span>
        <span v-if="selected.refundReason">退款原因：{{ selected.refundReason }}</span>
      </div>
      <div class="line-list">
        <div v-for="line in selected.items" :key="line.itemId" class="line-row"><span>{{ line.itemName }} ×{{ line.quantity }}</span><b>{{ money(line.totalPrice) }}</b></div>
      </div>
      <div class="fee-box"><span>商品 {{ money(selected.amount) }}</span><span>配送 {{ money(selected.deliveryFee) }}</span><span v-if="selected.refundAmount">已退款 {{ money(selected.refundAmount) }}</span><strong>实付 {{ money(selected.payableAmount) }}</strong></div>
      <div class="timeline-list">
        <div v-for="node in selected.deliveryTimeline?.nodes || []" :key="node.code" class="timeline-row" :class="{ done: node.reachedAt }"><i></i><span>{{ node.text }}</span><small>{{ timeText(node.reachedAt) }}</small></div>
      </div>
      <div class="row-actions">
        <button v-if="canAdvance(selected)" class="primary-btn small" @click="act(selected, 'delivery/advance')">推进配送</button>
        <button v-if="canRefund(selected)" class="secondary-btn small" @click="doRefund(selected)">平台退款</button>
        <button class="secondary-btn small" @click="act(selected, 'abnormal')">标记异常</button>
      </div>
    </aside>
  </section>
</template>
