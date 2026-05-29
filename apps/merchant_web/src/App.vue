<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { clearToken, fetchCurrentStore, fetchMerchantProfile, getToken, login } from './api';
import ConsoleFrame from './components/ConsoleFrame.vue';
import CatalogPage from './pages/CatalogPage.vue';
import DashboardPage from './pages/DashboardPage.vue';
import FulfillmentPage from './pages/FulfillmentPage.vue';
import LoginPage from './pages/LoginPage.vue';
import OrderPage from './pages/OrderPage.vue';
import ReviewPage from './pages/ReviewPage.vue';
import SessionsPage from './pages/SessionsPage.vue';
import StorePage from './pages/StorePage.vue';
import type { ConsolePage, MerchantProfile, MerchantStore } from './types';

const token = ref(getToken());
const loading = ref(false);
const notice = ref('');
const activePage = ref<ConsolePage>('dashboard');
const refreshKey = ref(0);
const profile = ref<MerchantProfile | null>(null);
const store = ref<MerchantStore | null>(null);

const loggedIn = computed(() => token.value.length > 0);
const isTakeaway = computed(() => store.value?.businessType === 'takeaway');
const pageTitle = computed(() => {
  const map: Record<ConsolePage, string> = {
    dashboard: '商家驾驶舱',
    orders: '订单中心',
    catalog: isTakeaway.value ? '商品管理' : '服务与套餐',
    fulfillment: '履约设置',
    store: '门店资料',
    reviews: '评价管理',
    sessions: '客服会话',
  };
  return map[activePage.value];
});

onMounted(() => {
  if (loggedIn.value) loadShell();
});

async function submitLogin(payload: { account: string; password: string }) {
  try {
    loading.value = true;
    const session = await login(payload.account, payload.password);
    token.value = session.token;
    notice.value = `欢迎回来，${session.profile.nickname || '商家'}`;
    await loadShell();
  } catch (error) {
    notice.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

async function loadShell() {
  try {
    loading.value = true;
    const [nextProfile, nextStore] = await Promise.all([
      fetchMerchantProfile(),
      fetchCurrentStore(),
    ]);
    profile.value = nextProfile;
    store.value = nextStore;
    refreshKey.value += 1;
  } catch (error) {
    notice.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

function refreshActivePage() {
  loadShell();
}

function logout() {
  clearToken();
  token.value = '';
  profile.value = null;
  store.value = null;
  notice.value = '';
  activePage.value = 'dashboard';
}

function setNotice(message: string) {
  notice.value = message;
}
</script>

<template>
  <LoginPage v-if="!loggedIn" :loading="loading" :message="notice" @submit="submitLogin" />

  <ConsoleFrame
    v-else-if="profile && store"
    :active-page="activePage"
    :title="pageTitle"
    :profile="profile"
    :store="store"
    :loading="loading"
    :notice="notice"
    @navigate="activePage = $event"
    @refresh="refreshActivePage"
    @logout="logout"
  >
    <OrderPage v-if="activePage === 'orders'" :store="store" :refresh-key="refreshKey" @notice="setNotice" />
    <DashboardPage v-else-if="activePage === 'dashboard'" :refresh-key="refreshKey" />
    <ReviewPage v-else-if="activePage === 'reviews'" :refresh-key="refreshKey" @notice="setNotice" />
    <SessionsPage v-else-if="activePage === 'sessions'" :refresh-key="refreshKey" @notice="setNotice" />
    <CatalogPage v-else-if="activePage === 'catalog'" :store="store" :refresh-key="refreshKey" @notice="setNotice" />
    <FulfillmentPage v-else-if="activePage === 'fulfillment'" :store="store" :refresh-key="refreshKey" @notice="setNotice" />
    <StorePage v-else :profile="profile" :store="store" @notice="setNotice" @changed="loadShell" />
  </ConsoleFrame>

  <main v-else class="loading-shell">加载商家资料...</main>
</template>
