import { api } from '@/services/api';
import { auth } from '@/services/auth';

const recipes = {
  namespaced: true,
  state: {
    recipes: [],
    currentRecipe: null,
    ingredients: {},
    lastEvaluatedId: null,
    selectedRecipes: [],
    shoppedIngredients: [],
    favouriteRecipes: [],
    isCurrentRecipeFavourite: null,
  },
  mutations: {
    SET_RECIPES(state, recipes) {
      state.recipes = [...state.recipes, ...recipes];
    },
    REPLACE_RECIPES(state, recipes) {
      state.recipes = recipes;
    },
    SET_CURRENT_RECIPE(state, data) {
      state.currentRecipe = data.recipe;
      state.isCurrentRecipeFavourite = data.isFavourite;
    },
    SET_INGREDIENTS(state, ingredients) {
      state.ingredients = { ...state.ingredients, ...ingredients };
    },
    SET_LAST_EVALUATED_ID(state, id) {
      state.lastEvaluatedId = id;
    },
    ADD_SHOPPED_INGREDIENT(state, ingredientId) {
      if (!state.shoppedIngredients.includes(ingredientId)) {
        state.shoppedIngredients.push(ingredientId);
      }
    },
    REMOVE_SHOPPED_INGREDIENT(state, ingredientId) {
      if (state.shoppedIngredients.includes(ingredientId)) {
        state.shoppedIngredients = state.shoppedIngredients.filter(id => id !== ingredientId);
      }
    },
    ADD_SELECTED_RECIPE(state, recipe) {
      const existingIndex = state.selectedRecipes.findIndex(r => r.id === recipe.id);
      if (existingIndex !== -1) {
        state.selectedRecipes[existingIndex].servings = recipe.servings;
      } else {
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
    TOGGLE_FAVOURITE(state, added) {
      state.isCurrentRecipeFavourite = added;
    },
    SET_FAVOURITE_RECIPES(state, favourites) {
      state.favouriteRecipes = favourites;
    },
  },
  actions: {
    async fetchRecipes({ commit }) {
      try {
        const data = await api.getRecipes();
        commit('SET_RECIPES', data.recipes);
      } catch (error) {
        console.error('Failed to fetch recipes:', error);
      }
    },
    async fetchShoppingCart({ commit, dispatch }) {
      try {
        const cart = await api.getShoppingCart();

        if (cart.recipes && typeof cart.recipes === 'object') {
          const recipePromises = Object.keys(cart.recipes).map(async (recipeId) => {
            const recipeData = await api.getRecipe(recipeId);
            commit('ADD_SELECTED_RECIPE', { ...recipeData.recipe, servings: cart.recipes[recipeId] });
        
            await dispatch('fetchIngredients', recipeData.recipe.ingredients.map(ingredient => ingredient.id));    
          });
          
          cart.shoppedIngredients.map(async (ingredientId) => {
            commit('ADD_SHOPPED_INGREDIENT', ingredientId); 
          });

          await Promise.all(recipePromises);
        } else {
          console.warn('No recipes found in the cart:', cart);
        }
      } catch (error) {
        console.error('Failed to fetch shopping cart:', error);
      }
    },
    async fetchFavourites({ commit }) {
      try {
        const user = await auth.getCurrentUser();
        if (!user) {
          throw new Error('User not authenticated');
        }
        const data = await api.getFavourites(user.username);
        commit('SET_FAVOURITE_RECIPES', data.recipes);
      } catch (error) {
        console.error('Failed to fetch favourites:', error);
      }
    },
    async fetchRecipe({ commit }, recipeId) {
      try {
        const user = await auth.getCurrentUser();
        if (!user) {
          throw new Error('User not authenticated');
        }
        const data = await api.getRecipe(recipeId, user.username);
        commit('SET_CURRENT_RECIPE', data);
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
    addSelectedRecipe({ commit, dispatch }, recipe) {
      commit('ADD_SELECTED_RECIPE', recipe);
      dispatch('updateCart');
    },
    removeSelectedRecipe({ commit, dispatch }, recipeId) {
      commit('REMOVE_SELECTED_RECIPE', recipeId);
      dispatch('updateCart');
    },
    updateRecipeServings({ commit, dispatch }, { recipeId, servings }) {
      commit('UPDATE_RECIPE_SERVINGS', { recipeId, servings });
      dispatch('updateCart');
    },
    addShoppedIngredient({ commit, dispatch }, { ingredientId }) {
      commit('ADD_SHOPPED_INGREDIENT', ingredientId);
      dispatch('updateCart');
    },
    removeShoppedIngredient({ commit, dispatch }, { ingredientId }) {
      commit('REMOVE_SHOPPED_INGREDIENT', ingredientId);
      dispatch('updateCart');
    },
    async addFavourite({ commit }, recipe) {
      try {
        const user = await auth.getCurrentUser();
        if (!user) {
          throw new Error('User not authenticated');
        }
        const response = await api.addFavourite(recipe.id, user.username);
        if (response.success) {
          commit('TOGGLE_FAVOURITE', true);
        }
      } catch (error) {
        console.error('Error toggling favourite:', error);
      }
    },

    async removeFavourite({ commit }, recipe) {
      try {
        const user = await auth.getCurrentUser();
        if (!user) {
          throw new Error('User not authenticated');
        }
        const response = await api.removeFavourite(recipe.id, user.username);
        if (response.success) {
          commit('TOGGLE_FAVOURITE', false);
        }
      } catch (error) {
        console.error('Error toggling favourite:', error);
      }
    },
    async searchRecipes({ commit }, searchTerm) {
      try {
        const response = await api.searchRecipes(searchTerm);
        commit('REPLACE_RECIPES', response.recipes);
      } catch (error) {
        console.error("Error fetching recipes:", error);
      }
    },
    async updateShoppingCart({ state }) {
      try {
        const cartData = {
          recipes: {},
          shoppedIngredients: state.shoppedIngredients,
        };

        state.selectedRecipes.forEach(recipe => {
          cartData.recipes[recipe.id] = recipe.servings;
        });

        await api.updateShoppingCart(cartData);
      } catch (error) {
        console.error('Failed to update shopping cart:', error);
      }
    },
    updateCart({ dispatch }) {
      dispatch('updateShoppingCart');
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
