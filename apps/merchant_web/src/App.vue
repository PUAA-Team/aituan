<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  clearToken,
  fetchDeliveryRule,
  fetchItems,
  fetchOrderDetail,
  fetchOrders,
  fetchStats,
  fetchTakeawaySetting,
  getToken,
  login,
  runOrderAction,
  updateDeliveryRule,
  updateItem,
  updateItemStatus,
  updateTakeawaySetting,
} from './api';
import type { DeliveryRule, MerchantItem, OpsOrder, OrderDetail, OrderStatusCount, TakeawaySetting } from './types';

const account = ref('demo_merchant');
const password = ref('aituan123');
const storeId = ref(1);
const token = ref(getToken());
const loading = ref(false);
const message = ref('');
const filter = ref('');
const itemFilter = ref('');
const activeTab = ref<'orders' | 'items' | 'rules'>('orders');
const orders = ref<OpsOrder[]>([]);
const stats = ref<OrderStatusCount[]>([]);
const items = ref<MerchantItem[]>([]);
const setting = ref<TakeawaySetting | null>(null);
const deliveryRule = ref<DeliveryRule | null>(null);
const selectedOrder = ref<OrderDetail | null>(null);
const editingItem = ref<MerchantItem | null>(null);

const loggedIn = computed(() => token.value.length > 0);

const statusOptions = [
  { value: '', label: '全部订单' },
  { value: 'merchant_pending', label: '待接单' },
  { value: 'accepted', label: '已接单' },
  { value: 'preparing', label: '备餐中' },
  { value: 'ready_for_delivery', label: '待配送' },
  { value: 'delivering', label: '配送中' },
  { value: 'delivered', label: '已送达' },
  { value: 'completed', label: '已完成' },
];

const itemStatusOptions = [
  { value: '', label: '全部商品' },
  { value: 'on_sale', label: '上架中' },
  { value: 'off_sale', label: '已下架' },
];

onMounted(() => {
  if (loggedIn.value) refresh();
});

async function submitLogin() {
  try {
    loading.value = true;
    const session = await login(account.value, password.value);
    token.value = session.token;
    message.value = `欢迎回来，${session.profile.nickname || '商家'}`;
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
    const [orderPage, statList, storeSetting, itemList, rule] = await Promise.all([
      fetchOrders(filter.value),
      fetchStats(),
      fetchTakeawaySetting(storeId.value),
      fetchItems(storeId.value, itemFilter.value),
      fetchDeliveryRule(storeId.value),
    ]);
    orders.value = orderPage.list;
    stats.value = statList;
    setting.value = storeSetting;
    items.value = itemList;
    deliveryRule.value = rule;
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

async function changeAcceptMode(mode: 'manual' | 'auto') {
  try {
    setting.value = await updateTakeawaySetting(storeId.value, mode);
    message.value = mode === 'auto' ? '已开启自动接单' : '已切换为手动接单';
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  }
}

async function act(order: OpsOrder, action: string) {
  try {
    loading.value = true;
    await runOrderAction(order.id, action);
    message.value = `订单 ${order.orderNo} 已处理`;
    await refresh();
    if (selectedOrder.value?.id === order.id) await openOrder(order);
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

async function openOrder(order: OpsOrder) {
  try {
    selectedOrder.value = await fetchOrderDetail(order.id);
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  }
}

async function toggleItemStatus(item: MerchantItem) {
  try {
    const nextStatus = item.status === 'on_sale' ? 'off_sale' : 'on_sale';
    const updated = await updateItemStatus(storeId.value, item.id, nextStatus);
    replaceItem(updated);
    message.value = `${updated.title} 已${nextStatus === 'on_sale' ? '上架' : '下架'}`;
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  }
}

function editItem(item: MerchantItem) {
  editingItem.value = { ...item };
}

async function saveItem() {
  if (!editingItem.value) return;
  try {
    const updated = await updateItem(storeId.value, editingItem.value);
    replaceItem(updated);
    editingItem.value = null;
    message.value = '商品信息已保存';
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  }
}

async function saveDeliveryRule() {
  if (!deliveryRule.value) return;
  try {
    deliveryRule.value = await updateDeliveryRule(storeId.value, deliveryRule.value);
    message.value = '配送规则已保存';
  } catch (error) {
    message.value = error instanceof Error ? error.message : String(error);
  }
}

function replaceItem(item: MerchantItem) {
  items.value = items.value.map((entry) => (entry.id === item.id ? item : entry));
}

function logout() {
  clearToken();
  token.value = '';
  orders.value = [];
  stats.value = [];
  items.value = [];
  setting.value = null;
  deliveryRule.value = null;
  selectedOrder.value = null;
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
  const found = statusOptions.find((item) => item.value === (order.currentStage || order.fulfillmentStatus));
  return found?.label || order.currentStageText || order.fulfillmentStatus;
}

function itemStatusText(status: string) {
  return status === 'on_sale' ? '上架中' : '已下架';
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}
</script>

<template>
  <main class="page-shell">
    <section v-if="!loggedIn" class="login-panel">
      <div>
        <p class="eyebrow">Aituan Merchant</p>
        <h1>外卖商家工作台</h1>
        <p>处理待接单订单、配置商品售卖状态，并维护门店配送规则。</p>
      </div>
      <form class="login-card" @submit.prevent="submitLogin">
        <label>账号<input v-model="account" autocomplete="username" /></label>
        <label>密码<input v-model="password" type="password" autocomplete="current-password" /></label>
        <button class="primary" :disabled="loading">{{ loading ? '登录中' : '登录商家端' }}</button>
        <p v-if="message" class="notice">{{ message }}</p>
      </form>
    </section>

    <section v-else class="workspace">
      <aside class="side-card">
        <p class="eyebrow">爱团商家端</p>
        <h2>外卖经营</h2>
        <p>订单、商品、配送规则放在一个工作台内处理。</p>
        <div class="side-tabs">
          <button :class="{ selected: activeTab === 'orders' }" @click="activeTab = 'orders'">订单处理</button>
          <button :class="{ selected: activeTab === 'items' }" @click="activeTab = 'items'">商品管理</button>
          <button :class="{ selected: activeTab === 'rules' }" @click="activeTab = 'rules'">配送规则</button>
        </div>
        <button class="ghost" @click="logout">退出登录</button>
      </aside>

      <section class="content-stack">
        <header class="top-bar">
          <div>
            <p class="eyebrow">Takeaway Ops</p>
            <h1>{{ activeTab === 'orders' ? '订单处理台' : activeTab === 'items' ? '商品管理' : '配送规则' }}</h1>
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
          <label>门店 ID<input v-model.number="storeId" type="number" min="1" @change="refresh" /></label>
          <label v-if="activeTab === 'orders'">
            订单状态
            <select v-model="filter" @change="refresh">
              <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <label v-if="activeTab === 'items'">
            商品状态
            <select v-model="itemFilter" @change="refresh">
              <option v-for="item in itemStatusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>
          <div class="mode-switch" v-if="setting">
            <span>{{ setting.storeName }}</span>
            <button :class="{ selected: setting.acceptMode === 'manual' }" @click="changeAcceptMode('manual')">手动接单</button>
            <button :class="{ selected: setting.acceptMode === 'auto' }" @click="changeAcceptMode('auto')">自动接单</button>
          </div>
        </section>

        <p v-if="message" class="notice">{{ message }}</p>

        <section v-if="activeTab === 'orders'" class="order-layout">
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
                <button class="ghost small" @click="openOrder(order)">详情</button>
                <button
                  v-for="item in actionsFor(order)"
                  :key="item.action"
                  :class="item.primary ? 'primary small' : 'ghost small'"
                  :disabled="loading"
                  @click="act(order, item.action)"
                >
                  {{ item.text }}
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
          </aside>
        </section>

        <section v-if="activeTab === 'items'" class="item-grid">
          <article v-for="item in items" :key="item.id" class="item-card">
            <div>
              <span class="status-pill" :class="item.status">{{ itemStatusText(item.status) }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.subtitle }}</p>
              <p>{{ item.categoryName }} · 月售 {{ item.salesCount }} · 库存 {{ item.stock }}</p>
            </div>
            <strong>{{ money(item.price) }}</strong>
            <div class="actions">
              <button class="ghost small" @click="editItem(item)">编辑</button>
              <button class="primary small" @click="toggleItemStatus(item)">{{ item.status === 'on_sale' ? '下架' : '上架' }}</button>
            </div>
          </article>
          <article v-if="items.length === 0" class="empty-card">暂无商品。</article>
        </section>

        <form v-if="editingItem" class="edit-card" @submit.prevent="saveItem">
          <div class="detail-head">
            <h3>编辑商品</h3>
            <button class="ghost small" type="button" @click="editingItem = null">取消</button>
          </div>
          <label>商品名<input v-model="editingItem.title" /></label>
          <label>副标题<input v-model="editingItem.subtitle" /></label>
          <div class="form-grid">
            <label>价格<input v-model.number="editingItem.price" type="number" min="0" step="0.1" /></label>
            <label>库存<input v-model.number="editingItem.stock" type="number" min="0" /></label>
            <label>
              状态
              <select v-model="editingItem.status">
                <option value="on_sale">上架中</option>
                <option value="off_sale">已下架</option>
              </select>
            </label>
          </div>
          <button class="primary">保存商品</button>
        </form>

        <form v-if="activeTab === 'rules' && deliveryRule" class="edit-card" @submit.prevent="saveDeliveryRule">
          <h3>配送规则维护</h3>
          <div class="form-grid">
            <label>起送价<input v-model.number="deliveryRule.startPrice" type="number" min="0" step="0.1" /></label>
            <label>配送费<input v-model.number="deliveryRule.deliveryFee" type="number" min="0" step="0.1" /></label>
            <label>预计时长<input v-model.number="deliveryRule.estimatedMinutes" type="number" min="1" /></label>
          </div>
          <label>配送范围说明<input v-model="deliveryRule.deliveryText" /></label>
          <button class="primary">保存配送规则</button>
        </form>
      </section>
    </section>
  </main>
</template>
