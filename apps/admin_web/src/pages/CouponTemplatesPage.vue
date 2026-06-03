<script setup lang="ts">
import { ref, watch } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import { createCouponTemplate, fetchCouponTemplates, updateCouponTemplate } from '../api';
import type { CouponTemplate, CouponTemplateForm } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const templates = ref<CouponTemplate[]>([]);
const editingId = ref<number | null>(null);
const editing = ref<CouponTemplateForm | null>(null);

watch(() => props.refreshKey, load, { immediate: true });

async function load() {
  try {
    templates.value = await fetchCouponTemplates();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function openCreate() {
  editingId.value = null;
  editing.value = {
    name: '',
    type: 'full_reduction',
    faceValue: 5,
    thresholdAmount: 0,
    businessScope: 'all',
    validKind: 'relative',
    validDays: 30,
    totalQty: 0,
    perUserLimit: 1,
    status: 'enabled',
  };
}

function openEdit(item: CouponTemplate) {
  editingId.value = item.id;
  editing.value = {
    name: item.name,
    type: item.type,
    faceValue: item.faceValue,
    thresholdAmount: item.thresholdAmount,
    businessScope: item.businessScope,
    validKind: item.validKind,
    validStart: item.validStart ? item.validStart.slice(0, 16) : undefined,
    validEnd: item.validEnd ? item.validEnd.slice(0, 16) : undefined,
    validDays: item.validDays,
    totalQty: item.totalQty,
    perUserLimit: item.perUserLimit,
    status: item.status,
  };
}

async function save() {
  if (!editing.value) return;
  try {
    const payload: CouponTemplateForm = {
      ...editing.value,
      name: editing.value.name.trim(),
      businessScope: editing.value.businessScope?.trim() || 'all',
      thresholdAmount: Number(editing.value.thresholdAmount || 0),
      totalQty: Number(editing.value.totalQty || 0),
      perUserLimit: Number(editing.value.perUserLimit || 1),
      validStart: editing.value.validKind === 'absolute' ? normalizeDateTime(editing.value.validStart) : undefined,
      validEnd: editing.value.validKind === 'absolute' ? normalizeDateTime(editing.value.validEnd) : undefined,
      validDays: editing.value.validKind === 'relative' ? Number(editing.value.validDays || 30) : undefined,
    };
    const saved = editingId.value
      ? await updateCouponTemplate(editingId.value, payload)
      : await createCouponTemplate(payload);
    templates.value = templates.value.some((item) => item.id === saved.id)
      ? templates.value.map((item) => (item.id === saved.id ? saved : item))
      : [saved, ...templates.value];
    editing.value = null;
    emit('notice', `${saved.name} 已保存`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function normalizeDateTime(value?: string) {
  return value ? `${value.length === 16 ? `${value}:00` : value}` : undefined;
}

function typeText(type: string) {
  return type === 'discount' ? '折扣券' : '满减券';
}

function validText(item: CouponTemplate) {
  if (item.validKind === 'relative') return `领取后 ${item.validDays || 30} 天有效`;
  return `${timeText(item.validStart)} 至 ${timeText(item.validEnd)}`;
}

function timeText(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '不限';
}

function money(value: number | undefined) {
  return Number(value || 0).toFixed(2);
}
</script>

<template>
  <section class="panel-card">
    <div class="panel-toolbar">
      <h2>优惠券模板配置</h2>
      <div class="toolbar-actions">
        <button class="secondary-btn" @click="load">刷新</button>
        <button class="primary-btn" @click="openCreate">新增模板</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead><tr><th>模板</th><th>规则</th><th>有效期</th><th>库存</th><th>限领</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in templates" :key="item.id">
            <td><strong>{{ item.name }}</strong><span>{{ typeText(item.type) }} · {{ item.businessScope }}</span></td>
            <td>
              <strong>{{ item.type === 'discount' ? `${Number(item.faceValue * 10).toFixed(1)}折` : `减${money(item.faceValue)}元` }}</strong>
              <span>{{ item.thresholdAmount > 0 ? `满${money(item.thresholdAmount)}可用` : '无门槛' }}</span>
            </td>
            <td>{{ validText(item) }}</td>
            <td>{{ item.totalQty === 0 ? '不限量' : `${item.issuedQty}/${item.totalQty}` }}</td>
            <td>每人 {{ item.perUserLimit }} 张</td>
            <td><span class="tag" :class="item.status">{{ item.status === 'enabled' ? '启用' : '停用' }}</span></td>
            <td><button class="secondary-btn small" @click="openEdit(item)">编辑</button></td>
          </tr>
          <tr v-if="templates.length === 0"><td colspan="7" class="empty-cell">暂无优惠券模板</td></tr>
        </tbody>
      </table>
    </div>
  </section>

  <PanelModal v-if="editing" :title="editingId ? '编辑优惠券模板' : '新增优惠券模板'" @close="editing = null">
    <form class="modal-form" @submit.prevent="save">
      <label>模板名称<input v-model="editing.name" required placeholder="新人满30减5" /></label>
      <div class="form-grid three">
        <label>
          类型
          <select v-model="editing.type">
            <option value="full_reduction">满减券</option>
            <option value="discount">折扣券</option>
          </select>
        </label>
        <label>面额/折扣率<input v-model.number="editing.faceValue" type="number" min="0" step="0.01" required /></label>
        <label>使用门槛<input v-model.number="editing.thresholdAmount" type="number" min="0" step="0.01" /></label>
      </div>
      <div class="form-grid three">
        <label>业务范围<input v-model="editing.businessScope" placeholder="all" /></label>
        <label>总库存<input v-model.number="editing.totalQty" type="number" min="0" placeholder="0表示不限" /></label>
        <label>每人限领<input v-model.number="editing.perUserLimit" type="number" min="1" /></label>
      </div>
      <div class="form-grid three">
        <label>
          有效期类型
          <select v-model="editing.validKind">
            <option value="relative">领后有效</option>
            <option value="absolute">固定日期</option>
          </select>
        </label>
        <label v-if="editing.validKind === 'relative'">有效天数<input v-model.number="editing.validDays" type="number" min="1" /></label>
        <label v-if="editing.validKind === 'absolute'">开始时间<input v-model="editing.validStart" type="datetime-local" /></label>
        <label v-if="editing.validKind === 'absolute'">结束时间<input v-model="editing.validEnd" type="datetime-local" /></label>
        <label>
          状态
          <select v-model="editing.status">
            <option value="enabled">启用</option>
            <option value="disabled">停用</option>
          </select>
        </label>
      </div>
      <div class="result-box">
        <strong>填写提示</strong>
        <span>满减券面额填减免金额；折扣券面额填 0.9 表示九折。总库存填 0 表示不限量。</span>
      </div>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="editing = null">取消</button>
        <button class="primary-btn">保存模板</button>
      </div>
    </form>
  </PanelModal>
</template>
