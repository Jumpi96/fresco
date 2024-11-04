<template>
  <div>
    <h2 class="favourites-title">Your Favourite Recipes</h2>
    <div class="recipe-grid">
      <RecipeCard 
        v-for="recipe in favouriteRecipes" 
        :key="recipe.id" 
        :recipe="recipe" />
    </div>
    <div class="recipe-grid-footer">
      <button @click="loadMore" v-if="lastEvaluatedId" class="load-more-button">Load more...</button>
    </div>
  </div>
</template>

<script>
import RecipeCard from '@/components/RecipeCard.vue';
import { mapActions, mapState } from 'vuex';

export default {
  name: 'FavouriteRecipes',
  components: {
    RecipeCard
  },
  computed: {
    ...mapState('recipes', ['favouriteRecipes', 'lastEvaluatedId']),
  },
  methods: {
    ...mapActions('recipes', ['fetchFavourites']),
    loadMore() {
      this.fetchFavourites(this.lastEvaluatedId);
    }
  },
  created() {
    if (this.favouriteRecipes.length === 0) {
      this.fetchFavourites(); // Fetch favourites when the component is created
    }
  }
};
</script>

<style scoped>
.favourites-title {
  font-size: 1.2rem;
  font-weight: 500;
  margin-bottom: 0.8rem;
  color: var(--color-heading);
  text-align: center;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
}

.load-more-button {
  margin: 2rem;
  padding: 0.8rem 1.6rem;
  background-color: #4caf50;
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
