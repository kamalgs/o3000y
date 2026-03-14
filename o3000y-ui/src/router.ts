import { createRouter, createWebHashHistory } from 'vue-router'
import ExploreView from './views/ExploreView.vue'
import QueryView from './views/QueryView.vue'
import TraceView from './views/TraceView.vue'

const routes = [
  { path: '/', redirect: '/explore' },
  { path: '/explore', component: ExploreView },
  { path: '/query', component: QueryView },
  { path: '/trace/:traceId', component: TraceView, props: true },
  { path: '/search', redirect: '/explore' },
]

export default createRouter({
  history: createWebHashHistory(),
  routes,
})
