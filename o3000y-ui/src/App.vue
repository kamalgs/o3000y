<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import ServiceSidebar from './components/ServiceSidebar.vue'
import { serializeQuery, deserializeQuery, defaultQuery } from './lib/queryBuilder'

const router = useRouter()
const route = useRoute()

const activeService = computed(() => {
  try {
    const params = route.query as Record<string, string>
    if (!params.filters) return undefined
    const filters = JSON.parse(params.filters)
    const svcFilter = filters.find(
      (f: { column: string; operator: string }) =>
        f.column === 'service_name' && f.operator === '=',
    )
    return svcFilter?.value
  } catch {
    return undefined
  }
})

function onSelectService(name: string) {
  const base = route.path === '/explore'
    ? deserializeQuery(route.query as Record<string, string>)
    : defaultQuery()

  // Toggle: if already selected, remove the filter
  if (name === activeService.value) {
    base.filters = base.filters.filter(
      (f) => !(f.column === 'service_name' && f.operator === '='),
    )
  } else {
    base.filters = [
      ...base.filters.filter((f) => !(f.column === 'service_name' && f.operator === '=')),
      { column: 'service_name', operator: '=' as const, value: name },
    ]
  }
  router.push({ path: '/explore', query: serializeQuery(base) })
}
</script>

<template>
  <div class="flex h-screen">
    <ServiceSidebar
      class="w-56 shrink-0"
      :active-service="activeService"
      @select-service="onSelectService"
    />
    <div class="flex-1 flex flex-col overflow-hidden">
      <header
        class="flex items-center gap-4 px-4 py-2"
        style="background: var(--color-surface); border-bottom: 1px solid var(--color-border-light)"
      >
        <span class="text-lg font-bold" style="color: var(--color-text)">o3000y</span>
        <nav class="flex gap-1">
          <router-link to="/explore" class="nav-link" active-class="nav-link--active">
            Explore
          </router-link>
          <router-link to="/query" class="nav-link" active-class="nav-link--active">
            SQL
          </router-link>
        </nav>
      </header>
      <main class="flex-1 overflow-auto p-4" style="background: var(--color-bg)">
        <router-view />
      </main>
    </div>
  </div>
</template>
