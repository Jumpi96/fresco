import { createRouter, createWebHistory } from 'vue-router';
import RecipeList from '../components/RecipeList.vue';
import RecipePage from '../components/RecipePage.vue';

export const routes = [
  { path: '/', name: 'RecipeList', component: RecipeList },
  { path: '/:id', name: 'RecipePage', component: RecipePage },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
