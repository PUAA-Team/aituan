<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { fetchCertification, resolveAssetUrl, updateCurrentStore, updateMerchantProfile, uploadCertificationMaterial, uploadStoreCover } from '../api';
import type { MerchantCertification, MerchantProfile, MerchantStore } from '../types';

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
  longitude: '',
  latitude: '',
});

const certification = ref<MerchantCertification | null>(null);
const materialType = ref('business_license');
const materialName = ref('营业执照');
const materialBusy = ref(false);

watch([() => props.profile, () => props.store], syncForms, { immediate: true });
watch(() => props.profile.merchantId, loadCertification);
onMounted(loadCertification);

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
  storeForm.longitude = props.store.longitude == null ? '' : String(props.store.longitude);
  storeForm.latitude = props.store.latitude == null ? '' : String(props.store.latitude);
}

async function loadCertification() {
  try {
    certification.value = await fetchCertification();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
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
    await updateCurrentStore({
      ...storeForm,
      longitude: numericOrNull(storeForm.longitude),
      latitude: numericOrNull(storeForm.latitude),
    });
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

async function onMaterialChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  materialBusy.value = true;
  try {
    await uploadCertificationMaterial(materialType.value, materialName.value, file);
    emit('notice', '认证材料已提交审核');
    await loadCertification();
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    materialBusy.value = false;
    input.value = '';
  }
}

function materialTypeText(value: string) {
  const map: Record<string, string> = {
    business_license: '营业执照',
    food_license: '食品经营许可证',
    identity: '法人身份证明',
    other: '其他材料',
  };
  return map[value] || value;
}

function statusText(value: string) {
  const map: Record<string, string> = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回',
  };
  return map[value] || value;
}

function numericOrNull(value: string) {
  const trimmed = value.trim();
  return trimmed ? Number(trimmed) : null;
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
        <span>资质编号：{{ props.profile.licenseNo || '-' }}</span>
        <span>审核状态：{{ statusText(props.profile.auditStatus) }}</span>
      </div>
      <div class="form-actions"><button class="primary-btn">保存主体资料</button></div>
    </form>

    <article class="panel-card wide">
      <div class="panel-toolbar"><h2>认证材料</h2></div>
      <div class="form-grid three">
        <label>
          材料类型
          <select v-model="materialType">
            <option value="business_license">营业执照</option>
            <option value="food_license">食品经营许可证</option>
            <option value="identity">法人身份证明</option>
            <option value="other">其他材料</option>
          </select>
        </label>
        <label>材料名称<input v-model="materialName" /></label>
        <label>
          上传材料
          <input type="file" accept="image/png,image/jpeg,image/webp" :disabled="materialBusy" @change="onMaterialChange" />
        </label>
      </div>
      <div class="audit-list">
        <div v-for="item in certification?.materials || []" :key="item.id" class="audit-row">
          <strong>{{ item.materialName || materialTypeText(item.materialType) }}</strong>
          <span>{{ materialTypeText(item.materialType) }} · {{ statusText(item.status) }}</span>
          <small>
            <a :href="resolveAssetUrl(item.fileUrl)" target="_blank" rel="noreferrer">查看材料</a>
            <template v-if="item.rejectReason"> · {{ item.rejectReason }}</template>
          </small>
        </div>
        <div v-if="!certification?.materials?.length" class="empty-card">暂无认证材料</div>
      </div>
    </article>

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
          经度
          <input v-model="storeForm.longitude" type="number" step="0.000001" min="-180" max="180" placeholder="留空则保存时自动解析" />
        </label>
        <label>
          纬度
          <input v-model="storeForm.latitude" type="number" step="0.000001" min="-90" max="90" placeholder="留空则保存时自动解析" />
        </label>
      </div>
      <p class="form-hint">经纬度用于附近商家排序、距离和预计送达；留空时后端会尝试根据门店地址自动解析，解析不准可手动修正。</p>
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
