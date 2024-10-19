<template>
  <header class="app-header">
    <div class="logo-container">
      <img src="@/assets/logo.jpg" alt="App Logo" class="logo">
    </div>
    <h1 class="title">fresco</h1>
    <button @click="goHome" class="home-button" aria-label="Go to home page">
      <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
        <polyline points="9 22 9 12 15 12 15 22"></polyline>
      </svg>
    </button>
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

    return { goHome };
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@600&display=swap');

.app-header {
  display: flex;
  align-items: center;
  background-color: #4caf50; /* Light green color */
  padding: 10px 20px;
  height: 60px; /* Adjust as needed */
  justify-content: space-between; /* Add this line */
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

.home-button {
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

.home-button:hover {
  background-color: #e8f5e9;
}

.home-button svg {
  width: 24px;
  height: 24px;
}
</style>
