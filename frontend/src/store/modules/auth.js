import { auth } from '@/services/auth';

const authModule = {
  namespaced: true,
  state: {
    user: null,
    isAuthenticated: false,
  },
  mutations: {
    SET_USER(state, user) {
      state.user = user;
      state.isAuthenticated = !!user;
    },
  },
  actions: {
    async signUp({ commit }, { username, email, password }) {
      try {
        await auth.signUp(username, email, password);
        // After sign up, the user still needs to sign in
        const user = await auth.signIn(username, password);
        commit('SET_USER', user);
      } catch (error) {
        console.error('Sign up error:', error);
        throw error;
      }
    },
    async signIn({ commit }, { username, password }) {
      try {
        const user = await auth.signIn(username, password);
        commit('SET_USER', user);
      } catch (error) {
        console.error('Sign in error:', error);
        throw error;
      }
    },
    async signOut({ commit }) {
      auth.signOut();
      commit('SET_USER', null);
    },
    async getCurrentUser({ commit }) {
      try {
        const user = await auth.getCurrentUser();
        commit('SET_USER', user);
      } catch (error) {
        console.error('Get current user error:', error);
        commit('SET_USER', null);
      }
    },
  },
};

export default authModule;

