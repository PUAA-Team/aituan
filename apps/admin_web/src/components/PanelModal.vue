<script setup lang="ts">
defineProps<{
  title: string;
  width?: string;
}>();

const emit = defineEmits<{
  close: [];
}>();
</script>

<template>
  <div class="modal-mask" @click.self="emit('close')">
    <section class="modal-panel" :style="{ width: width || '760px' }">
      <header class="modal-head">
        <h3>{{ title }}</h3>
        <button class="text-btn" type="button" @click="emit('close')">关闭</button>
      </header>
      <slot />
    </section>
  </div>
</template>
