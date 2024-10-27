<template>
  <div class="home">
    <AppHeader :isAuthenticated="isAuthenticated" />
    <div class="home-content">
      <div v-if="!isAuthenticated" class="auth-container">
        <div class="auth-form">
          <h2>Login</h2>
          <form @submit.prevent="login">
            <input v-model="loginForm.username" type="text" placeholder="Username" required>
            <input v-model="loginForm.password" type="password" placeholder="Password" required>
            <button type="submit" class="submit-button">Login</button>
            <p v-if="loginError" class="error-message">{{ loginError }}</p>
          </form>
        </div>
        <div class="auth-form">
          <h2>Sign Up</h2>
          <form @submit.prevent="signup" v-if="!showConfirmation">
            <input v-model="signupForm.username" type="text" placeholder="Username" required>
            <input v-model="signupForm.email" type="email" placeholder="Email" required>
            <input v-model="signupForm.password" type="password" placeholder="Password" required>
            <button type="submit" class="submit-button">Sign Up</button>
            <p v-if="signupError" class="error-message">{{ signupError }}</p>
          </form>
          <form @submit.prevent="confirmSignUp" v-else>
            <input v-model="confirmationCode" type="text" placeholder="Confirmation Code" required>
            <button type="submit" class="submit-button">Confirm</button>
            <p v-if="confirmationError" class="error-message">{{ confirmationError }}</p>
          </form>
        </div>
      </div>
      <div v-else class="welcome-container">
        <p class="welcome-message">Welcome back, <span class="username">{{ user.email }}</span>!</p>
        <button @click="goToRecipes" class="view-recipes-button">View Recipes</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';
import AppHeader from '@/components/Header.vue';

export default {
  name: 'Home',
  components: {
    AppHeader
  },
  setup() {
    const store = useStore();
    const router = useRouter();

    const loginForm = ref({ username: '', password: '' });
    const signupForm = ref({ username: '', email: '', password: '' });
    const loginError = ref('');
    const signupError = ref('');
    const showConfirmation = ref(false);
    const confirmationCode = ref('');
    const confirmationError = ref('');
    const pendingUsername = ref('');

    const isAuthenticated = computed(() => store.state.auth.isAuthenticated);
    const user = computed(() => store.state.auth.user);

    const login = async () => {
      try {
        loginError.value = ''; // Clear previous error
        await store.dispatch('auth/signIn', loginForm.value);
        router.push('/recipes');
      } catch (error) {
        console.error('Login failed:', error);
        loginError.value = handleAuthError(error);
        if (error.code === 'UserNotConfirmedException') {
          showConfirmation.value = true;
          pendingUsername.value = loginForm.value.username;
        }
      }
    };

    const signup = async () => {
      try {
        signupError.value = '';
        const result = await store.dispatch('auth/signUp', signupForm.value);
        showConfirmation.value = true;
        pendingUsername.value = signupForm.value.username;
      } catch (error) {
        console.error('Signup failed:', error);
        signupError.value = handleAuthError(error);
      }
    };

    const confirmSignUp = async () => {
      try {
        confirmationError.value = '';
        await store.dispatch('auth/confirmSignUp', {
          username: pendingUsername.value,
          code: confirmationCode.value
        });
        // After confirmation, sign in the user
        await store.dispatch('auth/signIn', {
          username: pendingUsername.value,
          password: signupForm.value.password
        });
        router.push('/recipes');
      } catch (error) {
        console.error('Confirmation failed:', error);
        confirmationError.value = handleAuthError(error);
      }
    };

    const handleAuthError = (error) => {
      if (error.code === 'UserNotFoundException') {
        return 'User not found. Please check your username.';
      } else if (error.code === 'NotAuthorizedException') {
        return 'Incorrect username or password.';
      } else if (error.code === 'UsernameExistsException') {
        return 'Username already exists. Please choose a different one.';
      } else if (error.code === 'InvalidPasswordException') {
        return 'Password does not meet the requirements. Please try a stronger password.';
      } else {
        return 'An error occurred. Please try again later.';
      }
    };

    onMounted(async () => {
      if (!isAuthenticated.value) {
        await store.dispatch('auth/getCurrentUser');
      }
    });
  }
};
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;600&display=swap');

.home {
  font-family: 'Poppins', Arial, sans-serif;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.home-content {
  background-color: #f8f8f8;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.page-title {
  font-size: 2.5em;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
}

.auth-container {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

.auth-form {
  width: 45%;
  background-color: #ffffff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

h2 {
  font-size: 1.8em;
  color: #444;
  margin-bottom: 15px;
  border-bottom: 2px solid #ddd;
  padding-bottom: 5px;
}

form {
  display: flex;
  flex-direction: column;
}

input {
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1em;
}

.submit-button {
  padding: 12px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1em;
  font-weight: 600;
  transition: background-color 0.3s;
}

.submit-button:hover {
  background-color: #45a049;
}

.error-message {
  color: #f44336;
  margin-top: 10px;
  font-size: 0.9em;
}

.welcome-container {
  text-align: center;
}

.welcome-message {
  font-size: 1.5em;
  margin-bottom: 20px;
}

.username {
  font-weight: 600;
  color: #4caf50;
}

.view-recipes-button {
  display: inline-block;
  padding: 12px 24px;
  background-color: #4caf50;
  color: white;
  text-decoration: none;
  border-radius: 5px;
  font-weight: 600;
  font-size: 1em;
  transition: background-color 0.3s;
  border: none;
  cursor: pointer;
}

.view-recipes-button:hover {
  background-color: #45a049;
}
</style>

