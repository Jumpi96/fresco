<template>
  <div class="card">
    <!-- Recipe Image -->
    <img :src="recipe.imagePath" alt="Recipe Image" class="recipe-image" />

    <div class="details">
      <!-- Recipe Title -->
      <h3>{{ recipe.name }}</h3>

      <!-- Recipe Info (Time, Protein, Kcal) -->
      <div class="info">
        <div class="info-item">
          <i class="fa fa-clock"></i>
          <span>{{ recipe.totalTime === "PT0S" ? "❓" : recipe.totalTime.substring(2).toLowerCase() }}</span>
        </div>

        <div class="info-item">
          <i class="fa fa-dumbbell"></i>
          <span>{{ recipe.macros.proteins }}g protein</span>
        </div>

        <div class="info-item">
          <i class="fa fa-fire"></i>
          <span>{{ calculateCalories(recipe) }} kcal</span>
        </div>
      </div>

      <!-- Recipe Link (Button) -->
      <router-link :to="`/${recipe.id}`" class="cook-button">Cook this recipe!</router-link>
    </div>
  </div>
</template>

<script>
import { calculateKcal } from '@/utils/nutritionCalculations';

export default {
  props: ['recipe'],
  methods: {
    calculateCalories(recipe) {
      return calculateKcal(recipe.macros.proteins, recipe.macros.carbs, recipe.macros.fats);
    }
  }
};
</script>

<style scoped>
.card {
  margin-top: 2rem;
  display: flex;
  flex-direction: column;
  position: relative;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.recipe-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.details {
  padding: 1rem;
  text-align: center;
}

h3 {
  font-size: 1.2rem;
  font-weight: 500;
  margin-bottom: 0.8rem;
  color: var(--color-heading);
}

.info {
  display: flex;
  justify-content: space-around;
  margin-bottom: 1rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.info-item i {
  font-size: 1rem;
}

.cook-button {
  display: inline-block;
  padding: 0.8rem 1.6rem;
  background-color: #4caf50;
  color: white;
  text-decoration: none;
  border-radius: 5px;
  font-weight: 600;
}

.cook-button:hover {
  background-color: #45a049;
}

@media (min-width: 1024px) {
  .card {
    flex-direction: row;
  }

  .recipe-image {
    width: 40%;
    height: 100%;
  }

  .details {
    text-align: left;
    padding: 2rem;
  }

  .info {
    justify-content: space-between;
  }
}
</style>
