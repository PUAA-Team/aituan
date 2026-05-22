<script setup lang="ts">
import { reactive, watch } from 'vue';
import { resolveAssetUrl, updateCurrentStore, updateMerchantProfile, uploadStoreCover } from '../api';
import type { MerchantProfile, MerchantStore } from '../types';

const props = defineProps<{
  profile: MerchantProfile;
  store: MerchantStore;
}>();

const emit = defineEmits<{
  notice: [message: string];
  changed: [];
}>();

const profileForm = reactive({
  merchantName: '',
  contactName: '',
  contactPhone: '',
});

const storeForm = reactive({
  storeName: '',
  summary: '',
  address: '',
  businessHoursText: '',
  tagText: '',
  contactPhone: '',
  announcement: '',
  status: 'open',
});

watch([() => props.profile, () => props.store], syncForms, { immediate: true });

function syncForms() {
  profileForm.merchantName = props.profile.merchantName;
  profileForm.contactName = props.profile.contactName;
  profileForm.contactPhone = props.profile.contactPhone;
  storeForm.storeName = props.store.storeName;
  storeForm.summary = props.store.summary;
  storeForm.address = props.store.address;
  storeForm.businessHoursText = props.store.businessHoursText || '';
  storeForm.tagText = props.store.tagText || '';
  storeForm.contactPhone = props.store.contactPhone || '';
  storeForm.announcement = props.store.announcement || '';
  storeForm.status = props.store.status || 'open';
}

async function saveProfile() {
  try {
    await updateMerchantProfile(profileForm);
    emit('notice', '商家主体资料已保存');
    emit('changed');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function saveStore() {
  try {
    await updateCurrentStore(storeForm);
    emit('notice', '门店资料已保存');
    emit('changed');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function onCoverChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  try {
    await uploadStoreCover(file);
    emit('notice', '门店图片已更新');
    emit('changed');
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    input.value = '';
  }
}
</script>

<template>
  <section class="page-grid two-col">
    <article class="panel-card">
      <div class="panel-toolbar"><h2>门店封面</h2></div>
      <div class="store-cover">
        <img v-if="props.store.coverUrl" :src="resolveAssetUrl(props.store.coverUrl)" :alt="props.store.storeName" />
        <span v-else>{{ props.store.storeName }}</span>
      </div>
      <label>
        上传图片
        <input type="file" accept="image/png,image/jpeg,image/webp" @change="onCoverChange" />
      </label>
    </article>

    <form class="panel-card" @submit.prevent="saveProfile">
      <div class="panel-toolbar"><h2>商家主体</h2></div>
      <label>
        商家名称
        <input v-model="profileForm.merchantName" required />
      </label>
      <div class="form-grid two">
        <label>
          联系人
          <input v-model="profileForm.contactName" required />
        </label>
        <label>
          联系电话
          <input v-model="profileForm.contactPhone" required />
        </label>
      </div>
      <div class="info-list compact">
        <span>资质编号：{{ props.profile.licenseNo }}</span>
        <span>审核状态：{{ props.profile.auditStatus }}</span>
      </div>
      <div class="form-actions"><button class="primary-btn">保存主体资料</button></div>
    </form>

    <form class="panel-card wide" @submit.prevent="saveStore">
      <div class="panel-toolbar"><h2>门店经营资料</h2></div>
      <div class="form-grid two">
        <label>
          门店名称
          <input v-model="storeForm.storeName" required />
        </label>
        <label>
          营业状态
          <select v-model="storeForm.status">
            <option value="open">营业中</option>
            <option value="closed">休息中</option>
          </select>
        </label>
      </div>
      <label>
        门店简介
        <input v-model="storeForm.summary" required />
      </label>
      <label>
        门店地址
        <input v-model="storeForm.address" required />
      </label>
      <div class="form-grid two">
        <label>
          营业时间
          <input v-model="storeForm.businessHoursText" />
        </label>
        <label>
          门店电话
          <input v-model="storeForm.contactPhone" />
        </label>
      </div>
      <label>
        门店标签
        <input v-model="storeForm.tagText" placeholder="出餐快,免预约" />
      </label>
      <label>
        门店公告
        <input v-model="storeForm.announcement" />
      </label>
      <div class="form-actions"><button class="primary-btn">保存门店资料</button></div>
    </form>
  </section>
</template>
