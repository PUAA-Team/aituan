<script setup lang="ts">
import { ref, watch } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import { createMemberLevel, fetchMemberLevels, updateMemberLevel } from '../api';
import type { MemberBenefitItem, MemberLevel, MemberLevelForm } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const levels = ref<MemberLevel[]>([]);
const editingId = ref<number | null>(null);
const editing = ref<MemberLevelForm | null>(null);

watch(() => props.refreshKey, load, { immediate: true });

async function load() {
  try {
    levels.value = await fetchMemberLevels();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function openCreate() {
  editingId.value = null;
  editing.value = {
    levelCode: '',
    levelName: '',
    minGrowthValue: 0,
    benefits: [emptyBenefit()],
    iconUrl: '',
    color: '#e4002b',
    sortOrder: levels.value.length + 1,
    status: 'enabled',
  };
}

function openEdit(item: MemberLevel) {
  editingId.value = item.id;
  editing.value = {
    levelCode: item.levelCode,
    levelName: item.levelName,
    minGrowthValue: item.minGrowthValue,
    benefits: item.benefits?.length ? item.benefits.map((benefit) => ({ ...benefit })) : [emptyBenefit()],
    iconUrl: item.iconUrl || '',
    color: item.color || '#e4002b',
    sortOrder: item.sortOrder,
    status: item.status,
  };
}

function emptyBenefit(): MemberBenefitItem {
  return { title: '', desc: '' };
}

function addBenefit() {
  editing.value?.benefits.push(emptyBenefit());
}

function removeBenefit(index: number) {
  if (!editing.value) return;
  editing.value.benefits.splice(index, 1);
  if (editing.value.benefits.length === 0) editing.value.benefits.push(emptyBenefit());
}

async function save() {
  if (!editing.value) return;
  try {
    const payload: MemberLevelForm = {
      ...editing.value,
      levelCode: editing.value.levelCode.trim(),
      levelName: editing.value.levelName.trim(),
      iconUrl: editing.value.iconUrl?.trim() || undefined,
      color: editing.value.color?.trim() || undefined,
      benefits: editing.value.benefits
        .map((benefit) => ({ title: benefit.title.trim(), desc: benefit.desc.trim() }))
        .filter((benefit) => benefit.title || benefit.desc),
    };
    const saved = editingId.value
      ? await updateMemberLevel(editingId.value, payload)
      : await createMemberLevel(payload);
    levels.value = levels.value.some((item) => item.id === saved.id)
      ? levels.value.map((item) => (item.id === saved.id ? saved : item))
      : [...levels.value, saved];
    editing.value = null;
    emit('notice', `${saved.levelName} 已保存`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}
</script>

<template>
  <section class="panel-card">
    <div class="panel-toolbar">
      <h2>会员等级配置</h2>
      <div class="toolbar-actions">
        <button class="secondary-btn" @click="load">刷新</button>
        <button class="primary-btn" @click="openCreate">新增等级</button>
      </div>
    </div>

    <div class="table-wrap">
      <table>
        <thead><tr><th>等级</th><th>成长门槛</th><th>权益</th><th>排序</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in levels" :key="item.id">
            <td>
              <strong>{{ item.levelName }}</strong>
              <span>{{ item.levelCode }} · {{ item.color || '默认色' }}</span>
            </td>
            <td>{{ item.minGrowthValue }}</td>
            <td>
              <span v-for="benefit in item.benefits" :key="benefit.title">{{ benefit.title }}：{{ benefit.desc }}</span>
              <span v-if="!item.benefits?.length">暂无权益</span>
            </td>
            <td>{{ item.sortOrder }}</td>
            <td><span class="tag" :class="item.status">{{ item.status === 'enabled' ? '启用' : '停用' }}</span></td>
            <td><button class="secondary-btn small" @click="openEdit(item)">编辑</button></td>
          </tr>
          <tr v-if="levels.length === 0"><td colspan="6" class="empty-cell">暂无会员等级</td></tr>
        </tbody>
      </table>
    </div>
  </section>

  <PanelModal v-if="editing" :title="editingId ? '编辑会员等级' : '新增会员等级'" @close="editing = null">
    <form class="modal-form" @submit.prevent="save">
      <div class="form-grid three">
        <label>等级编码<input v-model="editing.levelCode" required placeholder="GOLD" /></label>
        <label>等级名称<input v-model="editing.levelName" required placeholder="金卡会员" /></label>
        <label>成长门槛<input v-model.number="editing.minGrowthValue" type="number" min="0" required /></label>
      </div>
      <div class="form-grid three">
        <label>等级颜色<input v-model="editing.color" placeholder="#e4002b" /></label>
        <label>排序<input v-model.number="editing.sortOrder" type="number" min="0" /></label>
        <label>
          状态
          <select v-model="editing.status">
            <option value="enabled">启用</option>
            <option value="disabled">停用</option>
          </select>
        </label>
      </div>
      <label>图标地址<input v-model="editing.iconUrl" placeholder="可选" /></label>

      <div class="benefit-editor">
        <div class="panel-toolbar compact">
          <h3>权益条目</h3>
          <button class="secondary-btn small" type="button" @click="addBenefit">添加权益</button>
        </div>
        <div v-for="(benefit, index) in editing.benefits" :key="index" class="benefit-row">
          <input v-model="benefit.title" placeholder="权益标题" />
          <input v-model="benefit.desc" placeholder="权益说明" />
          <button class="text-btn small" type="button" @click="removeBenefit(index)">删除</button>
        </div>
      </div>

      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="editing = null">取消</button>
        <button class="primary-btn">保存等级</button>
      </div>
    </form>
  </PanelModal>
</template>
