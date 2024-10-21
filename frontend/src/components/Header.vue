<template>
  <header class="app-header">
    <div class="logo-container">
      <img src="@/assets/logo.jpg" alt="App Logo" class="logo">
    </div>
    <h1 class="title">fresco</h1>
    <div class="button-container">
      <button @click="goToShoppingCart" class="icon-button" aria-label="Go to shopping cart">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="9" cy="21" r="1"></circle>
          <circle cx="20" cy="21" r="1"></circle>
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
        </svg>
      </button>
      <button @click="goHome" class="icon-button" aria-label="Go to home page">
        <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
          <polyline points="9 22 9 12 15 12 15 22"></polyline>
        </svg>
      </button>
    </div>
  </header>
</template>

<script>
import { useRouter } from 'vue-router';

export default {
  name: 'AppHeader',
  setup() {
    const router = useRouter();

    const goHome = () => {
      router.push('/');
    };

    const goToShoppingCart = () => {
      router.push('/shopping-cart');
    };

    return { goHome, goToShoppingCart };
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
