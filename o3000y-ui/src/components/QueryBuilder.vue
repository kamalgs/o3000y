<script setup lang="ts">
import { computed } from 'vue'
import FilterBuilder from './FilterBuilder.vue'
import {
  type ExploreQuery,
  AGGREGATIONS,
  MEASURES,
  TIME_RANGES,
  INTERVALS,
  GROUP_BY_COLUMNS,
} from '@/lib/queryBuilder'

const query = defineModel<ExploreQuery>({ required: true })
const emit = defineEmits<{ run: [] }>()
defineProps<{ loading: boolean }>()

const needsMeasure = computed(() =>
  AGGREGATIONS.find((a) => a.value === query.value.aggregation)?.needsMeasure ?? false,
)

function set(field: string, value: string) {
  query.value = { ...query.value, [field]: value }
}
</script>

<template>
  <div class="card space-y-3">
    <!-- Row 1: Viz controls -->
    <div class="flex flex-wrap items-end gap-3">
      <div>
        <label class="form-label">Visualize</label>
        <select
          :value="query.aggregation"
          class="form-select"
          @change="set('aggregation', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="a in AGGREGATIONS" :key="a.value" :value="a.value">{{ a.label }}</option>
        </select>
      </div>

      <div v-if="needsMeasure">
        <label class="form-label">Of</label>
        <select
          :value="query.measure"
          class="form-select"
          @change="set('measure', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="m in MEASURES" :key="m" :value="m">{{ m }}</option>
        </select>
      </div>

      <div>
        <label class="form-label">Group by</label>
        <select
          :value="query.groupBy"
          class="form-select"
          @change="set('groupBy', ($event.target as HTMLSelectElement).value)"
        >
          <option value="">(none)</option>
          <option v-for="g in GROUP_BY_COLUMNS" :key="g" :value="g">{{ g }}</option>
        </select>
      </div>

      <div>
        <label class="form-label">Time range</label>
        <select
          :value="query.timeRange"
          class="form-select"
          @change="set('timeRange', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="r in TIME_RANGES" :key="r.value" :value="r.value">{{ r.label }}</option>
        </select>
      </div>

      <div>
        <label class="form-label">Interval</label>
        <select
          :value="query.interval"
          class="form-select"
          @change="set('interval', ($event.target as HTMLSelectElement).value)"
        >
          <option v-for="iv in INTERVALS" :key="iv.value" :value="iv.value">{{ iv.label }}</option>
        </select>
      </div>
    </div>

    <!-- Row 2: Filters -->
    <FilterBuilder v-model="query.filters" />

    <!-- Run -->
    <div class="pt-2 border-t" style="border-color: var(--color-border-light)">
      <button class="btn btn-primary" :disabled="loading" @click="emit('run')">
        {{ loading ? 'Running\u2026' : 'Run Query' }}
      </button>
    </div>
  </div>
</template>
