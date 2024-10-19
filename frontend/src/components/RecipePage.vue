<script>
import { useRoute, useRouter } from 'vue-router';
import { calculateKcal } from '@/utils/nutritionCalculations';
import { api } from '@/services/api';

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
  data() {
    return {
      ingredients: {},
      recipe: null
    }
  },
  methods: {
    formatTime(time) {
      // Convert PT15M to 15 minutes
      const minutes = time.replace('PT', '').replace('M', '');
      return `${minutes} minutes`;
    },
    calculateCalories(recipe) {
      return calculateKcal(recipe.macros.proteins, recipe.macros.carbs, recipe.macros.fats);
    },
    async fetchIngredients() {
      if (this.recipe && this.recipe.ingredients) {
        this.recipe.ingredients.forEach(ingredient => {
          api.getIngredient(ingredient.id)
            .then(data => {
              this.ingredients[ingredient.id] = {
                name: data.name,
                imagePath: data.imagePath || null
              };
            })
            .catch(error => {
              console.error(`Error fetching ingredient ${ingredient.id}:`, error);
            });
        });
      }
    }
  },
  created() {
    api.getRecipe(this.recipeId)
      .then(data => {
        this.recipe = data;
        this.fetchIngredients();
      })
      .catch(error => {
        console.error(`Error fetching recipe ${this.recipeId}:`, error);
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
        <p><strong>Preparation Time:</strong> {{ formatTime(recipe.totalTime) }}</p>
        <div class="macros">
          <span><strong>Carbs:</strong> {{ recipe.macros.carbs }}g</span>
          <span><strong>Fats:</strong> {{ recipe.macros.fats }}g</span>
          <span><strong>Proteins:</strong> {{ recipe.macros.proteins }}g</span>
          <span><strong>Calories:</strong> {{ calculateCalories(recipe) }} kcal</span>
        </div>
      </div>
      <div class="recipe-ingredients">
        <h2>Ingredients</h2>
        <ul>
          <li v-for="ingredient in recipe.ingredients" :key="ingredient.id" class="ingredient-item">
            <img v-if="ingredients[ingredient.id] && ingredients[ingredient.id].imagePath" 
                 :src="ingredients[ingredient.id].imagePath" 
                 :alt="ingredients[ingredient.id].name" 
                 class="ingredient-image">
            <div class="ingredient-details">
              <span class="ingredient-amount">{{ ingredient.amount }} {{ ingredient.unit }}</span>
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
  margin-right: 5px;
  vertical-align: middle;
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
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 10px;
}

.ingredient-details > * {
  margin-bottom: 5px;
}

.ingredient-details > *:last-child {
  margin-bottom: 0;
}

.ingredient-amount {
  color: #666;
  font-style: italic;
}

.macros {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
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
