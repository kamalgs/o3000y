<script setup lang="ts">
import { ref, onMounted } from 'vue'
import TraceWaterfall from '../components/TraceWaterfall.vue'
import { getTrace, type TraceResponse } from '../api/client'

const props = defineProps<{ traceId: string }>()

const trace = ref<TraceResponse | null>(null)
const error = ref('')
const loading = ref(true)

onMounted(async () => {
  try {
    trace.value = await getTrace(props.traceId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <div class="mb-4">
      <h2 class="text-lg font-semibold">Trace: {{ traceId }}</h2>
    </div>

    <div v-if="loading" class="text-gray-500">Loading trace...</div>

    <div v-else-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
      {{ error }}
    </div>

    <div v-else-if="trace && trace.spans.length === 0" class="text-gray-500">
      No spans found for this trace.
    </div>

    <TraceWaterfall v-else-if="trace" :trace="trace" />
  </div>
</template>
