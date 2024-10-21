import { api } from '@/services/api';

const recipes = {
  namespaced: true,
  state: {
    recipes: [],
    currentRecipe: null,
    ingredients: {},
    lastEvaluatedId: null,
    selectedRecipes: [],
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
    ADD_SELECTED_RECIPE(state, recipe) {
      const existingIndex = state.selectedRecipes.findIndex(r => r.id === recipe.id);
      if (existingIndex !== -1) {
        // If the recipe already exists, update its servings
        state.selectedRecipes[existingIndex].servings = recipe.servings;
      } else {
        // If it's a new recipe, add it to the list
        state.selectedRecipes.push({ ...recipe, servings: recipe.servings || 1 });
      }
    },
    REMOVE_SELECTED_RECIPE(state, recipeId) {
      state.selectedRecipes = state.selectedRecipes.filter(r => r.id !== recipeId);
    },
    UPDATE_RECIPE_SERVINGS(state, { recipeId, servings }) {
      const recipe = state.selectedRecipes.find(r => r.id === recipeId);
      if (recipe) {
        recipe.servings = servings;
      }
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
    addSelectedRecipe({ commit }, recipe) {
      commit('ADD_SELECTED_RECIPE', recipe);
    },
    removeSelectedRecipe({ commit }, recipeId) {
      commit('REMOVE_SELECTED_RECIPE', recipeId);
    },
    updateRecipeServings({ commit }, { recipeId, servings }) {
      commit('UPDATE_RECIPE_SERVINGS', { recipeId, servings });
    },
  },
  getters: {
    combinedIngredients: (state) => {
      const combined = {};
      state.selectedRecipes.forEach(recipe => {
        recipe.ingredients.forEach(ingredient => {
          const key = `${ingredient.id}-${ingredient.unit}`;
          if (combined[key]) {
            combined[key].amount += ingredient.amount * recipe.servings;
          } else {
            combined[key] = {
              id: ingredient.id,
              amount: ingredient.amount * recipe.servings,
              unit: ingredient.unit,
              name: state.ingredients[ingredient.id]?.name || 'Unknown Ingredient'
            };
          }
        });
      });
      return Object.values(combined);
    }
  }
};

export default recipes;
