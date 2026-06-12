<script setup lang="ts">
import { ref, watch } from 'vue';
import { confirmBooking, fetchBookings } from '../api';
import type { MerchantStore, OpsBooking } from '../types';

const props = defineProps<{
  store: MerchantStore;
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const statusFilter = ref('');
const businessFilter = ref('');
const remark = ref('已与顾客电话沟通');
const bookings = ref<OpsBooking[]>([]);
const loading = ref(false);

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'pending', label: '待确认' },
  { value: 'confirmed', label: '已确认' },
];

const businessOptions = [
  { value: '', label: '全部业务' },
  { value: 'hotel', label: '酒店' },
  { value: 'entertainment', label: '休闲娱乐' },
  { value: 'movie', label: '电影演出' },
  { value: 'beauty', label: '丽人医美' },
  { value: 'massage', label: '洗脚按摩' },
];

watch(() => props.refreshKey, load, { immediate: true });
watch(() => props.store.id, load);

async function load() {
  try {
    loading.value = true;
    const page = await fetchBookings({
      status: statusFilter.value,
      businessType: businessFilter.value,
    });
    bookings.value = page.list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    loading.value = false;
  }
}

async function confirm(orderId: number) {
  try {
    await confirmBooking(orderId, remark.value);
    emit('notice', `预约 ${orderId} 已确认`);
    await load();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function money(value: number | undefined) {
  return `￥${Number(value || 0).toFixed(1)}`;
}

function statusLabel(status: string) {
  return status === 'confirmed' ? '已确认' : status === 'pending' ? '待确认' : status;
}

function businessLabel(code: string) {
  const map: Record<string, string> = {
    hotel: '酒店',
    entertainment: '休闲娱乐',
    movie: '电影演出',
    beauty: '丽人医美',
    massage: '洗脚按摩',
    ticket: '景点门票',
    group_buy: '团购',
  };
  return map[code] || code;
}
</script>

<template>
  <section class="page-grid">
    <article class="panel-card">
      <div class="panel-toolbar">
        <h2>预约记录</h2>
        <div class="toolbar-actions">
          <select v-model="statusFilter" @change="load">
            <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <select v-model="businessFilter" @change="load">
            <option v-for="opt in businessOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
          <input v-model="remark" class="search-input" placeholder="确认备注" />
          <button class="secondary-btn" :disabled="loading" @click="load">{{ loading ? '加载中' : '刷新' }}</button>
        </div>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>订单</th>
            <th>联系人</th>
            <th>预约时间</th>
            <th>人数</th>
            <th>业务</th>
            <th>状态</th>
            <th>金额</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in bookings" :key="row.booking.orderId">
            <td>
              <div>{{ row.orderTitle }}</div>
              <small>{{ row.booking.orderNo }}</small>
            </td>
            <td>
              <div>{{ row.booking.contactName || '-' }}</div>
              <small>{{ row.booking.contactPhone || '-' }}</small>
            </td>
            <td>
              <div>{{ row.booking.bookingDate || '不限' }}</div>
              <small>{{ row.booking.bookingTimeSlot || '到店再约' }}</small>
            </td>
            <td>{{ row.booking.guestCount }}</td>
            <td>{{ businessLabel(row.booking.businessType) }}</td>
            <td>{{ statusLabel(row.booking.storeConfirmStatus) }}</td>
            <td>{{ money(row.payableAmount) }}</td>
            <td>
              <button
                class="text-btn"
                :disabled="row.booking.storeConfirmStatus === 'confirmed'"
                @click="confirm(row.booking.orderId)"
              >{{ row.booking.storeConfirmStatus === 'confirmed' ? '已确认' : '确认预约' }}</button>
            </td>
          </tr>
          <tr v-if="bookings.length === 0">
            <td colspan="8" class="empty-row">暂无预约记录</td>
          </tr>
        </tbody>
      </table>
    </article>
  </section>
</template>

<style scoped>
.empty-row {
  text-align: center;
  color: #86909c;
  padding: 18px 0;
}
</style>
