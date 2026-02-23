import { createRouter, createWebHashHistory } from 'vue-router'
import QueryView from './views/QueryView.vue'
import TraceView from './views/TraceView.vue'
import SearchView from './views/SearchView.vue'

const routes = [
  { path: '/', redirect: '/query' },
  { path: '/query', component: QueryView },
  { path: '/trace/:traceId', component: TraceView, props: true },
  { path: '/search', component: SearchView },
]

export default createRouter({
  history: createWebHashHistory(),
  routes,
})
