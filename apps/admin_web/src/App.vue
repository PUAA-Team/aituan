<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { clearToken, getToken, login } from './api';
import AdminFrame from './components/AdminFrame.vue';
import AnnouncementsPage from './pages/AnnouncementsPage.vue';
import AuditLogsPage from './pages/AuditLogsPage.vue';
import BookingsPage from './pages/BookingsPage.vue';
import CatalogPage from './pages/CatalogPage.vue';
import ComplaintsPage from './pages/ComplaintsPage.vue';
import DashboardPage from './pages/DashboardPage.vue';
import DeliveryPage from './pages/DeliveryPage.vue';
import LoginPage from './pages/LoginPage.vue';
import MerchantsPage from './pages/MerchantsPage.vue';
import OrdersPage from './pages/OrdersPage.vue';
import ReviewsPage from './pages/ReviewsPage.vue';
import SettingsPage from './pages/SettingsPage.vue';
import UsersPage from './pages/UsersPage.vue';
import VouchersPage from './pages/VouchersPage.vue';
import type { AdminPage } from './types';

const token = ref(getToken());
const loading = ref(false);
const notice = ref('');
const activePage = ref<AdminPage>('dashboard');
const refreshKey = ref(0);

const loggedIn = computed(() => token.value.length > 0);
const pageTitle = computed(() => {
  const map: Record<AdminPage, string> = {
    dashboard: '平台总览',
    orders: '订单治理',
    merchants: '商户门店',
    users: '用户管理',
    catalog: '商品治理',
    delivery: '配送任务',
    vouchers: '券码治理',
    bookings: '预约治理',
    announcements: '公告运营',
    reviews: '评价审核',
    complaints: '投诉工单',
    audit: '审计日志',
    settings: '平台设置',
  };
  return map[activePage.value];
});

onMounted(() => {
  if (loggedIn.value) refreshKey.value += 1;
});

async function submitLogin(payload: { account: string; password: string }) {
  try {
    loading.value = true;
    const session = await login(payload.account, payload.password);
    token.value = session.token;
    notice.value = `欢迎回来，${session.profile.nickname || '管理员'}`;
    refreshKey.value += 1;
  } catch (error) {
    notice.value = error instanceof Error ? error.message : String(error);
  } finally {
    loading.value = false;
  }
}

function refresh() {
  refreshKey.value += 1;
}

function logout() {
  clearToken();
  token.value = '';
  notice.value = '';
  activePage.value = 'dashboard';
}

function setNotice(message: string) {
  notice.value = message;
}
</script>

<template>
  <LoginPage v-if="!loggedIn" :loading="loading" :message="notice" @submit="submitLogin" />
  <AdminFrame
    v-else
    :active-page="activePage"
    :title="pageTitle"
    :loading="loading"
    :notice="notice"
    @navigate="activePage = $event"
    @refresh="refresh"
    @logout="logout"
  >
    <DashboardPage v-if="activePage === 'dashboard'" :refresh-key="refreshKey" @notice="setNotice" />
    <OrdersPage v-else-if="activePage === 'orders'" :refresh-key="refreshKey" @notice="setNotice" />
    <MerchantsPage v-else-if="activePage === 'merchants'" :refresh-key="refreshKey" @notice="setNotice" />
    <UsersPage v-else-if="activePage === 'users'" :refresh-key="refreshKey" @notice="setNotice" />
    <CatalogPage v-else-if="activePage === 'catalog'" :refresh-key="refreshKey" @notice="setNotice" />
    <DeliveryPage v-else-if="activePage === 'delivery'" :refresh-key="refreshKey" @notice="setNotice" />
    <VouchersPage v-else-if="activePage === 'vouchers'" :refresh-key="refreshKey" @notice="setNotice" />
    <BookingsPage v-else-if="activePage === 'bookings'" :refresh-key="refreshKey" @notice="setNotice" />
    <AnnouncementsPage v-else-if="activePage === 'announcements'" :refresh-key="refreshKey" @notice="setNotice" />
    <ReviewsPage v-else-if="activePage === 'reviews'" :refresh-key="refreshKey" @notice="setNotice" />
    <ComplaintsPage v-else-if="activePage === 'complaints'" :refresh-key="refreshKey" @notice="setNotice" />
    <AuditLogsPage v-else-if="activePage === 'audit'" :refresh-key="refreshKey" @notice="setNotice" />
    <SettingsPage v-else :refresh-key="refreshKey" @notice="setNotice" />
  </AdminFrame>
</template>
