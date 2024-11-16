<template>
  <div>
    <div class="search-container">
      <input 
        type="text" 
        v-model="searchTerm" 
        placeholder="Search for recipes..." 
        class="search-input" 
      />
      <button @click="searchOrFetchRecipes" class="search-button">Search</button>
    </div>
    
    <div class="recipe-grid">
      <RecipeCard 
        v-for="recipe in recipes" 
        :key="recipe.id" 
        :recipe="recipe" 
      />
    </div>
    
    <div class="recipe-grid-footer">
      <button @click="loadMore" class="load-more-button">Load more...</button>
    </div>
  </div>
</template>

<script>
import RecipeCard from '@/components/RecipeCard.vue';
import { mapActions, mapState } from 'vuex';

export default {
  name: 'RecipeList',
  components: {
    RecipeCard
  },
  data() {
    return {
      searchTerm: '' // Data property to hold the search term
    };
  },
  computed: {
    ...mapState('recipes', ['recipes']),
  },
  methods: {
    ...mapActions('recipes', ['fetchRecipes', 'searchRecipes']), // Assuming you have a searchRecipes action
    loadMore() {
      this.fetchRecipes();
    },
    searchOrFetchRecipes() {
      if (this.searchTerm.trim()) {
        this.searchRecipes(this.searchTerm); // Call the search action with the search term
      } else {
        this.fetchRecipes(); // If search term is empty, fetch all recipes
      }
    }
  },
  created() {
    if (this.recipes.length === 0) {
      this.fetchRecipes();
    }
  }
};
</script>

<style scoped>
.search-container {
  display: flex;
  justify-content: center;
  margin: 1rem 0; /* Adjust margin for better spacing */
  background-color: #f9f9f9; /* Light background for contrast */
  padding: 1rem; /* Add padding for better spacing */
  border-radius: 8px; /* Rounded corners */
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); /* Subtle shadow for depth */
}

.search-input {
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 5px;
  margin-right: 0.5rem;
  flex: 1; /* Allow input to grow */
}

.search-button {
  padding: 0.5rem 1rem;
  background-color: #4caf50; /* Green background */
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-weight: bold; /* Bold text for emphasis */
}

.search-button:hover {
  background-color: #45a049; /* Darker green on hover */
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.load-more-button {
  margin: 2rem;
  padding: 0.8rem 1.6rem;
  background-color: #4caf50; /* Green background */
  color: white;
  text-decoration: none;
  border-radius: 5px;
  font-weight: 600;
}

.recipe-grid-footer {
  display: flex;
  justify-content: center;
}
</style>
