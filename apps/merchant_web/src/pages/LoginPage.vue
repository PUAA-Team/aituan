<script setup lang="ts">
import { reactive, ref } from 'vue';
import PanelModal from '../components/PanelModal.vue';
import { submitMerchantApplication } from '../api';

const props = defineProps<{
  loading: boolean;
  message: string;
}>();

const emit = defineEmits<{
  submit: [payload: { account: string; password: string }];
}>();

const account = ref('demo_merchant');
const password = ref('123456');
const applying = ref(false);
const applicationBusy = ref(false);
const applicationMessage = ref('');
const applicationForm = reactive({
  merchantName: '',
  contactName: '',
  contactPhone: '',
  businessType: 'takeaway',
  storeName: '',
  address: '',
});

const businessTypes = [
  { value: 'takeaway', label: '外卖' },
  { value: 'group_buy', label: '团购' },
  { value: 'hotel', label: '酒店' },
  { value: 'entertainment', label: '休闲娱乐' },
  { value: 'movie', label: '电影演出' },
  { value: 'beauty', label: '丽人医美' },
  { value: 'ticket', label: '景点门票' },
  { value: 'massage', label: '洗脚按摩' },
];

async function submitApplication() {
  applicationBusy.value = true;
  applicationMessage.value = '';
  try {
    const result = await submitMerchantApplication(applicationForm);
    applicationMessage.value = `申请已提交，申请编号 ${result.applicationNo}。审核通过后可用申请编号和默认密码 123456 登录。`;
    Object.assign(applicationForm, {
      merchantName: '',
      contactName: '',
      contactPhone: '',
      businessType: 'takeaway',
      storeName: '',
      address: '',
    });
  } catch (error) {
    applicationMessage.value = error instanceof Error ? error.message : String(error);
  } finally {
    applicationBusy.value = false;
  }
}
</script>

<template>
  <main class="login-shell">
    <section class="login-copy">
      <span class="section-kicker">AITUAN MERCHANT</span>
      <h1>商家经营控制台</h1>
      <div class="login-points">
        <span>订单履约</span>
        <span>商品维护</span>
        <span>门店资料</span>
      </div>
    </section>

    <form class="login-card" @submit.prevent="emit('submit', { account, password })">
      <h2>商家登录</h2>
      <label>
        账号
        <input v-model="account" autocomplete="username" />
      </label>
      <label>
        密码
        <input v-model="password" type="password" autocomplete="current-password" />
      </label>
      <button class="primary-btn" :disabled="props.loading">{{ props.loading ? '登录中' : '登录' }}</button>
      <button class="secondary-btn" type="button" @click="applying = true">申请入驻</button>
      <p v-if="props.message" class="notice-bar">{{ props.message }}</p>
    </form>
  </main>

  <PanelModal v-if="applying" title="商家入驻申请" width="760px" @close="applying = false">
    <form class="modal-form" @submit.prevent="submitApplication">
      <div class="form-grid two">
        <label>商家主体名称<input v-model="applicationForm.merchantName" required /></label>
        <label>门店名称<input v-model="applicationForm.storeName" required /></label>
      </div>
      <div class="form-grid three">
        <label>联系人<input v-model="applicationForm.contactName" required /></label>
        <label>联系电话<input v-model="applicationForm.contactPhone" required /></label>
        <label>
          业务类型
          <select v-model="applicationForm.businessType">
            <option v-for="item in businessTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
      </div>
      <label>经营地址<input v-model="applicationForm.address" required /></label>
      <p v-if="applicationMessage" class="notice-bar">{{ applicationMessage }}</p>
      <div class="form-actions">
        <button class="secondary-btn" type="button" @click="applying = false">关闭</button>
        <button class="primary-btn" :disabled="applicationBusy">{{ applicationBusy ? '提交中' : '提交申请' }}</button>
      </div>
    </form>
  </PanelModal>
</template>
