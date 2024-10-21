import { shallowMount, createLocalVue } from '@vue/test-utils'
import Vuex from 'vuex'
import ShoppingCart from '@/components/ShoppingCart.vue'
import IngredientItem from '@/components/IngredientItem.vue'

const localVue = createLocalVue()
localVue.use(Vuex)

describe('ShoppingCart.vue', () => {
  let store
  let state
  let getters
  let actions

  beforeEach(() => {
    state = {
      selectedRecipes: [
        { id: 1, name: 'Recipe 1', servings: 2, imagePath: 'path/to/image1' },
        { id: 2, name: 'Recipe 2', servings: 1, imagePath: 'path/to/image2' }
      ],
      ingredients: {
        1: { name: 'Ingredient 1', imagePath: 'path/to/ingredient1' },
        2: { name: 'Ingredient 2', imagePath: 'path/to/ingredient2' }
      }
    }
    getters = {
      combinedIngredients: () => [
        { id: 1, amount: 200, unit: 'g', name: 'Ingredient 1' },
        { id: 2, amount: 100, unit: 'ml', name: 'Ingredient 2' }
      ]
    }
    actions = {
      removeSelectedRecipe: jest.fn(),
      updateRecipeServings: jest.fn()
    }
    store = new Vuex.Store({
      modules: {
        recipes: {
          namespaced: true,
          state,
          getters,
          actions
        }
      }
    })
  })

  it('renders selected recipes', () => {
    const wrapper = shallowMount(ShoppingCart, { store, localVue })
    const recipeItems = wrapper.findAll('.recipe-item')
    expect(recipeItems).toHaveLength(2)
    expect(recipeItems.at(0).find('h3').text()).toBe('Recipe 1')
    expect(recipeItems.at(1).find('h3').text()).toBe('Recipe 2')
  })

  it('renders the shopping list', () => {
    const wrapper = shallowMount(ShoppingCart, { store, localVue })
    const ingredientItems = wrapper.findAllComponents(IngredientItem)
    expect(ingredientItems).toHaveLength(2)
  })

  it('calls removeSelectedRecipe when remove button is clicked', async () => {
    const wrapper = shallowMount(ShoppingCart, { store, localVue })
    await wrapper.find('.remove-button').trigger('click')
    expect(actions.removeSelectedRecipe).toHaveBeenCalledWith(expect.anything(), 1)
  })

  it('calls updateRecipeServings when servings are adjusted', async () => {
    const wrapper = shallowMount(ShoppingCart, { store, localVue })
    await wrapper.find('.servings-control button:last-child').trigger('click')
    expect(actions.updateRecipeServings).toHaveBeenCalledWith(expect.anything(), { recipeId: 1, servings: 3 })
  })

  it('displays empty cart message when no recipes are selected', () => {
    state.selectedRecipes = []
    const wrapper = shallowMount(ShoppingCart, { store, localVue })
    expect(wrapper.text()).toContain('Your shopping cart is empty')
  })
})
