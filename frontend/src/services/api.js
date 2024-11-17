import axios from 'axios';
import { auth } from '@/services/auth';

const API_BASE_URL = import.meta.env.VITE_FRESCO_API_BASE_URL;

const getAuthHeaders = () => {
  const session = auth.getSession();
  return {
    Authorization: session ? `Bearer ${session.idToken}` : '',
  };
};

export const api = {
  async getIngredients(pageSize = 12, lastEvaluatedId = null) {
    let url = `${API_BASE_URL}/ingredients`;
    const params = new URLSearchParams();
    if (lastEvaluatedId) {
      params.append('lastEvaluatedId', lastEvaluatedId);
    }
    params.append('pageSize', pageSize);
    url += `?${params.toString()}`;
    const response = await axios.get(url, { headers: getAuthHeaders() });
    return response.data;
  },

  async getRecipes(pageSize = 12) {
    let url = `${API_BASE_URL}/recipes`;
    const params = new URLSearchParams();
    params.append('pageSize', pageSize);
    url += `?${params.toString()}`;
    const response = await axios.get(url, { headers: getAuthHeaders() });
    return response.data;
  },

  async searchRecipes(searchTerm, pageSize = 12) {
    let url = `${API_BASE_URL}/recipes`;
    const params = new URLSearchParams();
    params.append('search', searchTerm); // Add the search term as a query parameter
    params.append('pageSize', pageSize); // Include page size for pagination
    url += `?${params.toString()}`;
    const response = await axios.get(url);
    return response.data; // Return the response data
  },

  async getRecipe(recipeId, userId) {
    const response = await axios.get(`${API_BASE_URL}/recipes/${recipeId}`, { headers: getAuthHeaders() });
    return response.data;
  },

  async getIngredient(ingredientId) {
    const response = await axios.get(`${API_BASE_URL}/ingredients/${ingredientId}`);
    return response.data;
  },

  async addFavourite(recipeId, userId) {
    try {
      const response = await axios.post(`${API_BASE_URL}/favourites`, { recipeId, userId }, { headers: getAuthHeaders() });
      return { success: true, message: 'Favourite added successfully' };
    } catch (error) {
      console.error('Error adding favourite:', error);
      throw error;
    }
  },

  async removeFavourite(recipeId, userId) {
    try {
      await axios.delete(`${API_BASE_URL}/favourites?recipeId=${recipeId}&userId=${userId}`, { headers: getAuthHeaders() });
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
    const response = await axios.get(url, { headers: getAuthHeaders() });
    return response.data;
  },
};
