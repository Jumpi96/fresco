<template>
  <div>
    <div class="recipe-grid">
      <RecipeCard 
        v-for="recipe in recipes" 
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
  name: 'RecipeList',
  components: {
    RecipeCard
  },
  computed: {
    ...mapState('recipes', ['recipes', 'lastEvaluatedId']),
  },
  methods: {
    ...mapActions('recipes', ['fetchRecipes']),
    loadMore() {
      this.fetchRecipes(this.lastEvaluatedId);
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
