<template>
  <div>
    <div class="recipe-grid">
      <RecipeCard 
        v-for="recipe in recipes" 
        :key="recipe.id" 
        :recipe="recipe" />
    </div>
    <div class="recipe-grid-footer">
      <button @click="loadMoreRecipes" v-if="lastEvaluatedId" class="load-more-button">Load more...</button>
    </div>
  </div>
</template>

<script>
import RecipeCard from '@/components/RecipeCard.vue';

export default {
  components: {
    RecipeCard
  },
  data() {
    return {
      recipes: [],
      lastEvaluatedId: null
    };
  },
  methods: {
    fetchRecipes() {
      let url = 'http://127.0.0.1:8080/recipes?pageSize=12';
      if (this.lastEvaluatedId) {
        url += `&lastEvaluatedId=${this.lastEvaluatedId}`;
      }
      fetch(url)
        .then(response => response.json())
        .then(data => {
          this.recipes = [...this.recipes, ...data.recipes];
          this.lastEvaluatedId = data.lastEvaluatedId;
        })
        .catch(error => {
          console.error('Error fetching recipes:', error);
        });
    },
    loadMoreRecipes() {
      this.fetchRecipes();
    }
  },
  created() {
    this.fetchRecipes();
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