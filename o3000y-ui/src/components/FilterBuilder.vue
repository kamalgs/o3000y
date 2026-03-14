<script setup lang="ts">
import FilterRow from './FilterRow.vue'
import { type Filter } from '@/lib/queryBuilder'

const filters = defineModel<Filter[]>({ required: true })

function addFilter() {
  filters.value = [...filters.value, { column: 'service_name', operator: '=', value: '' }]
}

function updateFilter(index: number, updated: Filter) {
  const copy = [...filters.value]
  copy[index] = updated
  filters.value = copy
}

function removeFilter(index: number) {
  filters.value = filters.value.filter((_, i) => i !== index)
}
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center justify-between">
      <span class="form-label mb-0">WHERE</span>
      <button class="btn btn-ghost text-xs" @click="addFilter">+ Add filter</button>
    </div>

    <div
      v-if="filters.length === 0"
      class="text-xs italic"
      style="color: var(--color-text-muted)"
    >
      No filters — all spans in time range
    </div>

    <div v-for="(filter, i) in filters" :key="i" class="flex items-center gap-2">
      <span
        v-if="i > 0"
        class="text-xs w-8 text-right shrink-0"
        style="color: var(--color-text-muted)"
      >AND</span>
      <span v-else class="w-8 shrink-0" />
      <FilterRow
        :filter="filter"
        @update="updateFilter(i, $event)"
        @remove="removeFilter(i)"
      />
    </div>
  </div>
</template>
