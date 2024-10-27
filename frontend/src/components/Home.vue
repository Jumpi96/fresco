<template>
  <div class="home">
    <h1>Welcome to Fresco</h1>
    <div v-if="!isAuthenticated">
      <div class="auth-forms">
        <div class="login-form">
          <h2>Login</h2>
          <form @submit.prevent="login">
            <input v-model="loginForm.username" type="text" placeholder="Username" required>
            <input v-model="loginForm.password" type="password" placeholder="Password" required>
            <button type="submit">Login</button>
            <p v-if="loginError" class="error-message">{{ loginError }}</p>
          </form>
        </div>
        <div class="signup-form">
          <h2>Sign Up</h2>
          <form @submit.prevent="signup" v-if="!showConfirmation">
            <input v-model="signupForm.username" type="text" placeholder="Username" required>
            <input v-model="signupForm.email" type="email" placeholder="Email" required>
            <input v-model="signupForm.password" type="password" placeholder="Password" required>
            <button type="submit">Sign Up</button>
            <p v-if="signupError" class="error-message">{{ signupError }}</p>
          </form>
          <form @submit.prevent="confirmSignUp" v-else>
            <input v-model="confirmationCode" type="text" placeholder="Confirmation Code" required>
            <button type="submit">Confirm</button>
            <p v-if="confirmationError" class="error-message">{{ confirmationError }}</p>
          </form>
        </div>
      </div>
    </div>
    <div v-else>
      <p>Welcome back, {{ user.username }}!</p>
      <button @click="goToRecipes">View Recipes</button>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue';
import { useStore } from 'vuex';
import { useRouter } from 'vue-router';

export default {
  name: 'Home',
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

    const goToRecipes = () => {
      router.push('/recipes');
    };

    return {
      loginForm,
      signupForm,
      isAuthenticated,
      user,
      login,
      signup,
      goToRecipes,
      loginError,
      signupError,
      showConfirmation,
      confirmationCode,
      confirmationError,
      pendingUsername,
      confirmSignUp
    };
  }
};
</script>

<style scoped>
.home {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.auth-forms {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

.login-form, .signup-form {
  width: 45%;
}

form {
  display: flex;
  flex-direction: column;
}

input {
  margin-bottom: 10px;
  padding: 8px;
}

button {
  padding: 10px;
  background-color: #4caf50;
  color: white;
  border: none;
  cursor: pointer;
}

button:hover {
  background-color: #45a049;
}

.error-message {
  color: #f44336;
  margin-top: 10px;
  font-size: 0.9em;
}
</style>
