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
    async signUp({ commit, dispatch }, { username, email, password }) {
      try {
        const result = await auth.signUp(username, email, password);
        if (!result.userConfirmed) {
          return { userConfirmed: false, username };
        }
        // If user is automatically confirmed, sign them in
        return dispatch('signIn', { username, password });
      } catch (error) {
        console.error('Sign up error:', error);
        throw error;
      }
    },
    async signIn({ commit }, { username, password }) {
      try {
        const user = await auth.signIn(username, password);
        commit('SET_USER', user);
        auth.setSession(user);
        return user;
      } catch (error) {
        console.error('Sign in error:', error);
        throw error;
      }
    },
    async signOut({ commit }) {
      auth.signOut();
      auth.clearSession();
      commit('SET_USER', null);
    },
    async getCurrentUser({ commit }) {
      try {
        const session = auth.getSession();
        if (session) {
          commit('SET_USER', session);
          return session;
        }
        const user = await auth.getCurrentUser();
        if (user) {
          commit('SET_USER', user);
          auth.setSession(user);
        }
        return user;
      } catch (error) {
        console.error('Get current user error:', error);
        commit('SET_USER', null);
        return null;
      }
    },
    async confirmSignUp({ commit }, { username, code }) {
      try {
        await auth.confirmSignUp(username, code);
        return true;
      } catch (error) {
        console.error('Confirm sign up error:', error);
        throw error;
      }
    },
  },
};

export default authModule;
