import { describe, it, expect, vi } from 'vitest';
import { mount } from '@vue/test-utils';
import RecipeList from '../RecipeList.vue';
import RecipeCard from '../RecipeCard.vue';
import { createStore } from 'vuex';

// Mock the Vuex store
const createVuexStore = () => {
  return createStore({
    modules: {
      recipes: {
        namespaced: true,
        state: {
          recipes: [],
          lastEvaluatedId: null,
        },
        actions: {
          fetchRecipes: vi.fn(),
        },
      },
    },
  });
};

describe('RecipeList', () => {
  it('renders correctly with no recipes', () => {
    const store = createVuexStore();
    const wrapper = mount(RecipeList, {
      global: {
        plugins: [store],
        stubs: {
          RecipeCard: true,
        },
      },
    });

    expect(wrapper.findComponent(RecipeCard).exists()).toBe(false);
    expect(wrapper.find('.load-more-button').exists()).toBe(false);
  });

  it('renders recipes and load more button', async () => {
    const store = createVuexStore();
    store.state.recipes.recipes = [
      { id: 1, name: 'Recipe 1' },
      { id: 2, name: 'Recipe 2' },
    ];
    store.state.recipes.lastEvaluatedId = 'lastId';

    const wrapper = mount(RecipeList, {
      global: {
        plugins: [store],
        stubs: {
          RecipeCard: true,
        },
      },
    });

    expect(wrapper.findAllComponents(RecipeCard)).toHaveLength(2);
    expect(wrapper.find('.load-more-button').exists()).toBe(true);
  });

  it('calls fetchRecipes on created if recipes are empty', async () => {
    const store = createVuexStore();
    const fetchRecipesSpy = vi.spyOn(store._actions['recipes/fetchRecipes'], '0');
  
    mount(RecipeList, {
      global: {
        plugins: [store],
        stubs: {
          RecipeCard: true,
        },
      },
    });
  
    expect(fetchRecipesSpy).toHaveBeenCalled();
  });

  it('calls loadMore method when load more button is clicked', async () => {
    const store = createVuexStore();
    store.state.recipes.recipes = [{ id: 1, name: 'Recipe 1' }];
    store.state.recipes.lastEvaluatedId = 'lastId';

    const wrapper = mount(RecipeList, {
      global: {
        plugins: [store],
        stubs: {
          RecipeCard: true,
        },
      },
    });

    const fetchRecipesSpy = vi.spyOn(store._actions['recipes/fetchRecipes'], '0');
    await wrapper.find('.load-more-button').trigger('click');

    expect(fetchRecipesSpy).toHaveBeenCalledWith('lastId');
  });
});
