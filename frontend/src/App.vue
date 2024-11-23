<script setup>
import { RouterView } from 'vue-router';
import AppHeader from '@/components/Header.vue';
import { computed, onMounted } from 'vue';
import { useStore } from 'vuex';

const store = useStore();
const isAuthenticated = computed(() => store.state.auth.isAuthenticated);

onMounted(async () => {
  await store.dispatch('auth/getCurrentUser');
  if (isAuthenticated.value) {
    await store.dispatch('recipes/fetchShoppingCart');
  }
});
</script>

<template>
  <main>
    <AppHeader v-if="isAuthenticated" />
    <RouterView />
  </main>
</template>

<style scoped>

</style>
