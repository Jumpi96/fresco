import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import Header from '@/components/Header.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'Home' },
    { path: '/shopping-cart', name: 'ShoppingCart' }
  ]
})

describe('Header.vue', () => {
  it('renders the logo', () => {
    const wrapper = mount(Header, {
      global: {
        plugins: [router]
      }
    })
    const logo = wrapper.find('.logo')
    expect(logo.exists()).toBe(true)
    expect(logo.attributes('alt')).toBe('App Logo')
  })

  it('renders the title', () => {
    const wrapper = mount(Header, {
      global: {
        plugins: [router]
      }
    })
    const title = wrapper.find('.title')
    expect(title.exists()).toBe(true)
    expect(title.text()).toBe('fresco')
  })

  it('renders the shopping cart button', () => {
    const wrapper = mount(Header, {
      global: {
        plugins: [router]
      }
    })
    const cartButton = wrapper.find('.icon-button[aria-label="Go to shopping cart"]')
    expect(cartButton.exists()).toBe(true)
  })

  it('renders the home button', () => {
    const wrapper = mount(Header, {
      global: {
        plugins: [router]
      }
    })
    const homeButton = wrapper.find('.icon-button[aria-label="Go to home page"]')
    expect(homeButton.exists()).toBe(true)
  })
})
