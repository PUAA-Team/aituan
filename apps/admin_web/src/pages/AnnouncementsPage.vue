<script setup lang="ts">
import { ref, watch } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import { createAnnouncement, fetchAnnouncements, updateAnnouncement, updateAnnouncementStatus } from '../api';
import type { Announcement, AnnouncementForm } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const status = ref('');
const announcements = ref<Announcement[]>([]);
const editingId = ref<number | null>(null);
const editing = ref<AnnouncementForm | null>(null);

watch(() => props.refreshKey, load, { immediate: true });
watch(status, load);

async function load() {
  try {
    announcements.value = (await fetchAnnouncements(status.value)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function openCreate() {
  editingId.value = null;
  editing.value = {
    title: '',
    content: '',
    targetClient: 'all',
    coverUrl: '',
    status: 'draft',
    sortOrder: 0,
  };
}

function openEdit(item: Announcement) {
  editingId.value = item.id;
  editing.value = {
    title: item.title,
    content: item.content,
    targetClient: item.targetClient,
    coverUrl: item.coverUrl || '',
    status: item.status,
    startAt: item.startAt,
    endAt: item.endAt,
    sortOrder: item.sortOrder || 0,
  };
}

async function save() {
  if (!editing.value) return;
  try {
    const saved = editingId.value
      ? await updateAnnouncement(editingId.value, editing.value)
      : await createAnnouncement(editing.value);
    const exists = announcements.value.some((item) => item.id === saved.id);
    announcements.value = exists
      ? announcements.value.map((item) => (item.id === saved.id ? saved : item))
      : [saved, ...announcements.value];
    editing.value = null;
    emit('notice', '公告已保存');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function setStatus(item: Announcement, next: string) {
  try {
    const updated = await updateAnnouncementStatus(item.id, next);
    announcements.value = announcements.value.map((entry) => (entry.id === updated.id ? updated : entry));
    emit('notice', `${updated.title} 状态已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="panel-card">
    <div class="panel-toolbar">
      <h2>公告运营</h2>
      <div class="toolbar-actions">
        <select v-model="status">
          <option value="">全部状态</option>
          <option value="draft">草稿</option>
          <option value="published">已发布</option>
          <option value="offline">已下线</option>
        </select>
        <button class="primary-btn" @click="openCreate">新建公告</button>
      </div>
    </div>
    <div class="table-wrap">
      <table>
        <thead><tr><th>标题</th><th>目标端</th><th>状态</th><th>排序</th><th>更新时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="item in announcements" :key="item.id">
            <td><strong>{{ item.title }}</strong><span>{{ item.content }}</span></td>
            <td>{{ item.targetClient }}</td>
            <td><span class="tag" :class="item.status">{{ item.status }}</span></td>
            <td>{{ item.sortOrder }}</td>
            <td>{{ timeText(item.updatedAt) }}</td>
            <td>
              <div class="row-actions">
                <button class="secondary-btn small" @click="openEdit(item)">编辑</button>
                <button class="primary-btn small" @click="setStatus(item, item.status === 'published' ? 'offline' : 'published')">
                  {{ item.status === 'published' ? '下线' : '发布' }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="announcements.length === 0"><td colspan="6" class="empty-cell">暂无公告</td></tr>
        </tbody>
      </table>
    </div>
  </section>

  <PanelModal v-if="editing" :title="editingId ? '编辑公告' : '新建公告'" @close="editing = null">
    <form class="modal-form" @submit.prevent="save">
      <label>标题<input v-model="editing.title" required /></label>
      <label>内容<input v-model="editing.content" required /></label>
      <div class="form-grid three">
        <label>
          目标端
          <select v-model="editing.targetClient">
            <option value="all">全部</option>
            <option value="user">用户端</option>
            <option value="merchant">商家端</option>
          </select>
        </label>
        <label>
          状态
          <select v-model="editing.status">
            <option value="draft">草稿</option>
            <option value="published">发布</option>
            <option value="offline">下线</option>
          </select>
        </label>
        <label>排序<input v-model.number="editing.sortOrder" type="number" /></label>
      </div>
      <label>封面地址<input v-model="editing.coverUrl" /></label>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="editing = null">取消</button>
        <button class="primary-btn">保存公告</button>
      </div>
    </form>
  </PanelModal>
</template>
