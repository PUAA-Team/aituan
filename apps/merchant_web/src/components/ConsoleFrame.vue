<script setup lang="ts">
import { computed } from 'vue';
import type { ConsolePage, MerchantProfile, MerchantStore } from '../types';

const props = defineProps<{
  activePage: ConsolePage;
  title: string;
  profile: MerchantProfile;
  store: MerchantStore;
  loading: boolean;
  notice: string;
}>();

const emit = defineEmits<{
  navigate: [page: ConsolePage];
  refresh: [];
  logout: [];
}>();

const isTakeaway = computed(() => props.store.businessType === 'takeaway');

type NavItem = { key: ConsolePage; label: string; desc: string; icon: string; show?: boolean };
type NavGroup = { title: string; detail: string; items: NavItem[] };

const navGroups = computed<NavGroup[]>(() => [
  {
    title: '经营',
    detail: '今日经营与待办',
    items: [
      { key: 'dashboard', label: '经营总览', desc: '今日订单、营业额、待办指标', icon: '营' },
    ],
  },
  {
    title: '门店与商品',
    detail: isTakeaway.value ? '外卖门店、商品、配送规则' : '门店资料、服务套餐、预约规则',
    items: [
      { key: 'store', label: '门店资料', desc: '图片、公告、营业信息', icon: '店' },
      {
        key: 'catalog',
        label: isTakeaway.value ? '商品管理' : '服务与套餐',
        desc: isTakeaway.value ? '新增、编辑、上下架' : '套餐、服务项、上下架',
        icon: '品',
      },
      {
        key: 'fulfillment',
        label: '履约设置',
        desc: isTakeaway.value ? '接单模式、配送规则' : '券码核销、预约预留',
        icon: '设',
      },
    ],
  },
  {
    title: '订单履约',
    detail: isTakeaway.value ? '接单、备餐、配送推进' : '订单、预约与券码核销',
    items: [
      {
        key: 'orders',
        label: '订单中心',
        desc: isTakeaway.value ? '接单、备餐、配送推进' : '券码订单、预约记录',
        icon: '单',
      },
      { key: 'bookings', label: '预约确认', desc: '查看预约、到店确认', icon: '约', show: !isTakeaway.value },
      { key: 'vouchers', label: '券码核销', desc: '券码查询、扫码核销', icon: '券', show: !isTakeaway.value },
    ],
  },
  {
    title: '互动售后',
    detail: '评价与用户咨询',
    items: [
      { key: 'reviews', label: '评价管理', desc: '查看、回复、关注被举报评价', icon: '评' },
      { key: 'sessions', label: '客服会话', desc: '用户咨询、快捷回复模板', icon: '客' },
    ],
  },
]);

const visibleNavGroups = computed(() => navGroups.value
  .map((group) => ({ ...group, items: group.items.filter((item) => item.show !== false) }))
  .filter((group) => group.items.length > 0));

const currentItem = computed(() => visibleNavGroups.value.flatMap((group) => group.items).find((item) => item.key === props.activePage));

function businessTypeText(code: string) {
  const map: Record<string, string> = {
    takeaway: '外卖',
    group_buy: '团购',
    hotel: '酒店',
    entertainment: '休闲娱乐',
    movie: '电影演出',
    beauty: '丽人医美',
    ticket: '景点门票',
    massage: '洗脚按摩',
  };
  return map[code] || code;
}
</script>

<template>
  <section class="console-layout">
    <aside class="console-side">
      <div class="brand-block">
        <span class="brand-eyebrow">爱团商家</span>
        <strong>{{ props.store.storeName }}</strong>
        <small>{{ businessTypeText(props.store.businessType) }} · {{ props.profile.auditStatus === 'approved' ? '已认证' : '待审核' }}</small>
      </div>

      <div class="side-scroll">
        <nav class="side-nav" aria-label="商家功能导航">
          <section v-for="group in visibleNavGroups" :key="group.title" class="nav-group">
            <p class="nav-group-title">
              <span>{{ group.title }}</span>
              <small>{{ group.detail }}</small>
            </p>
            <button
              v-for="item in group.items"
              :key="item.key"
              class="nav-item"
              :class="{ active: props.activePage === item.key }"
              @click="emit('navigate', item.key)"
            >
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-copy">
                <strong>{{ item.label }}</strong>
                <small>{{ item.desc }}</small>
              </span>
            </button>
          </section>
        </nav>
      </div>

      <div class="side-footer">
        <span>{{ props.profile.contactName }} · {{ props.profile.contactPhone }}</span>
        <button class="text-btn" @click="emit('logout')">退出登录</button>
      </div>
    </aside>

    <section class="console-main">
      <header class="console-topbar">
        <div class="topbar-title">
          <span class="section-kicker">{{ businessTypeText(props.store.businessType) }} 工作台</span>
          <h1>{{ props.title }}</h1>
          <p>{{ currentItem?.desc || '商家经营工作台' }}</p>
        </div>
        <div class="top-actions">
          <span class="store-status" :class="props.store.status">{{ props.store.status === 'open' ? '营业中' : '休息中' }}</span>
          <button class="secondary-btn" :disabled="props.loading" @click="emit('refresh')">
            {{ props.loading ? '刷新中' : '刷新' }}
          </button>
        </div>
      </header>

      <p v-if="props.notice" class="notice-bar">{{ props.notice }}</p>
      <slot />
    </section>
  </section>
</template>
