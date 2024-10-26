// Remove Vue import as it's not needed in Vue 3
import { createStore } from 'vuex';
import recipes from './modules/recipes';
import auth from './modules/auth';

export default createStore({
  modules: {
    recipes,
    auth,
  },
});
