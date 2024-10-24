import { describe, it, expect } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import { routes } from './index';

describe('Router', () => {
  it('should have the correct routes', () => {
    const router = createRouter({
      history: createWebHistory(),
      routes,
    });

    expect(router.getRoutes()).toHaveLength(3);

    const homeRoute = router.getRoutes().find(route => route.path === '/');
    expect(homeRoute).toBeDefined();
    expect(homeRoute.name).toBe('RecipeList');

    const recipeRoute = router.getRoutes().find(route => route.path === '/:id');
    expect(recipeRoute).toBeDefined();
    expect(recipeRoute.name).toBe('RecipePage');

    const shoppingCartRoute = router.getRoutes().find(route => route.path === '/shopping-cart');
    expect(shoppingCartRoute).toBeDefined();
    expect(shoppingCartRoute.name).toBe('ShoppingCart');
  });
});
