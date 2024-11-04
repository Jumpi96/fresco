<template>
  <header class="app-header">
    <div class="logo-container">
      <img src="@/assets/logo.jpg" alt="App Logo" class="logo">
    </div>
    <h1 class="title">fresco</h1>
    <div class="button-container">
      <button @click="goHome" class="icon-button" aria-label="Go to home page">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
          <polyline points="9 22 9 12 15 12 15 22"></polyline>
        </svg>
      </button>
      <button @click="goToShoppingCart" class="icon-button" aria-label="Go to shopping cart">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1"></circle>
          <circle cx="20" cy="21" r="1"></circle>
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
        </svg>
      </button>
      <button @click="goToFavourites" class="icon-button" aria-label="Go to favourites">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path>
        </svg>
      </button>
      <button @click="logout" class="icon-button" aria-label="Logout">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
          <polyline points="16 17 21 12 16 7"></polyline>
          <line x1="21" y1="12" x2="9" y2="12"></line>
        </svg>
      </button>
    </div>
  </header>
</template>

<script>
import { useRouter } from 'vue-router';
import { useStore } from 'vuex';
import { computed } from 'vue';

export default {
  name: 'AppHeader',
  props: {
    isAuthenticated: {
      type: Boolean,
      default: false
    }
  },
  setup(props) {
    const router = useRouter();
    const store = useStore();

    const goHome = () => {
      router.push('/recipes');
    };

    const goToShoppingCart = () => {
      router.push('/shopping-cart');
    };

    const goToFavourites = () => {
      router.push('/favourites');
    };

    const logout = async () => {
      await store.dispatch('auth/signOut');
      router.push('/');
    };

    return { goHome, goToShoppingCart, goToFavourites, logout };
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@600&display=swap');

.app-header {
  display: flex;
  align-items: center;
  background-color: #4caf50;
  padding: 10px 20px;
  height: 60px;
  justify-content: space-between;
}

.logo-container {
  width: 120px;
  height: 60px;
  overflow: hidden;
  margin-right: 15px;
}

.logo {
  width: 200%;
  height: 200%;
  object-fit: cover;
  object-position: center 25%;
  transform: translateX(-25%) translateY(-20%);
}

.title {
  font-family: 'Poppins', sans-serif;
  font-size: 28px;
  color: white;
  font-weight: 600;
  letter-spacing: 1px;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.1);
}

.button-container {
  display: flex;
  gap: 10px;
}

.icon-button {
  padding: 8px;
  background-color: #ffffff;
  color: #4caf50;
  border: none;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s, color 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
}

.icon-button:hover {
  background-color: #e8f5e9;
}

.icon-button svg {
  width: 24px;
  height: 24px;
}
</style>
