import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_FRESCO_API_BASE_URL;

export const api = {
  async getIngredients(pageSize = 12, lastEvaluatedId = null) {
    let url = `${API_BASE_URL}/ingredients`;
    const params = new URLSearchParams();
    if (lastEvaluatedId) {
      params.append('lastEvaluatedId', lastEvaluatedId);
    }
    params.append('pageSize', pageSize); // Assuming a default page size
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

  async getRecipe(recipeId, userId) {
    const response = await axios.get(`${API_BASE_URL}/recipes/${recipeId}?userId=${userId}`);
    return response.data;
  },

  async getIngredient(ingredientId) {
    const response = await axios.get(`${API_BASE_URL}/ingredients/${ingredientId}`);
    return response.data;
  },

  async addFavourite(recipeId, userId) {
    try {
      const response = await axios.post(`${API_BASE_URL}/favourites`, { recipeId, userId });
      return { success: true, message: 'Favourite added successfully' };
    } catch (error) {
      console.error('Error adding favourite:', error);
      throw error;
    }
  },

  async removeFavourite(recipeId, userId) {
    try {
      await axios.delete(`${API_BASE_URL}/favourites?recipeId=${recipeId}&userId=${userId}`);
      return { success: true, message: 'Favourite removed successfully' };
    } catch (error) {
      console.error('Error removing favourite:', error);
      throw { success: false, message: 'Failed to remove favourite' };
    }
  },

  async getFavourites(userId, lastEvaluatedId = null, pageSize = 12) {
    let url = `${API_BASE_URL}/favourites`;
    const params = new URLSearchParams();
    if (lastEvaluatedId) {
      params.append('lastEvaluatedId', lastEvaluatedId);
    }
    params.append('pageSize', pageSize);
    params.append('userId', userId);
    url += `?${params.toString()}`;
    const response = await axios.get(url);
    return response.data;
  },
};
