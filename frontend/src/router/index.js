import { createRouter, createWebHistory } from 'vue-router';
import RecipeList from '../components/RecipeList.vue';
import RecipePage from '../components/RecipePage.vue';

const routes = [
  { path: '/', component: RecipeList },
  { path: '/:id', component: RecipePage },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
