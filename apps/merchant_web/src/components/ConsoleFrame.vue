<script setup lang="ts">
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

const navItems: Array<{ key: ConsolePage; label: string; desc: string }> = [
  { key: 'orders', label: '订单中心', desc: '接单、备餐、配送推进' },
  { key: 'catalog', label: '商品与服务', desc: '新增、编辑、上下架' },
  { key: 'fulfillment', label: '履约设置', desc: '接单规则、券码核销' },
  { key: 'store', label: '门店资料', desc: '图片、公告、营业信息' },
];

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
        <span>爱团商家</span>
        <strong>{{ props.store.storeName }}</strong>
        <small>{{ businessTypeText(props.store.businessType) }} · {{ props.profile.auditStatus === 'approved' ? '已认证' : '待审核' }}</small>
      </div>

      <nav class="side-nav">
        <button
          v-for="item in navItems"
          :key="item.key"
          :class="{ active: props.activePage === item.key }"
          @click="emit('navigate', item.key)"
        >
          <span>{{ item.label }}</span>
          <small>{{ item.desc }}</small>
        </button>
      </nav>

      <div class="side-footer">
        <span>{{ props.profile.contactName }} · {{ props.profile.contactPhone }}</span>
        <button class="text-btn" @click="emit('logout')">退出登录</button>
      </div>
    </aside>

    <section class="console-main">
      <header class="console-topbar">
        <div>
          <span class="section-kicker">{{ businessTypeText(props.store.businessType) }} 工作台</span>
          <h1>{{ props.title }}</h1>
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
