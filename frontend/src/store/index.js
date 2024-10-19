// Remove Vue import as it's not needed in Vue 3
import Vuex from 'vuex';
import recipes from './modules/recipes';

//Vue.use(Vuex);

export default new Vuex.Store({
  modules: {
    recipes,
  },
});
