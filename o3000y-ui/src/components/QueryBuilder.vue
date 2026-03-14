<script setup lang="ts">
import { computed } from 'vue'
import FilterBuilder from '@/components/FilterBuilder.vue'
import {
  type ExploreQuery,
  AGGREGATIONS,
  MEASURES,
  TIME_RANGES,
  INTERVALS,
} from '@/lib/queryBuilder'

const query = defineModel<ExploreQuery>({ required: true })
const emit = defineEmits<{ run: [] }>()
defineProps<{ loading: boolean }>()

const needsMeasure = computed(() => {
  const agg = AGGREGATIONS.find((a) => a.value === query.value.aggregation)
  return agg?.needsMeasure ?? false
})

function onAggChange(value: string) {
  query.value = { ...query.value, aggregation: value as ExploreQuery['aggregation'] }
}
function onMeasureChange(value: string) {
  query.value = { ...query.value, measure: value }
}
function onRangeChange(value: string) {
  query.value = { ...query.value, timeRange: value }
}
function onIntervalChange(value: string) {
  query.value = { ...query.value, interval: value }
}
</script>

<template>
  <div class="bg-white rounded-lg border border-gray-200 p-4 space-y-4">
    <!-- Row 1: Aggregation -->
    <div class="flex flex-wrap items-end gap-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">VISUALIZE</label>
        <select
          :value="query.aggregation"
          class="block rounded border-gray-300 text-sm py-1.5 px-2 border focus:ring-indigo-500 focus:border-indigo-500"
          @change="onAggChange(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="agg in AGGREGATIONS" :key="agg.value" :value="agg.value">
            {{ agg.label }}
          </option>
        </select>
      </div>

      <div v-if="needsMeasure">
        <label class="block text-sm font-medium text-gray-700 mb-1">OF</label>
        <select
          :value="query.measure"
          class="block rounded border-gray-300 text-sm py-1.5 px-2 border focus:ring-indigo-500 focus:border-indigo-500"
          @change="onMeasureChange(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="m in MEASURES" :key="m" :value="m">{{ m }}</option>
        </select>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">TIME RANGE</label>
        <select
          :value="query.timeRange"
          class="block rounded border-gray-300 text-sm py-1.5 px-2 border focus:ring-indigo-500 focus:border-indigo-500"
          @change="onRangeChange(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="r in TIME_RANGES" :key="r.value" :value="r.value">
            {{ r.label }}
          </option>
        </select>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">INTERVAL</label>
        <select
          :value="query.interval"
          class="block rounded border-gray-300 text-sm py-1.5 px-2 border focus:ring-indigo-500 focus:border-indigo-500"
          @change="onIntervalChange(($event.target as HTMLSelectElement).value)"
        >
          <option v-for="iv in INTERVALS" :key="iv.value" :value="iv.value">
            {{ iv.label }}
          </option>
        </select>
      </div>
    </div>

    <!-- Row 2: Filters -->
    <FilterBuilder v-model="query.filters" />

    <!-- Run button -->
    <div class="flex items-center justify-between pt-2 border-t border-gray-100">
      <button
        class="px-4 py-1.5 bg-indigo-600 text-white text-sm font-medium rounded hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
        :disabled="loading"
        @click="emit('run')"
      >
        {{ loading ? 'Running...' : 'Run Query' }}
      </button>
    </div>
  </div>
</template>
