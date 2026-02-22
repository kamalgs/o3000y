<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getServices, getOperations, type SearchParams } from '../api/client'

defineProps<{ loading: boolean }>()
const emit = defineEmits<{ search: [params: SearchParams] }>()

const route = useRoute()
const services = ref<string[]>([])
const operations = ref<string[]>([])

const service = ref((route.query.service as string) || '')
const operation = ref((route.query.operation as string) || '')
const minDuration = ref('')
const maxDuration = ref('')
const status = ref('')
const limit = ref('100')

onMounted(async () => {
  try {
    services.value = await getServices()
  } catch {
    // ignore
  }
  if (service.value) {
    await loadOperations()
  }
})

async function loadOperations() {
  operations.value = []
  if (service.value) {
    try {
      operations.value = await getOperations(service.value)
    } catch {
      // ignore
    }
  }
}

function onSubmit() {
  const params: SearchParams = {}
  if (service.value) params.service = service.value
  if (operation.value) params.operation = operation.value
  if (minDuration.value) params.minDuration = Number(minDuration.value)
  if (maxDuration.value) params.maxDuration = Number(maxDuration.value)
  if (status.value) params.status = status.value
  if (limit.value) params.limit = Number(limit.value)
  emit('search', params)
}
</script>

<template>
  <form @submit.prevent="onSubmit" class="bg-white rounded-lg border border-gray-200 p-4">
    <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
      <div>
        <label class="block text-xs font-medium text-gray-600 mb-1">Service</label>
        <select
          v-model="service"
          @change="loadOperations"
          class="w-full border border-gray-300 rounded px-2 py-1.5 text-sm"
        >
          <option value="">All</option>
          <option v-for="s in services" :key="s" :value="s">{{ s }}</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-600 mb-1">Operation</label>
        <select v-model="operation" class="w-full border border-gray-300 rounded px-2 py-1.5 text-sm">
          <option value="">All</option>
          <option v-for="o in operations" :key="o" :value="o">{{ o }}</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-600 mb-1">Status</label>
        <select v-model="status" class="w-full border border-gray-300 rounded px-2 py-1.5 text-sm">
          <option value="">All</option>
          <option value="OK">OK</option>
          <option value="ERROR">ERROR</option>
        </select>
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-600 mb-1">Min Duration (us)</label>
        <input
          v-model="minDuration"
          type="number"
          class="w-full border border-gray-300 rounded px-2 py-1.5 text-sm"
          placeholder="0"
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-600 mb-1">Max Duration (us)</label>
        <input
          v-model="maxDuration"
          type="number"
          class="w-full border border-gray-300 rounded px-2 py-1.5 text-sm"
          placeholder="unlimited"
        />
      </div>
      <div>
        <label class="block text-xs font-medium text-gray-600 mb-1">Limit</label>
        <input
          v-model="limit"
          type="number"
          class="w-full border border-gray-300 rounded px-2 py-1.5 text-sm"
        />
      </div>
    </div>
    <div class="mt-4">
      <button
        type="submit"
        :disabled="loading"
        class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-sm"
      >
        {{ loading ? 'Searching...' : 'Search' }}
      </button>
    </div>
  </form>
</template>
