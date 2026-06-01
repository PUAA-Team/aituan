<script setup lang="ts">
import { ref, watch } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import { fetchConfigs, updateConfig } from '../api';
import type { AdminConfig } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const configs = ref<AdminConfig[]>([]);
const creating = ref(false);
const newConfig = ref({ configKey: '', configValue: '', remark: '' });

watch(() => props.refreshKey, load, { immediate: true });

async function load() {
  try {
    configs.value = await fetchConfigs();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function openCreate() {
  newConfig.value = { configKey: '', configValue: '', remark: '' };
  creating.value = true;
}

async function createConfig() {
  const key = newConfig.value.configKey.trim();
  if (!key) return;
  try {
    configs.value = await updateConfig(key, newConfig.value.configValue, newConfig.value.remark);
    creating.value = false;
    emit('notice', `${key} 已新增`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function saveConfig(item: AdminConfig) {
  try {
    configs.value = await updateConfig(item.configKey, item.configValue, item.remark);
    emit('notice', `${item.configKey} 已保存`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}
</script>

<template>
  <section class="page-grid">
    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>平台设置</h2>
        <div class="toolbar-actions">
          <button class="primary-btn" @click="openCreate">新增配置</button>
        </div>
      </div>
      <div class="config-list">
        <form v-for="item in configs" :key="item.configKey" class="config-row" @submit.prevent="saveConfig(item)">
          <strong>{{ item.configKey }}</strong>
          <input v-model="item.configValue" />
          <input v-model="item.remark" placeholder="备注" />
          <button class="secondary-btn small">保存</button>
        </form>
        <div v-if="configs.length === 0" class="empty-card">暂无配置</div>
      </div>
      <p class="hint">审计日志已独立为「审计日志」菜单，方便按操作人 / 动作类型筛选。</p>
    </section>
  </section>

  <PanelModal v-if="creating" title="新增配置" @close="creating = false">
    <form class="modal-form" @submit.prevent="createConfig">
      <label>配置键<input v-model="newConfig.configKey" required placeholder="例如 delivery_tick_minutes" /></label>
      <label>配置值<input v-model="newConfig.configValue" required /></label>
      <label>备注<input v-model="newConfig.remark" /></label>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="creating = false">取消</button>
        <button class="primary-btn">保存配置</button>
      </div>
    </form>
  </PanelModal>
</template>

<style scoped>
.hint {
  margin-top: 12px;
  color: #86909c;
  font-size: 12px;
}
</style>
