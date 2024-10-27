<script setup>
import { RouterView } from 'vue-router';
import AppHeader from '@/components/Header.vue';
import { computed } from 'vue';
import { useStore } from 'vuex';

const store = useStore();
const isAuthenticated = computed(() => store.state.auth.isAuthenticated);
</script>

<template>
  <main>
    <AppHeader v-if="isAuthenticated" />
    <RouterView />
  </main>
</template>

<style scoped>

</style>
