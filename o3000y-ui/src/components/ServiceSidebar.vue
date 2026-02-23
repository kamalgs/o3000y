<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getServices, getOperations } from '../api/client'

const router = useRouter()
const services = ref<string[]>([])
const operations = ref<string[]>([])
const selectedService = ref<string | null>(null)
const error = ref('')

onMounted(async () => {
  try {
    services.value = await getServices()
  } catch {
    error.value = 'Failed to load services'
  }
})

watch(selectedService, async (svc) => {
  operations.value = []
  if (svc) {
    try {
      operations.value = await getOperations(svc)
    } catch {
      // ignore
    }
  }
})

function selectOperation(op: string) {
  router.push({
    path: '/search',
    query: { service: selectedService.value || undefined, operation: op },
  })
}
</script>

<template>
  <aside class="bg-white border-r border-gray-200 overflow-y-auto">
    <div class="p-4">
      <h2 class="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-3">Services</h2>
      <div v-if="error" class="text-xs text-red-500 mb-2">{{ error }}</div>
      <ul class="space-y-1">
        <li v-if="services.length === 0" class="text-xs text-gray-400">No services found</li>
        <li
          v-for="svc in services"
          :key="svc"
          @click="selectedService = selectedService === svc ? null : svc"
          class="px-2 py-1.5 rounded text-sm cursor-pointer hover:bg-gray-100 truncate"
          :class="{ 'bg-blue-50 text-blue-700': selectedService === svc }"
        >
          {{ svc }}
        </li>
      </ul>
    </div>
    <div v-if="selectedService && operations.length > 0" class="p-4 border-t border-gray-200">
      <h2 class="text-sm font-semibold text-gray-600 uppercase tracking-wide mb-3">Operations</h2>
      <ul class="space-y-1">
        <li
          v-for="op in operations"
          :key="op"
          @click="selectOperation(op)"
          class="px-2 py-1.5 rounded text-sm cursor-pointer hover:bg-gray-100 truncate"
        >
          {{ op }}
        </li>
      </ul>
    </div>
  </aside>
</template>
