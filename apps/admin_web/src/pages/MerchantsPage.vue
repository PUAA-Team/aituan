<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import {
  approveMerchantApplication,
  createMerchant,
  createStore,
  fetchCertificationMaterials,
  fetchMerchantApplications,
  fetchMerchants,
  fetchStores,
  rejectMerchantApplication,
  resolveAssetUrl,
  updateCertificationMaterialStatus,
  updateMerchant,
  updateMerchantStatus,
  updateStore,
  updateStoreStatus,
  uploadStoreCover,
} from '../api';
import type { AdminCertificationMaterial, AdminMerchant, AdminMerchantApplication, AdminMerchantForm, AdminStore, AdminStoreForm } from '../types';

const props = defineProps<{
  refreshKey: number;
}>();

const emit = defineEmits<{
  notice: [message: string];
}>();

const merchants = ref<AdminMerchant[]>([]);
const stores = ref<AdminStore[]>([]);
const applications = ref<AdminMerchantApplication[]>([]);
const materials = ref<AdminCertificationMaterial[]>([]);
const keyword = ref('');
const applicationStatus = ref('pending');
const materialStatus = ref('pending');
const applicationRemark = ref('');
const materialRemark = ref('');
const filters = reactive({ businessType: '', status: '' });
const merchantEditingId = ref<number | null>(null);
const merchantEditing = ref<AdminMerchantForm | null>(null);
const storeEditingId = ref<number | null>(null);
const storeEditing = ref<AdminStoreForm | null>(null);

const businessTypes = [
  { value: '', label: '全部业务' },
  { value: 'takeaway', label: '外卖' },
  { value: 'group_buy', label: '团购' },
  { value: 'hotel', label: '酒店' },
  { value: 'entertainment', label: '休闲娱乐' },
  { value: 'movie', label: '电影演出' },
  { value: 'beauty', label: '丽人医美' },
  { value: 'ticket', label: '景点门票' },
  { value: 'massage', label: '洗脚按摩' },
];

const selectableBusinessTypes = computed(() => businessTypes.filter((item) => item.value));

watch(() => props.refreshKey, load, { immediate: true });
watch([() => filters.businessType, () => filters.status], loadStores);
watch(applicationStatus, loadApplications);
watch(materialStatus, loadMaterials);

async function load() {
  await Promise.all([loadMerchants(), loadStores(), loadApplications(), loadMaterials()]);
}

async function loadMerchants() {
  try {
    merchants.value = (await fetchMerchants(keyword.value)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function loadStores() {
  try {
    stores.value = (await fetchStores(filters)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function loadApplications() {
  try {
    applications.value = (await fetchMerchantApplications(applicationStatus.value)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function loadMaterials() {
  try {
    materials.value = (await fetchCertificationMaterials(materialStatus.value)).list;
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function auditApplication(application: AdminMerchantApplication, action: 'approve' | 'reject') {
  try {
    const updated = action === 'approve'
      ? await approveMerchantApplication(application.id, applicationRemark.value)
      : await rejectMerchantApplication(application.id, applicationRemark.value);
    applications.value = applications.value.map((item) => (item.id === updated.id ? updated : item));
    applicationRemark.value = '';
    emit('notice', action === 'approve' ? `${updated.merchantName} 已通过，登录账号为 ${updated.applicationNo}，默认密码 123456` : `${updated.merchantName} 已驳回`);
    await Promise.all([loadMerchants(), loadStores()]);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function auditMaterial(material: AdminCertificationMaterial, status: 'approved' | 'rejected') {
  try {
    const updated = await updateCertificationMaterialStatus(material.id, status, materialRemark.value);
    materials.value = materials.value.map((item) => (item.id === updated.id ? updated : item));
    materialRemark.value = '';
    emit('notice', `${updated.materialName} 已${status === 'approved' ? '通过' : '驳回'}`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function openCreateMerchant() {
  merchantEditingId.value = null;
  merchantEditing.value = {
    accountId: undefined,
    merchantName: '',
    contactName: '',
    contactPhone: '',
    licenseNo: '',
    status: 'normal',
    auditStatus: 'approved',
  };
}

function openEditMerchant(merchant: AdminMerchant) {
  merchantEditingId.value = merchant.merchantId;
  merchantEditing.value = {
    accountId: merchant.accountId,
    merchantName: merchant.merchantName,
    contactName: merchant.contactName,
    contactPhone: merchant.contactPhone,
    licenseNo: merchant.licenseNo || '',
    status: merchant.status,
    auditStatus: merchant.auditStatus,
  };
}

async function saveMerchant() {
  if (!merchantEditing.value) return;
  try {
    const saved = merchantEditingId.value
      ? await updateMerchant(merchantEditingId.value, merchantEditing.value)
      : await createMerchant(merchantEditing.value);
    merchants.value = merchants.value.some((item) => item.merchantId === saved.merchantId)
      ? merchants.value.map((item) => (item.merchantId === saved.merchantId ? saved : item))
      : [saved, ...merchants.value];
    merchantEditing.value = null;
    emit('notice', `${saved.merchantName} 已保存`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function setMerchantStatus(merchant: AdminMerchant, status: string) {
  try {
    const updated = await updateMerchantStatus(merchant.merchantId, status);
    merchants.value = merchants.value.map((item) => (item.merchantId === updated.merchantId ? updated : item));
    emit('notice', `${updated.merchantName} 状态已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

function openCreateStore(merchant?: AdminMerchant) {
  storeEditingId.value = null;
  storeEditing.value = {
    merchantId: merchant?.merchantId,
    storeName: merchant?.merchantName || '',
    businessType: filters.businessType || 'takeaway',
    summary: '',
    address: '',
    status: 'open',
    businessHoursText: '09:00-22:00',
    tagText: '',
    coverUrl: '',
    contactPhone: merchant?.contactPhone || '',
    announcement: '',
  };
}

function openEditStore(store: AdminStore) {
  storeEditingId.value = store.storeId;
  storeEditing.value = {
    merchantId: store.merchantId,
    storeName: store.storeName,
    businessType: store.businessType,
    summary: store.summary,
    address: store.address,
    status: store.status,
    businessHoursText: store.businessHoursText || '',
    tagText: store.tagText || '',
    coverUrl: store.coverUrl || '',
    contactPhone: store.contactPhone || '',
    announcement: store.announcement || '',
  };
}

async function saveStore() {
  if (!storeEditing.value?.merchantId) return;
  try {
    const saved = storeEditingId.value
      ? await updateStore(storeEditingId.value, storeEditing.value)
      : await createStore(storeEditing.value);
    stores.value = stores.value.some((item) => item.storeId === saved.storeId)
      ? stores.value.map((item) => (item.storeId === saved.storeId ? saved : item))
      : [saved, ...stores.value];
    storeEditing.value = null;
    emit('notice', `${saved.storeName} 已保存`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function setStoreStatus(store: AdminStore, status: string) {
  try {
    const updated = await updateStoreStatus(store.storeId, status);
    stores.value = stores.value.map((item) => (item.storeId === updated.storeId ? updated : item));
    emit('notice', `${updated.storeName} 状态已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  }
}

async function onCoverChange(store: AdminStore, event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  try {
    const updated = await uploadStoreCover(store.storeId, file);
    stores.value = stores.value.map((item) => (item.storeId === updated.storeId ? updated : item));
    emit('notice', `${updated.storeName} 图片已更新`);
  } catch (error) {
    emit('notice', error instanceof Error ? error.message : String(error));
  } finally {
    input.value = '';
  }
}

function statusText(value: string) {
  const map: Record<string, string> = { pending: '待审核', approved: '已通过', rejected: '已驳回' };
  return map[value] || value;
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

function timeText(value: string | undefined) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-';
}
</script>

<template>
  <section class="page-grid">
    <section class="panel-card wide">
      <div class="panel-toolbar">
        <h2>入驻申请审核</h2>
        <div class="toolbar-actions">
          <select v-model="applicationStatus">
            <option value="pending">待审核</option>
            <option value="approved">已通过</option>
            <option value="rejected">已驳回</option>
            <option value="">全部</option>
          </select>
          <input v-model="applicationRemark" class="search-input" placeholder="审核意见" />
          <button class="secondary-btn" @click="loadApplications">刷新</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>申请</th><th>联系人</th><th>业务与门店</th><th>状态</th><th>提交时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in applications" :key="item.id">
              <td><strong>{{ item.merchantName }}</strong><span>{{ item.applicationNo }}</span></td>
              <td>{{ item.contactName }}<span>{{ item.contactPhone }}</span></td>
              <td>{{ item.businessType }}<span>{{ item.storeName }} · {{ item.address }}</span></td>
              <td><span class="tag" :class="item.status">{{ statusText(item.status) }}</span><span>{{ item.auditRemark || '-' }}</span></td>
              <td>{{ timeText(item.submittedAt) }}</td>
              <td>
                <div class="row-actions">
                  <button class="primary-btn small" :disabled="item.status !== 'pending'" @click="auditApplication(item, 'approve')">通过</button>
                  <button class="secondary-btn small" :disabled="item.status !== 'pending'" @click="auditApplication(item, 'reject')">驳回</button>
                </div>
              </td>
            </tr>
            <tr v-if="applications.length === 0"><td colspan="6" class="empty-cell">暂无入驻申请</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel-card wide">
      <div class="panel-toolbar">
        <h2>认证材料审核</h2>
        <div class="toolbar-actions">
          <select v-model="materialStatus">
            <option value="pending">待审核</option>
            <option value="approved">已通过</option>
            <option value="rejected">已驳回</option>
            <option value="">全部</option>
          </select>
          <input v-model="materialRemark" class="search-input" placeholder="材料审核意见" />
          <button class="secondary-btn" @click="loadMaterials">刷新</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>商家</th><th>材料</th><th>状态</th><th>提交时间</th><th>文件</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="item in materials" :key="item.id">
              <td><strong>{{ item.merchantName || '-' }}</strong><span>#{{ item.merchantId }}</span></td>
              <td>{{ item.materialName }}<span>{{ materialTypeText(item.materialType) }}</span></td>
              <td><span class="tag" :class="item.status">{{ statusText(item.status) }}</span><span>{{ item.rejectReason || '-' }}</span></td>
              <td>{{ timeText(item.submittedAt) }}</td>
              <td><a :href="resolveAssetUrl(item.fileUrl)" target="_blank" rel="noreferrer">查看</a></td>
              <td>
                <div class="row-actions">
                  <button class="primary-btn small" :disabled="item.status !== 'pending'" @click="auditMaterial(item, 'approved')">通过</button>
                  <button class="secondary-btn small" :disabled="item.status !== 'pending'" @click="auditMaterial(item, 'rejected')">驳回</button>
                </div>
              </td>
            </tr>
            <tr v-if="materials.length === 0"><td colspan="6" class="empty-cell">暂无认证材料</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>商户管理</h2>
        <div class="toolbar-actions">
          <input v-model="keyword" class="search-input" placeholder="商户/联系人" @keyup.enter="loadMerchants" />
          <button class="secondary-btn" @click="loadMerchants">查询</button>
          <button class="primary-btn" @click="openCreateMerchant">新增商户</button>
        </div>
      </div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>商户</th><th>联系人</th><th>门店/商品</th><th>状态</th><th>入驻时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="merchant in merchants" :key="merchant.merchantId">
              <td><strong>{{ merchant.merchantName }}</strong><span>#{{ merchant.merchantId }} · {{ merchant.auditStatus }}</span></td>
              <td>{{ merchant.contactName }}<span>{{ merchant.contactPhone }}</span></td>
              <td>{{ merchant.storeCount }} 家门店<span>{{ merchant.itemCount }} 个商品</span></td>
              <td><span class="tag" :class="merchant.status">{{ merchant.status }}</span></td>
              <td>{{ timeText(merchant.settledAt) }}</td>
              <td>
                <div class="row-actions">
                  <button class="secondary-btn small" @click="openEditMerchant(merchant)">编辑</button>
                  <button class="secondary-btn small" @click="openCreateStore(merchant)">加门店</button>
                  <button class="secondary-btn small" @click="setMerchantStatus(merchant, merchant.status === 'normal' ? 'disabled' : 'normal')">
                    {{ merchant.status === 'normal' ? '停用' : '启用' }}
                  </button>
                  <button class="secondary-btn small" @click="setMerchantStatus(merchant, 'blocked')">拉黑</button>
                </div>
              </td>
            </tr>
            <tr v-if="merchants.length === 0"><td colspan="6" class="empty-cell">暂无商户</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="panel-card">
      <div class="panel-toolbar">
        <h2>门店治理</h2>
        <div class="toolbar-actions">
          <select v-model="filters.businessType">
            <option v-for="item in businessTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option value="open">营业中</option>
            <option value="closed">休息中</option>
          </select>
          <button class="primary-btn" @click="openCreateStore()">新增门店</button>
        </div>
      </div>
      <div class="store-grid">
        <article v-for="store in stores" :key="store.storeId" class="store-card">
          <div class="store-thumb">
            <img v-if="store.coverUrl" :src="resolveAssetUrl(store.coverUrl)" :alt="store.storeName" />
            <span v-else>{{ store.storeName.slice(0, 4) }}</span>
          </div>
          <div>
            <strong>{{ store.storeName }}</strong>
            <p>{{ store.merchantName }} · {{ store.businessType }}</p>
            <p>{{ store.address }}</p>
            <span class="tag" :class="store.status">{{ store.status === 'open' ? '营业中' : '休息中' }}</span>
          </div>
          <div class="row-actions">
            <button class="secondary-btn small" @click="openEditStore(store)">编辑</button>
            <button class="secondary-btn small" @click="setStoreStatus(store, store.status === 'open' ? 'closed' : 'open')">
              {{ store.status === 'open' ? '休息' : '营业' }}
            </button>
            <label class="file-btn small">
              换图
              <input type="file" accept="image/png,image/jpeg,image/webp" @change="onCoverChange(store, $event)" />
            </label>
          </div>
        </article>
        <article v-if="stores.length === 0" class="empty-card">暂无门店</article>
      </div>
    </section>
  </section>

  <PanelModal v-if="merchantEditing" :title="merchantEditingId ? '编辑商户' : '新增商户'" @close="merchantEditing = null">
    <form class="modal-form" @submit.prevent="saveMerchant">
      <div class="form-grid two">
        <label>商户名称<input v-model="merchantEditing.merchantName" required /></label>
        <label>绑定账号ID<input v-model.number="merchantEditing.accountId" type="number" min="1" /></label>
      </div>
      <div class="form-grid three">
        <label>联系人<input v-model="merchantEditing.contactName" /></label>
        <label>联系电话<input v-model="merchantEditing.contactPhone" /></label>
        <label>营业执照号<input v-model="merchantEditing.licenseNo" /></label>
      </div>
      <div class="form-grid two">
        <label>
          账号状态
          <select v-model="merchantEditing.status">
            <option value="normal">正常</option>
            <option value="disabled">停用</option>
            <option value="blocked">拉黑</option>
          </select>
        </label>
        <label>
          审核状态
          <select v-model="merchantEditing.auditStatus">
            <option value="pending">待审核</option>
            <option value="approved">已通过</option>
            <option value="rejected">已驳回</option>
          </select>
        </label>
      </div>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="merchantEditing = null">取消</button>
        <button class="primary-btn">保存商户</button>
      </div>
    </form>
  </PanelModal>

  <PanelModal v-if="storeEditing" :title="storeEditingId ? '编辑门店' : '新增门店'" @close="storeEditing = null">
    <form class="modal-form" @submit.prevent="saveStore">
      <div class="form-grid three">
        <label>商户ID<input v-model.number="storeEditing.merchantId" type="number" min="1" required /></label>
        <label>门店名称<input v-model="storeEditing.storeName" required /></label>
        <label>
          业务类型
          <select v-model="storeEditing.businessType">
            <option v-for="item in selectableBusinessTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
      </div>
      <label>简介<input v-model="storeEditing.summary" required /></label>
      <label>地址<input v-model="storeEditing.address" required /></label>
      <div class="form-grid three">
        <label>
          状态
          <select v-model="storeEditing.status">
            <option value="open">营业中</option>
            <option value="closed">休息中</option>
          </select>
        </label>
        <label>营业时间<input v-model="storeEditing.businessHoursText" /></label>
        <label>联系电话<input v-model="storeEditing.contactPhone" /></label>
      </div>
      <label>标签<input v-model="storeEditing.tagText" /></label>
      <label>封面地址<input v-model="storeEditing.coverUrl" /></label>
      <label>公告<input v-model="storeEditing.announcement" /></label>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="storeEditing = null">取消</button>
        <button class="primary-btn">保存门店</button>
      </div>
    </form>
  </PanelModal>
</template>
