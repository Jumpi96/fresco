import { createRouter, createWebHistory } from 'vue-router';
import Home from '../components/Home.vue';
import RecipeList from '../components/RecipeList.vue';
import RecipePage from '../components/RecipePage.vue';
import ShoppingCart from '../components/ShoppingCart.vue';
import { store } from '../store';

export const routes = [
  { path: '/', name: 'Home', component: Home },
  { 
    path: '/recipes', 
    name: 'RecipeList', 
    component: RecipeList,
    meta: { requiresAuth: true }
  },
  { 
    path: '/recipes/:id', 
    name: 'RecipePage', 
    component: RecipePage,
    meta: { requiresAuth: true }
  },
  { 
    path: '/shopping-cart', 
    name: 'ShoppingCart', 
    component: ShoppingCart,
    meta: { requiresAuth: true }
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!store.state.auth.isAuthenticated) {
      next('/');
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
