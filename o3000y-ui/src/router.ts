import { createRouter, createWebHashHistory } from 'vue-router'
import QueryView from './views/QueryView.vue'
import TraceView from './views/TraceView.vue'
import SearchView from './views/SearchView.vue'
import ExploreView from './views/ExploreView.vue'

const routes = [
  { path: '/', redirect: '/explore' },
  { path: '/explore', component: ExploreView },
  { path: '/query', component: QueryView },
  { path: '/trace/:traceId', component: TraceView, props: true },
  { path: '/search', component: SearchView },
]

export default createRouter({
  history: createWebHashHistory(),
  routes,
})
