<template>
  <header class="app-header">
    <div class="logo-container">
      <img src="@/assets/logo.jpg" alt="App Logo" class="logo">
    </div>
    <h1 class="title">fresco</h1>
  </header>
</template>

<script>
export default {
  name: 'AppHeader'
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
</style>
