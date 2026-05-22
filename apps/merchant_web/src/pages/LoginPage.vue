<script setup lang="ts">
import { ref } from 'vue';

defineProps<{
  loading: boolean;
  message: string;
}>();

const emit = defineEmits<{
  submit: [payload: { account: string; password: string }];
}>();

const account = ref('demo_merchant');
const password = ref('aituan123');
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
      <button class="primary-btn" :disabled="loading">{{ loading ? '登录中' : '登录' }}</button>
      <p v-if="message" class="notice-bar">{{ message }}</p>
    </form>
  </main>
</template>
