<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getServices } from '@/api/client'

defineProps<{ activeService?: string }>()
defineEmits<{ selectService: [name: string] }>()

const services = ref<string[]>([])

onMounted(async () => {
  try {
    services.value = await getServices()
  } catch {
    services.value = []
  }
})
</script>

<template>
  <aside
    class="h-full overflow-y-auto"
    style="background: var(--color-surface); border-right: 1px solid var(--color-border-light)"
  >
    <div class="px-3 py-3">
      <span class="form-label">Services</span>
    </div>

    <div v-if="services.length === 0" class="px-3 pb-3 text-xs" style="color: var(--color-text-muted)">
      No services found
    </div>

    <nav class="px-2 pb-3 space-y-0.5">
      <button
        v-for="svc in services"
        :key="svc"
        class="sidebar-item w-full text-left"
        :class="{ 'sidebar-item--active': svc === activeService }"
        @click="$emit('selectService', svc)"
      >
        {{ svc }}
      </button>
    </nav>
  </aside>
</template>
