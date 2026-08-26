<script setup lang="ts">
import { ref, watch } from 'vue';
import { fetchDeliverySettings, fetchDeliveryTasks, updateDeliverySettings, updateDeliveryTask } from '../api';
import type { DeliverySetting, DeliveryTask } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const stage = ref('');
const remark = ref('平台人工处理');
const tasks = ref<DeliveryTask[]>([]);
const settings = ref<DeliverySetting | null>(null);

const stages = [
  { value: '', label: '全部阶段' },
  { value: 'merchant_pending', label: '待商家接单' },
  { value: 'accepted', label: '已接单' },
  { value: 'preparing', label: '备餐中' },
  { value: 'ready_for_delivery', label: '待配送' },
  { value: 'delivering', label: '配送中' },
  { value: 'delivered', label: '已送达' },
  { value: 'abnormal', label: '异常' },
];

watch(() => props.refreshKey, load, { immediate: true });
watch(stage, loadTasks);

async function load() {
  await Promise.all([loadTasks(), loadSettings()]);
}

async function loadTasks() {
  try {
    tasks.value = (await fetchDeliveryTasks(stage.value)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function loadSettings() {
  try {
    settings.value = await fetchDeliverySettings();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function saveSettings() {
  if (!settings.value) return;
  try {
    settings.value = await updateDeliverySettings(settings.value);
    emit('notice', '配送全局设置已保存');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function act(task: DeliveryTask, action: 'advance' | 'pause' | 'resume' | 'abnormal') {
  try {
    const updated = await updateDeliveryTask(task.taskId, action, remark.value);
    tasks.value = tasks.value.map((item) => (item.taskId === updated.taskId ? updated : item));
    emit('notice', `${task.orderNo} 已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function stageText(value: string) {
  return stages.find((item) => item.value === value)?.label || value;
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="page-grid two-col">
    <form v-if="settings" class="panel-card" @submit.prevent="saveSettings">
      <div class="panel-toolbar"><h2>自动推进设置</h2></div>
      <label class="check-line">
        <input v-model="settings.autoAdvanceEnabled" type="checkbox" />
        启用配送自动推进
      </label>
      <label>
        推进间隔（分钟）
        <input v-model.number="settings.tickMinutes" type="number" min="1" />
      </label>
      <div class="form-actions"><button class="primary-btn">保存设置</button></div>
    </form>

    <section class="panel-card">
      <div class="panel-toolbar"><h2>处理备注</h2></div>
      <label>
        异常/人工推进备注
        <input v-model="remark" />
      </label>
    </section>

    <section class="panel-card wide">
      <div class="panel-toolbar">
        <h2>配送任务</h2>
        <div class="toolbar-actions">
          <select v-model="stage">
            <option v-for="item in stages" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <button class="secondary-btn" @click="loadTasks">查询</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>订单</th><th>门店</th><th>当前阶段</th><th>自动推进</th><th>下次推进</th><th>异常</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="task in tasks" :key="task.taskId">
              <td class="mono">{{ task.orderNo }}<span>#{{ task.taskId }}</span></td>
              <td>{{ task.storeName }}</td>
              <td><span class="tag">{{ task.currentStageText || stageText(task.currentStage) }}</span></td>
              <td>{{ task.autoAdvanceEnabled ? '开启' : '关闭' }}<span>{{ task.pausedAt ? '已暂停' : '运行中' }}</span></td>
              <td>{{ timeText(task.nextTickAt) }}</td>
              <td>{{ task.abnormalReason || '-' }}</td>
              <td>
                <div class="row-actions">
                  <button class="primary-btn small" @click="act(task, 'advance')">推进</button>
                  <button class="secondary-btn small" @click="act(task, task.pausedAt ? 'resume' : 'pause')">{{ task.pausedAt ? '恢复' : '暂停' }}</button>
                  <button class="secondary-btn small" @click="act(task, 'abnormal')">异常</button>
                </div>
              </td>
            </tr>
            <tr v-if="tasks.length === 0"><td colspan="7" class="empty-cell">暂无配送任务</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>
