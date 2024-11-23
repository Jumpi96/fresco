<script>
import { useRoute } from 'vue-router';
import { mapState, mapActions } from 'vuex';
import { calculateKcal } from '@/utils/nutritionCalculations';
import { ref, computed, watch } from 'vue';
import IngredientItem from '@/components/IngredientItem.vue';

export default {
  name: 'RecipePage',
  components: {
    IngredientItem
  },
  setup() {
    const route = useRoute();
    const recipeId = route.params.id;
    const servings = ref(1);
    const boughtIngredients = ref({});

    return { recipeId, servings, boughtIngredients };
  },
  computed: {
    ...mapState('recipes', ['currentRecipe', 'ingredients', 'selectedRecipes', 'isCurrentRecipeFavourite']),
    recipe() {
      return this.currentRecipe;
    },
    isSelected() {
      return this.selectedRecipes.some(r => r.id === this.recipe.id);
    },
    isFavourite() {
      return this.isCurrentRecipeFavourite;
    },
    selectedRecipeServings() {
      const selectedRecipe = this.selectedRecipes.find(r => r.id === this.recipe.id);
      return selectedRecipe ? selectedRecipe.servings : 1;
    }
  },
  methods: {
    ...mapActions('recipes', ['fetchRecipe', 'fetchIngredients', 'addSelectedRecipe', 'removeSelectedRecipe', 'updateRecipeServings', 'addFavourite', 'removeFavourite']),
    formatTime(time) {
      if (time === 'PT0S') {
        return '🤷‍♂️';
      }
      const minutes = time.replace('PT', '').replace('M', '');
      return `${minutes} minutes`;
    },
    calculateCalories(recipe) {
      return calculateKcal(recipe.macros.proteins, recipe.macros.carbs, recipe.macros.fats);
    },
    adjustServings(change) {
      this.servings = Math.max(1, this.servings + change);
      if (this.isSelected) {
        this.updateRecipeServings({ recipeId: this.recipe.id, servings: this.servings });
      }
    },
    calculateAdjustedAmount(amount) {
      const adjusted = (amount * this.servings).toFixed(2);
      return parseFloat(adjusted).toString();
    },
    toggleSelectedRecipe() {
      if (this.isSelected) {
        this.removeSelectedRecipe(this.recipe.id);
      } else {
        this.addSelectedRecipe({ ...this.recipe, servings: this.servings });
      }
    },
    toggleFavourite() {
      if (this.isFavourite) {
        this.removeFavourite(this.recipe);
      } else {
        this.addFavourite(this.recipe);
      }
    },
    handleIngredientBoughtToggle({ id, isBought }) {
      this.boughtIngredients[id] = isBought;
    },
    resetBoughtIngredients() {
      this.boughtIngredients = {};
    }
  },
  created() {
    this.fetchRecipe(this.recipeId).then(() => {
      if (this.recipe) {
        this.fetchIngredients(this.recipe.ingredients.map(ing => ing.id));
        if (this.isSelected) {
          this.servings = this.selectedRecipeServings;
        }
      }
    });
  }
}
</script>

<template>
  <div class="recipe-page">
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
      <div class="recipe-controls">
        
      </div>
      <div class="recipe-ingredients">
        <div class="ingredients-header">
          <h2>Ingredients</h2>
          <div class="recipe-controls">
            <div class="servings-adjuster">
              <button @click="adjustServings(-1)" :disabled="servings <= 1">-</button>
              <span>{{ servings }} {{ servings === 1 ? 'serving' : 'servings' }}</span>
              <button @click="adjustServings(1)">+</button>
            </div>
            <button @click="toggleSelectedRecipe" class="select-button" :class="{ 'selected': isSelected }">
              {{ isSelected ? '✓' : '+' }}
            </button>
            <button @click="toggleFavourite" class="select-button" :class="{ 'favourited': isFavourite }">
              <span v-if="isFavourite">❤️</span>
              <span v-else>🤍</span>
            </button>
          </div>
        </div>
        <ul class="ingredients-grid">
          <IngredientItem
            v-for="ingredient in recipe.ingredients"
            :key="ingredient.id"
            :ingredient="ingredient"
            :ingredientDetails="ingredients[ingredient.id]"
            :servings="servings"
            :showCheckbox="false"
            @ingredientBoughtToggle="handleIngredientBoughtToggle"
          />
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

.ingredients-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.servings-adjuster {
  display: flex;
  align-items: center;
}

.servings-adjuster button {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 5px 10px;
  margin: 0 5px;
  cursor: pointer;
  font-size: 1.2em;
  border-radius: 4px;
}

.servings-adjuster button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.servings-adjuster span {
  font-weight: bold;
  margin: 0 10px;
}

.recipe-controls {
  display: flex;
  align-items: center;
}

.select-button {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 5px 10px;
  margin-left: 10px;
  cursor: pointer;
  font-size: 1.2em;
  border-radius: 4px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.select-button.selected {
  background-color: #45a049;
}

.select-button:hover {
  background-color: #45a049;
}

.favourite-button {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 5px 10px;
  margin-left: 10px;
  cursor: pointer;
  font-size: 1.2em;
  border-radius: 4px;
  width: 100px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.favourite-button.favourited {
  background-color: #45a049;
}

.favourite-button:hover {
  background-color: #45a049;
}
</style>
