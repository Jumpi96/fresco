<script>
import { useRoute, useRouter } from 'vue-router';
import { mapState, mapActions } from 'vuex';
import { calculateKcal } from '@/utils/nutritionCalculations';

export default {
  name: 'RecipePage',
  setup() {
    const route = useRoute();
    const router = useRouter();

    const recipeId = route.params.id;

    const goBack = () => {
      router.push('/');
    };

    return { recipeId, goBack };
  },
  computed: {
    ...mapState('recipes', ['currentRecipe', 'ingredients']),
    recipe() {
      return this.currentRecipe;
    }
  },
  methods: {
    ...mapActions('recipes', ['fetchRecipe', 'fetchIngredients']),
    formatTime(time) {
      if (time === 'PT0S') {
        return '🤷‍♂️';
      }
      const minutes = time.replace('PT', '').replace('M', '');
      return `${minutes} minutes`;
    },
    calculateCalories(recipe) {
      return calculateKcal(recipe.macros.proteins, recipe.macros.carbs, recipe.macros.fats);
    }
  },
  created() {
    this.fetchRecipe(this.recipeId).then(() => {
      if (this.recipe) {
        this.fetchIngredients(this.recipe.ingredients.map(ing => ing.id));
      }
    });
  }
}
</script>

<template>
  <div class="recipe-page">
    <button @click="goBack" class="back-button">Back to List</button>
    <div v-if="recipe">
      <a :href="recipe.websiteUrl" class="recipe-title">{{ recipe.name }}</a>
      <img :src="recipe.imagePath" :alt="recipe.name" class="recipe-image">
      <div class="recipe-details">
        <p><strong class="recipe-details-label">Preparation Time:</strong> <span class="recipe-details-amount">{{ formatTime(recipe.totalTime) }}</span></p>
        <div class="macros">
          <span><strong class="recipe-details-label">Carbs:</strong> <span class="recipe-details-amount">{{ recipe.macros.carbs }}g</span></span>
          <span><strong class="recipe-details-label">Fats:</strong> <span class="recipe-details-amount">{{ recipe.macros.fats }}g</span></span>
          <span><strong class="recipe-details-label">Proteins:</strong> <span class="recipe-details-amount">{{ recipe.macros.proteins }}g</span></span>
          <span><strong class="recipe-details-label">Calories:</strong> <span class="recipe-details-amount">{{ calculateCalories(recipe) }} kcal</span></span>
        </div>
      </div>
      <div class="recipe-ingredients">
        <h2>Ingredients</h2>
        <ul class="ingredients-grid">
          <li v-for="ingredient in recipe.ingredients" :key="ingredient.id" class="ingredient-item">
            <img v-if="ingredients[ingredient.id] && ingredients[ingredient.id].imagePath" 
                 :src="ingredients[ingredient.id].imagePath" 
                 :alt="ingredients[ingredient.id].name" 
                 class="ingredient-image">
            <div class="ingredient-details">
              <span class="ingredient-amount">
                <template v-if="ingredient.amount !== 0">{{ ingredient.amount }} </template>
                {{ ingredient.unit }}
              </span>
              <span class="ingredient-name">{{ ingredients[ingredient.id] ? ingredients[ingredient.id].name : 'Loading...' }}</span>
            </div>
          </li>
        </ul>
      </div>
      <div class="recipe-instructions">
        <h2>Instructions</h2>
        <ol>
          <li v-for="step in recipe.steps" :key="step.index" v-html="step.instructionsHTML"></li>
        </ol>
      </div>
    </div>
    <div v-else>Loading recipe...</div>
  </div>
</template>

<style scoped>
.recipe-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

.recipe-title {
  font-size: 2.5em;
  color: #333;
  margin-bottom: 20px;
  text-align: center;
}

.recipe-image {
  width: 100%;
  height: auto;
  border-radius: 8px;
  margin-bottom: 20px;
}

.ingredient-image {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 50%;
  margin-right: 10px;
}

.recipe-details {
  background-color: #f8f8f8;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.ingredient-name {
  font-weight: bold;
  margin-right: 10px;
}

.ingredient-details {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ingredients-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  padding: 0;
  list-style-type: none;
}

.ingredient-item {
  display: flex;
  align-items: flex-start;
  background-color: #f8f8f8;
  padding: 10px;
  border-radius: 8px;
}

.ingredient-amount {
  font-weight: bold;
  color: #666;
}

.macros {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.macros span {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.recipe-details-label {
  font-weight: bold;
}

.recipe-details-amount {
  font-weight: bold;
  color: #666;
}

.recipe-ingredients, .recipe-instructions {
  margin-bottom: 30px;
}

h2 {
  font-size: 1.8em;
  color: #444;
  margin-bottom: 15px;
  border-bottom: 2px solid #ddd;
  padding-bottom: 5px;
}

ul, ol {
  padding-left: 20px;
}

li {
  margin-bottom: 10px;
  line-height: 1.6;
}

.recipe-instructions ol li {
  margin-bottom: 20px;
}

.recipe-instructions ol li p {
  margin-top: 5px;
  font-style: italic;
  color: #666;
}

.hf-button {
  display: inline-block;
  padding: 0.8rem 1.6rem;
  background-color: #4caf50;
  color: white;
  text-decoration: none;
  border-radius: 5px;
  font-weight: 600;
}

.back-button {
  margin-bottom: 20px;
  padding: 10px 20px;
  background-color: #f0f0f0;
  border: none;
  border-radius: 5px;
  cursor: pointer;
}
</style>
