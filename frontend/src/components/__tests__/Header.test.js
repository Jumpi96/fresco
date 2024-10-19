import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import Header from '@/components/Header.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', name: 'Home' }]
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

  it('renders the home button', () => {
    const wrapper = mount(Header, {
      global: {
        plugins: [router]
      }
    })
    const homeButton = wrapper.find('.home-button')
    expect(homeButton.exists()).toBe(true)
    expect(homeButton.attributes('aria-label')).toBe('Go to home page')
  })

  it('navigates to home page when home button is clicked', async () => {
    const wrapper = mount(Header, {
      global: {
        plugins: [router]
      }
    })
    const homeButton = wrapper.find('.home-button')
    await homeButton.trigger('click')
    expect(router.currentRoute.value.path).toBe('/')
  })
})