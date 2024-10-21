import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import RecipePage from '../RecipePage.vue';
import { createStore } from 'vuex';
import { calculateKcal } from '@/utils/nutritionCalculations';

vi.mock('@/utils/nutritionCalculations', () => ({
  calculateKcal: vi.fn()
}));

vi.mock('vue-router', () => ({
    useRoute: vi.fn(() => ({
      params: {
        id: '1'
      }
    })),
    useRouter: vi.fn(() => ({
      push: vi.fn()
    }))
  }));

const mockStore = createStore({
  modules: {
    recipes: {
      namespaced: true,
      state: {
        currentRecipe: null,
        ingredients: {},
        selectedRecipes: []
      },
      actions: {
        fetchRecipe: vi.fn(),
        fetchIngredients: vi.fn(),
        addSelectedRecipe: vi.fn(),
        removeSelectedRecipe: vi.fn(),
        updateRecipeServings: vi.fn()
      }
    }
  }
});

describe('RecipePage', () => {
  const mockRecipe = {
    id: 1,
    name: 'Test Recipe',
    imagePath: 'test-image.jpg',
    websiteUrl: 'http://example.com',
    totalTime: 'PT30M',
    macros: {
      proteins: 20,
      carbs: 30,
      fats: 10
    },
    ingredients: [
      { id: 1, amount: 100, unit: 'g' },
      { id: 2, amount: 2, unit: 'tbsp' }
    ],
    steps: [
      { index: 1, instructionsHTML: '<p>Step 1</p>' },
      { index: 2, instructionsHTML: '<p>Step 2</p>' }
    ]
  };

  beforeEach(() => {
    mockStore.state.recipes.currentRecipe = mockRecipe;
    mockStore.state.recipes.ingredients = {
      1: { name: 'Ingredient 1', imagePath: 'ing1.jpg' },
      2: { name: 'Ingredient 2', imagePath: 'ing2.jpg' }
    };
    mockStore.state.recipes.selectedRecipes = [];
  });

  it('renders recipe details correctly', async () => {
    const wrapper = mount(RecipePage, {
      global: {
        plugins: [mockStore],
        stubs: ['router-link']
      }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.find('.recipe-title').text()).toBe('Test Recipe');
    expect(wrapper.find('.recipe-image').attributes('src')).toBe('test-image.jpg');
    expect(wrapper.find('.recipe-details-amount').text()).toBe('30 minutes');
    expect(wrapper.findAll('.macros span')).toHaveLength(8);
  });

  it('formats time correctly', () => {
    const wrapper = mount(RecipePage, {
      global: {
        plugins: [mockStore],
        stubs: ['router-link']
      }
    });

    expect(wrapper.vm.formatTime('PT45M')).toBe('45 minutes');
    expect(wrapper.vm.formatTime('PT0S')).toBe('🤷‍♂️');
  });

  it('calculates calories correctly', () => {
    calculateKcal.mockReturnValue(290);
    const wrapper = mount(RecipePage, {
      global: {
        plugins: [mockStore],
        stubs: ['router-link']
      }
    });

    expect(wrapper.vm.calculateCalories(mockRecipe)).toBe(290);
    expect(calculateKcal).toHaveBeenCalledWith(20, 30, 10);
  });

  it('renders ingredients correctly', async () => {
    const wrapper = mount(RecipePage, {
      global: {
        plugins: [mockStore],
        stubs: ['router-link']
      }
    });

    await wrapper.vm.$nextTick();

    const ingredients = wrapper.findAll('.ingredient-item');
    expect(ingredients).toHaveLength(2);
    expect(ingredients[0].find('.ingredient-name').text()).toBe('Ingredient 1');
    expect(ingredients[1].find('.ingredient-name').text()).toBe('Ingredient 2');
  });

  it('renders instructions correctly', async () => {
    const wrapper = mount(RecipePage, {
      global: {
        plugins: [mockStore],
        stubs: ['router-link']
      }
    });

    await wrapper.vm.$nextTick();

    const instructions = wrapper.findAll('.recipe-instructions ol li');
    expect(instructions).toHaveLength(2);
    expect(instructions[0].html()).toContain('<p>Step 1</p>');
    expect(instructions[1].html()).toContain('<p>Step 2</p>');
  });

  it('adjusts servings and updates ingredient amounts', async () => {
    const wrapper = mount(RecipePage, {
      global: {
        plugins: [mockStore],
        stubs: ['router-link']
      }
    });

    await wrapper.vm.$nextTick();

    // Check initial servings
    expect(wrapper.find('.servings-adjuster span').text()).toBe('1 serving');

    // Check initial ingredient amounts
    let ingredients = wrapper.findAll('.ingredient-item');
    expect(ingredients[0].find('.ingredient-amount').text()).toBe('100 g');
    expect(ingredients[1].find('.ingredient-amount').text()).toBe('2 tbsp');

    // Increase servings
    await wrapper.find('.servings-adjuster button:last-child').trigger('click');

    // Check updated servings
    expect(wrapper.find('.servings-adjuster span').text()).toBe('2 servings');

    // Check updated ingredient amounts
    ingredients = wrapper.findAll('.ingredient-item');
    expect(ingredients[0].find('.ingredient-amount').text()).toBe('200 g');
    expect(ingredients[1].find('.ingredient-amount').text()).toBe('4 tbsp');

    // Decrease servings
    await wrapper.find('.servings-adjuster button:first-child').trigger('click');

    // Check updated servings
    expect(wrapper.find('.servings-adjuster span').text()).toBe('1 serving');

    // Check updated ingredient amounts
    ingredients = wrapper.findAll('.ingredient-item');
    expect(ingredients[0].find('.ingredient-amount').text()).toBe('100 g');
    expect(ingredients[1].find('.ingredient-amount').text()).toBe('2 tbsp');

    // Try to decrease below 1 serving
    await wrapper.find('.servings-adjuster button:first-child').trigger('click');

    // Check that servings didn't go below 1
    expect(wrapper.find('.servings-adjuster span').text()).toBe('1 serving');
  });


  it('defaults to 1 serving when recipe is not selected', async () => {
    mockStore.state.recipes.selectedRecipes = [];
    
    const wrapper = mount(RecipePage, {
      global: {
        plugins: [mockStore],
        stubs: ['router-link']
      }
    });

    await wrapper.vm.$nextTick();

    expect(wrapper.find('.servings-adjuster span').text()).toBe('1 serving');
  });
});
