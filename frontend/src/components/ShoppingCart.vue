<script>
import { mapState, mapActions, mapGetters } from 'vuex';
import IngredientItem from '@/components/IngredientItem.vue';
import { ref } from 'vue';

export default {
  name: 'ShoppingCart',
  components: {
    IngredientItem
  },
  computed: {
    ...mapState('recipes', ['selectedRecipes', 'ingredients']),
    ...mapGetters('recipes', ['combinedIngredients']),
  },
  methods: {
    ...mapActions('recipes', ['removeSelectedRecipe', 'updateRecipeServings']),
    removeRecipe(recipeId) {
      this.removeSelectedRecipe(recipeId);
    },
    updateServings(recipeId, change) {
      const recipe = this.selectedRecipes.find(r => r.id === recipeId);
      if (recipe) {
        const newServings = Math.max(1, recipe.servings + change);
        this.updateRecipeServings({ recipeId, servings: newServings });
      }
    },
    handleIngredientBoughtToggle({ id, isBought }) {
      this.boughtIngredients[id] = isBought;
    },
    resetBoughtIngredients() {
      this.boughtIngredients = {};
    }
  },
  setup() {
    const boughtIngredients = ref({});

    return { boughtIngredients };
  },
  watch: {
    selectedRecipes: {
      handler() {
        this.resetBoughtIngredients();
      },
      deep: true
    },
    combinedIngredients: {
      handler() {
        this.resetBoughtIngredients();
      },
      deep: true
    }
  }
}
</script>

<template>
  <div class="shopping-cart">
    <h1>Shopping Cart</h1>
    <div v-if="selectedRecipes.length > 0">
      <div class="selected-recipes">
        <h2>Selected Recipes</h2>
        <div v-for="recipe in selectedRecipes" :key="recipe.id" class="recipe-item">
          <img :src="recipe.imagePath" :alt="recipe.name" class="recipe-image">
          <div class="recipe-details">
            <h3>
              <router-link :to="{ name: 'RecipePage', params: { id: recipe.id } }" class="recipe-link">
                {{ recipe.name }}
              </router-link>
            </h3>
            <div class="servings-control">
              <button @click="updateServings(recipe.id, -1)" :disabled="recipe.servings <= 1">-</button>
              <span>{{ recipe.servings }} {{ recipe.servings === 1 ? 'serving' : 'servings' }}</span>
              <button @click="updateServings(recipe.id, 1)">+</button>
            </div>
            <button @click="removeRecipe(recipe.id)" class="remove-button">Remove</button>
          </div>
        </div>
      </div>
      <div class="shopping-list">
        <h2>Shopping List</h2>
        <ul class="ingredients-grid">
          <IngredientItem
            v-for="ingredient in combinedIngredients"
            :key="`${ingredient.id}-${ingredient.unit}`"
            :ingredient="{ id: ingredient.id, amount: ingredient.amount, unit: ingredient.unit }"
            :ingredientDetails="{ name: ingredient.name, imagePath: ingredients[ingredient.id]?.imagePath }"
            :servings="1"
            @ingredientBoughtToggle="handleIngredientBoughtToggle"
          />
        </ul>
      </div>
    </div>
    <div v-else>
      <p>Your shopping cart is empty. Add some recipes to get started!</p>
    </div>
  </div>
</template>

<style scoped>
.shopping-cart {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

h1, h2 {
  color: #333;
}

h1 {
  font-size: 2.5em;
  margin-bottom: 20px;
}

h2 {
  font-size: 1.8em;
  margin-bottom: 15px;
}

.selected-recipes {
  margin-bottom: 40px;
}

.recipe-item {
  display: flex;
  align-items: center;
  background-color: #f8f8f8;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 10px;
}

.recipe-image {
  width: 100px;
  height: 100px;
  object-fit: cover;
  border-radius: 8px;
  margin-right: 20px;
}

.recipe-details {
  flex-grow: 1;
}

.recipe-details h3 {
  margin: 0 0 10px 0;
}

.remove-button {
  background-color: #ff4d4d;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
}

.shopping-list ul {
  list-style-type: none;
  padding: 0;
}

.shopping-list li {
  background-color: #f8f8f8;
  padding: 10px;
  margin-bottom: 5px;
  border-radius: 4px;
}

.servings-control {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.servings-control button {
  background-color: #4caf50;
  color: white;
  border: none;
  padding: 5px 10px;
  margin: 0 5px;
  cursor: pointer;
  font-size: 1.2em;
  border-radius: 4px;
}

.servings-control button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.servings-control span {
  font-weight: bold;
  margin: 0 10px;
}

.ingredients-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  padding: 0;
  list-style-type: none;
}

.recipe-link {
  color: #4caf50;
  text-decoration: none;
  font-weight: bold;
}

.recipe-link:hover {
  text-decoration: underline;
}
</style>
