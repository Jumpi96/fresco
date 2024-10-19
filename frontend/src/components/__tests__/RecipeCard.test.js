import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import RecipeCard from '../RecipeCard.vue';
import { calculateKcal } from '@/utils/nutritionCalculations';

vi.mock('@/utils/nutritionCalculations', () => ({
  calculateKcal: vi.fn()
}));

describe('RecipeCard', () => {
  const mockRecipe = {
    id: 1,
    name: 'Test Recipe',
    imagePath: 'test-image.jpg',
    totalTime: 'PT30M',
    macros: {
      proteins: 20,
      carbs: 30,
      fats: 10
    }
  };

  it('renders recipe details correctly', () => {
    const wrapper = mount(RecipeCard, {
      props: {
        recipe: mockRecipe
      },
      global: {
        stubs: ['router-link']
      }
    });

    expect(wrapper.find('h3').text()).toBe('Test Recipe');
    expect(wrapper.find('.recipe-image').attributes('src')).toBe('test-image.jpg');
    expect(wrapper.find('.fa-clock').exists()).toBe(true);
    expect(wrapper.find('.fa-dumbbell').exists()).toBe(true);
    expect(wrapper.find('.fa-fire').exists()).toBe(true);
  });

  it('formats time correctly', () => {
    const wrapper = mount(RecipeCard, {
      props: {
        recipe: mockRecipe
      },
      global: {
        stubs: ['router-link']
      }
    });

    expect(wrapper.find('.fa-clock').element.nextSibling.textContent).toBe('30m');
  });

  it('displays unknown time when totalTime is PT0S', () => {
    const recipe = { ...mockRecipe, totalTime: 'PT0S' };
    const wrapper = mount(RecipeCard, {
      props: {
        recipe
      },
      global: {
        stubs: ['router-link']
      }
    });

    expect(wrapper.find('.fa-clock').element.nextSibling.textContent).toBe('❓');
  });

  it('calculates calories correctly', () => {
    calculateKcal.mockReturnValue(290);
    const wrapper = mount(RecipeCard, {
      props: {
        recipe: mockRecipe
      },
      global: {
        stubs: ['router-link']
      }
    });

    expect(wrapper.find('.fa-fire').element.nextSibling.textContent).toBe('290 kcal');
    expect(calculateKcal).toHaveBeenCalledWith(20, 30, 10);
  });
});
