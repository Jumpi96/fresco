import { api } from '@/services/api';

const recipes = {
  namespaced: true,
  state: {
    recipes: [],
    lastEvaluatedId: null,
  },
  mutations: {
    SET_RECIPES(state, recipes) {
      state.recipes = [...state.recipes, ...recipes];
    },
    SET_LAST_EVALUATED_ID(state, id) {
      state.lastEvaluatedId = id;
    },
  },
  actions: {
    async fetchRecipes({ commit }, lastEvaluatedId = null) {
      try {
        const data = await api.getRecipes(lastEvaluatedId);
        commit('SET_RECIPES', data.recipes);
        commit('SET_LAST_EVALUATED_ID', data.lastEvaluatedId);
      } catch (error) {
        console.error('Failed to fetch recipes:', error);
      }
    },
  },
};

export default recipes;
