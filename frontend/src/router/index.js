import { createRouter, createWebHistory } from 'vue-router';
import RecipeList from '../components/RecipeList.vue';
import RecipePage from '../components/RecipePage.vue';
import ShoppingCart from '../components/ShoppingCart.vue';

export const routes = [
  { path: '/', name: 'RecipeList', component: RecipeList },
  { path: '/:id', name: 'RecipePage', component: RecipePage },
  { path: '/shopping-cart', name: 'ShoppingCart', component: ShoppingCart },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
