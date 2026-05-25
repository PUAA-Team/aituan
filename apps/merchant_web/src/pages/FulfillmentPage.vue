<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import {
  fetchDeliveryRule,
  fetchTakeawaySetting,
  redeemVoucher,
  updateDeliveryRule,
  updateTakeawaySetting,
} from '../api';
import type { DeliveryRule, MerchantStore, OrderDetail, TakeawaySetting } from '../types';

const props = defineProps<{
  store: MerchantStore;
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const loading = ref(false);
const setting = ref<TakeawaySetting | null>(null);
const rule = ref<DeliveryRule | null>(null);
const voucherCode = ref('');
const redeemedOrder = ref<OrderDetail | null>(null);

const isTakeaway = computed(() => props.store.businessType === 'takeaway');

watch(() => props.refreshKey, load, { immediate: true });
watch(() => props.store.id, load);
watch(() => props.store.businessType, load);

async function load() {
  if (!isTakeaway.value) {
    setting.value = null;
    rule.value = null;
    return;
  }
  try {
    loading.value = true;
    const [nextSetting, nextRule] = await Promise.all([
      fetchTakeawaySetting(props.store.id),
      fetchDeliveryRule(props.store.id),
    ]);
    setting.value = nextSetting;
    rule.value = nextRule;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function setAcceptMode(mode: 'manual' | 'auto') {
  try {
    setting.value = await updateTakeawaySetting(props.store.id, mode);
    emit('notice', mode === 'auto' ? '已开启自动接单' : '已切换为手动接单');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function saveRule() {
  if (!rule.value) return;
  try {
    rule.value = await updateDeliveryRule(props.store.id, rule.value);
    emit('notice', '配送规则已保存');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function submitVoucher() {
  const code = voucherCode.value.trim();
  if (!code) return;
  try {
    redeemedOrder.value = await redeemVoucher(code);
    emit('notice', '券码核销成功');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}
</script>

<template>
  <section class="page-grid two-col">
    <article v-if="isTakeaway" class="panel-card">
      <div class="panel-toolbar">
        <h2>接单模式</h2>
        <span v-if="setting" class="tag">{{ setting.acceptMode === 'auto' ? '自动接单' : '手动接单' }}</span>
      </div>
      <div class="mode-grid">
        <button class="mode-card" :class="{ active: setting?.acceptMode === 'manual' }" @click="setAcceptMode('manual')">
          <strong>手动接单</strong>
          <span>新订单进入待接单队列，商家确认后开始履约。</span>
        </button>
        <button class="mode-card" :class="{ active: setting?.acceptMode === 'auto' }" @click="setAcceptMode('auto')">
          <strong>自动接单</strong>
          <span>支付后自动进入已接单状态，适合稳定出餐门店。</span>
        </button>
      </div>
    </article>

    <form v-if="isTakeaway && rule" class="panel-card" @submit.prevent="saveRule">
      <div class="panel-toolbar"><h2>配送规则</h2></div>
      <div class="form-grid three">
        <label>
          起送价
          <input v-model.number="rule.startPrice" type="number" min="0" step="0.1" />
        </label>
        <label>
          配送费
          <input v-model.number="rule.deliveryFee" type="number" min="0" step="0.1" />
        </label>
        <label>
          预计分钟
          <input v-model.number="rule.estimatedMinutes" type="number" min="1" />
        </label>
      </div>
      <label>
        配送说明
        <input v-model="rule.deliveryText" />
      </label>
      <div class="form-actions">
        <button class="primary-btn" :disabled="loading">保存规则</button>
      </div>
    </form>

    <article v-if="!isTakeaway" class="panel-card">
      <div class="panel-toolbar"><h2>券码核销</h2></div>
      <form class="voucher-form" @submit.prevent="submitVoucher">
        <input v-model="voucherCode" placeholder="输入用户出示的券码" />
        <button class="primary-btn">核销</button>
      </form>
      <div v-if="redeemedOrder" class="result-box">
        <strong>{{ redeemedOrder.title }}</strong>
        <span>{{ redeemedOrder.orderNo }} · {{ money(redeemedOrder.payableAmount) }}</span>
      </div>
    </article>

    <article v-if="!isTakeaway" class="panel-card">
      <div class="panel-toolbar"><h2>预约入口</h2></div>
      <div class="info-list">
        <span>到店服务订单可在此确认顾客预约信息。</span>
        <span>到店服务按券码和预约信息完成履约。</span>
      </div>
    </article>

    <article class="panel-card">
      <div class="panel-toolbar"><h2>当前门店</h2></div>
      <div class="info-list">
        <span>{{ props.store.storeName }}</span>
        <span>{{ props.store.address }}</span>
        <span>{{ props.store.businessHoursText || '营业时间未设置' }}</span>
        <span>{{ props.store.announcement || '暂无公告' }}</span>
      </div>
    </article>
  </section>
</template>
