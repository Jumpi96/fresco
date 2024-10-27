import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_FRESCO_API_BASE_URL;

export const api = {
  async getIngredients(lastEvaluatedId = null) {
    let url = `${API_BASE_URL}/ingredients`;
    if (lastEvaluatedId) {
      params.append('lastEvaluatedId', lastEvaluatedId);
    }
    params.append('pageSize', pageSize);
    url += `?${params.toString()}`;
    const response = await axios.get(url);
    return response.data;
  },

  async getRecipes(lastEvaluatedId = null, pageSize = 12) {
    let url = `${API_BASE_URL}/recipes`;
    const params = new URLSearchParams();
    if (lastEvaluatedId) {
      params.append('lastEvaluatedId', lastEvaluatedId);
    }
    params.append('pageSize', pageSize);
    url += `?${params.toString()}`;
    const response = await axios.get(url);
    return response.data;
  },

  async getRecipe(recipeId) {
    const response = await axios.get(`${API_BASE_URL}/recipes/${recipeId}`);
    return response.data;
  },

  async getIngredient(ingredientId) {
    const response = await axios.get(`${API_BASE_URL}/ingredients/${ingredientId}`);
    return response.data;
  },
};
