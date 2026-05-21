<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { clearToken, fetchOrderDetail, fetchOrders, fetchStats, getToken, login, runOrderAction } from './api';
import type { OpsOrder, OrderDetail, OrderStatusCount } from './types';

const account = ref('demo_admin');
const password = ref('aituan123');
const token = ref(getToken());
const loading = ref(false);
const message = ref('');
const filter = ref('');
const orders = ref<OpsOrder[]>([]);
const stats = ref<OrderStatusCount[]>([]);
const selectedOrder = ref<OrderDetail | null>(null);
const abnormalRemark = ref('配送异常，平台已介入处理');

const loggedIn = computed(() => token.value.length > 0);

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
];

onMounted(() => {
  if (loggedIn.value) refresh();
});

async function submitLogin() {
  try {
    loading.value = true;
    const session = await login(account.value, password.value);
    token.value = session.token;
    message.value = `欢迎回来，${session.profile.nickname || '管理员'}`;
    await refresh();
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

async function refresh() {
  try {
    loading.value = true;
    const [orderPage, statList] = await Promise.all([fetchOrders(filter.value), fetchStats()]);
    orders.value = orderPage.list;
    stats.value = statList;
    if (selectedOrder.value) await openOrderById(selectedOrder.value.id);
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

async function openOrder(order: OpsOrder) {
  await openOrderById(order.id);
}

async function openOrderById(orderId: number) {
  try {
    selectedOrder.value = await fetchOrderDetail(orderId);
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  }
}

async function act(order: OpsOrder | OrderDetail, action: string, remark = '') {
  try {
    loading.value = true;
    await runOrderAction(order.id, action, remark);
    message.value = `订单 ${order.orderNo} 已更新`;
    await refresh();
    await openOrderById(order.id);
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

function logout() {
  clearToken();
  token.value = '';
  orders.value = [];
  stats.value = [];
  selectedOrder.value = null;
}

function statusText(order: OpsOrder) {
  return stageText(order.currentStage || order.fulfillmentStatus, order.currentStageText);
}

function detailStageText(order: OrderDetail) {
  return stageText(order.deliveryTimeline?.currentStage || order.fulfillmentStatus);
}

function stageText(stage: string, fallback = '') {
  const found = statusOptions.find((item) => item.value === stage);
  return found?.label || fallback || stage;
}

function canAdvance(order: OpsOrder | OrderDetail) {
  const stage = 'currentStage' in order ? order.currentStage || order.fulfillmentStatus : order.deliveryTimeline?.currentStage || order.fulfillmentStatus;
  return !['completed', 'merchant_rejected', 'abnormal'].includes(stage);
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}
</script>

<template>
  <main class="page-shell">
    <section v-if="!loggedIn" class="login-panel">
      <div>
        <p class="eyebrow">Aituan Admin</p>
        <h1>平台外卖治理</h1>
        <p>查看全平台外卖订单、人工推进配送状态，并标记异常处理。</p>
      </div>
      <form class="login-card" @submit.prevent="submitLogin">
        <label>账号<input v-model="account" autocomplete="username" /></label>
        <label>密码<input v-model="password" type="password" autocomplete="current-password" /></label>
        <button class="primary" :disabled="loading">{{ loading ? '登录中' : '登录后台' }}</button>
        <p v-if="message" class="notice">{{ message }}</p>
      </form>
    </section>

    <section v-else class="workspace">
      <aside class="side-card">
        <p class="eyebrow">爱团后台</p>
        <h2>外卖治理</h2>
        <p>本阶段聚焦订单状态、配送履约和异常占位处理。</p>
        <div class="audit-card">
          <strong>干预留痕</strong>
          <span>人工推进和异常标记会写入后台审计日志，后续退款审核接入同一治理链路。</span>
        </div>
        <button class="ghost" @click="logout">退出登录</button>
      </aside>

      <section class="content-stack">
        <header class="top-bar">
          <div>
            <p class="eyebrow">Takeaway Control</p>
            <h1>外卖订单总览</h1>
          </div>
          <button class="primary" :disabled="loading" @click="refresh">{{ loading ? '刷新中' : '刷新' }}</button>
        </header>

        <div class="metrics-grid">
          <article v-for="item in stats" :key="item.status" class="metric-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
          </article>
        </div>

        <section class="toolbar-card">
          <label>
            订单状态
            <select v-model="filter" @change="refresh">
              <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label>异常备注<input v-model="abnormalRemark" /></label>
        </section>

        <p v-if="message" class="notice">{{ message }}</p>

        <section class="order-layout">
          <div class="order-list">
            <article v-for="order in orders" :key="order.id" class="order-card">
              <div class="order-main">
                <div>
                  <p class="order-no">{{ order.orderNo }}</p>
                  <h3>{{ order.title }}</h3>
                  <p>{{ order.storeName }} · {{ statusText(order) }}</p>
                </div>
                <strong>{{ money(order.amount) }}</strong>
              </div>
              <div class="actions">
                <button class="ghost small" @click="openOrder(order)">查看详情</button>
                <button v-if="canAdvance(order)" class="primary small" :disabled="loading" @click="act(order, 'delivery/advance')">
                  人工推进
                </button>
                <button class="ghost small" :disabled="loading" @click="act(order, 'abnormal', abnormalRemark)">
                  标记异常
                </button>
              </div>
            </article>
            <article v-if="orders.length === 0" class="empty-card">暂无符合条件的外卖订单。</article>
          </div>

          <aside class="detail-card" v-if="selectedOrder">
            <div class="detail-head">
              <div>
                <p class="order-no">{{ selectedOrder.orderNo }}</p>
                <h3>{{ selectedOrder.title }}</h3>
              </div>
              <button class="ghost small" @click="selectedOrder = null">收起</button>
            </div>
            <span class="stage-pill">{{ detailStageText(selectedOrder) }}</span>
            <p>{{ selectedOrder.storeName }}</p>
            <p>{{ selectedOrder.addressSnapshot || '暂无收货地址' }}</p>
            <p v-if="selectedOrder.remark">备注：{{ selectedOrder.remark }}</p>

            <div class="line-list">
              <div v-for="line in selectedOrder.items" :key="line.itemId" class="line-row">
                <span>{{ line.itemName }} ×{{ line.quantity }}</span>
                <b>{{ money(line.totalPrice) }}</b>
              </div>
            </div>

            <div class="fee-box">
              <span>商品 {{ money(selectedOrder.amount) }}</span>
              <span>配送 {{ money(selectedOrder.deliveryFee) }}</span>
              <strong>实付 {{ money(selectedOrder.payableAmount) }}</strong>
            </div>

            <div class="timeline">
              <div v-for="node in selectedOrder.deliveryTimeline?.nodes || []" :key="node.code" class="timeline-node" :class="{ done: node.reachedAt }">
                <span></span>
                <p>{{ node.text }}</p>
              </div>
            </div>

            <div class="detail-actions">
              <button v-if="canAdvance(selectedOrder)" class="primary small" :disabled="loading" @click="act(selectedOrder, 'delivery/advance')">
                推进配送状态
              </button>
              <button class="ghost small" :disabled="loading" @click="act(selectedOrder, 'abnormal', abnormalRemark)">
                标记异常并留痕
              </button>
            </div>
          </aside>
        </section>
      </section>
    </section>
  </main>
</template>
