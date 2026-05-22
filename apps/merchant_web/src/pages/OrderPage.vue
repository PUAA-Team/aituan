<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { fetchOrderDetail, fetchOrders, fetchStats, runOrderAction } from '../api';
import type { OpsOrder, OrderDetail, OrderStatusCount } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const loading = ref(false);
const filter = ref('');
const orders = ref<OpsOrder[]>([]);
const stats = ref<OrderStatusCount[]>([]);
const selectedOrder = ref<OrderDetail | null>(null);

const statusOptions = [
  { value: '', label: '全部履约' },
  { value: 'merchant_pending', label: '待接单' },
  { value: 'accepted', label: '已接单' },
  { value: 'preparing', label: '备餐中' },
  { value: 'ready_for_delivery', label: '待配送' },
  { value: 'delivering', label: '配送中' },
  { value: 'delivered', label: '已送达' },
  { value: 'completed', label: '已完成' },
  { value: 'cancelled', label: '已取消' },
  { value: 'abnormal', label: '异常' },
];

onMounted(load);
watch(() => props.refreshKey, load);
watch(filter, load);

async function load() {
  try {
    loading.value = true;
    const [orderPage, statList] = await Promise.all([
      fetchOrders({ fulfillmentStatus: filter.value }),
      fetchStats(),
    ]);
    orders.value = orderPage.list;
    stats.value = statList;
    if (selectedOrder.value) {
      selectedOrder.value = await fetchOrderDetail(selectedOrder.value.id);
    }
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function openOrder(order: OpsOrder) {
  try {
    selectedOrder.value = await fetchOrderDetail(order.id);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function act(order: OpsOrder, action: string, label: string) {
  try {
    loading.value = true;
    selectedOrder.value = await runOrderAction(order.id, action, label);
    emit('notice', `${order.orderNo} 已${label}`);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

function actionsFor(order: OpsOrder) {
  switch (order.currentStage || order.fulfillmentStatus) {
    case 'merchant_pending':
      return [
        { text: '接单', action: 'accept', primary: true },
        { text: '拒单', action: 'reject', primary: false },
      ];
    case 'accepted':
      return [{ text: '开始备餐', action: 'prepare', primary: true }];
    case 'preparing':
      return [{ text: '出餐', action: 'ready', primary: true }];
    case 'ready_for_delivery':
    case 'delivering':
      return [{ text: '推进配送', action: 'delivery/advance', primary: true }];
    case 'delivered':
      return [{ text: '完成订单', action: 'complete', primary: true }];
    default:
      return [];
  }
}

function statusText(order: OpsOrder) {
  const value = order.currentStage || order.fulfillmentStatus;
  const option = statusOptions.find((item) => item.value === value);
  return option?.label || order.currentStageText || value;
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}

function timeText(value: string | undefined) {
  if (!value) return '-';
  return value.replace('T', ' ').slice(0, 16);
}
</script>

<template>
  <section class="page-grid">
    <div class="metric-row">
      <article v-for="item in stats" :key="item.status" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.count }}</strong>
      </article>
      <article v-if="stats.length === 0" class="metric-card">
        <span>待处理</span>
        <strong>0</strong>
      </article>
    </div>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>订单列表</h2>
        <label class="inline-field">
          履约状态
          <select v-model="filter">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
      </div>

      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>订单号</th>
              <th>订单内容</th>
              <th>状态</th>
              <th>金额</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.id">
              <td class="mono">{{ order.orderNo }}</td>
              <td>
                <strong>{{ order.title }}</strong>
                <span>{{ order.storeName }}</span>
              </td>
              <td><span class="tag">{{ statusText(order) }}</span></td>
              <td class="amount">{{ money(order.amount) }}</td>
              <td>{{ timeText(order.createdAt) }}</td>
              <td>
                <div class="row-actions">
                  <button class="secondary-btn small" @click="openOrder(order)">详情</button>
                  <button
                    v-for="item in actionsFor(order)"
                    :key="item.action"
                    :class="item.primary ? 'primary-btn small' : 'secondary-btn small'"
                    :disabled="loading"
                    @click="act(order, item.action, item.text)"
                  >
                    {{ item.text }}
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="orders.length === 0">
              <td colspan="6" class="empty-cell">暂无订单</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <aside v-if="selectedOrder" class="drawer-card">
      <div class="panel-toolbar">
        <div>
          <span class="mono">{{ selectedOrder.orderNo }}</span>
          <h2>{{ selectedOrder.title }}</h2>
        </div>
        <button class="text-btn" @click="selectedOrder = null">收起</button>
      </div>
      <div class="info-list">
        <span>收货地址：{{ selectedOrder.addressSnapshot || '暂无' }}</span>
        <span>备注：{{ selectedOrder.remark || '无' }}</span>
        <span>支付状态：{{ selectedOrder.paymentStatus }}</span>
      </div>
      <div class="line-list">
        <div v-for="line in selectedOrder.items" :key="line.itemId" class="line-row">
          <span>{{ line.itemName }} ×{{ line.quantity }}</span>
          <b>{{ money(line.totalPrice) }}</b>
        </div>
      </div>
      <div class="fee-box">
        <span>商品金额 {{ money(selectedOrder.amount) }}</span>
        <span>配送费 {{ money(selectedOrder.deliveryFee) }}</span>
        <strong>实付 {{ money(selectedOrder.payableAmount) }}</strong>
      </div>
      <div class="timeline-list">
        <div
          v-for="node in selectedOrder.deliveryTimeline?.nodes || []"
          :key="node.code"
          class="timeline-row"
          :class="{ done: node.reachedAt, current: node.code === selectedOrder.fulfillmentStatus }"
        >
          <i></i>
          <span>{{ node.text }}</span>
          <small>{{ timeText(node.reachedAt) }}</small>
        </div>
      </div>
    </aside>
  </section>
</template>
