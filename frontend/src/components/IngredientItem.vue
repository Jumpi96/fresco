<template>
  <li class="ingredient-item">
    <img v-if="ingredientDetails && ingredientDetails.imagePath" 
         :src="ingredientDetails.imagePath" 
         :alt="ingredientDetails.name" 
         class="ingredient-image">
    <div class="ingredient-details">
      <span class="ingredient-amount">
        <template v-if="amount !== 0">
          {{ adjustedAmount }}
        </template>
        {{ unit }}
      </span>
      <span class="ingredient-name">{{ ingredientDetails ? ingredientDetails.name : 'Loading...' }}</span>
    </div>
  </li>
</template>

<script>
export default {
  name: 'IngredientItem',
  props: {
    ingredient: {
      type: Object,
      required: true
    },
    ingredientDetails: {
      type: Object,
      default: null
    },
    servings: {
      type: Number,
      required: true
    }
  },
  computed: {
    amount() {
      return this.ingredient.amount;
    },
    unit() {
      return this.ingredient.unit;
    },
    adjustedAmount() {
      const adjusted = (this.amount * this.servings).toFixed(2);
      return parseFloat(adjusted).toString();
    }
  }
}
</script>

<style scoped>
.ingredient-item {
  display: flex;
  align-items: flex-start;
  background-color: #f8f8f8;
  padding: 10px;
  border-radius: 8px;
}

.ingredient-image {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 50%;
  margin-right: 10px;
}

.ingredient-details {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.ingredient-amount {
  font-weight: bold;
  color: #666;
}

.ingredient-name {
  font-weight: bold;
  margin-right: 10px;
}
</style>
