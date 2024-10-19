import { api } from '@/services/api';

const recipes = {
  namespaced: true,
  state: {
    recipes: [],
    currentRecipe: null,
    ingredients: {},
    lastEvaluatedId: null,
  },
  mutations: {
    SET_RECIPES(state, recipes) {
      state.recipes = [...state.recipes, ...recipes];
    },
    SET_CURRENT_RECIPE(state, recipe) {
      state.currentRecipe = recipe;
    },
    SET_INGREDIENTS(state, ingredients) {
      state.ingredients = { ...state.ingredients, ...ingredients };
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
    async fetchRecipe({ commit }, recipeId) {
      try {
        const recipe = await api.getRecipe(recipeId);
        commit('SET_CURRENT_RECIPE', recipe);
      } catch (error) {
        console.error(`Error fetching recipe ${recipeId}:`, error);
      }
    },
    async fetchIngredients({ commit }, ingredientIds) {
      try {
        const ingredients = {};
        await Promise.all(ingredientIds.map(async (id) => {
          const data = await api.getIngredient(id);
          ingredients[id] = {
            name: data.name,
            imagePath: data.imagePath || null
          };
        }));
        commit('SET_INGREDIENTS', ingredients);
      } catch (error) {
        console.error('Error fetching ingredients:', error);
      }
    },
  },
};

export default recipes;
